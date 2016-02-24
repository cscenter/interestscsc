package com.interestscsc.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.core.Instances;

public class KNNClassifier {
    public static Classifier trainClassifier(Instances isTrainingSet, int k) throws Exception {
        IBk cModel = new IBk(k);
        cModel.buildClassifier(isTrainingSet);
        return cModel;
    }

    public static Evaluation validateClassifier(Classifier cModel, Instances isValidationSet) throws Exception {
        Evaluation eTest = new Evaluation(isValidationSet);
        eTest.evaluateModel(cModel, isValidationSet);

        return eTest;
    }
}
