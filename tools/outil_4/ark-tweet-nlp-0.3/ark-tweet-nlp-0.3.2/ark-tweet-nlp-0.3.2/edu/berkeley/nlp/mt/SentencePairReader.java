package edu.berkeley.nlp.mt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import edu.berkeley.nlp.util.Factory;
import edu.berkeley.nlp.util.Filter;
import edu.berkeley.nlp.util.Filters;
import edu.berkeley.nlp.util.Iterators;
import edu.berkeley.nlp.util.Lists;
import edu.berkeley.nlp.util.Logger;
import edu.berkeley.nlp.util.SuffixFilter;
import edu.berkeley.nlp.util.Iterators.IteratorIterator;
import fig.basic.IOUtils;
import fig.basic.NumUtils;
import fig.basic.OrderedStringMap;
import fig.basic.Pair;
import fig.basic.StrUtils;
import fig.exec.Execution;

/**
 * TODO Add tag reading
 *
 * @author John DeNero & Percy Liang
 */
public class SentencePairReader {
    /**
     * A PairDepot iterates through sentence pairs that have already been loaded
     * into memory if they are available, or from disk if they are not.
     */
    public class PairDepot implements Iterable<SentencePair> {

        private List<String> sources;
        private int offset;
        private int maxPairs;
        private Filter<SentencePair> filter;
        private List<SentencePair> pairs;
        private int size = -1;
        private boolean ignoreAnnotations;

        public PairDepot(List<String> sources, int offset, int maxPairs,
                Filter<SentencePair> filter, boolean batch) {
            this(sources, offset, maxPairs, filter, batch, false);
        }

		public SentencePairReader getSentencePairReader()
		{
			return SentencePairReader.this;
		}

        public PairDepot(List<String> sources, int offset, int maxPairs,
                Filter<SentencePair> filter, boolean batch, boolean ignoreAnnotations) {
            this.sources = sources;
            this.offset = offset;
            this.maxPairs = maxPairs;
            this.filter = filter;
            this.ignoreAnnotations = ignoreAnnotations;
            if (!batch) loadSentenceCache();
        }

        public int size() {
            if (size >= 0) return size;
            if (pairs != null) {
                size = pairs.size();
            } else {
                int count = 0;
                Iterator<SentencePair> it = this.iterator();
                while (it.hasNext()) {
                    it.next();
                    count++;
                }
                size = count;
            }
            return size;
        }

        public Iterator<SentencePair> iterator() {
            // If pairs are loaded already, return them
            if (pairs != null) return pairs.iterator();

            // Factory to generate iterators from source strings
            Factory<Iterator<SentencePair>> factory = new Factory<Iterator<SentencePair>>() {
                public Iterator<SentencePair> newInstance(Object... args) {
                    String src = (String) args[0];
                    return getSentencePairIteratorFromSource(src, filter, ignoreAnnotations);
                }
            };

            // Create the iterator (which iterates over sources internally)
            Iterator<SentencePair> iter;
            iter = new IteratorIterator<SentencePair>(sources.iterator(), factory);

            // Burn off the offset
            for (int i = 0; i < offset; i++) {
                if (iter.hasNext()) {
                    iter.next();
                } else {
                    Logger.err("Pairs available (%d) less than offset (%d)", i, offset);
                    break;
                }
            }

            return Iterators.maxLengthIterator(iter, maxPairs);
        }

        /**
         * @return
         */
        public List<SentencePair> asList() {
            if (pairs != null) return pairs;
            ArrayList<SentencePair> allPairs = new ArrayList<SentencePair>(Math
                    .max(0, size));
            for (SentencePair pair : this) {
                allPairs.add(pair);
            }
            return allPairs;
        }

        /**
         *
         */
        public void flushCache() {
            pairs = null;
        }

        /**
         *
         */
        public List<SentencePair> loadSentenceCache() {
            pairs = asList();
            return pairs;
        }

