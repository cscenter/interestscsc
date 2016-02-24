package com.interestscsc.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instances;

public class TreeClassifier {
    public static Classifier trainClassifier(Instances isTrainingSet) throws Exception {
        J48 cModel = new J48();
        cModel.buildClassifier(isTrainingSet);
        return cModel;
    }

    public static Evaluation validateClassifier(Classifier cModel, Instances isValidationSet) throws Exception {
        Evaluation eTest = new Evaluation(isValidationSet);
        eTest.evaluateModel(cModel, isValidationSet);

        return eTest;
    }
}
