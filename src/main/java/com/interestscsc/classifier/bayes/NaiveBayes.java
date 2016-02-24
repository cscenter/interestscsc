package com.interestscsc.classifier.bayes;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class NaiveBayes extends AbstractClassifier {

    @Override
    public Classifier trainClassifier(Instances trainingSet) throws Exception {
        Classifier cModel = new weka.classifiers.bayes.NaiveBayes();
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