        public List<Pair<String, Iterator<SentencePair>>> getSourceIterators() {
            List<Pair<String, Iterator<SentencePair>>> iters;
            iters = new ArrayList<Pair<String, Iterator<SentencePair>>>();
            for (String source : sources) {
                List<String> files = getBaseFileNamesFromSource(source);
                for (String f : files) {
                    Iterator<SentencePair> it = getSentencePairsIteratorFromFile(f, filter);
                    iters.add(Pair.newPair(f, it));
                }
            }
            return iters;
        }
    }

    public class PairIterator implements Iterator<SentencePair> {
        private String baseFileName;
        private Filter<SentencePair> filter;

        // Input files
        private String englishFN;
        private String foreignFN;
        private String englishTreeFN;
        private String foreignTreeFN;

        private String englishTagFN;
        private String foreignTagFN;

        private String alignmentFN;
        // Output files
        private PrintWriter englishOutput;
        private PrintWriter foreignOutput;
        private PrintWriter englishTagOutput;
        private PrintWriter foreignTagOutput;
        private PrintWriter englishTreeOutput;
        private PrintWriter foreignTreeOutput;
        private PrintWriter alignmentOutput;
        // Reject files
        private PrintWriter englishRejects;
        private PrintWriter foreignRejects;
        private PrintWriter englishTagRejects;
        private PrintWriter foreignTagRejects;
        private PrintWriter englishTreeRejects;
        private PrintWriter foreignTreeRejects;
        private PrintWriter alignmentRejects;

        // Input files
        private BufferedReader englishIn;
        private BufferedReader frenchIn;
        private BufferedReader alIn;
        private SentencePair next;
        private BufferedReader englishTrees;
        private BufferedReader foreignTrees;
        private boolean isEmpty;

        private Map<String, Iterator<String>> alignFiles;

        private void saveAcceptedInput(SentencePair pair) {
            if (saveInput) {
                englishOutput.println(StrUtils.join(pair.getEnglishWords(), " "));
                foreignOutput.println(StrUtils.join(pair.getForeignWords(), " "));
                if (pair.getEnglishTags() != null) {
                    englishTagOutput.println(StrUtils.join(pair.getEnglishTags(), " "));
                }
                if (pair.getForeignTags() != null) {
                    foreignTagOutput.println(StrUtils.join(pair.getForeignTags(), " "));
                }
                if (pair.getEnglishTree() != null) {
                    englishTreeOutput.println(pair.getEnglishTree());
                }
                if (pair.getForeignTree() != null) {
                    foreignTreeOutput.println(pair.getForeignTree());
                }
                if (pair.getAlignment() != null) {
                    alignmentOutput.println(pair.getAlignment().output());
                }
            }
        }

        private void saveRejectedInput(SentencePair pair) {
            if (saveRejects) {
                englishRejects.println(StrUtils.join(pair.getEnglishWords(), " "));
                foreignRejects.println(StrUtils.join(pair.getForeignWords(), " "));
                if (pair.getEnglishTags() != null) {
                    englishTagRejects.println(StrUtils.join(pair.getEnglishTags(), " "));
                }
                if (pair.getForeignTags() != null) {
                    foreignTagRejects.println(StrUtils.join(pair.getForeignTags(), " "));
                }
                if (pair.getEnglishTree() != null) {
                    englishTreeRejects.println(pair.getEnglishTree());
                }
                if (pair.getForeignTree() != null) {
                    foreignTreeRejects.println(pair.getForeignTree());
                }
                if (pair.getAlignment() != null) {
                    alignmentRejects.println(pair.getAlignment().output());
                }
            }
        }

