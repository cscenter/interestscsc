package com.interestscsc.classifier.bayes.test;

import com.interestscsc.classifier.AbstractClassifier;
import com.interestscsc.classifier.CommonClassifierTest;
import com.interestscsc.classifier.bayes.NaiveBayes;
import org.apache.log4j.Logger;

public class NaiveBayesTest extends CommonClassifierTest {

    private static final Logger logger = Logger.getLogger(NaiveBayesTest.class);

    @Override
    public AbstractClassifier getClassifier() {
        return new NaiveBayes();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
