package dataset;

import data.NGram;
import db.DBConnector;
import meka.classifiers.multilabel.BCC;
import meka.core.Result;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.core.*;

import java.sql.SQLException;
import java.util.*;

import static meka.classifiers.multilabel.Evaluation.testClassifier;

public class Estimator {

    // точность сравнивания double
    private double epsilon = 0.01;
    private int truePositive;
    private int trueNegative;
    private int falsePositive;
    private int falseNegative;

    public Estimator() {
    }

    public void clear() {
        this.truePositive = 0;
        this.falsePositive = 0;
        this.falsePositive = 0;
        this.falseNegative = 0;
    }

    //dataset[startIndex; lastIndex-1] -- true answers
    public void estimate(Result result, Instances dataset, int startIndex, int lastIndex) throws Exception {
        this.clear();
        double [][] predictions = result.allPredictions();
        for (int j = 0; j < dataset.size(); j++) {
            for (int i = startIndex; i < lastIndex; i++) {
                this.truePositive += this.isTruePositive(dataset.get(j).value(i), predictions[j][i]);
                this.trueNegative += this.isTrueNegaive(dataset.get(j).value(i), predictions[j][i]);
                this.falsePositive += this.isFalsePositive(dataset.get(j).value(i), predictions[j][i]);
                this.falseNegative += this.isFalseNegative(dataset.get(j).value(i), predictions[j][i]);
                }
            }
        }

    public double estimateClassification(Instances dataset) {
        return this.truePositive / dataset.size();
    }


    private int isTruePositive(double trueValue, double predictedValue) {
        // trueValue == predictedValue and trueValue == 1
        if ((Math.abs(trueValue - predictedValue) < epsilon) && (Math.abs(trueValue - 1) < epsilon)) {
            return 1;
        }
        return 0;
    }

    private int isTrueNegaive(double trueValue, double predictedValue) {
        // trueValue == predictedValue and trueValue == 0
        if ((Math.abs(trueValue - predictedValue) < epsilon) && (Math.abs(trueValue - 0) < epsilon)) {
            return 1;
        }
        return 0;
    }

    private int isFalsePositive(double trueValue, double predictedValue) {
        // predictedValue == 1 and trueValue == 0
        if ((Math.abs(trueValue) < epsilon) && (Math.abs(1 - predictedValue) < epsilon)) {
            return 1;
        }
        return 0;
    }

    private int isFalseNegative(double trueValue, double predictedValue) {
        // trueValue == 1 and predictedValue == 0
        if ((Math.abs(1 - trueValue) < epsilon) && (Math.abs(predictedValue) < epsilon)) {
            return 1;
        }
        return 0;
    }

    public int getTruePositive() {
        return this.truePositive;
    }

    public int getTrueNegative() {
        return this.trueNegative;
    }

    public int getFalsePositive() {
        return this.falsePositive;
    }

    public int getFalseNegative() {
        return this.falseNegative;
    }

    public double getAccuracy() {
        //tp+tn / (tp+tn+fp+fn)
        return (this.trueNegative + this.truePositive) / (double)(this.truePositive + this.trueNegative +
                this.falsePositive + this.falseNegative);
    }

    public double getPrecision() {
        //tp / (tp + fp)
        return this.truePositive / (double)(this.truePositive + this.falsePositive);
    }

    public double getRecall() {
        // tp / (tp + fn)
        return this.truePositive / (double)(this.truePositive + this.falseNegative);
    }

    public double getWeightedFMeasure(double beta) {
        // (beta^2 + 1)PR / (beta^2 * P + R)
        double precision = this.getPrecision();
        double recall = this.getRecall();
        return ((beta * beta + 1) * precision * recall) / (beta * beta * precision + recall);
    }

    public double getFMeasure() {
        return this.getWeightedFMeasure(1);
    }
}
