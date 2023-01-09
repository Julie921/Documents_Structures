package pos_tagging;

import util.StationaryForwardBackward;
import edu.berkeley.nlp.math.SloppyMath;
import edu.berkeley.nlp.sequence.stationary.StationarySequenceInstance;
import edu.berkeley.nlp.sequence.stationary.StationarySequenceModel;
import fig.basic.LogInfo;
import fig.basic.NumUtils;

public class ForwardBackwardGlobal implements ForwardBackward {

	private static final int JOINT_COMPUTE_LENGTH = 200;
	private static final double JOINT_DIVERGE_THRESH = Double.POSITIVE_INFINITY;

	private int numLabels;
	private int numObservations;
	private int[][] observations;
	private int startLabel;
	private int stopLabel;
	private double[][] transPotentials;
	private double[][] emitPotentials;
	private double[] condExpectedLabelCounts;
	private double[][] condExpectedTransCounts;
	private double[][] condExpectedEmitCounts;
	private double[][] jointExpectedTransCounts;
	private double[][] jointExpectedEmitCounts;
	private int[][] posteriorDecoding;
	private double marginalLogLikelihood;
	private double globalLogNormalizationConstant;
	private boolean diverged = false;

	private class SequenceModel implements StationarySequenceModel {

		private int maxSeqLength;
		private int[][] allowableBackwardTrans;
		private int[][] allowableForwardTrans;
		private double[][] backwardEdgePotentials;
		private double[][] forwardEdgePotentials;

		public SequenceModel() {
			maxSeqLength=0;
			for (int s=0; s<observations.length; ++s) {
				maxSeqLength = Math.max(observations[s].length, maxSeqLength);
			}

			allowableBackwardTrans = new int[numLabels-2][numLabels-2];
			allowableForwardTrans = new int[numLabels-2][numLabels-2];
			for(int l0=0; l0<numLabels-2; ++l0) {
				for(int l1=0; l1<numLabels-2; ++l1) {
					allowableBackwardTrans[l0][l1] = l1;
					allowableForwardTrans[l0][l1] = l1;
				}
			}

			backwardEdgePotentials = new double[numLabels-2][numLabels-2];
			forwardEdgePotentials = new double[numLabels-2][numLabels-2];
			for (int l0=0; l0<numLabels-2; ++l0) {
				for (int l1=0; l1<numLabels-2; ++l1) {
					backwardEdgePotentials[l1][l0] = transPotentials[l0][l1];
					forwardEdgePotentials[l0][l1] = transPotentials[l0][l1];
				}
			}
		}

		public int[][] getAllowableBackwardTransitions() {
			return allowableBackwardTrans;
		}

		public int[][] getAllowableForwardTransitions() {
			return allowableForwardTrans;
		}

		public double[][] getBackwardEdgePotentials() {
			return backwardEdgePotentials;
		}

		public double[][] getForwardEdgePotentials() {
			return forwardEdgePotentials;
		}

		public int getMaximumSequenceLength() {
			return maxSeqLength;
		}

		public int getNumStates() {
			return numLabels-2;
		}

	}

	private class SequenceInstance implements StationarySequenceInstance {

		private int[] observationSequence;

		public SequenceInstance(int[] observationSequence) {
			this.observationSequence = observationSequence;
		}

		public void fillNodePotentials(double[][] potentials) {
			for (int i=0; i<observationSequence.length; ++i) {
				for (int l=0; l<numLabels-2; ++l) {
					if (i == 0) {
						potentials[i][l] = emitPotentials[l][observationSequence[i]] * transPotentials[startLabel][l];
					} else if (i == observationSequence.length-1) {
						potentials[i][l] = emitPotentials[l][observationSequence[i]] * transPotentials[l][stopLabel];
					} else {
						potentials[i][l] = emitPotentials[l][observationSequence[i]];
					}
				}
			}
		}

		public int getSequenceLength() {
			return observationSequence.length;
		}

	}

