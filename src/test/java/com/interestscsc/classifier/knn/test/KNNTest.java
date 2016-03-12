package com.interestscsc.classifier.knn.test;

/**
 * Created by Maxim on 05.03.2016.
 */
import com.interestscsc.classifier.CommonClassifierTest;
import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;

public class KNNTest extends CommonClassifierTest {

    private static final int NUMBER_NN = 3;
    private static final Logger logger = Logger.getLogger(KNNTest.class);

    @Override
    public Classifier getClassifier() {
        return new IBk(NUMBER_NN);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