        private void setInputFileNames(String baseFileName) {
            englishFN = baseFileName + "." + englishExtension;
            foreignFN = baseFileName + "." + foreignExtension;
            englishTagFN = baseFileName + "." + englishTagExtension;
            foreignTagFN = baseFileName + "." + foreignTagExtension;
            englishTreeFN = baseFileName + "." + englishTreeExtension;
            foreignTreeFN = baseFileName + "." + foreignTreeExtension;
            alignmentFN = baseFileName + "." + wordAlignmentExtension;
            setInputAlignFile();
        }

        private final Pattern alignFilePattrn = Pattern
                .compile("^.*?\\.(.*)\\.posteriors$");
        private boolean ignoreAnnotations;

        private void setInputAlignFile() {
            File f = new File(englishFN);
            File dir = f.getParentFile();
            File[] files = dir.listFiles();
            alignFiles = new HashMap<String, Iterator<String>>();
            for (File file : files) {
                String name = file.getAbsolutePath();
                if (name.contains(baseFileName) && name.endsWith("posteriors")) {
                    Matcher matcher = alignFilePattrn.matcher(file.getName());
                    if (matcher.matches()) {
                        String alignID = matcher.group(1);
                        if (alignID.length() > 0)
                            alignFiles.put(alignID, IOUtils.readLinesHard(name).iterator());
                    }
                }
            }
        }

        private void openOutputAndRejectFiles(String baseFileName) {
            if (saveInput) {
                String fileName = saveInputDir + "/" + (new File(baseFileName)).getName();
                englishOutput = IOUtils.openOutHard(fileName + "." + englishExtension);
                foreignOutput = IOUtils.openOutHard(fileName + "." + foreignExtension);
                englishTagOutput = IOUtils
                        .openOutHard(fileName + "." + englishTagExtension);
                foreignTagOutput = IOUtils
                        .openOutHard(fileName + "." + foreignTagExtension);
                englishTreeOutput = IOUtils.openOutHard(fileName + "."
                        + englishTreeExtension);
                foreignTreeOutput = IOUtils.openOutHard(fileName + "."
                        + foreignTreeExtension);
                alignmentOutput = IOUtils.openOutHard(fileName + "."
                        + wordAlignmentExtension);
            }
            if (saveRejects) {
                String fileName = saveRejectsDir + "/" + (new File(baseFileName)).getName();
                englishRejects = IOUtils.openOutHard(fileName + "." + englishExtension);
                foreignRejects = IOUtils.openOutHard(fileName + "." + foreignExtension);
                englishTagRejects = IOUtils.openOutHard(fileName + "."
                        + englishTagExtension);
                foreignTagRejects = IOUtils.openOutHard(fileName + "."
                        + foreignTagExtension);
                englishTreeRejects = IOUtils.openOutHard(fileName + "."
                        + englishTreeExtension);
                foreignTreeRejects = IOUtils.openOutHard(fileName + "."
                        + foreignTreeExtension);
                alignmentRejects = IOUtils.openOutHard(fileName + "."
                        + wordAlignmentExtension);
            }
        }

        private void closeOutputAndRejectFiles() {
            if (saveInput) {
                englishOutput.close();
                foreignOutput.close();
                englishTagOutput.close();
                foreignTagOutput.close();
                englishTreeOutput.close();
                foreignTreeOutput.close();
                alignmentOutput.close();
            }
            if (saveRejects) {
                englishRejects.close();
                foreignRejects.close();
                englishTagRejects.close();
                foreignTagRejects.close();
                englishTreeRejects.close();
                foreignTreeRejects.close();
                alignmentRejects.close();
            }

        }

        private void addAlign(SentencePair sp, String baseFileName) {
            sp.keyedAligns = new HashMap<String, Alignment>();
            for (Map.Entry<String, Iterator<String>> entry : alignFiles.entrySet()) {
                String alignKey = entry.getKey();
                String alignLine = entry.getValue().next();
                Alignment align = new Alignment(sp.getEnglishWords(), sp.getForeignWords());
                align.parseAlignments(alignLine);
                sp.keyedAligns.put(alignKey, align);
            }
        }

