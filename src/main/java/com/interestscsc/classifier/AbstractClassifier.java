package com.interestscsc.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public abstract class AbstractClassifier {
    public abstract Classifier trainClassifier(Instances trainingSet) throws Exception;

    public abstract Evaluation validateClassifier(Classifier cModel, Instances validationSet) throws Exception;
}
