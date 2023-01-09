package edu.berkeley.nlp.mt;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.berkeley.nlp.mt.SentencePairReader.PairDepot;
import edu.berkeley.nlp.mt.WordMap.String2DoubleWordMap;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Filters;
import edu.berkeley.nlp.util.Lists;
import fig.basic.IOUtils;
import fig.basic.MapUtils;
import fig.basic.NumUtils;
import fig.basic.Pair;
import fig.basic.StrUtils;
import fig.basic.String2DoubleMap;
import fig.basic.StringDoubleMap;

/**
 * Alignments serve two purposes, both to indicate your system's guessed
 * alignment, and to hold the gold standard alignments. Alignments map index
 * pairs to one of three values, unaligned, possibly aligned, and surely
 * aligned. Your alignment guesses should only contain sure and unaligned pairs,
 * but the gold alignments contain possible pairs as well.
 *
 * To build an alignment, start with an empty one and use
 * addAlignment(i,j,true). To display one, use the render method.
 */
public class Alignment implements Serializable {

    public static class AlignmentNode implements Serializable {
        private static final long serialVersionUID = 1L;

        boolean isWord;
        String word;
        Tree<String> internalNode;

        AlignmentNode(String word) {
            isWord = true;
            this.word = word;
            internalNode = null;
        }

        AlignmentNode(Tree<String> internalNode) {
            isWord = false;
            this.word = internalNode.getLabel();
            this.internalNode = internalNode;
        }

        public boolean isInternalNode() {
            return !isWord;
        }

        public Tree<String> getTreeNode() {
            return internalNode;
        }
    }

    private static final long serialVersionUID = 1L;

    Set<Pair<Integer, Integer>> sureAlignments;
    boolean[][] sureAlignmentsDense;
    boolean sureAlignmentsAreDense;
    Set<Pair<Integer, Integer>> possibleAlignments;
    private List<List<Integer>> foreignAlignments, englishAlignments;
    Map<Pair<Integer, Integer>, Double> strengths; // Strength of alignments
    private List<String> englishSentence, foreignSentence;
    private Tree<String> englishTree, foreignTree;
    private List<AlignmentNode> englishNodes, foreignNodes;
    private IdentityHashMap<Tree<String>, Integer> englishPositions, foreignPositions;
    private IdentityHashMap<Tree<String>, Pair<Integer, Integer>> englishBounds,
            foreignBounds;
    boolean isNodeAlignment;
    public double logLikelihood; // Container

    /////////////////////////////
    // Constructors
    /////////////////////////////

    public Alignment(List<String> englishSentence, List<String> foreignSentence,
            Tree<String> englishTree, Tree<String> foreignTree, boolean isNodeAlignment) {
        this.englishSentence = englishSentence;
        this.foreignSentence = foreignSentence;
        sureAlignments = new HashSet<Pair<Integer, Integer>>();
        possibleAlignments = new HashSet<Pair<Integer, Integer>>();
        strengths = new HashMap<Pair<Integer, Integer>, Double>();
        this.isNodeAlignment = isNodeAlignment;

        setTrees(englishTree, foreignTree);
        englishPositions = constructPositionMap(englishTree);
        foreignPositions = constructPositionMap(foreignTree);
        englishBounds = constructBoundsMap(englishTree, englishPositions);
        foreignBounds = constructBoundsMap(foreignTree, foreignPositions);
    }

    private IdentityHashMap<Tree<String>, Integer> constructPositionMap(Tree<String> tree) {
        if (tree == null) {
            return null;
        }
        IdentityHashMap<Tree<String>, Integer> map = new IdentityHashMap<Tree<String>, Integer>();
        int pos = 0;
        for (Tree<String> preTerminal : tree.getPreTerminals()) {
            map.put(preTerminal, pos++);
        }
        for (Tree<String> node : tree.getPreOrderTraversal()) {
            if (!(node.isPreTerminal() || node.isLeaf())) {
                map.put(node, pos++);
            }
        }
        return map;
    }