        private SentencePair readNextSentencePair(String englishLine, String frenchLine,
                String baseFileName) {
            Pair<Integer, List<String>> englishIDAndSentence = readSentence(englishLine);
            Pair<Integer, List<String>> frenchIDAndSentence = readSentence(frenchLine);

            int enID = englishIDAndSentence.getFirst();
            int frID = frenchIDAndSentence.getFirst();
            if (enID != frID)
                throw new RuntimeException("Sentence ID confusion in file " + baseFileName
                        + ", lines were:\n\t" + englishLine + "\n\t" + frenchLine);
            if (enID == -1) enID = frID = currSentenceID;
            SentencePair sp = new SentencePair(enID, baseFileName, englishIDAndSentence
                    .getSecond(), frenchIDAndSentence.getSecond());
            addAlign(sp, baseFileName);
            return sp;
        }

        private Pair<Integer, List<String>> readSentence(String line) {
            int id = -1;
            List<String> words = new ArrayList<String>();
            String[] tokens = line.trim().split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.equals("<s")) continue;
                if (token.equals("</s>")) continue;
                if (token.startsWith("snum=")) {
                    String idString = token.substring(5, token.length() - 1);
                    id = Integer.parseInt(idString);
                    continue;
                }
                if (lowercaseWords) token = token.toLowerCase();
                words.add(token.intern());
            }

