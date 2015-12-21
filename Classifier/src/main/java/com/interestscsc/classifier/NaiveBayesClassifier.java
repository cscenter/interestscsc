package com.interestscsc.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;

public class NaiveBayesClassifier {

    public static Classifier trainClassifier(Instances isTrainingSet) throws Exception {
        Classifier cModel = new NaiveBayes();
        cModel.buildClassifier(isTrainingSet);
        return cModel;
    }

    public static Evaluation validateClassifier(Classifier cModel, Instances isValidationSet) throws Exception {
        Evaluation eTest = new Evaluation(isValidationSet);
        eTest.evaluateModel(cModel, isValidationSet);

        return eTest;
    }

    public static String testClassifier(Instances isTrainingSet) throws Exception {

        Classifier cModel = new NaiveBayes();
        cModel.buildClassifier(isTrainingSet);

        // Test the model
        Evaluation eTest = new Evaluation(isTrainingSet);
        eTest.evaluateModel(cModel, isTrainingSet);

        return eTest.toSummaryString();
    }
}
