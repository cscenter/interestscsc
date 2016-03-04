package com.interestscsc.classifier.knn.test;

import com.interestscsc.classifier.AbstractClassifier;
import com.interestscsc.classifier.CommonClassifierTest;
import com.interestscsc.classifier.knn.KNNClassifier;
import org.apache.log4j.Logger;

public class KNNTest extends CommonClassifierTest {

    private static final int NUMBER_NN = 3;
    private static final Logger logger = Logger.getLogger(KNNTest.class);


    @Override
    public AbstractClassifier getClassifier() {
        return new KNNClassifier(NUMBER_NN);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
