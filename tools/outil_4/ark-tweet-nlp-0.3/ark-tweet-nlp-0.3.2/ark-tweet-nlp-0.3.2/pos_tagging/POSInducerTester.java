package pos_tagging;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import util.LBFGSMinimizer;

import edu.berkeley.nlp.optimize.BipartiteMatchings;
import edu.berkeley.nlp.util.CallbackFunction;
import edu.berkeley.nlp.util.Counter;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.Pair;
import fig.exec.Execution;

public class POSInducerTester implements Runnable {
	
	public static Random baseRand = new Random(43569);
	public static Random[] rands;
	public static CoarsePOSFinder coarsePOSFinder = new CoarsePOSFinder();
	
	static {
		rands = new Random[10];
		for (int i=0; i<10; ++i) {
			rands[i] = new Random(baseRand.nextInt());
		}
	}
	
	public class Options {
		
		@Option(gloss = "Parsed/tagged sentences input mrg file. Used for traning and testing.")
		public String treeBank = "/Users/tberg/corpora/EnglishTreebank/WSJ.mrg";

		@Option(gloss = "Number of sentences to read from input file.")
		public int numSentences = 5000;
		
		@Option(gloss = "Skip over sentences that have length longer than this number.")
		public int maxSentenceLength = 10;
		
		@Option(gloss = "Number of unsupervised tag clusters to use.")
		public int numLabels = 45;
		
		@Option(gloss = "Number of iterations to run for.")
		public int iters = 1000;
		
		@Option(gloss = "Number of iterations between recording weights to file.")
		public int printRate = 1000;
		
		@Option(gloss = "Forces only full-indicator features, and does standard M-step by normalizing expected counts.")
		public boolean useStandardMultinomialMStep = true;
		
		@Option(gloss = "Add this pseudo-count to expected counts when doing standard M-step.")
		public double standardMStepCountSmoothing = 0.1;
		
		@Option(gloss = "If true, use direct gradient method. Otherwise use EM.")
		public boolean useGradient = false;
		
		@Option(gloss = "If true, use locally normalized potentials. Otherwise use globally normalized.")
		public boolean useGlobal = false;
		
		@Option(gloss = "Upper end point of interval from which initial weights are drawn UAR.")
		public double initialWeightsUpper = 0.01;
		
		@Option(gloss = "Lower end point of interval from which initial weights are drawn UAR.")
		public double initialWeightsLower = -0.01;
		
		@Option(gloss = "Regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
		public double regularizationWeight = 0.0;
		
		@Option(gloss = "Regulariztion term is sum_i[ c*(w_i - b)^2 ] where c is regularization weight and b is regularization bias.")
		public double regularizationBias = 0.0;
		
		@Option(gloss = "If true, in addition to full-indicator features use the following features: 1) initial capital 2) contains digit 3) contains hyphen 4) 3-gram suffix indicators. Otherwise just use full-indicators.")
		public boolean useStandardFeatures = false;

		@Option(gloss = "n to use in n-gram suffix features.")
		public int lengthNGramSuffixFeature = 3;
		
		@Option(gloss = "Use coarse POS indicator features.")
		public boolean useCoarsePOSFeatures = false;

		@Option(gloss = "Use a bias feature that is constant.")
		public boolean useBiasFeature = false;
		
		@Option(gloss = "Regularization bias for weight for bias feature.")
		public double biasFeatureBias = -10.0;

		@Option(gloss = "Regularization weight for weight for bias feature.")
		public double biasFeatureRegularizationWeight = 10.0;
		
		@Option(gloss = "Index of random seed to use. (10 possible random seeds are deterministically precomputed.)")
		public int randSeedIndex = 0;
		
	}
	
	public interface EmitFeatureTemplate {
		
		public List<Pair<String,Double>> getFeatures(int label, String emission);
		
		public String getName();
		
	}
	
	public interface TransFeatureTemplate {
		
		public List<Pair<String,Double>> getFeatures(int label1, int label2);
		
		public String getName();
		
	}
	
	public class TransIndicatorFeature implements TransFeatureTemplate {
		
		public String name = "tind"; 
		
		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|%d|%d", label1, label2), 1.0));
			return features;
		}
		
	}
	
