package com.interestscsc.classifier;

import meka.core.Result;
import weka.core.*;

public class Estimator {

    // точность сравнивания double
    private double EPSILON = 0.01;
    private int truePositive;
    private int trueNegative;
    private int falsePositive;
    private int falseNegative;

    public Estimator() {
    }

    public void clear() {
        truePositive = 0;
        falsePositive = 0;
        falsePositive = 0;
        falseNegative = 0;
    }

    //dataset[startIndex; lastIndex-1] -- true answers
    public void estimate(Result result, Instances dataset, int startIndex, int lastIndex) {
        clear();
        double [][] predictions = result.allPredictions();
        for (int j = 0; j < dataset.size(); j++) {
            for (int i = startIndex; i < lastIndex; i++) {
                truePositive += isTruePositive(dataset.get(j).value(i), predictions[j][i]);
                trueNegative += isTrueNegaive(dataset.get(j).value(i), predictions[j][i]);
                falsePositive += isFalsePositive(dataset.get(j).value(i), predictions[j][i]);
                falseNegative += isFalseNegative(dataset.get(j).value(i), predictions[j][i]);
                }
            }
        }


    private boolean matchesPair(double first, double second, double firstMatch, double secondMatch) {
        return (Math.abs(first - firstMatch) < EPSILON) && (Math.abs(second - secondMatch) < EPSILON);
    }

    private int isTruePositive(double trueValue, double predictedValue) {
        // trueValue == predictedValue and trueValue == 1
        return matchesPair(trueValue, predictedValue, 1, 1) ? 1 : 0;
    }

    private int isTrueNegaive(double trueValue, double predictedValue) {
        // trueValue == predictedValue and trueValue == 0
        return matchesPair(trueValue, predictedValue, 0, 0) ? 1 : 0;
    }

    private int isFalsePositive(double trueValue, double predictedValue) {
        // predictedValue == 1 and trueValue == 0
        return matchesPair(trueValue, predictedValue, 0, 1) ? 1 : 0;
    }

    private int isFalseNegative(double trueValue, double predictedValue) {
        // trueValue == 1 and predictedValue == 0
        return matchesPair(trueValue, predictedValue, 1, 0) ? 1 : 0;
    }

    public int getTruePositive() {
        return truePositive;
    }

    public int getTrueNegative() {
        return trueNegative;
    }

    public int getFalsePositive() {
        return falsePositive;
    }

    public int getFalseNegative() {
        return falseNegative;
    }

    public double getAccuracy() {
        //tp+tn / (tp+tn+fp+fn)
        return (trueNegative + truePositive) / (double)(truePositive + trueNegative + falsePositive + falseNegative);
    }

    public double getPrecision() {
        //tp / (tp + fp)
        return truePositive / (double)(truePositive + falsePositive);
    }

    public double getRecall() {
        // tp / (tp + fn)
        return truePositive / (double)(truePositive + falseNegative);
    }

    public double getWeightedFMeasure(double beta) {
        // (beta^2 + 1)PR / (beta^2 * P + R)
        double precision = getPrecision();
        double recall = getRecall();
        return ((beta * beta + 1) * precision * recall) / (beta * beta * precision + recall);
    }

    public double getFMeasure() {
        return getWeightedFMeasure(1);
    }
}

