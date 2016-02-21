package com.interestscsc.classifier.bayes;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class NaiveBayes {

    public static Classifier trainClassifier(Instances isTrainingSet) throws Exception {
        Classifier cModel = new weka.classifiers.bayes.NaiveBayes();
        cModel.buildClassifier(isTrainingSet);
        return cModel;
    }

    public static Evaluation validateClassifier(Classifier cModel, Instances isValidationSet) throws Exception {
        Evaluation eTest = new Evaluation(isValidationSet);
        eTest.evaluateModel(cModel, isValidationSet);

        return eTest;
    }

    public static String testClassifier(Instances isTrainingSet) throws Exception {

        Classifier cModel = new weka.classifiers.bayes.NaiveBayes();
        cModel.buildClassifier(isTrainingSet);

        // Test the model
        Evaluation eTest = new Evaluation(isTrainingSet);
        eTest.evaluateModel(cModel, isTrainingSet);

        return eTest.toSummaryString();
    }
}