	public class CoarseTransIndicatorFeature implements TransFeatureTemplate {
		
		public String name = "coarsetind"; 
		
		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (label1 < indexToPOS.size() && label2 < indexToPOS.size()) {
				String pos1 = indexToPOS.get(label1);
				String pos2 = indexToPOS.get(label2);
				List<String> coarsePOSs1 = coarsePOSFinder.getCoarsePOS(pos1);
				List<String> coarsePOSs2 = coarsePOSFinder.getCoarsePOS(pos2);
				for (String coarsePOS1 : coarsePOSs1) {
					for (String coarsePOS2 : coarsePOSs2) {
						features.add(Pair.makePair(String.format(name+"cc|%s|%s", coarsePOS1, coarsePOS2), 1.0));
						features.add(Pair.makePair(String.format(name+"cp|%s|%s", coarsePOS1, pos2), 1.0));
						features.add(Pair.makePair(String.format(name+"pc|%s|%s", pos1, coarsePOS2), 1.0));
					}
				}
			}
			return features;
		}
		
	}
	
	public class EmitIndicatorFeature implements EmitFeatureTemplate {
		
		public String name = "eind"; 
		
		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|%d|%s", label, word), 1.0));
			return features;
		}

	}
	
	public class CoarseEmitIndicatorFeature implements EmitFeatureTemplate {
		
		public String name = "coarseeind"; 
		
		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (label < indexToPOS.size()) {
				List<String> coarsePOSs = coarsePOSFinder.getCoarsePOS(indexToPOS.get(label));
				for (String coarsePOS : coarsePOSs) {
					features.add(Pair.makePair(String.format(name+"|%s|%s", coarsePOS, word), 1.0));
				}
			}
			return features;
		}

	}
	
	
	public class NGramSuffixIndicatorFeature implements EmitFeatureTemplate {

		public String name; 
		private int n;
		
		public NGramSuffixIndicatorFeature(int n0) {
			this.n = n0;
			this.name = "suff"+n0;
		}
		
		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (word.length() >= n) {
				features.add(Pair.makePair(String.format(name+"|%d|%s",  label, word.substring(word.length()-n, word.length())), 1.0));
			}
			return features;
		}

	}
	
	
	public class CoarseNGramSuffixIndicatorFeature implements EmitFeatureTemplate {

		public String name; 
		private int n;
		
		public CoarseNGramSuffixIndicatorFeature(int n0) {
			this.n = n0;
			this.name = "coarsesuff"+n0;
		}
		
		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (label < indexToPOS.size()) {
				List<String> coarsePOSs = coarsePOSFinder.getCoarsePOS(indexToPOS.get(label));
				for (String coarsePOS : coarsePOSs) {
					if (word.length() >= n) {
						features.add(Pair.makePair(String.format(name+"|%s|%s", coarsePOS, word.substring(word.length()-n, word.length())), 1.0));
					}
				}
			}
			return features;
		}

	}
	
	public class InitialCapitalIndicatorFeature implements EmitFeatureTemplate {
		
		public String name = "initcap"; 
		
		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (Character.isUpperCase(word.charAt(0))) {
				features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
			}
			return features;
		}
		
	}
	
	public class CoarseInitialCapitalIndicatorFeature implements EmitFeatureTemplate {
		
		public String name = "coarseinitcap"; 
		
		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (label < indexToPOS.size()) {
				if (Character.isUpperCase(word.charAt(0))) {
					features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
				}
			}
			return features;
		}
		
	}
	
	public class ContainsHyphenIndicatorFeature implements EmitFeatureTemplate {
		
		public String name = "hyphen"; 

		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (word.contains("-")) {
				features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
			}
			return features;
		}
		
	}

	public class ContainsDigitIndicatorFeature implements EmitFeatureTemplate {
		
		public String name = "digit"; 
		
		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			boolean containsDigit = false;
			for (int i=0; i<word.length(); ++i) {
				if (Character.isDigit(word.charAt(i))) {
					containsDigit = true;
				}
			}
			if (containsDigit) {
				features.add(Pair.makePair(String.format(name+"|%d", label), 1.0));
			}
			return features;
		}
		
	}

	public class CoarseContainsHyphenIndicatorFeature implements EmitFeatureTemplate {

		public String name = "coarsehyphen"; 

		public String getName() {return name;}

		public List<Pair<String, Double>> getFeatures(int label, String word) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			if (label < indexToPOS.size()) {
				List<String> coarsePOSs = coarsePOSFinder.getCoarsePOS(indexToPOS.get(label));
				for (String coarsePOS : coarsePOSs) {
					if (word.contains("-")) {
						features.add(Pair.makePair(String.format(name+"|%s", coarsePOS), 1.0));
					}
			}
			}
			return features;
		}
		
	}
	
	public class BiasIndicatorFeature implements TransFeatureTemplate {
		
		public String name = "bias"; 
		
		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name), 1.0));
			return features;
		}
		
	}
	
	public class NodeIndicatorFeature implements TransFeatureTemplate {
		
		public String name = "nind"; 
		
		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|%d", label1), 1.0));
			return features;
		}
		
	}
	
	public class NextNodeIndicatorFeature implements TransFeatureTemplate {
		
		public String name = "nnind"; 
		
		public String getName() {return name;}
		
		public List<Pair<String, Double>> getFeatures(int label1, int label2) {
			List<Pair<String, Double>> features = new ArrayList<Pair<String,Double>>();
			features.add(Pair.makePair(String.format(name+"|%d", label2), 1.0));
			return features;
		}
		
	}
	
	public Options opts;
	private ArrayList<String> indexToPOS;
	private ArrayList<String> indexToWord;
	private ArrayList<String> indexToFeature;
	private Map<String, Integer> posToIndex;
	private Map<String, Integer> wordToIndex;
	private Map<String, Integer> featureToIndex;
	private ArrayList<Integer> featureIndexCounts;
	private List<Pair<Integer,Double>>[][] activeTransFeatures;
	private List<Pair<Integer,Double>>[][] activeEmitFeatures;
	private int[][] observations;
	private int[][] goldLabels;
	
	public POSInducerTester() {
		this.opts = new Options();
		this.featureIndexCounts = new ArrayList<Integer>();
		this.indexToPOS = new ArrayList<String>();
		this.indexToWord = new ArrayList<String>();
		this.indexToFeature = new ArrayList<String>();
		this.posToIndex = new HashMap<String, Integer>();
		this.wordToIndex = new HashMap<String, Integer>();
		this.featureToIndex = new HashMap<String, Integer>();
	}

	public static void main(String[] args) {
		POSInducerTester tester = new POSInducerTester();
		Execution.run(args,tester,tester.opts);
	}
	
	public void run() {
		// Get observations and correct labels
		Pair<int[][], int[][]> observationsAndLabels = getObservationsAndGoldLabels(PennTreeBankPOSSequenceReader.readPOSSeqences(opts.treeBank, opts.numSentences, opts.maxSentenceLength));
		observations = observationsAndLabels.getFirst();
		goldLabels = observationsAndLabels.getSecond();
		int numTokens = 0;
		int maxSeqLength = 0;
		for (int s=0; s<observations.length; ++s) {
			numTokens += observations[s].length;
			maxSeqLength = Math.max(maxSeqLength, observations[s].length);
		}
		
		LogInfo.logss("Use standard multinomial MStep: %b", opts.useStandardMultinomialMStep);
		LogInfo.logss("Use global: %b", opts.useGlobal);
		LogInfo.logss("Use gradient: %b", opts.useGradient);
		LogInfo.logss("Use standard features: %b", opts.useStandardFeatures);
		LogInfo.logss("Use coarse features: %b", opts.useCoarsePOSFeatures);
		LogInfo.logss("Num tokens: %d", numTokens);
		LogInfo.logss("Max seq length: %d", maxSeqLength);
		LogInfo.logss("Num sentences: %d", observations.length);
		LogInfo.logss("Num gold labels: %d", indexToPOS.size());
		LogInfo.logss("Num guess labels: %d", opts.numLabels);
		LogInfo.logss("Num word types: %d", indexToWord.size());
		LogInfo.logss("Regularization weight: %f", opts.regularizationWeight);
		LogInfo.logss("Regularization bias: %f", opts.regularizationBias);
		LogInfo.logss("Use bias feature: %b", opts.useBiasFeature);
		LogInfo.logss("Bias feature bias: %f", opts.biasFeatureBias);
		LogInfo.logss("Bias feature regularization weight: %f", opts.biasFeatureRegularizationWeight);
		LogInfo.logss("Gold POS labels: " + indexToPOS.toString());
		LogInfo.logss("Initial random weights lower: %f", opts.initialWeightsLower);
		LogInfo.logss("Initial random weights upper: %f", opts.initialWeightsUpper);
		
		// Build feature templates
		List<TransFeatureTemplate> transFeatures = new ArrayList<TransFeatureTemplate>();
		List<EmitFeatureTemplate> emitFeatures = new ArrayList<EmitFeatureTemplate>();
		transFeatures.add(new TransIndicatorFeature());
		if (opts.useBiasFeature) {
			transFeatures.add(new BiasIndicatorFeature());
		}
		emitFeatures.add(new EmitIndicatorFeature());
		if (opts.useStandardFeatures) {
			emitFeatures.add(new InitialCapitalIndicatorFeature());
			emitFeatures.add(new ContainsHyphenIndicatorFeature());
			emitFeatures.add(new ContainsDigitIndicatorFeature());
			for (int i=1; i<=opts.lengthNGramSuffixFeature; ++i) {
				emitFeatures.add(new NGramSuffixIndicatorFeature(i));
			}
		}
		if (opts.useCoarsePOSFeatures) {
			transFeatures.add(new CoarseTransIndicatorFeature());
//			emitFeatures.add(new CoarseEmitIndicatorFeature());
//			emitFeatures.add(new CoarseInitialCapitalIndicatorFeature());
//			emitFeatures.add(new CoarseContainsHyphenIndicatorFeature());
//			for (int i=1; i<=opts.lengthNGramSuffixFeature; ++i) {
//				emitFeatures.add(new CoarseNGramSuffixIndicatorFeature(i));
//			}
		}

		LogInfo.logss("Trans features:");
		for (TransFeatureTemplate feat : transFeatures) {
			LogInfo.logss(feat.getName());
		}
		LogInfo.logss("Emit features:");
		for (EmitFeatureTemplate feat : emitFeatures) {
			LogInfo.logss(feat.getName());
		}

		int[][] guessLabels = null;

		FileWriter curveOut = null;
		try {
			curveOut = new FileWriter(Execution.getFile("curve"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (!opts.useGradient) {
			// Do EM
			EMSequenceModel em = null;
			if (!opts.useGlobal) {
				em = new EMGenSequenceModel(observations, opts.numLabels, indexToWord.size());
			} else {
				em = new EMGlobalSequenceModel(observations, opts.numLabels, indexToWord.size());
			}
			// Cache active features
			activeTransFeatures = getActiveTransFeatures(transFeatures, em.getNumObservations(), em.getNumLabels(), em.getStartLabel(), em.getStopLabel());
			activeEmitFeatures = getActiveEmitFeatures(emitFeatures, em.getNumObservations(), em.getNumLabels(), em.getStartLabel(), em.getStopLabel());
			LogInfo.logss("Num features: %d", indexToFeature.size());
			double[] regularizationWeights = getRegularizationWeights(transFeatures, emitFeatures);
			double[] regularizationBiases = getRegularizationBiases(transFeatures, emitFeatures, em.getNumLabels(), em.getStartLabel(), em.getStopLabel());
			em.setActiveFeatures(activeTransFeatures, activeEmitFeatures, indexToFeature.size(), regularizationWeights, regularizationBiases);
			// Initialize weights
			double[] initialWeights = uniformRandomWeights(em.getNumFeatures(), opts.initialWeightsLower, opts.initialWeightsUpper, rands[opts.randSeedIndex]);
			if (opts.useBiasFeature) {
				initialWeights[featureToIndex.get("bias")] = opts.biasFeatureBias;
			}
			em.setWeights(initialWeights);
			if (opts.useStandardMultinomialMStep) {
				((EMGenSequenceModel) em).initializeStandardMultinomialPotentials(opts.initialWeightsLower, opts.initialWeightsUpper, rands[opts.randSeedIndex]);
				((EMGenSequenceModel) em).standardMultinomialEStep();
			} else {
				em.EStep();
			}
			double margProb = em.calculateRegularizedLogMarginalLikelihood();
			guessLabels = em.getForwardBackward().posteriorDecode();
			LogInfo.logss("Initial log marginal prob: %.2f", margProb);
			evaluateCurrentScore(curveOut, em.getWeights(), guessLabels, margProb, 0);
			double prevMargProb = margProb;
			for (int iter=0; iter<opts.iters; ++iter) {
				if (opts.useStandardMultinomialMStep) {
					((EMGenSequenceModel) em).standardMultinomialMStep(opts.standardMStepCountSmoothing);
					((EMGenSequenceModel) em).standardMultinomialEStep();
				} else {
					em.MStep();
					em.EStep();
				}
				margProb = em.calculateRegularizedLogMarginalLikelihood();
				guessLabels = em.getForwardBackward().posteriorDecode();
				LogInfo.logss("Iter %d log marginal prob: %.2f", iter, margProb);
				evaluateCurrentScore(curveOut, em.getWeights(), guessLabels, margProb, iter+1);
				if (margProb < prevMargProb - 10e-8) {
					LogInfo.error("Marginal probability not monotonic increasing!");
				}
				prevMargProb = margProb;
			}
			guessLabels = em.getForwardBackward().posteriorDecode();
		} else {
			// Do gradient ascent
			GradientSequenceModel grad = null;
			if (!opts.useGlobal) {
				grad = new GradientGenSequenceModel(observations, opts.numLabels, indexToWord.size());
			} else {
				grad = new GradientGlobalSequenceModel(observations, opts.numLabels, indexToWord.size());
			}
			// Cache active features
			activeTransFeatures = getActiveTransFeatures(transFeatures, grad.getNumObservations(), grad.getNumLabels(), grad.getStartLabel(), grad.getStopLabel());
			activeEmitFeatures = getActiveEmitFeatures(emitFeatures, grad.getNumObservations(), grad.getNumLabels(), grad.getStartLabel(), grad.getStopLabel());
			LogInfo.logss("Num features: %d", indexToFeature.size());
			double[] regularizationWeights = getRegularizationWeights(transFeatures, emitFeatures);
			double[] regularizationBiases = getRegularizationBiases(transFeatures, emitFeatures, grad.getNumLabels(), grad.getStartLabel(), grad.getStopLabel());
			grad.setActiveFeatures(activeTransFeatures, activeEmitFeatures, indexToFeature.size(), regularizationWeights, regularizationBiases);
			// Initialize weights
			double[] initialWeights = uniformRandomWeights(grad.getNumFeatures(), opts.initialWeightsLower, opts.initialWeightsUpper, rands[opts.randSeedIndex]);
			if (opts.useBiasFeature) {
				initialWeights[featureToIndex.get("bias")] = opts.biasFeatureBias;
			}
			grad.setWeights(initialWeights);
			grad.computePotentials();
			grad.getForwardBackward().compute();
			double margProb = grad.calculateRegularizedLogMarginalLikelihood();
			guessLabels = grad.getForwardBackward().posteriorDecode();
			LogInfo.logss("Initial log marginal prob: %.2f", margProb);
			evaluateCurrentScore(curveOut, grad.getWeights(), guessLabels, margProb, 0);
			LBFGSMinimizer minimizer = new LBFGSMinimizer();
			minimizer.setMaxIterations(opts.iters);
			minimizer.setMinIteratons(opts.iters);
			minimizer.setIterationCallbackFunction(new PrintLikelihoodCallback(curveOut, grad));
			minimizer.minimize(grad, grad.getWeights(), 1e-6, true);
			guessLabels = grad.getForwardBackward().posteriorDecode();
		}

		// Evaluate
		LogInfo.logss("Many-1 Score: %f", scoreLabelsManyToOne(goldLabels, guessLabels, indexToPOS.size(), opts.numLabels));
		LogInfo.logss("1-1 Score: %f", scoreLabelsOneToOne(goldLabels, guessLabels, indexToPOS.size(), opts.numLabels));
		
		int[][] randLabels = randomLabels(goldLabels, opts.numLabels, rands[opts.randSeedIndex]);
		LogInfo.logss("Rand many-1 Score: %f", scoreLabelsManyToOne(goldLabels, randLabels, indexToPOS.size(), opts.numLabels));
		LogInfo.logss("Rand 1-1 Score: %f", scoreLabelsOneToOne(goldLabels, randLabels, indexToPOS.size(), opts.numLabels));
	}
	
	private void evaluateCurrentScore(FileWriter curveOut, double[] weights, int[][] guessLabels, double margProb, int iter) {
		double manyToOneScore = scoreLabelsManyToOne(goldLabels, guessLabels, indexToPOS.size(), opts.numLabels);
		double oneToOneScore = scoreLabelsOneToOne(goldLabels, guessLabels, indexToPOS.size(), opts.numLabels);
		LogInfo.logss("1-1 Score: %f", oneToOneScore);
		LogInfo.logss("Many-1 Score: %f", manyToOneScore);
		if (curveOut != null) {
			try {
				curveOut.append(String.format("%d\t%f\t%f\t%f%n", iter, margProb, manyToOneScore, oneToOneScore));
				curveOut.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		printWeightsToFile(iter, weights);
		printTaggedSentencesToFile(iter, observations, guessLabels, goldLabels);
	}
	
	private void printWeightsToFile(int iter, double[] weights) {
		if (iter % opts.printRate == 0) {
			try {
				FileWriter w = new FileWriter(Execution.getFile("weights"+iter));
				Counter<String> weightsCounter = new Counter<String>();
				for (int f=0; f<indexToFeature.size(); ++f) {
					weightsCounter.setCount(indexToFeature.get(f), weights[f]);
				}
				List<String> sortedFeatures = weightsCounter.getSortedKeys();
				for (int f=0; f<sortedFeatures.size(); ++f) {
					w.write(String.format("%s\t%f\n", sortedFeatures.get(f), weightsCounter.getCount(sortedFeatures.get(f))));
					w.flush();
				}
				w.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void printTaggedSentencesToFile(int iter, int[][] sentences, int[][] guessLabels, int[][] goldLabels) {
		if (iter % opts.printRate == 0) {
			try {
				FileWriter w = new FileWriter(Execution.getFile("guess"+iter));
				for (int i=0; i<sentences.length; ++i) {
					for (int j=0; j<sentences[i].length; ++j) {
						w.write(String.format("%s|%s|%d ", indexToWord.get(sentences[i][j]), indexToPOS.get(goldLabels[i][j]), guessLabels[i][j]));
					}
					w.write("\n");
					w.flush();
				}
				w.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class PrintLikelihoodCallback implements CallbackFunction {
		
		private FileWriter writer;
		private GradientSequenceModel func;
		
		public PrintLikelihoodCallback(FileWriter writer0, GradientSequenceModel func0) {
			this.writer = writer0;
			this.func = func0;
		}

		public void callback(Object... args) {
			double margProb = -((Double) args[2]);
			int[][] guessLabels = func.getForwardBackward().posteriorDecode();
//			evaluateCurrentScore(writer, (double[]) args[0], guessLabels, margProb, func.getNumCalculates()-1);
			evaluateCurrentScore(writer, (double[]) args[0], guessLabels, margProb, ((Integer) args[1])+1);
			double[] grad = (double[]) args[3];
			double derivative = 0.0;
			for (int f=0; f<grad.length; ++f) {
				derivative += grad[f] * grad[f];
			}
			LogInfo.logss("Derivative: %f", derivative);
		}	
		
	}
	
	public static int[][] randomLabels(int[][] goldLabels, int numGuessLabels, Random rand) {
		int[][] guess = new int[goldLabels.length][];
		for (int s=0; s<goldLabels.length; ++s) {
			guess[s] = new int[goldLabels[s].length];
			for (int i=0; i<guess[s].length; ++i) {
				guess[s][i] = rand.nextInt(numGuessLabels);
			}
		}
		return guess;
	}
	
	public static double scoreLabelsManyToOne(int[][] goldLabels, int[][] guessLabels, int numGoldLabels, int numGuessLabels) {
		double totalInstances = 0.0;
		double[][] labelMapCounts = new double[numGuessLabels][numGoldLabels];
		for (int s=0; s<goldLabels.length; ++s) {
			for (int i=0; i<goldLabels[s].length; ++i) {
				labelMapCounts[guessLabels[s][i]][goldLabels[s][i]]++;
				totalInstances++;
			}
		}
		
		// Do mapping
		double greedyCorrectInstances = 0.0;
		for (int l0=0; l0<numGuessLabels; ++l0) {
			double bestScore = Double.NEGATIVE_INFINITY;
			for (int l1=0; l1<numGoldLabels; ++l1) {
				bestScore = Math.max(bestScore, labelMapCounts[l0][l1]);
			}
			greedyCorrectInstances += bestScore;
		}
		
		return greedyCorrectInstances/totalInstances;
	}
	
	public static double scoreLabelsOneToOne(int[][] goldLabels, int[][] guessLabels, int numGoldLabels, int numGuessLabels) {
		double totalInstances = 0.0;
		double[][] labelMapCounts = new double[numGuessLabels][numGoldLabels];
		for (int s=0; s<goldLabels.length; ++s) {
			for (int i=0; i<goldLabels[s].length; ++i) {
				labelMapCounts[guessLabels[s][i]][goldLabels[s][i]]++;
				totalInstances++;
			}
		}
		
		// Do bipartite matching
		BipartiteMatchings matcher = new BipartiteMatchings();
		double[][] negLabelMapCounts = new double[numGuessLabels][numGoldLabels];
		for (int s0=0; s0< numGuessLabels; ++s0) {
			for (int s1=0; s1<numGoldLabels; ++s1) {
				negLabelMapCounts[s0][s1] = -labelMapCounts[s0][s1];
			}
		}
		int[] matching = matcher.getMaxMatching(negLabelMapCounts);
		double bipartite = matcher.getMatchingCost(labelMapCounts, matching);
		
		// Do greedy mapping
		boolean[] guessMapped = new boolean[numGuessLabels];
		for (int i=0; i<numGuessLabels; ++i) guessMapped[i] = false;
		boolean[] goldMapped = new boolean[numGoldLabels];
		for (int i=0; i<numGoldLabels; ++i) goldMapped[i] = false;
		int guessRemaining = numGuessLabels;
		int goldRemaining = numGoldLabels;
		
		double greedy = 0.0;
		while (guessRemaining > 0 && goldRemaining > 0) {
			double bestCount = Double.NEGATIVE_INFINITY;
			int bestGuess = 0;
			int bestGold = 0;
			for (int guess=0; guess<numGuessLabels; ++guess) {
				if (!guessMapped[guess]) {
					for (int gold=0; gold<numGoldLabels; ++gold) {
						if (!goldMapped[gold]) {
							if (labelMapCounts[guess][gold] > bestCount) {
								bestCount = labelMapCounts[guess][gold];
								bestGuess = guess;
								bestGold = gold;
							}
						}
					}
				}
			}
			guessMapped[bestGuess] = true;
			goldMapped[bestGold] = true;
			greedy += bestCount;
			guessRemaining--;
			goldRemaining--;
		}
		
		LogInfo.logss("Greedy %f", greedy / totalInstances);
		
		return bipartite / totalInstances;
	}
	
	public List<Pair<Integer,Double>>[][] getActiveTransFeatures(List<TransFeatureTemplate> templates, int numObservations, int numLabels, int startLabel, int stopLabel) {
		List<Pair<Integer,Double>>[][] activeFeatures = new List[numLabels][numLabels];
		for (int s0=0; s0<numLabels; ++s0) {
			if (s0 != stopLabel) {
				for (int s1=0; s1<numLabels; ++s1) {
					if (s1 != startLabel) {
						activeFeatures[s0][s1] = new ArrayList<Pair<Integer,Double>>();
						for (TransFeatureTemplate template : templates) {
							List<Pair<String, Double>> features = template.getFeatures(s0, s1);
							for (Pair<String, Double> feature : features) {
								int index = indexString(feature.getFirst(), indexToFeature, featureToIndex);
								if (index >= featureIndexCounts.size()) {
									featureIndexCounts.add(1);
								} else {
									featureIndexCounts.set(index, featureIndexCounts.get(index)+1);
								}
								activeFeatures[s0][s1].add(Pair.makePair(index, feature.getSecond()));
							}
						}
					}
				}
			}
		}
		return activeFeatures;
	}

	public List<Pair<Integer,Double>>[][] getActiveEmitFeatures(List<EmitFeatureTemplate> templates, int numObservations, int numLabels, int startLabel, int stopLabel) {
		List<Pair<Integer,Double>>[][] activeFeatures = new List[numLabels][numObservations];
		for (int s=0; s<numLabels; ++s) {
			if (s != startLabel && s != stopLabel) {
				for (int i=0; i<numObservations; ++i) {
					activeFeatures[s][i] = new ArrayList<Pair<Integer,Double>>();
					for (EmitFeatureTemplate template : templates) {
						List<Pair<String, Double>> features = template.getFeatures(s, indexToWord.get(i));
						for (Pair<String, Double> feature : features) {
							int index = indexString(feature.getFirst(), indexToFeature, featureToIndex);
							if (index >= featureIndexCounts.size()) {
								featureIndexCounts.add(1);
							} else {
								featureIndexCounts.set(index, featureIndexCounts.get(index)+1);
							}
							activeFeatures[s][i].add(Pair.makePair(index, feature.getSecond()));
						}
					}
				}
			}
		}
		return activeFeatures;
	}
	
	public double[] getRegularizationWeights(List<TransFeatureTemplate> transFeatures, List<EmitFeatureTemplate> emitFeatures) {
		double[] regularizationWeights = new double[indexToFeature.size()];
		for (int f = 0; f<indexToFeature.size(); ++f) {
			if (indexToFeature.get(f).startsWith("bias")) {
				regularizationWeights[f] = opts.biasFeatureRegularizationWeight;
			} else {
				regularizationWeights[f] = opts.regularizationWeight;
			}
		}
		return regularizationWeights;
	}

	public double[] getRegularizationBiases(List<TransFeatureTemplate> transFeatures, List<EmitFeatureTemplate> emitFeatures, int numLabels, int startLabel, int stopLabel) {
		double[] regularizationBiases = new double[indexToFeature.size()];
		for (int f = 0; f<indexToFeature.size(); ++f) {
			if (indexToFeature.get(f).startsWith("bias")) {
				regularizationBiases[f] = opts.biasFeatureBias;
			} else {
				regularizationBiases[f] = opts.regularizationBias;
			}
		}
		return regularizationBiases;
	}
	
	public Pair<int[][], int[][]> getObservationsAndGoldLabels(Collection<Pair<List<String>, List<String>>> sequences) {
		int[][] observations = new int[sequences.size()][];
		int[][] goldLabels = new int[sequences.size()][];
		int i=0;
		for (Pair<List<String>, List<String>> sequence : sequences) {
			observations[i] = new int[sequence.getFirst().size()];
			for (int j=0; j<sequence.getFirst().size(); ++j) {
				observations[i][j] = indexString(sequence.getFirst().get(j), indexToWord, wordToIndex);
			}
			goldLabels[i] = new int[sequence.getSecond().size()];
			for (int j=0; j<sequence.getSecond().size(); ++j) {
				goldLabels[i][j] = indexString(sequence.getSecond().get(j), indexToPOS, posToIndex);
			}
			++i;
		}
		return Pair.makePair(observations, goldLabels);
	}
	
	public static int indexString(String name, ArrayList<String> indexToName, Map<String, Integer> nameToIndex) {
		Integer index = nameToIndex.get(name);
		if (index == null) {
			index = indexToName.size();
			nameToIndex.put(name, index);
			indexToName.add(name);
		}
		return index;
	}
	
	public static double[] uniformRandomWeights(int dim, double lower, double upper, Random rand) {
		double range = upper - lower;
		double[] weights = new double[dim];
		for (int i=0; i<dim; ++i) {
			double randVal = rand.nextDouble();
			weights[i] = lower + (range*randVal);
		}
		return weights;
	}
	
}
