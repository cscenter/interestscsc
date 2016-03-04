package com.interestscsc.classifier.bayes.test;

/**
 * Created by Maxim on 05.03.2016.
 */
import com.interestscsc.classifier.CommonClassifierTest;
import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;

public class NaiveBayesTest extends CommonClassifierTest {

    private static final Logger logger = Logger.getLogger(NaiveBayesTest.class);

    @Override
    public Classifier getClassifier() {
        return new NaiveBayes();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