	public ForwardBackwardGlobal(int[][] observations0, int numLabels0, int numObservations0, double[][] transProbs0, double[][] emitProbs0) {
		this.numLabels = numLabels0+2;
		this.numObservations = numObservations0;
		this.observations = observations0;
		this.transPotentials = transProbs0;
		this.emitPotentials = emitProbs0;
		this.startLabel = numLabels-1;
		this.stopLabel = numLabels-2;
		this.condExpectedLabelCounts = new double[numLabels];
		this.condExpectedTransCounts = new double[numLabels][numLabels];
		this.condExpectedEmitCounts = new double[numLabels][numObservations];
		this.posteriorDecoding = new int[observations.length][];
	}
	
	public boolean diverged() {
		return diverged;
	}

	public double getGlobalLogNormalizationConstant() {
		return globalLogNormalizationConstant;
	}
	
	public int[][] posteriorDecode() {
		return posteriorDecoding;
	}

	public double getMarginalLogLikelihood() {
		return marginalLogLikelihood;
	}
	
	public boolean computeJointExpectAndGlobalNormConst() {
		globalLogNormalizationConstant = Double.NEGATIVE_INFINITY;
		jointExpectedTransCounts = new double[numLabels][numLabels];
		for (int l0=0; l0<numLabels; ++l0) {
			for (int l1=0; l1<numLabels; ++l1) {
				jointExpectedTransCounts[l0][l1] = Double.NEGATIVE_INFINITY;
			}			
		}
		jointExpectedEmitCounts = new double[numLabels][numObservations];
		for (int l=0; l<numLabels; ++l) {
			for (int i=0; i<numObservations; ++i) {
				jointExpectedEmitCounts[l][i] = Double.NEGATIVE_INFINITY;
			}
		}
		
		// Calculate joint alphas and betas
		double[] logEmitPotentialSums = new double[numLabels];
		for (int l=0; l<numLabels; ++l) {
			if (l != startLabel && l != stopLabel) {
				logEmitPotentialSums[l] = Double.NEGATIVE_INFINITY;
				for (int e=0; e<numObservations; ++e) {
					logEmitPotentialSums[l] = SloppyMath.logAdd(logEmitPotentialSums[l], Math.log(emitPotentials[l][e]));
				}
			}
		}
		double[][] alphas = new double[JOINT_COMPUTE_LENGTH][numLabels-2];
		double[][] betas = new double[JOINT_COMPUTE_LENGTH][numLabels-2];
		for (int t=0; t<JOINT_COMPUTE_LENGTH; ++t) {
			for (int l=0; l<numLabels-2; ++l) {
				alphas[t][l] = Double.NEGATIVE_INFINITY;
				betas[t][l] = Double.NEGATIVE_INFINITY;
			}
		}
		// Alphas
		for (int l=0; l<numLabels-2; ++l) {
			alphas[0][l] = Math.log(transPotentials[startLabel][l]) + logEmitPotentialSums[l];
		}
		for (int t=1; t<JOINT_COMPUTE_LENGTH; ++t) {
			for (int l1=0; l1<numLabels-2; ++l1) {
				for (int l0=0; l0<numLabels-2; ++l0) {
					alphas[t][l1] = SloppyMath.logAdd(alphas[t][l1], alphas[t-1][l0] + Math.log(transPotentials[l0][l1]) + logEmitPotentialSums[l1]);
				}
			}
		}
		// Betas
		for (int l=0; l<numLabels-2; ++l) {
			betas[JOINT_COMPUTE_LENGTH-1][l] = Math.log(transPotentials[l][stopLabel]);
		}
		for (int t=JOINT_COMPUTE_LENGTH-2; t>=0; --t) {
			for (int l0=0; l0<numLabels-2; ++l0) {
				for (int l1=0; l1<numLabels-2; ++l1) {
					betas[t][l0] = SloppyMath.logAdd(betas[t][l0], Math.log(transPotentials[l0][l1]) + logEmitPotentialSums[l1] + betas[t+1][l1]);
				}
			}
		}
		
		// Calculate global normalization constant
		globalLogNormalizationConstant = NumUtils.logAdd(globalLogNormalizationConstant, Math.log(transPotentials[startLabel][stopLabel]));
		double diff = 0.0;
		for (int len=1; len<=JOINT_COMPUTE_LENGTH; len++) {
			double prev = globalLogNormalizationConstant;
			for (int l=0; l<numLabels-2; l++) {
				globalLogNormalizationConstant = NumUtils.logAdd(globalLogNormalizationConstant, alphas[0][l] + betas[JOINT_COMPUTE_LENGTH-len][l]);
			}
			diff = Math.abs(globalLogNormalizationConstant - prev);
		}
		
//		LogInfo.logss("Global norm const: %f", globalLogNormalizationConstant);
		
		if (diff > JOINT_DIVERGE_THRESH || Double.isNaN(globalLogNormalizationConstant) || Double.isInfinite(globalLogNormalizationConstant)) {
			// The normalization constant blew up, we're done here, try again line searcher
			return false;
		}
		
		// Calculate trans expectations
		// Length 0
		jointExpectedTransCounts[startLabel][stopLabel] = SloppyMath.logAdd(jointExpectedTransCounts[startLabel][stopLabel],  Math.log(transPotentials[startLabel][stopLabel]));
		// Length 1 to JOINT_COMPUTE_LENGTH
		for (int len=1; len<=JOINT_COMPUTE_LENGTH; ++len) {
			for (int l=0; l<numLabels-2; ++l) {
				jointExpectedTransCounts[startLabel][l] = SloppyMath.logAdd(jointExpectedTransCounts[startLabel][l], alphas[0][l] + betas[JOINT_COMPUTE_LENGTH-len][l]);
			}
			for (int t=0; t<len-1; t++) {
				for (int l0=0; l0<numLabels-2; ++l0) {
					for (int l1=0; l1<numLabels-2; ++l1) {
						jointExpectedTransCounts[l0][l1] = SloppyMath.logAdd(jointExpectedTransCounts[l0][l1], alphas[t][l0] + Math.log(transPotentials[l0][l1]) + logEmitPotentialSums[l1] + betas[JOINT_COMPUTE_LENGTH-len+t+1][l1]);
					}					
				}
			}
			for (int l=0; l<numLabels-2; ++l) {
				jointExpectedTransCounts[l][stopLabel] = SloppyMath.logAdd(jointExpectedTransCounts[l][stopLabel], alphas[len-1][l] + betas[JOINT_COMPUTE_LENGTH-1][l]);
			}
		}
		
		for (int l0=0; l0<numLabels; ++l0) {
			for (int l1=0; l1<numLabels; ++l1) {
				jointExpectedTransCounts[l0][l1] = observations.length * Math.exp(jointExpectedTransCounts[l0][l1] - globalLogNormalizationConstant);
			}			
		}
		
		// Calculate emit expectations
		// Length 1 to JOINT_COMPUTE_LENGTH
		for (int l=0; l<numLabels-2; ++l) {
			if (l != startLabel && l != stopLabel) {
				double jointLabelExpectation = Double.NEGATIVE_INFINITY;
				for (int len=1; len<=JOINT_COMPUTE_LENGTH; ++len) {
					for (int t=0; t<len; ++t) {
						jointLabelExpectation = SloppyMath.logAdd(jointLabelExpectation, alphas[t][l] + betas[JOINT_COMPUTE_LENGTH-len+t][l]);
					}
				}
				for (int i=0; i<numObservations; ++i) {
					jointExpectedEmitCounts[l][i] = jointLabelExpectation - logEmitPotentialSums[l] + Math.log(emitPotentials[l][i]);
				}
			}
		}
		
		for (int l=0; l<numLabels; ++l) {
			for (int i=0; i<numObservations; ++i) {
				jointExpectedEmitCounts[l][i] = observations.length * Math.exp(jointExpectedEmitCounts[l][i] - globalLogNormalizationConstant);
			}
		}
		
		// True because we didn't diverge
		return true;
	}
	
