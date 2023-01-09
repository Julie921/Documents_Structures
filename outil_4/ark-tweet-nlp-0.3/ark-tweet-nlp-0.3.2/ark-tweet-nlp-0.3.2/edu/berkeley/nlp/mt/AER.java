package edu.berkeley.nlp.mt;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.berkeley.nlp.mt.SentencePairReader.PairDepot;
import edu.berkeley.nlp.util.Filters;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Pair;
import fig.basic.String2DoubleMap;

public class AER {
	public AER(double precision, double recall, double aer, double f1) {
		this.precision = precision;
		this.recall = recall;
		this.aer = aer;
		this.f1 = f1;
	}

	public AER() {
	}

	public void addAlignment(Alignment hypothesis, Alignment reference) {
		Set<Pair<Integer, Integer>> guess = hypothesis.sureAlignments;
		Set<Pair<Integer, Integer>> sure = reference.sureAlignments;
		Set<Pair<Integer, Integer>> possible = reference.possibleAlignments;
		Set<Pair<Integer, Integer>> all = new HashSet<Pair<Integer, Integer>>(guess);
		all.addAll(sure);
		all.addAll(possible);
		for (Pair<Integer, Integer> p : all) {
			addPoint(guess.contains(p), sure.contains(p), possible.contains(p));
		}
	}

	public void addPoint(boolean proposed, boolean sure, boolean possible) {
		// Update counts
		if (proposed && sure) proposedSureCount += 1;
		if (proposed && possible) proposedPossibleCount += 1;
		if (proposed) proposedCount += 1;
		if (sure) sureCount++;
	}

	// Compute precision, recall, AER from counts.
	public void computeFromCounts() {
		precision = proposedPossibleCount / (double) proposedCount;
		recall = proposedSureCount / (double) sureCount;
		aer = 1.0 - (proposedSureCount + proposedPossibleCount)
				/ (double) (sureCount + proposedCount);
		f1 = 2 * precision * recall / (precision + recall);
	}

	public String simpleString() {
		return String.format("%f %f %f", precision, recall, aer);
	}

	public String toString() {
		return String.format("Prec = %f, Recall = %f, AER = %f, F1 = %f", precision,
				recall, aer, f1);
	}

	int proposedSureCount = 0;
	int proposedPossibleCount = 0;
	int sureCount = 0;
	int proposedCount = 0;
	double precision, recall, aer, f1;

	public static void main(String[] args) {
		if (args.length < 4) throw new RuntimeException("Usage: AER [hyps] [refs] [en-extension] [ch-extension] (dictionary)");
		String hypotheses = args[0], references = args[1];
		System.out.println("Hypotheses: " + hypotheses + "; References: " + references);
		SentencePairReader reader = new SentencePairReader(true);
		reader.setEnglishExtension(args[2]);
		reader.setForeignExtension(args[3]);
		String2DoubleMap gloss = null;
		if (args.length == 5) gloss = loadDictionary(args[4]);

		PairDepot hypDepot = reader.pairDepotFromSource(hypotheses, 0, Integer.MAX_VALUE,
				Filters.acceptFilter(), true);
		Iterator<SentencePair> refIter = reader.pairDepotFromSource(references, 0,
				Integer.MAX_VALUE, Filters.acceptFilter(), true).iterator();
		AER score = new AER();
		int k = 0;
		for (SentencePair h : hypDepot) {
			if (!refIter.hasNext()) break;
			k++;
			SentencePair r = refIter.next();
			assert (r.englishWords.equals(h.englishWords)) : r.toString() + "\n"
					+ h.toString();
			System.out.println(Alignment.render(r.alignment, h.alignment, gloss));
			score.addAlignment(h.alignment, r.alignment);
		}
		System.out.println("Evaluated on " + k + " sentences");
		score.computeFromCounts();
		System.out.println(score.toString());
	}

	private static String2DoubleMap loadDictionary(String fname) {
		String2DoubleMap dict = new String2DoubleMap();
		BufferedReader dictReader = IOUtils.openInEasy(fname);
		if (dictReader == null) {
			return null;
		} else {
			try {
				while (dictReader.ready()) {
					String[] words = dictReader.readLine().split("\\t");
					String[] translations = words[1].split("/");
					for (int i = 1; i < Math.min(2, translations.length); i++) {
						String translation = translations[i];
						translation = translation.toLowerCase();
						dict.incr(words[0].intern(), translation.intern(), 1);
					}
				}
				LogInfo.logss("Dictionary loaded");
				return dict;
			} catch (IOException e) {
				LogInfo.error("Problem loading dictionary file: " + fname);
				return null;
			}
		}
	}

	public double getAER() {
		return aer;
	}
}
