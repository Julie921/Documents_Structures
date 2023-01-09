package edu.berkeley.nlp.bp;

import edu.berkeley.nlp.math.SloppyMath;

import java.util.Arrays;

/**
 * User: aria42
 * Date: Jan 27, 2009
 */
public class TripletFactorPotential implements FactorPotential {

  private double[][][] logPotentials;
  private int D1, D2, D3;
  private int[] vals;

  public TripletFactorPotential(double[][][] logPotentials) {
    this.logPotentials = logPotentials;
    D1 = logPotentials.length;
    D2 = logPotentials[0].length;
    D3 = logPotentials[0][0].length;
    vals = new int[]{D1,D2,D3};
  }

  public void computeLogMessages(double[][] inputMessages, double[][] outputMessages) {
    // 0
    for (int d1 = 0; d1 < D1; d1++) {
      double[] pieces = new double[D2*D3];
      int index = 0;
      for (int d2 = 0; d2 < D2; d2++) {
        for (int d3 = 0; d3 < D3; d3++) {
          pieces[index++] = logPotentials[d1][d2][d3] +
              inputMessages[1][d2] + inputMessages[2][d3];
        }
      }
      outputMessages[0][d1] = SloppyMath.logAdd(pieces);
    }
    // 1
    for (int d2 = 0; d2 < D2; d2++) {
      double[] pieces = new double[D1*D3];
      int index = 0;
      for (int d1 = 0; d1 < D1; d1++) {
        for (int d3 = 0; d3 < D3; d3++) {
          pieces[index++] = logPotentials[d1][d2][d3] +
              inputMessages[0][d1] + inputMessages[2][d3];
        }
      }
      outputMessages[1][d2] = SloppyMath.logAdd(pieces);
    }
    // 2
    for (int d3 = 0; d3 < D3; d3++) {
      double[] pieces = new double[D1*D3];
      int index = 0;
      for (int d1 = 0; d1 < D1; d1++) {
        for (int d2 = 0; d2 < D2; d2++) {        
          pieces[index++] = logPotentials[d1][d2][d3] +
              inputMessages[0][d1] + inputMessages[1][d2];
        }
      }
      outputMessages[2][d3] = SloppyMath.logAdd(pieces);
    }
  }

  public Object computeMarginal(double[][] inputMessages) {
    double[][][] marginals = new double[D1][D2][D3];
    double[] pieces = new double[D1*D2*D3];
    int index = 0;
    for (int d1 = 0; d1 < D1; d1++) {
      for (int d2 = 0; d2 < D2; d2++) {
        for (int d3 = 0; d3 < D3; d3++) {
          int[] vals = {d1,d2,d3};
          double sum = logPotentials[d1][d2][d3];
          for (int i = 0; i < 3; i++) {
            sum += inputMessages[i][vals[i]];
          }
          pieces[index++] = sum;
          marginals[d1][d2][d3] = sum;
        }
      }
    }
    double logSum = SloppyMath.logAdd(pieces);
    for (int d1 = 0; d1 < D1; d1++) {
      for (int d2 = 0; d2 < D2; d2++) {
        for (int d3 = 0; d3 < D3; d3++) {
          marginals[d1][d2][d3] = Math.exp(marginals[d1][d2][d3] - logSum);
        }
      }
    }
    return marginals;
  }


}