    private IdentityHashMap<Tree<String>, Pair<Integer, Integer>> constructBoundsMap(
            Tree<String> tree, IdentityHashMap<Tree<String>, Integer> positionMap) {
        if (tree == null) {
            return null;
        }
        IdentityHashMap<Tree<String>, Pair<Integer, Integer>> map = new IdentityHashMap<Tree<String>, Pair<Integer, Integer>>();
        for (Tree<String> node : tree.getPreOrderTraversal()) {
            if (!(node.isLeaf())) {
                List<Tree<String>> preterminals = node.getPreTerminals();
                int min = positionMap.get(preterminals.get(0));
                int max = positionMap.get(preterminals.get(preterminals.size() - 1));
                map.put(node, Pair.makePair(min, max));
            }
        }
        return map;
    }

    public void setTrees(Tree<String> englishTree, Tree<String> foreignTree) {
        this.englishTree = englishTree;
        this.foreignTree = foreignTree;
        englishNodes = constructAlignmentNodes(this.englishSentence, englishTree);
        foreignNodes = constructAlignmentNodes(this.foreignSentence, foreignTree);
    }

    public Alignment(List<String> englishSentence, List<String> foreignSentence) {
        this(englishSentence, foreignSentence, null, null, false);
    }

    public Alignment(SentencePair pair, boolean isNodeAlignment) {
        this(pair.getEnglishWords(), pair.getForeignWords(), pair.getEnglishTree(), pair
                .getForeignTree(), isNodeAlignment);
    }

    public Alignment(Alignment al) {
        this(al.englishSentence, al.foreignSentence, al.englishTree, al.foreignTree,
                al.isNodeAlignment);
    }

    private List<AlignmentNode> constructAlignmentNodes(List<String> sentence,
            Tree<String> tree) {
        List<AlignmentNode> nodes = new ArrayList<AlignmentNode>(sentence.size());
        for (String word : sentence) {
            nodes.add(new AlignmentNode(word));
        }
        int preTerminalIndex = 0;
        if (tree != null && isNodeAlignment) {
            for (Tree<String> internalNode : tree.getPreOrderTraversal()) {
                if (!(internalNode.isPreTerminal() || internalNode.isLeaf())) {
                    nodes.add(new AlignmentNode(internalNode));
                }
                if (internalNode.isPreTerminal()) {
                    nodes.get(preTerminalIndex++).internalNode = internalNode;
                }
            }
        }
        return nodes;
    }

