package optimize;

import util.LineSearcher;
import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.math.GradientMinimizer;
import edu.berkeley.nlp.util.CallbackFunction;
import fig.basic.LogInfo;

public class GradientDescentMinimizer implements GradientMinimizer {

	int minIterations = -1;
	double initialShrinkStepSizeMultiplier = 0.01;
	double shrinkStepSizeMultiplier = .5;
	double growStepSizeMultiplier = 1.1;
	double EPS = 1e-10;
	int maxIterations = 2000;
	transient CallbackFunction iterCallbackFunction = null;

	public void setIterationCallbackFunction(CallbackFunction callbackFunction) {
		this.iterCallbackFunction = callbackFunction;
	}

	public void setMinIteratons(int minIterations) {
		this.minIterations = minIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public void setInitialStepSizeMultiplier(double initialStepSizeMultiplier) {
		this.initialShrinkStepSizeMultiplier = initialStepSizeMultiplier;
	}

	public void setStepSizeMultiplier(double stepSizeMultiplier) {
		this.shrinkStepSizeMultiplier = stepSizeMultiplier;
	}
	
	public void setStepSizeGrowMultiplier(double growStepSizeMultiplier) {
		this.growStepSizeMultiplier = growStepSizeMultiplier;
	}

	public double[] minimize(DifferentiableFunction function, double[] initial, double tolerance){
		 return minimize(function, initial, tolerance, false);
	}
	
	public double[] minimize(DifferentiableFunction function, double[] initial,
			double tolerance, boolean printProgress) {
		LineSearcher lineSearcher = new LineSearcher();
		lineSearcher.growStepSizeMultiplier = growStepSizeMultiplier;
		lineSearcher.stepSize = 1.0;
		double[] guess = DoubleArrays.clone(initial);
		for (int iteration = 0; iteration < maxIterations; iteration++) {
			double[] gradient = function.derivativeAt(guess);
			double value = function.valueAt(guess);
			double[] direction = gradient;
			DoubleArrays.scale(direction, -1.0);
			if (iteration == 0) lineSearcher.shrinkStepSizeMultiplier = initialShrinkStepSizeMultiplier;
			else lineSearcher.shrinkStepSizeMultiplier = shrinkStepSizeMultiplier;
			double[] nextGuess = lineSearcher.minimize(function, guess, direction);
			double[] nextDerivative = function.derivativeAt(nextGuess);
			double nextValue = function.valueAt(nextGuess);
			if (printProgress) {
				LogInfo.logss("[Gradient] Iteration %d: %.6f", iteration, nextValue);
			}

			if (iteration >= minIterations
					&& converged(value, nextValue, tolerance)) {
				return nextGuess;
			}
			guess = nextGuess;
			value = nextValue;
			gradient = nextDerivative;
			if (iterCallbackFunction != null) {
				iterCallbackFunction.callback(guess, iteration,value, gradient);
			}
		}
		return guess;
	}

	private boolean converged(double value, double nextValue, double tolerance) {
		if (value == nextValue) return true;
		double valueChange = Math.abs(nextValue - value);
		double valueAverage = Math.abs(nextValue + value + EPS) / 2.0;
		if (valueChange / valueAverage < tolerance) return true;
		return false;
	}

}