	public void compute() {
		diverged = !computeJointExpectAndGlobalNormConst();
		if (diverged == true) {
			// Weights are bad, try again line searcher
			marginalLogLikelihood = Double.NEGATIVE_INFINITY;
			return;
		}

		for (int s=0; s<numLabels; ++s) {
			condExpectedLabelCounts[s] = 0.0;
			for (int s0=0; s0<numLabels; ++s0) {
				condExpectedTransCounts[s][s0] = 0.0;
			}
			for (int e=0; e<numObservations; ++e) {
				condExpectedEmitCounts[s][e] = 0.0;
			}
		}
		posteriorDecoding = new int[observations.length][];
		marginalLogLikelihood = 0.0;

		SequenceModel seqModel = new SequenceModel();
		for (int s=0; s<observations.length; ++s) {
			if (observations[s].length == 0) {
				condExpectedLabelCounts[startLabel] += 1.0;
				condExpectedLabelCounts[stopLabel] += 1.0;
				condExpectedTransCounts[startLabel][stopLabel] += 1.0;
				posteriorDecoding[s] = new int[0];
				marginalLogLikelihood += Math.log(transPotentials[startLabel][stopLabel]) - globalLogNormalizationConstant;
				continue;
			}

			int[] observationSequence = observations[s];
			SequenceInstance seqInstance = new SequenceInstance(observationSequence);
			StationaryForwardBackward forwardBackward = new StationaryForwardBackward(seqModel);
			try {
				forwardBackward.setInput(seqInstance);
			} catch (RuntimeException e) {
				// If forward backward has problems, maybe it's because our weights are bad, try again line searcher
				diverged = true;
				LogInfo.logss(e.getMessage());
				marginalLogLikelihood = Double.NEGATIVE_INFINITY;
				return;
			}
			
			posteriorDecoding[s] = forwardBackward.nodePosteriorDecode();
			marginalLogLikelihood += forwardBackward.getLogNormalizationConstant() - globalLogNormalizationConstant;

			// Label counts and emission counts
			condExpectedLabelCounts[startLabel] += 1.0;
			for (int i=0; i<observationSequence.length; ++i) {
				for (int l=0; l<numLabels-2; ++l) {
					condExpectedLabelCounts[l] += forwardBackward.getNodeMarginals()[i][l];
					condExpectedEmitCounts[l][observationSequence[i]] += forwardBackward.getNodeMarginals()[i][l];
				}
			}
			condExpectedLabelCounts[stopLabel] += 1.0;

			// Trans counts
			for (int l=0; l<numLabels-2; ++l) {
				condExpectedTransCounts[startLabel][l] += forwardBackward.getNodeMarginals()[0][l];
			}
			for (int l0=0; l0<numLabels-2; ++l0) {
				for (int l1=0; l1<numLabels-2; ++l1) {
					condExpectedTransCounts[l0][l1] += forwardBackward.getEdgeMarginalSums()[l0][l1];
				}
			}
			for (int l=0; l<numLabels-2; ++l) {
				condExpectedTransCounts[l][stopLabel] += forwardBackward.getNodeMarginals()[observationSequence.length-1][l];
			}

		}
	}

	public double[][] getConditionalExpectedTransCounts() {
		return condExpectedTransCounts;
	}

	public double[] getConditionalExpectedLabelCounts() {
		return condExpectedLabelCounts;
	}

	public double[][] getConditionalExpectedEmitCounts() {
		return condExpectedEmitCounts;
	}

	public double[][] getJointExpectedTransCounts() {
		return jointExpectedTransCounts;
	}
	
	public double[][] getJointExpectedEmitCounts() {
		return jointExpectedEmitCounts;
	}

}