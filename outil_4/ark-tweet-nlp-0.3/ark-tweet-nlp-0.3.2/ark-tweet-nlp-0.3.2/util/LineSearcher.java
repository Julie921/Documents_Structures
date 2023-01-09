package util;

import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.math.GradientLineSearcher;

/**
 */
public class LineSearcher implements GradientLineSearcher {
  private double EPS = 1e-10;
  public double shrinkStepSizeMultiplier = 0.5;//was 0.9;
  public double growStepSizeMultiplier = 1.0;
  public double sufficientDecreaseConstant = 1e-4;//0.9;
  public double stepSize = 1.0;
  public boolean stepSizeUnderflow = false;

	public double[] minimize(DifferentiableFunction function, double[] initial, double[] direction){
		return minimize(function, initial, direction, false);
	}

  
  public double[] minimize(DifferentiableFunction function, double[] initial, double[] direction, boolean project) {
	stepSizeUnderflow = false;
    double initialFunctionValue = function.valueAt(initial);
    double initialDirectionalDerivative = DoubleArrays.innerProduct(function.derivativeAt(initial), direction);
    double[] guess = null;
    double guessValue = 0.0;
    boolean sufficientDecreaseObtained = false;
//    if (false) {
//      guess = DoubleArrays.addMultiples(initial, 1.0, direction, EPS);
//      double guessValue = function.valueAt(guess);
//      double sufficientDecreaseValue = initialFunctionValue + sufficientDecreaseConstant * initialDirectionalDerivative * EPS;
//      System.out.println("NUDGE TEST:");
//      System.out.println("  Trying step size:  "+EPS);
//      System.out.println("  Required value is: "+sufficientDecreaseValue);
//      System.out.println("  Value is:          "+guessValue);
//      System.out.println("  Initial was:       "+initialFunctionValue);
//      if (guessValue > initialFunctionValue) {
//        System.err.println("NUDGE TEST FAILED");
//        return initial;
//      }
//    }
    while (! sufficientDecreaseObtained) {
      guess = DoubleArrays.addMultiples(initial, 1.0, direction, stepSize);
      if (project) DoubleArrays.project2(guess, initial); //keep the guess within the same orthant
      guessValue = function.valueAt(guess);
      double sufficientDecreaseValue = initialFunctionValue + sufficientDecreaseConstant * initialDirectionalDerivative * stepSize;
//      System.out.println("Trying step size:  "+stepSize);
//      System.out.println("Required value is: "+sufficientDecreaseValue);
//      System.out.println("Value is:          "+guessValue);
//      System.out.println("Initial was:       "+initialFunctionValue);
      sufficientDecreaseObtained = (guessValue <= sufficientDecreaseValue);
      if (! sufficientDecreaseObtained) {
        stepSize *= shrinkStepSizeMultiplier;
        if (stepSize < EPS) {
          //throw new RuntimeException("BacktrackingSearcher.minimize: stepSize underflow.");
          System.err.println("BacktrackingSearcher.minimize: stepSize underflow.");
          stepSizeUnderflow = true;
          return initial;
        }
      }
    }
    stepSize = stepSize*growStepSizeMultiplier;
//    double lastGuessValue = guessValue;
//    double[] lastGuess = guess;
//    while (lastGuessValue >= guessValue) {
//      lastGuessValue = guessValue;
//      lastGuess = guess;
//      stepSize *= stepSizeMultiplier;
//      guess = DoubleArrays.addMultiples(initial, 1.0, direction, stepSize);
//      guessValue = function.valueAt(guess);
//    }
//    return lastGuess;
    return guess;
  }
  public static void main(String[] args) {
    DifferentiableFunction function = new DifferentiableFunction() {
      public int dimension() {
        return 1;
      }

      public double valueAt(double[] x) {
        return x[0] * (x[0] - 0.01);
      }

      public double[] derivativeAt(double[] x) {
        return new double[] { 2*x[0] - 0.01 };
      }
      
      public double[] unregularizedDerivativeAt(double[] x) {
        return new double[] { 2*x[0] - 0.01 };
      }
    };
    LineSearcher lineSearcher = new LineSearcher();
    lineSearcher.minimize(function, new double[] { 0 }, new double[] { 1 }, false);
  }
}
