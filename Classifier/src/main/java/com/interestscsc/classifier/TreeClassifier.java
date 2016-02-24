package com.interestscsc.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instances;

public class TreeClassifier extends AbstractClassifier {

    @Override
    public Classifier trainClassifier(Instances trainingSet) throws Exception {
        J48 cModel = new J48();
        cModel.buildClassifier(trainingSet);
        return cModel;
    }

    @Override
    public Evaluation validateClassifier(Classifier cModel, Instances validationSet) throws Exception {
        Evaluation eTest = new Evaluation(validationSet);
        eTest.evaluateModel(cModel, validationSet);

        return eTest;
    }
}