    public static List<Tree<String>> getOrderedNodeList(Tree<String> tree) {
        List<Tree<String>> nodes = new ArrayList<Tree<String>>();
        for (Tree<String> preTerminal : tree.getPreTerminals()) {
            nodes.add(preTerminal);
        }
        for (Tree<String> node : tree.getPreOrderTraversal()) {
            if (!(node.isPreTerminal() || node.isLeaf())) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /////////////////////////////
    // Getter Methods
    /////////////////////////////

    public int getEnglishLength() {
        return englishNodes.size();
    }

    public List<String> getEnglishSentence() {
        return englishSentence;
    }

    public Tree<String> getEnglishTree() {
        return englishTree;
    }

    public List<AlignmentNode> getEnglishNodes() {
        return englishNodes;
    }

    public int getEnglishPosition(Tree<String> englishTreeNode) {
        return englishPositions.get(englishTreeNode);
    }

    public Pair<Integer, Integer> getEnglishBounds(Tree<String> englishTreeNode) {
        return englishBounds.get(englishTreeNode);
    }

    public int getEnglishSpanLength(Tree<String> englishTreeNode) {
        Pair<Integer, Integer> bounds = englishBounds.get(englishTreeNode);
        return bounds.getSecond() - bounds.getFirst() + 1;
    }

    public int getEnglishSpanLength(int index) {
        return getEnglishSpanLength(englishNodes.get(index).getTreeNode());
    }

    public int getForeignLength() {
        return foreignNodes.size();
    }

    public List<String> getForeignSentence() {
        return foreignSentence;
    }

    public Tree<String> getForeignTree() {
        return foreignTree;
    }

    public List<AlignmentNode> getForeignNodes() {
        return foreignNodes;
    }

    public int getForeignPosition(Tree<String> foreignTreeNode) {
        return foreignPositions.get(foreignTreeNode);
    }

    public Pair<Integer, Integer> getForeignBounds(Tree<String> foreignTreeNode) {
        return foreignBounds.get(foreignTreeNode);
    }

    public int getForeignSpanLength(Tree<String> foreignTreeNode) {
        Pair<Integer, Integer> bounds = foreignBounds.get(foreignTreeNode);
        return bounds.getSecond() - bounds.getFirst() + 1;
    }

    public int getForeignSpanLength(int index) {
        return getForeignSpanLength(foreignNodes.get(index).getTreeNode());
    }

    public Set<Pair<Integer, Integer>> getPossibleAlignments() {
        return possibleAlignments;
    }

    public Set<Pair<Integer, Integer>> getSureAlignments() {
        if (sureAlignmentsAreDense) {
            switchToSparseAlignments();
        }
        return sureAlignments;
    }

    public void switchToSparseAlignments() {
        if (!sureAlignmentsAreDense) return;
        sureAlignments = new HashSet<Pair<Integer, Integer>>();
        for (int i = 0; i < getEnglishLength(); i++) {
            for (int j = 0; j < getForeignLength(); j++) {
                if (sureAlignmentsDense[i][j]) {
                    sureAlignments.add(Pair.newPair(i, j));
                }
            }
        }
        sureAlignmentsAreDense = false;
    }

    public void switchToDenseAlignments() {
        if (sureAlignmentsAreDense) return;
        sureAlignmentsDense = new boolean[getEnglishLength()][getForeignLength()];
        for (Pair<Integer, Integer> link : sureAlignments) {
            sureAlignmentsDense[link.getFirst()][link.getSecond()] = true;
        }
        sureAlignmentsAreDense = true;
    }

    public Map<Pair<Integer, Integer>, Double> getStrengths() {
        return strengths;
    }

    public boolean containsSureAlignment(int englishPosition, int foreignPosition) {
        if (sureAlignmentsAreDense) {
            return sureAlignmentsDense[englishPosition][foreignPosition];
        } else {
            return sureAlignments.contains(new Pair<Integer, Integer>(englishPosition,
                    foreignPosition));
        }
    }

    public boolean containsPossibleAlignment(int englishPosition, int foreignPosition) {
        return possibleAlignments.contains(new Pair<Integer, Integer>(englishPosition,
                foreignPosition));
    }

    public boolean containsEnglishParentAlignment(int englishPosition, int foreignPosition) {
        Tree<String> englishNode = englishNodes.get(englishPosition).internalNode;
        for (int i = englishSentence.size(); i < englishNodes.size(); i++) {
            if (containsSureAlignment(i, foreignPosition)) {
                assert !englishNodes.get(i).isWord;
                Tree<String> root = englishNodes.get(i).internalNode;
                for (Tree<String> node : root.getPreOrderTraversal()) {
                    if (node == englishNode) return true;
                }
            }
        }
        return false;
    }

    public boolean containsForeignParentAlignment(int englishPosition, int foreignPosition) {
        Tree<String> foreignNode = foreignNodes.get(foreignPosition).internalNode;
        for (int j = foreignSentence.size(); j < foreignNodes.size(); j++) {
            if (containsSureAlignment(englishPosition, j)) {
                assert !foreignNodes.get(j).isWord;
                Tree<String> root = foreignNodes.get(j).internalNode;
                for (Tree<String> node : root.getPreOrderTraversal()) {
                    if (node == foreignNode) return true;
                }
            }
        }
        return false;
    }

    public double getStrength(int i, int j) {
        return MapUtils.get(strengths, new Pair<Integer, Integer>(i, j), 0.0);
    }

    double[][] cachedPosteriors = null;
    double[][] cachedTransposePosteriors = null;

    public double[][] getForeignByEnglishPosteriors() {
        int n = getEnglishLength();
        int m = getForeignLength();
        if (cachedPosteriors == null) {
            cachedPosteriors = new double[m][n];
            for (int j = 0; j < m; j++)
                for (int i = 0; i < n; i++)
                    cachedPosteriors[j][i] = getStrength(i, j);
        }
        return cachedPosteriors;
    }

    // Return [English x foreign] posteriors
    public double[][] getEnglishByForeignPosteriors() {
        if (cachedTransposePosteriors == null) {
            cachedTransposePosteriors = NumUtils.transpose(getForeignByEnglishPosteriors());
        }
        return cachedTransposePosteriors;
    }

    public double[][] getPosteriors(int I, int J) {
        double[][] posteriors = new double[J][I];
        for (int j = 0; j < J; j++)
            for (int i = 0; i < I; i++)
                posteriors[j][i] = getStrength(i, j);
        return posteriors;
    }

    public double[][] getPosteriors() {
        int I = getEnglishLength();
        int J = getForeignLength();
        double[][] posteriors = new double[J][I];
        for (int j = 0; j < J; j++)
            for (int i = 0; i < I; i++)
                posteriors[j][i] = getStrength(i, j);
        return posteriors;
    }

    public List<Integer> getAlignmentsToEnglish(int englishPos) {
        if (englishAlignments == null) {
            gatherAlignmentArrays();
        }
        if (englishPos >= 0 && englishPos < englishAlignments.size()) {
            return englishAlignments.get(englishPos);
        }
        return new ArrayList<Integer>();
    }

    public List<Integer> getAlignmentsToForeign(int sourcepos) {
        if (englishAlignments == null) {
            gatherAlignmentArrays();
        }
        return foreignAlignments.get(sourcepos);
    }

    private void gatherAlignmentArrays() {
        foreignAlignments = new ArrayList<List<Integer>>(foreignNodes.size());
        for (int i = 0; i < foreignNodes.size(); i++) {
            foreignAlignments.add(new ArrayList<Integer>());
        }
        englishAlignments = new ArrayList<List<Integer>>(englishNodes.size());
        for (int i = 0; i < englishNodes.size(); i++) {
            englishAlignments.add(new ArrayList<Integer>());
        }
        for (Pair<Integer, Integer> alignment : getSureAlignments()) {
            Integer englishPos = alignment.getFirst();
            Integer foreignPos = alignment.getSecond();
            if (englishPos >= 0 && foreignPos >= 0) {
                foreignAlignments.get(foreignPos).add(englishPos);
                englishAlignments.get(englishPos).add(foreignPos);
            }
        }
    }

    /////////////////////////////
    // Setter Methods
    /////////////////////////////
    public void addAlignment(int englishPos, int foreignPos, boolean sure) {
        if (englishPos < 0 || foreignPos < 0) throw new RuntimeException("Neg alignment");
        if (englishPos >= englishNodes.size())
            throw new RuntimeException("En:" + englishPos + ">" + englishNodes.size()
                    + this);
        if (foreignPos >= foreignNodes.size())
            throw new RuntimeException("Fr:" + englishPos + ">" + foreignNodes.size()
                    + this);
        Pair<Integer, Integer> alignment = new Pair<Integer, Integer>(englishPos,
                foreignPos);
        if (sure) addSureAlignment(alignment);
        possibleAlignments.add(alignment);
        englishAlignments = null;
        foreignAlignments = null;
    }

    private void addSureAlignment(Pair<Integer, Integer> alignment) {
        if (sureAlignmentsAreDense) {
            addAlignment(alignment.getFirst(), alignment.getSecond());
        }
        sureAlignments.add(alignment);
    }

    public void addAlignment(int i, int j) {
        addAlignment(i, j, true);
    }

    public void removeAlignment(int i, int j) {
        Pair<Integer, Integer> al = Pair.newPair(i, j);
        if (containsSureAlignment(al)) {
            if (sureAlignmentsAreDense) {
                sureAlignmentsDense[al.getFirst()][al.getSecond()] = false;
            } else {
                sureAlignments.remove(al);
            }
        }
        if (possibleAlignments.contains(al)) possibleAlignments.remove(al);
    }

    public void setStrength(int i, int j, double strength) {
        strengths.put(new Pair<Integer, Integer>(i, j), strength);
    }

    /////////////////////
    // Thresholding
    /////////////////////

    // Create a new alignment based on thresholding the strengths of the provided alignment.
    public Alignment thresholdAlignmentByStrength(double threshold) {
        Alignment newAlignment = new Alignment(this);
        for (Pair<Integer, Integer> ij : strengths.keySet()) {
            int i = ij.getFirst(), j = ij.getSecond();
            double strength = strengths.get(ij);
            newAlignment.setStrength(i, j, strength);
            if (strength >= threshold) newAlignment.addAlignment(i, j, true);
        }
        return newAlignment;
    }

    /**
     * Generates an alignment for the same sentence pair according to the
     * supplied posteriors. Posteriors are stored as strengths and
     * above-threshold are stored as sure alignments.
     *
     * @param posteriors
     * @param threshold
     * @return
     */
    public Alignment thresholdPosteriors(double[][] posteriors, double threshold) {
        Alignment newAlignment = new Alignment(this);
        int J = posteriors.length; // Foreign length
        int I = posteriors[0].length; // English length
        assert (J == getForeignLength() && I == getEnglishLength());
        for (int j = 0; j < J; j++) {
            for (int i = 0; i < I; i++) {
                if (posteriors[j][i] == 0) continue;
                newAlignment.setStrength(i, j, posteriors[j][i]);
                if (posteriors[j][i] >= threshold) {
                    newAlignment.addAlignment(i, j, true);
                }
            }
        }
        return newAlignment;
    }

    // Create new alignments for a whole set of alignments.
    public static Map<Integer, Alignment> thresholdAlignmentsByStrength(
            Map<Integer, Alignment> alignments, double threshold) {
        Map<Integer, Alignment> newAlignments = new HashMap<Integer, Alignment>();
        for (int sid : alignments.keySet()) {
            Alignment alignment = alignments.get(sid);
            alignment = alignment.thresholdAlignmentByStrength(threshold);
            newAlignments.put(sid, alignment);
        }
        return newAlignments;
    }

    ///////////////////
    // Combining
    ///////////////////

    // Not quite a true intersection -- if an alignment from one source is outside the
    // dimensions of the other, it will be included (if we only have information from one
    // source, no combination is possible).
    public Alignment intersect(Alignment a) {
        Alignment ia = new Alignment(this);
        for (Pair<Integer, Integer> p : getSureAlignments())
            if (a.containsSureAlignment(p) || p.getFirst() >= a.getEnglishLength()
                    || p.getSecond() >= a.getForeignLength()) ia.addSureAlignment(p);
        for (Pair<Integer, Integer> p : a.getSureAlignments())
            if (p.getFirst() >= getEnglishLength() || p.getSecond() >= getForeignLength())
                ia.addSureAlignment(p);
        for (Pair<Integer, Integer> p : possibleAlignments)
            if (a.possibleAlignments.contains(p) || p.getFirst() >= a.getEnglishLength()
                    || p.getSecond() >= a.getForeignLength()) ia.possibleAlignments.add(p);
        for (Pair<Integer, Integer> p : a.possibleAlignments)
            if (p.getFirst() >= getEnglishLength() || p.getSecond() >= getForeignLength())
                ia.possibleAlignments.add(p);
        return ia;
    }

    public Alignment subtract(Alignment a) {
        Alignment ia = new Alignment(this);
        for (Pair<Integer, Integer> p : getSureAlignments())
            if (!ia.containsSureAlignment(p)) ia.addSureAlignment(p);
        for (Pair<Integer, Integer> p : possibleAlignments)
            if (!a.possibleAlignments.contains(p)) ia.possibleAlignments.add(p);
        return ia;
    }

    public Alignment union(Alignment a) {
        Alignment ua = new Alignment(this);
        for (Pair<Integer, Integer> p : getSureAlignments())
            ua.addSureAlignment(p);
        for (Pair<Integer, Integer> p : a.getSureAlignments())
            ua.addSureAlignment(p);
        for (Pair<Integer, Integer> p : possibleAlignments)
            ua.possibleAlignments.add(p);
        for (Pair<Integer, Integer> p : a.possibleAlignments)
            ua.possibleAlignments.add(p);
        return ua;
    }

    public Alignment reverse() {
        Alignment a2 = new Alignment(foreignSentence, englishSentence, foreignTree,
                englishTree, isNodeAlignment);
        for (Pair<Integer, Integer> p : getSureAlignments())
            a2.addSureAlignment(p.reverse());
        for (Pair<Integer, Integer> p : possibleAlignments)
            a2.possibleAlignments.add(p.reverse());
        if (strengths != null) {
            for (Pair<Integer, Integer> p : strengths.keySet())
                a2.setStrength(p.getFirst(), p.getSecond(), strengths.get(p));
        }
        return a2;
    }

    // TODO: figure out how to make this function accommodate trees -- for now, it
    //       just drops all tree-related information
    public Alignment chop(int i1, int i2, int j1, int j2) {
        Alignment choppedAlignment = new Alignment(englishSentence.subList(i1, i2),
                foreignSentence.subList(j1, j2));
        for (int i = i1; i < i2; i++) {
            for (int j = j1; j < j2; j++) {
                boolean isPossible = containsPossibleAlignment(i, j);
                boolean isSure = containsSureAlignment(i, j);
                if (isPossible) {
                    choppedAlignment.addAlignment(i - i1, j - j1, isSure);
                }
                choppedAlignment.setStrength(i - i1, j - j1, getStrength(i, j));
            }
        }
        return choppedAlignment;
    }

    ///////////////////////////
    // Rendering and Reading
    ///////////////////////////

    public String toString() {
        return render(this, this);
    }

    /**
     * Renders a proposed alignment relative to a reference alignment. Strengths
     * are ignored.
     *
     * @param reference
     * @param proposed
     * @return
     */
    public static String render(Alignment reference, Alignment proposed) {
        return render(reference, proposed, null);
    }

    /**
     * Renders a proposed alignment relative to a reference with a gloss of the
     * foreign sentence.
     *
     * @param reference
     * @param proposed
     * @param glossDictionary
     * @return
     */
    public static String render(Alignment reference, Alignment proposed,
            String2DoubleMap glossDictionary) {
        StringBuilder sb = new StringBuilder();
        List<String> englishWords = reference.englishSentence;
        List<String> foreignWords = reference.foreignSentence;
        for (int sourcePosition = 0; sourcePosition < foreignWords.size(); sourcePosition++) {
            for (int targetPosition = 0; targetPosition < englishWords.size(); targetPosition++) {
                boolean sure = reference.containsSureAlignment(targetPosition,
                        sourcePosition);
                boolean possible = reference.containsPossibleAlignment(targetPosition,
                        sourcePosition);
                char proposedChar = ' ';
                if (proposed.containsSureAlignment(targetPosition, sourcePosition)) proposedChar = '#';
                else if (proposed.containsEnglishParentAlignment(targetPosition,
                        sourcePosition)) proposedChar = '*';
                if (sure) {
                    sb.append('[');
                    sb.append(proposedChar);
                    sb.append(']');
                } else {
                    if (possible) {
                        sb.append('(');
                        sb.append(proposedChar);
                        sb.append(')');
                    } else {
                        sb.append(' ');
                        sb.append(proposedChar);
                        sb.append(' ');
                    }
                }
            }
            sb.append("| ");
            String fword = foreignWords.get(sourcePosition);
            sb.append(fword);
            // Include gloss if it exists
            if (glossDictionary != null) {
                StringDoubleMap eMap = glossDictionary.getMap(fword, false);

                if (eMap != null) {
                    String eWords = eMap.keySet().toString();
                    sb.append(" ");
                    sb.append(eWords);
                }
            }
            sb.append('\n');
        }
        for (int targetPosition = 0; targetPosition < englishWords.size(); targetPosition++) {
            sb.append("---");
        }
        sb.append("'\n");
        boolean printed = true;
        int index = 0;
        while (printed) {
            printed = false;
            StringBuilder lineSB = new StringBuilder();
            for (int targetPosition = 0; targetPosition < englishWords.size(); targetPosition++) {
                String targetWord = englishWords.get(targetPosition);
                if (targetWord.length() > index) {
                    printed = true;
                    lineSB.append(' ');
                    lineSB.append(targetWord.charAt(index));
                    lineSB.append(' ');
                } else {
                    lineSB.append("   ");
                }
            }
            index += 1;
            if (printed) {
                sb.append(lineSB);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Detects whether this is a soft or hard alignment and writes the
     * appropriate format (soft if available, otherwise hard).
     */
    public String output() {
        return dumpModifiedPharaoh(!(strengths == null || strengths.isEmpty()), 0.0, false);
    }

    /**
     * Writes the sure and proposed alignments in a modified version of the
     * Pharaoh format.
     *
     * For example, if we have 7 sure alignments and two possibles, we get:
     *
     * enPos1-frPos1 enPos2-frPos2 ... enPos8-frPos8-P enPos9-frPos9-P
     *
     * here, the -P indicates possible alignments.
     */
    public String outputHard() {
        return outputHard(false);
    }

    public String outputHard(boolean skipTreeNodes) {
        return dumpModifiedPharaoh(false, 0.0, skipTreeNodes);
    }

    /**
     * Writes the posterior alignments in a modified version of the Pharaoh
     * format. Each alignment is a triple:
     *
     * enPos-frPos-strength
     */
    public String outputSoft() {
        return outputSoft(0.0);
    }

    public String outputSoft(double threshold) {
        return outputSoft(threshold, false);
    }

    public String outputSoft(double threshold, boolean skipTreeNodes) {
        return dumpModifiedPharaoh(true, threshold, skipTreeNodes);
    }

    private String dumpModifiedPharaoh(boolean soft, double threshold, boolean skipTreeNodes) {
        StringBuffer sbuf = new StringBuffer();
        if (soft) {
            for (Pair<Integer, Integer> pair : strengths.keySet()) {
                if (skipTreeNodes
                        && (pair.getFirst() >= englishSentence.size() || pair.getSecond() >= foreignSentence
                                .size())) {
                    continue;
                }
                double strength = strengths.get(pair);
                if (strength >= threshold) {
                    sbuf.append((pair.getSecond()) + "-" + (pair.getFirst()) + "-"
                            + strength);
                    sbuf.append(" ");
                }
            }
        } else {
            for (Pair<Integer, Integer> pair : sureAlignments) {
                if (skipTreeNodes
                        && (pair.getFirst() >= englishSentence.size() || pair.getSecond() >= foreignSentence
                                .size())) {
                    continue;
                }
                sbuf.append((pair.getSecond()) + "-" + (pair.getFirst()) + " ");
            }
            for (Pair<Integer, Integer> pair : possibleAlignments) {
                if (skipTreeNodes
                        && (pair.getFirst() >= englishSentence.size() || pair.getSecond() >= foreignSentence
                                .size())) {
                    continue;
                }
                if (!sureAlignments.contains(pair)) {
                    sbuf.append((pair.getSecond()) + "-" + (pair.getFirst()) + "-P ");
                }
            }
        }
        return sbuf.toString();
    }

    private boolean containsSureAlignment(Pair<Integer, Integer> pair) {
        if (sureAlignmentsAreDense) {
            return sureAlignmentsDense[pair.getFirst()][pair.getSecond()];
        } else {
            return sureAlignments.contains(pair);
        }
    }

    /**
     * Reads a string of alignments generated by an output function (or Pharaoh)
     * and adds those alignments.
     *
     * @param string
     *            The alignments to parse.
     */
    public void parseAlignments(String line) {
        parseAlignments(line, false, false);
    }

    /**
     * Reads a string of alignments generated by an output function (or Pharaoh)
     * and adds those alignments.
     *
     * @param line
     *            The alignments to parse.
     * @param reverse
     *            Whether to reverse each link
     * @param oneIndexed
     *            If alignments are off by 1, we need to subtract and make them
     *            0-indexed
     */
    public void parseAlignments(String line, boolean reverse, boolean oneIndexed) {
        //		String noComment = StrUtils.split(line, "#")[0];
        String noComment = line;
        String[] aligns = StrUtils.split(noComment);
        for (int i = 0; i < aligns.length; i++) {
            String[] els = StrUtils.split(aligns[i], "-");

            int fr = Integer.parseInt((reverse ? els[1] : els[0]));
            int en = Integer.parseInt((reverse ? els[0] : els[1]));

            en = oneIndexed ? en - 1 : en;
            fr = oneIndexed ? fr - 1 : fr;

            if (els.length == 2) {
                addAlignment(en, fr, true);
            } else if (els[2].equals("P")) {
                addAlignment(en, fr, false);
            } else if (els.length == 3) {
                double strength = Double.parseDouble(els[2]);
                setStrength(en, fr, strength);
            } else if (els.length == 4) {
                double strength = Double.parseDouble(els[2] + "-" + els[3]);
                setStrength(en, fr, strength);
            } else {
                throw new RuntimeException(
                        "Alignment.parseAlignments Can't parse alignment " + aligns[i]
                                + " len==" + els.length);
            }
        }
    }

    /////////////////////////
    // Outputting GIZA format
    /////////////////////////

    public List<Integer> getNullAlignedEnglishIndices() {
        List<Integer> nulls = new ArrayList<Integer>();
        boolean[] hasAlignment = new boolean[getEnglishLength()];
        for (Pair<Integer, Integer> al : getSureAlignments()) {
            hasAlignment[al.getFirst()] = true;
        }

        for (int en = 0; en < getEnglishLength(); en++) {
            if (!hasAlignment[en]) nulls.add(en);
        }

        return nulls;
    }

    public List<Integer> getNullAlignedForeignIndices() {
        List<Integer> nulls = new ArrayList<Integer>();
        boolean[] hasAlignment = new boolean[getForeignLength()];
        for (Pair<Integer, Integer> al : getSureAlignments()) {
            hasAlignment[al.getSecond()] = true;
        }

        for (int fr = 0; fr < getForeignLength(); fr++) {
            if (!hasAlignment[fr]) nulls.add(fr);
        }

        return nulls;
    }

    private List<Integer> addOne(List<Integer> list) {
        List<Integer> newList = new ArrayList<Integer>();
        for (int x : list)
            newList.add(x + 1);
        return newList;
    }

    public void writeGIZA(PrintWriter out, int idx) {
        out
                .printf(
                        "# sentence pair (%d) source length %d target length %d alignment score : 0\n",
                        idx, englishSentence.size(), foreignSentence.size());
        out.println(StrUtils.join(foreignSentence));
        out.printf("NULL ({ %s })", StrUtils.join(addOne(getNullAlignedForeignIndices())));
        for (int i = 0; i < englishSentence.size(); i++) {
            List<Integer> alignments = addOne(getAlignmentsToEnglish(i));
            Collections.sort(alignments);
            out.printf(" %s ({ %s })", englishSentence.get(i), StrUtils.join(alignments));
        }
        out.println("");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length != 5)
            throw new RuntimeException("Usage: [frSuff] [enSuff] [root] [alignFile] [dict]");
        SentencePairReader reader = new SentencePairReader(false);
        reader.setForeignExtension(args[0]);
        reader.setEnglishExtension(args[1]);
        PairDepot pd = reader.pairDepotFromSources(Lists.newList(args[2]), 0, 1000000,
                Filters.acceptFilter(), false, false);
        Iterator<String> aligns = IOUtils.readLinesEasy(args[3]).iterator();
        String2DoubleMap glossDictionary = String2DoubleWordMap.fromFile(args[4], false,
                false);
        for (SentencePair sp : pd) {
            Alignment align = new Alignment(sp, false);
            align.parseAlignments(aligns.next(), false, false);
            System.out.println(Alignment.render(sp.getAlignment(), align, glossDictionary));
        }
    }

}