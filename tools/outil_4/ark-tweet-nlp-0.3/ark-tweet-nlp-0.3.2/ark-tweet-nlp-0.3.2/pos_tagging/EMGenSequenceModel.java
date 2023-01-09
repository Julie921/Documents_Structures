package pos_tagging;

import java.util.List;
import java.util.Random;

import util.LBFGSMinimizer;

import edu.berkeley.nlp.math.CachingDifferentiableFunction;
import fig.basic.LogInfo;
import fig.basic.Pair;

public class EMGenSequenceModel implements EMSequenceModel {

	private static boolean PRINT_MINIMIZER = false;
	private static final int MAX_MINIMIZER_ITERS = 500;
	private static final double MINIMIZER_TOL = 1e-6;
	
	private int numLabels;
	private int numObservations;
	private int[][] observations;
	private int startLabel;
	private int stopLabel;
	private double[][] transProbs;
	private double[][] emitProbs;
	private double[] weights;
	private double[] regularizationWeights;
	private double[] regularizationBiases;
	private List<Pair<Integer,Double>>[][] activeTransFeatures;
	private List<Pair<Integer,Double>>[][] activeEmitFeatures;
	private int numFeatures;
	public ForwardBackwardGen forwardBackward;
	
	public EMGenSequenceModel(int[][] observations0, int numLabels0, int numObservations0) {
		this.numLabels = numLabels0+2;
		this.numObservations = numObservations0;
		this.observations = observations0;
		this.transProbs = new double[numLabels][numLabels];
		this.emitProbs = new double[numLabels][numObservations];
		this.weights = new double[numFeatures];
		this.regularizationWeights = new double[numFeatures];
		this.regularizationBiases = new double[numFeatures];
		this.stopLabel = numLabels-2;
		this.startLabel = numLabels-1;
		this.forwardBackward = new ForwardBackwardGen(observations, numLabels0, numObservations, transProbs, emitProbs);
	}
	
	public void setActiveFeatures(List<Pair<Integer,Double>>[][] activeTransFeatures0, List<Pair<Integer,Double>>[][] activeEmitFeatures0, int numFeatures0, double[] regularizationWeights0, double[] regularizationBiases0) {
		this.numFeatures = numFeatures0;
		this.activeTransFeatures = activeTransFeatures0;
		this.activeEmitFeatures = activeEmitFeatures0;
		this.regularizationWeights = regularizationWeights0;
		this.regularizationBiases = regularizationBiases0;
	}
	
	public ForwardBackward getForwardBackward() {
		return forwardBackward;
	}
	
	public double[][] getTransPotentials() {
		return transProbs;
	}
	
	public double[][] getEmitPotentials() {
		return emitProbs;
	}
	
	public double[] getWeights() {
		return weights;
	}
	
	public int getNumFeatures() {
		return numFeatures;
	}
	
	public int getNumLabels() {
		return numLabels;
	}
	
	public int getNumObservations() {
		return numObservations;
	}
	
	public int getStartLabel() {
		return startLabel;
	}
	
	public int getStopLabel() {
		return stopLabel;
	}
	
	public void setWeights(double[] weights0) {
		this.weights = weights0;
	}
	
