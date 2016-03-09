package com.interestscsc.classifier;

/**
 * Created by Maxim on 05.03.2016.
 * Updated by Maxim on 09.03.2016.
 */

import com.interestscsc.exceptions.GeneralClassifierException;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class ClassifierBuilder {

    private Classifier classifier;

    public ClassifierBuilder(Classifier classifier) {
        this.classifier = classifier;
    }

    public void buildClassifier(Instances trainingSet) throws GeneralClassifierException {
        try {
            classifier.buildClassifier(trainingSet);
        } catch (Exception e) {
            throw new GeneralClassifierException("A classifier's building failed! " + e.getMessage());
        }
    }

    public Evaluation validateClassifier(Instances validationSet) throws GeneralClassifierException {
        Evaluation evaluationModel;
        try {
            evaluationModel = new Evaluation(validationSet);
            evaluationModel.evaluateModel(classifier, validationSet);
        } catch (Exception e) {
            throw new GeneralClassifierException("A classifier's evaluation failed! " + e.getMessage());
        }
        return evaluationModel;
    }
}
