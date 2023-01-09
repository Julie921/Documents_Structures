package edu.berkeley.nlp.mt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.Iterators;
import fig.basic.IOUtils;

/**
 * Does a statistical significance check on two competing translation sets.
 * Modelled after http://www.ark.cs.cmu.edu/MT/
 * 
 * @author Adam Pauls
 */
public class BleuStatisticalSignificanceChecker {
	

	private BleuScorer bleuScorer;

	/**
	 * If weights == null, this is equivalent to calling the anonymous constructor.
	 * @param weights The weigths used to combine the individual n-grams
	 */
	public BleuStatisticalSignificanceChecker(BleuScorer bleuScorer)
	{
		this.bleuScorer = bleuScorer;
	}

	/**
	 * Make sure the better scoring system is first
	 * 
	 * 
	 * @param reference
	 * @return
	 */
	public boolean significant(List<TestSentence> reference, List<List<String>> goodTranslations, List<List<String>> badTranslations, int $num_samples, double $p)
	{

		Random random = new Random();

		int $num_total_lines = reference.size();
		int $curr_sample = 0;
		List<Double> $system1_bleu= new ArrayList<Double>();
		List<Double> $system2_bleu = new ArrayList<Double>();
		int $max_Ngram = 4;
		while ($curr_sample < $num_samples) {
		 
			int $cum_ref_length_1 = 0;
			int $cum_ref_length_2 = 0;
			

			List<Integer> $inds = new ArrayList<Integer>();
		
			for (int $i = 0; $i < $num_total_lines; $i++) {
				int $r = random.nextInt($num_total_lines);
				$inds.add($r);
			}

		
			Collections.sort($inds);
			List<Integer> $sinds = $inds;
			
		
			
			List<TestSentence> tmpReferences = new ArrayList<TestSentence>();
			List<List<String>> tmpGoodTranslations = new ArrayList<List<String>>();
			List<List<String>> tmpBadTranslations = new ArrayList<List<String>>();
			for (int i : $sinds)
			{
				tmpReferences.add(reference.get(i));
				tmpGoodTranslations.add(goodTranslations.get(i));
				tmpBadTranslations.add(badTranslations.get(i));
			}
			$system1_bleu.add(bleuScorer.evaluateBleu(tmpGoodTranslations, tmpReferences,true).getBleuScore());
			$system2_bleu.add(bleuScorer.evaluateBleu(tmpBadTranslations, tmpReferences, true).getBleuScore());
		

			$curr_sample++;
		}

		
		
		int $system_1_better_bleu = 0;
		for (int $i = 0; $i < $num_samples; $i++) {
			final Double system1 = $system1_bleu.get($i);
			final Double system2 = $system2_bleu.get($i);
			System.out.println("First system " + system1 + " " + system2);
			if (system1 > system2)
			{
				
				$system_1_better_bleu++;
			}
		}
		
		double $system_1_better_bleu_fraction = $system_1_better_bleu / $num_samples;
		return (1 - $system_1_better_bleu_fraction) < $p;
		

		
	}
	
	public static void main(String[] argv)
	{
		if (argv.length < 3 || argv.length > 6)
		{
			System.err.println("Args: [reference file] [good system] [bad system]  <p=0.05> <numSamples=100> <max length of reference sentences=unlimited>.");
			System.exit(1);
		}
//		 CorrespondingIterable<String> pairIterable = new CorrespondingIterable<String>(Iterators.able(IOUtils.lineIterator(argv[0])), Iterators.able(IOUtils.lineIterator(argv[1])));
		
		 List<TestSentence> references = new ArrayList<TestSentence>();
		 List<List<String>> goodCandidates = new ArrayList<List<String>>();
		 List<List<String>> badCandidates = new ArrayList<List<String>>();
		 double p = argv.length >= 4 ? Double.parseDouble(argv[3]) : 0.05;
		 int numSamples = argv.length >= 5 ? Integer.parseInt(argv[4]) : 100;
		 int maxLength = argv.length >= 6 ? Integer.parseInt(argv[5]) : Integer.MAX_VALUE;
		Set<Integer> filtered = new HashSet<Integer>();
		 
		 try
		{
			 int x = 0;
			for (String line : Iterators.able(IOUtils.lineIterator(argv[0])))
			{
				
				final List<String> asList = Arrays.asList(line.split(" "));
				final TestSentence o = new TestSentence(Collections.singletonList("dummy"), Collections.singletonList(asList));
				if (asList.size() > maxLength)
				{
					filtered.add(x);
				}
				else
				{
					references.add(o);
				}
				x++;
			}
			int y = 0;
			for (String line : Iterators.able(IOUtils.lineIterator(argv[1])))
			{
				if (!filtered.contains(y)) 
					goodCandidates.add(Arrays.asList(line.split(" ")));
				y++;
			}
			for (String line : Iterators.able(IOUtils.lineIterator(argv[2])))
			{
				if (!filtered.contains(y)) 
					badCandidates.add(Arrays.asList(line.split(" ")));
				y++;
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			throw new RuntimeException(e);

		}

		if (goodCandidates.size() != references.size())
			throw new RuntimeException("Reference length = " + references.size() + " and candidate length = " + goodCandidates.size());
			 
		 BleuStatisticalSignificanceChecker scorer = new BleuStatisticalSignificanceChecker(new BleuScorer());
		boolean significant = scorer.significant(references, goodCandidates, badCandidates, numSamples, p);
		System.out.println("Bleu difference is " + (significant ? " significant " : " NOT SIGNIFICANT"));
		System.exit(1);
			 
	}

	/**
	 * Extract all the ngrams and their counts in a given sentence.
	 * 
	 * @param n n in n-gram
	 * @param sentences
	 * @return
	 */
	protected Counter<List<String>> extractNGramCounts(int n, List<String> sentences) {
		Counter<List<String>> nGrams = new Counter<List<String>>();
		for (int i = 0; i <= sentences.size() - n; i++) {
			nGrams.incrementCount(sentences.subList(i, i + n), 1.0);
		}
		return nGrams;
	}

}