	public void computePotentials() {
		// Trans probs
		for (int l0=0; l0<numLabels; ++l0) {
			if (l0 != stopLabel) {
				double norm = 0.0;
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transProbs[l0][l1] = Math.exp(computeScore(weights, activeTransFeatures[l0][l1]));
						norm += transProbs[l0][l1];
					}
				}
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transProbs[l0][l1] /= norm;
					}
				}
			}
		}

		// Emit probs
		for (int l=0; l<numLabels; ++l) {
			if (l != startLabel && l != stopLabel) {
				double norm = 0.0;
				for (int i=0; i<numObservations; ++i) {
					emitProbs[l][i] = Math.exp(computeScore(weights, activeEmitFeatures[l][i]));
					norm += emitProbs[l][i];
				}
				for (int i=0; i<numObservations; ++i) {
					emitProbs[l][i] /= norm;
				}
			}
		}
	}
	
	private static double computeScore(double[] weights, List<Pair<Integer,Double>> activeFeatures) {
		double score = 0.0;
		for (int i=0; i<activeFeatures.size(); ++i) {
			Pair<Integer,Double> feat = activeFeatures.get(i);
			score += weights[feat.getFirst()] * feat.getSecond();
		}
		return score;
	}
	
	public void initializeStandardMultinomialPotentials(double lower, double upper, Random rand) {
		// Trans probs
		for (int l0=0; l0<numLabels; ++l0) {
			if (l0 != stopLabel) {
				double norm = 0.0;
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transProbs[l0][l1] = lower + (upper - lower)*rand.nextDouble();
						norm += transProbs[l0][l1];
					}
				}
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transProbs[l0][l1] /= norm;
					}
				}
			}
		}

		// Emit probs
		for (int l=0; l<numLabels; ++l) {
			if (l != startLabel && l != stopLabel) {
				double norm = 0.0;
				for (int i=0; i<numObservations; ++i) {
					emitProbs[l][i] = lower + (upper - lower)*rand.nextDouble();
					norm += emitProbs[l][i];
				}
				for (int i=0; i<numObservations; ++i) {
					emitProbs[l][i] /= norm;
				}
			}
		}
	}
	
	public void standardMultinomialEStep() {
		forwardBackward.compute();
	}
	
	public void standardMultinomialMStep(double smoothing) {
		double[][] expectedTransCounts = forwardBackward.getConditionalExpectedTransCounts();
		double[][] expectedEmitCounts = forwardBackward.getConditionalExpectedEmitCounts();
		// Trans probs
		for (int l0=0; l0<numLabels; ++l0) {
			if (l0 != stopLabel) {
				double norm = 0.0;
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transProbs[l0][l1] = expectedTransCounts[l0][l1] + smoothing;
						norm += transProbs[l0][l1];
					}
				}
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						if (norm != 0) {
							transProbs[l0][l1] /= norm;
						} else {
							transProbs[l0][l1] = 0.0;
						}
					}
				}
			}
		}
		
		// Emit probs
		for (int l=0; l<numLabels; ++l) {
			if (l != startLabel && l != stopLabel) {
				double norm = 0.0;
				for (int i=0; i<numObservations; ++i) {
					emitProbs[l][i] = expectedEmitCounts[l][i] + smoothing;
					norm += emitProbs[l][i];
				}
				for (int i=0; i<numObservations; ++i) {
					if (norm != 0) {
						emitProbs[l][i] /= norm;
					} else {
						emitProbs[l][i] = 0.0;
					}
				}
			}
		}
	}

	public void EStep() {
		computePotentials();
		forwardBackward.compute();
	}
	
	
	public void MStep() {
		NegativeRegularizedExpectedLogLikelihood negativeLikelihood = new NegativeRegularizedExpectedLogLikelihood();
		LBFGSMinimizer minimizer = new LBFGSMinimizer();
		minimizer.setMaxIterations(MAX_MINIMIZER_ITERS);
		minimizer.minimize(negativeLikelihood, weights, MINIMIZER_TOL, PRINT_MINIMIZER);
	}
	
	public double calculateRegularizedLogMarginalLikelihood() {
		return forwardBackward.getMarginalLogLikelihood() - calculateRegularizer();
	}
	
	public double calculateRegularizedExpectedLogLikelihood() {
		// Don't recalculate expected counts, just get them
		double[][] expectedTransCounts = forwardBackward.getConditionalExpectedTransCounts();
		double[][] expectedEmitCounts = forwardBackward.getConditionalExpectedEmitCounts();
		
		// Calculate expected log likelihood
		double expectedLogLikelihood = 0.0;
		for (int s0=0; s0<numLabels; ++s0) {
			if (s0 != stopLabel) {
				for (int s1=0; s1<numLabels; ++s1) {
					if (s1 != startLabel) {
						expectedLogLikelihood += expectedTransCounts[s0][s1]*Math.log(transProbs[s0][s1]);
					}
				}
			}
		}
		for (int s=0; s<numLabels; ++s) {
			if (s != startLabel && s != stopLabel) {
				for (int i=0; i<numObservations; ++i) {
					expectedLogLikelihood += expectedEmitCounts[s][i]*Math.log(emitProbs[s][i]);
				}
			}
		}
		
		return expectedLogLikelihood-calculateRegularizer();
	}
	
	public double calculateRegularizer() {
		double result = 0.0;
		for (int f=0; f<numFeatures; ++f) {
			result += regularizationWeights[f]*(weights[f] - regularizationBiases[f])*(weights[f] - regularizationBiases[f]);
		}
		return result;
	}
	
	private class NegativeRegularizedExpectedLogLikelihood extends CachingDifferentiableFunction {
		
		protected Pair<Double, double[]> calculate(double[] x) {
			setWeights(x);
			computePotentials();
			
			// Don't recalculate expected counts, just get them
			double[][] expectedTransCounts = forwardBackward.getConditionalExpectedTransCounts();
			double[][] expectedEmitCounts = forwardBackward.getConditionalExpectedEmitCounts();
			double[] expectedLabelCounts = forwardBackward.getConditionalExpectedLabelCounts();
		
			double negativeRegularizedExpectedLogLikelihood = -calculateRegularizedExpectedLogLikelihood();
			
			// Calculate gradient
			double[] gradient = new double[weights.length];

			// Gradient of trans weights
			for (int s0=0; s0<numLabels; ++s0) {
				if (s0 != stopLabel) {
					for (int s1=0; s1<numLabels; ++s1) {
						if (s1 != startLabel) {
							for (int f=0; f<activeTransFeatures[s0][s1].size(); ++f) {
								Pair<Integer,Double> feat = activeTransFeatures[s0][s1].get(f);
								gradient[feat.getFirst()] -= expectedTransCounts[s0][s1]*feat.getSecond();
								gradient[feat.getFirst()] -= -expectedLabelCounts[s0]*transProbs[s0][s1]*feat.getSecond();
							}
						}
					}
				}
			}

			// Gradient of emit weights
			for (int s=0; s<numLabels; ++s) {
				if (s != startLabel && s != stopLabel) {
					for (int i=0; i<numObservations; ++i) {
						for (int f=0; f<activeEmitFeatures[s][i].size(); ++f) {
							Pair<Integer,Double> feat = activeEmitFeatures[s][i].get(f);
							gradient[feat.getFirst()] -= expectedEmitCounts[s][i]*feat.getSecond();
							gradient[feat.getFirst()] -= -expectedLabelCounts[s]*emitProbs[s][i]*feat.getSecond();
						}
					}
				}
			}
			
			// Add gradient of regularizer
			for (int f=0; f<numFeatures; ++f) {
				gradient[f] += 2.0*regularizationWeights[f]*(weights[f] - regularizationBiases[f]);
			}
			
			return Pair.makePair(negativeRegularizedExpectedLogLikelihood, gradient);
		}

		public int dimension() {
			return numFeatures;
		}
	
	}
	
}
