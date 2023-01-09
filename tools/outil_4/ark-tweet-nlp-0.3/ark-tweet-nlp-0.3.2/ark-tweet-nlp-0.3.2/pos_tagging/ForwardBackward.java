package pos_tagging;

public interface ForwardBackward {

	public abstract int[][] posteriorDecode();

	public abstract double getMarginalLogLikelihood();

	public abstract void compute();

	public abstract double[][] getConditionalExpectedTransCounts();

	public abstract double[] getConditionalExpectedLabelCounts();

	public abstract double[][] getConditionalExpectedEmitCounts();

}