            return new Pair<Integer, List<String>>(id, words);
        }

        private void addAlignmentToPair(SentencePair pair) {
            if (alIn == null) return;
            try {
                if (!alIn.ready()) {
                    Logger.warn("Ran out of alignments (%s)", baseFileName);
                    alIn.close();
                    alIn = null;
                } else {
                    Alignment alignment = new Alignment(pair, treesInAlignment);
                    alignment.parseAlignments(alIn.readLine(), reverseAlignments,
                            oneIndexed);
                    pair.setAlignment(alignment);
                }
            } catch (IOException e) {
                Logger.err("Problem reading alignment file: " + e.getLocalizedMessage());
            }
        }

        private void addTreeToPair(SentencePair pair, boolean foreign) {
            BufferedReader reader = foreign ? foreignTrees : englishTrees;
            if (reader == null) return; // No file available
            try {
                if (!reader.ready()) {
                    Logger.warn("Ran out of parses (%s)", baseFileName);
                    if (foreign) {
                        foreignTrees.close();
                        foreignTrees = null;
                    } else {
                        englishTrees.close();
                        englishTrees = null;
                    }
                } else {
                    String treeString = reader.readLine();
                    Tree<String> tree = Trees.PennTreeReader.parseEasy(treeString,
                            lowercaseWords);
                    if (foreign) pair.setForeignTree(tree);
                    if (!foreign) pair.setEnglishTree(tree);
                }
            } catch (IOException e) {
                Logger.err("Problem reading parse file: " + e.getLocalizedMessage());
            }
        }

        public PairIterator(String baseFileName, Filter<SentencePair> filter,
                boolean ignoreAnnotations) {
            this.baseFileName = baseFileName;
            this.filter = filter;
            this.ignoreAnnotations = ignoreAnnotations;
            setInputFileNames(baseFileName);
            openOutputAndRejectFiles(baseFileName);

            // Open all relevant files, setting a dead state if they don't exist
            englishIn = IOUtils.openInEasy(englishFN);
            frenchIn = IOUtils.openInEasy(foreignFN);
            if (englishIn == null || frenchIn == null) {
                Logger.warn("File base %s does not have %s and %s extensions",
                        baseFileName, englishExtension, foreignExtension);
                this.isEmpty = true;
            } else {
                if (!ignoreAnnotations) {
                    alIn = IOUtils.openInEasy(alignmentFN);
                    englishTrees = IOUtils.openInEasy(englishTreeFN);
                    foreignTrees = IOUtils.openInEasy(foreignTreeFN);
                }
                loadNext();
            }
        }

        public boolean hasNext() {
            if (isEmpty) return false;
            return next != null;
        }

        public SentencePair next() {
            SentencePair output = next;
            loadNext();
            return output;
        }

        private void loadNext() {
            try {
                next = null;
                if (englishIn.ready() != frenchIn.ready()) {
                    Logger.warn("%s and %s files are different lengths (%s)",
                            englishExtension, foreignExtension, baseFileName);
                }
                while (next == null && englishIn.ready() && frenchIn.ready()) {
                    // Check bounds on desired sentences
                    String englishLine = englishIn.readLine();
                    String frenchLine = frenchIn.readLine();
                    currSentenceID--;

                    // Construct sentence pair
                    SentencePair pair = readNextSentencePair(englishLine, frenchLine,
                            baseFileName);
                    addAlignmentToPair(pair);
                    addTreeToPair(pair, false);
                    addTreeToPair(pair, true);

                    // TODO Support tag input

                    if (filter.accept(pair)) {
                        next = pair;
                        saveAcceptedInput(pair);
                    } else {
                        saveRejectedInput(pair);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void skipNext() {
            try {
                englishIn.readLine();
                frenchIn.readLine();
                alIn.readLine();
                englishTrees.readLine();
                foreignTrees.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            currSentenceID--;
        }

        public void finish() {
            closeOutputAndRejectFiles();
        }

    }

    private String englishExtension = "e";

	public String getEnglishExtension()
	{
		return englishExtension;
	}

	public String getEnglishTreeExtension()
	{
		return englishTreeExtension;
	}

	public String getForeignExtension()
	{
		return foreignExtension;
	}

	public String getForeignTreeExtension()
	{
		return foreignTreeExtension;
	}

	private String englishTreeExtension = "etrees";
    private String englishTagExtension = "etags";
    private String foreignExtension = "f";
    private String foreignTreeExtension = "ftrees";
    private String foreignTagExtension = "ftags";
    private String wordAlignmentExtension = "align";

    private boolean reverseAlignments = false;
    private boolean oneIndexed = false;

    private boolean treesInAlignment = false;

    private static int currSentenceID = 0;

    private boolean lowercaseWords = false;
    private String saveInputDir = null;
    private String saveRejectsDir = null;
    private boolean saveInput = false;
    private boolean saveRejects = false;

    /**
     * Creates a sentence pair reader that saves accepted inputs and rejects.
     *
     * @param lowercase
     *            Whether to lowercase input.
     * @param saveInputDir
     *            Where to save input files.
     * @param saveRejectsDir
     *            Where to save rejected files.
     */
    public SentencePairReader(boolean lowercase, String saveInputDir, String saveRejectsDir) {
        this.lowercaseWords = lowercase;
        this.saveInputDir = saveInputDir;
        this.saveRejectsDir = saveRejectsDir;
    }

    /**
     * Creates a sentence pair reader that does not save inputs.
     *
     * @param lowercase
     */
    public SentencePairReader(boolean lowercase) {
        this(lowercase, null, null);
    }

    public static void assertSentenceIDsAreUnique(List<SentencePair> sentencePairs) {
        Map<Integer, SentencePair> map = new HashMap<Integer, SentencePair>();
        for (SentencePair sp : sentencePairs) {
            int sid = sp.getSentenceID();
            if (map.containsKey(sid)) {
                throw new RuntimeException("Two sentences have same sentence ID: " + sid);
            }
            map.put(sid, sp);
        }
    }

    /**
     * Reads translation data (sentence pairs) from a set of parallel files. The
     * files can include several sources of information, including tags, trees
     * and word alignments.
     *
     * @param path
     *            Input directory path
     * @param maxSentencePairs
     *            Number of sentences to return
     * @param sentencePairs
     *            Database of sentence pairs
     */
    @SuppressWarnings("unchecked")
    public void readSentencePairsFromSource(String path, int maxSentencePairs,
            List<SentencePair> sentencePairs) {
        readSentencePairsFromSource(path, 0, maxSentencePairs, sentencePairs, Filters
                .acceptFilter());
    }

    public Iterator<SentencePair> getSentencePairIteratorFromSource(String path,
            final Filter<SentencePair> f, final boolean ignoreAnnotations) {
        // Load file names
        final List<String> filenames = getBaseFileNamesFromSource(path);
        if (filenames.size() == 0) Logger.err("No files found at source " + path);
        Collections.sort(filenames);
        initSaveDirectories();

        // Create iterator factory from file names
        Factory<Iterator<SentencePair>> factory = new Factory<Iterator<SentencePair>>() {
            public Iterator<SentencePair> newInstance(Object... args) {
                String filename = (String) args[0];
                return new PairIterator(filename, f, ignoreAnnotations);
            }
        };

        return new IteratorIterator<SentencePair>(filenames.iterator(), factory);
    }

    public PairDepot pairDepotFromSources(List<String> sources, int offset,
            int numSentences, Filter<SentencePair> filter, boolean batch) {
        return new PairDepot(sources, offset, numSentences, filter, batch);
    }

    public PairDepot pairDepotFromSources(List<String> sources, int offset,
            int numSentences, Filter<SentencePair> filter, boolean batch,
            boolean ignoreAnnotations) {
        return new PairDepot(sources, offset, numSentences, filter, batch,
                ignoreAnnotations);
    }

    public PairDepot pairDepotFromSource(String source, int offset, int numSentences,
            Filter<SentencePair> filter, boolean batch) {
        ArrayList<String> sources = new ArrayList<String>(1);
        sources.add(source);
        return new PairDepot(sources, offset, numSentences, filter, batch);
    }

    /**
     * Reads translation data (sentence pairs) from a set of parallel files. The
     * files can include several sources of information, including tags, trees
     * and word alignments.
     *
     * @param path
     *            Input directory path
     * @param offset
     *            Number of sentences to skip at outset
     * @param maxSentencePairs
     *            Number of sentences to return
     * @param sentencePairs
     *            Database of sentence pairs
     * @param filter
     *            Filter for sentence pairs
     */
    public void readSentencePairsFromSource(String path, int offset, int maxSentencePairs,
            List<SentencePair> sentencePairs, Filter<SentencePair> filter) {
        Logger.startTrack("readSentencePairs(" + path + ")");
        int startCount = sentencePairs.size();
        List<String> filenames = getBaseFileNamesFromSource(path);
        Collections.sort(filenames);
        readSentencePairsUsingList(filenames, offset, maxSentencePairs, sentencePairs,
                filter);
        Logger.logss("Finished reading %d sentences", sentencePairs.size() - startCount);
        Logger.endTrack();
    }

    // If path is a directory, return the list of files in the directory
    // If path is a file, return the files whose names are in the path file
    private List<String> getBaseFileNamesFromSource(String path) {
        if (new File(path).isDirectory()) return getBaseFileNamesFromDir(path);
        else if (new File(path).exists()) {
            // A file containing file names
            try {
                return OrderedStringMap.fromFile(path).keys();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // The root of a file
            if (new File(path + "." + englishExtension).exists()) {
                return Lists.newList(path);
            } else {
                throw new RuntimeException("File not found: " + path);
            }
        }
    }

    private List<String> getBaseFileNamesFromDir(String path) {
        SuffixFilter filter = new SuffixFilter(englishExtension);
        List<File> englishFiles = IOUtils.getFilesUnder(path, filter);
        List<String> baseFileNames = new ArrayList<String>();
        for (File englishFile : englishFiles) {
            String baseFileName = chop(englishFile.getAbsolutePath(), "."
                    + englishExtension);
            baseFileNames.add(baseFileName);
        }
        return baseFileNames;
    }

    private void readSentencePairsUsingList(List<String> baseFileNames, int offset,
            int maxSentencePairs, List<SentencePair> sentencePairs,
            Filter<SentencePair> filter) {
        initSaveDirectories();

        int numFiles = 0;
        for (String baseFileName : baseFileNames) {
            if (sentencePairs.size() >= maxSentencePairs) return;
            numFiles++;
            Logger.logs("Reading " + numFiles + "/" + baseFileNames.size() + ": "
                    + baseFileName);
            List<SentencePair> subSentencePairs = readSentencePairsFromFile(baseFileName,
                    maxSentencePairs - sentencePairs.size(), filter);
            int lowerBound = NumUtils.bound(offset, 0, subSentencePairs.size());
            sentencePairs.addAll(subSentencePairs.subList(lowerBound, subSentencePairs
                    .size()));
            offset -= Math.max(0, subSentencePairs.size());
        }
    }

    private void initSaveDirectories() {
        // Choose whether to save inputs
        if (saveInputDir != null) {
            File inputDir = new File(saveInputDir);
            if (!inputDir.exists()) {
                inputDir.mkdir();
            }
            if (inputDir.exists() && inputDir.isDirectory()) saveInput = true;
        }

        // Choose whether to save rejects
        if (saveRejectsDir != null) {
            File rejectsDir = new File(saveRejectsDir);
            if (!rejectsDir.exists()) {
                rejectsDir.mkdir();
            }
            if (rejectsDir.exists() && rejectsDir.isDirectory()) saveRejects = true;
        }
    }

    // For sentence pairs before offset, just stick null instead of the actual sentence
    public List<SentencePair> readSentencePairsFromFile(String baseFileName,
            int maxSentencePairs, Filter<SentencePair> filter) {
        List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
        PairIterator pairIterator = getSentencePairsIteratorFromFile(baseFileName, filter);

        // Process offset
        //		for (int i = 0; i < offset; i++) {
        //			sentencePairs.add(null);
        //			if (pairIterator.hasNext())
        //				pairIterator.skipNext();
        //			else
        //				return sentencePairs;
        //		}

        int numPairs = 0;
        while (pairIterator.hasNext() && numPairs < maxSentencePairs) {
            sentencePairs.add(pairIterator.next());
            numPairs++;
        }
        pairIterator.finish();
        return sentencePairs;
    }

    public PairIterator getSentencePairsIteratorFromFile(String baseFileName,
            Filter<SentencePair> filter) {
        return new PairIterator(baseFileName, filter, false);
    }

    private static String chop(String name, String extension) {
        if (!name.endsWith(extension)) return name;
        return name.substring(0, name.length() - extension.length());
    }

    public void setEnglishExtension(String ext) {
        this.englishExtension = ext;
        this.englishTagExtension = ext + "tags";
        this.englishTreeExtension = ext + "trees";
    }

    public void setForeignExtension(String ext) {
        this.foreignExtension = ext;
        this.foreignTagExtension = ext + "tags";
        this.foreignTreeExtension = ext + "trees";
    }

    public void setReverseAndOneIndex(boolean reverseAlignments, boolean oneIndexed) {
        this.reverseAlignments = reverseAlignments;
        this.oneIndexed = oneIndexed;
    }

    public static void main(String[] args) {
        Execution.init(args);
        String testDir = "/Users/denero/Documents/workspace/data/wordalignment/en-fr/utf8/train";
        SentencePairReader reader = new SentencePairReader(true);
        List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
        reader.readSentencePairsFromSource(testDir, 10000, sentencePairs);
        for (int i = 0; i < 10000; i += 1000) {
            SentencePair p = sentencePairs.get(i);
            System.out.println(p.dump());
        }
    }

    public void setTreesInAlignment(boolean treesInAlignment) {
        this.treesInAlignment = treesInAlignment;
    }

	public String getAlignmentExtension()
	{
		return wordAlignmentExtension;
	}
}
