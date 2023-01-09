package pos_tagging;

import java.util.List;

import fig.basic.LogInfo;
import fig.basic.Pair;

public class GradientGlobalSequenceModel extends GradientSequenceModel {
	
	private int numCalculates;
	private int numLabels;
	private int numObservations;
	private int[][] observations;
	private int startLabel;
	private int stopLabel;
	private double[][] transPotentials;
	private double[][] emitPotentials;
	private double[] weights;
	private double[] regularizationWeights;
	private double[] regularizationBiases;
	private List<Pair<Integer,Double>>[][] activeTransFeatures;
	private List<Pair<Integer,Double>>[][] activeEmitFeatures;
	private int numFeatures;
	public ForwardBackwardGlobal forwardBackward;
	
	public GradientGlobalSequenceModel(int[][] observations0, int numLabels0, int numObservations0) {
		this.regularizationBiases = new double[numFeatures];
		this.regularizationWeights = new double[numFeatures];
		this.numCalculates = 0;
		this.numLabels = numLabels0+2;
		this.numObservations = numObservations0;
		this.observations = observations0;
		this.transPotentials = new double[numLabels][numLabels];
		this.emitPotentials = new double[numLabels][numObservations];
		this.weights = new double[numFeatures];
		this.stopLabel = numLabels-2;
		this.startLabel = numLabels-1;
		this.forwardBackward = new ForwardBackwardGlobal(observations, numLabels0, numObservations, transPotentials, emitPotentials);
	}
	
	public ForwardBackward getForwardBackward() {
		return forwardBackward;
	}
	
	public void resetNumCalculates() {
		this.numCalculates = 0;
	}
	
	public int getNumCalculates() {
		return numCalculates;
	}
	
	public void setActiveFeatures(List<Pair<Integer,Double>>[][] activeTransFeatures0, List<Pair<Integer,Double>>[][] activeEmitFeatures0, int numFeatures0, double[] regularizationWeights0, double[] regularizationBiases0) {
		this.regularizationWeights = regularizationWeights0;
		this.regularizationBiases = regularizationBiases0;
		this.numFeatures = numFeatures0;
		this.activeTransFeatures = activeTransFeatures0;
		this.activeEmitFeatures = activeEmitFeatures0;
	}
	
	public double[][] getTransPotentials() {
		return transPotentials;
	}
	
	public double[][] getEmitPotentials() {
		return emitPotentials;
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
		// Trans potent
		for (int l0=0; l0<numLabels; ++l0) {
			if (l0 != stopLabel) {
				for (int l1=0; l1<numLabels; ++l1) {
					if (l1 != startLabel) {
						transPotentials[l0][l1] = Math.exp(computeScore(weights, activeTransFeatures[l0][l1]));
					}
				}
			}
		}

		// Emit potent
		for (int l=0; l<numLabels; ++l) {
			if (l != startLabel && l != stopLabel) {
				for (int i=0; i<numObservations; ++i) {
					emitPotentials[l][i] = Math.exp(computeScore(weights, activeEmitFeatures[l][i]));
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
	
	public double calculateRegularizedLogMarginalLikelihood() {
		return forwardBackward.getMarginalLogLikelihood() - calculateRegularizer();
	}
	
	public double calculateRegularizer() {
		double result = 0.0;
		for (int f=0; f<numFeatures; ++f) {
			result += regularizationWeights[f]*(weights[f] - regularizationBiases[f])*(weights[f] - regularizationBiases[f]);
		}
		return result;
	}
	
	public Pair<Double, double[]> calculate(double[] x) {
		setWeights(x);
		computePotentials();
		forwardBackward.compute();
		double[][] condExpectedTransCounts = forwardBackward.getConditionalExpectedTransCounts();
		double[][] condExpectedEmitCounts = forwardBackward.getConditionalExpectedEmitCounts();
		double[][] jointExpectedTransCounts = forwardBackward.getJointExpectedTransCounts();
		double[][] jointExpectedEmitCounts = forwardBackward.getJointExpectedEmitCounts();

		double negativeRegularizedLogMarginalLikelihood = -calculateRegularizedLogMarginalLikelihood();
		LogInfo.logss("Calc %d log marginal prob: %.2f", numCalculates, -negativeRegularizedLogMarginalLikelihood);

		// Calculate gradient
		double[] gradient = new double[weights.length];

		// Gradient of trans weights
		for (int s0=0; s0<numLabels; ++s0) {
			if (s0 != stopLabel) {
				for (int s1=0; s1<numLabels; ++s1) {
					if (s1 != startLabel) {
						for (int f=0; f<activeTransFeatures[s0][s1].size(); ++f) {
							Pair<Integer,Double> feat = activeTransFeatures[s0][s1].get(f);
							gradient[feat.getFirst()] -= condExpectedTransCounts[s0][s1]*feat.getSecond();
							gradient[feat.getFirst()] -= -jointExpectedTransCounts[s0][s1]*feat.getSecond();
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
						gradient[feat.getFirst()] -= condExpectedEmitCounts[s][i]*feat.getSecond();
						gradient[feat.getFirst()] -= -jointExpectedEmitCounts[s][i]*feat.getSecond();
					}
				}
			}
		}
		
		// Add gradient of regularizer
		for (int f=0; f<numFeatures; ++f) {
			gradient[f] -= -2.0*regularizationWeights[f]*(weights[f] - regularizationBiases[f]);
		}

		++numCalculates;
		return Pair.makePair(negativeRegularizedLogMarginalLikelihood, gradient);
	}

	public int dimension() {
		return numFeatures;
	}
	
}
