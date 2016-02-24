package com.interestscsc.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.core.Instances;

public class KNNClassifier extends AbstractClassifier {

    private final int k;

    public KNNClassifier() {
        k = 1;
    }

    public KNNClassifier(final int k) {
        this.k = k;
    }

    public Classifier trainClassifier(Instances trainingSet, int k) throws Exception {
        IBk cModel = new IBk(k);
        cModel.buildClassifier(trainingSet);
        return cModel;
    }

    @Override
    public Classifier trainClassifier(Instances trainingSet) throws Exception {
        IBk cModel = new IBk(k);
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
