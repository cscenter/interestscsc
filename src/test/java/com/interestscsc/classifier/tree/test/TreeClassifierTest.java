package com.interestscsc.classifier.tree.test;

/**
 * Created by Maxim on 05.03.2016.
 */
import com.interestscsc.classifier.CommonClassifierTest;
import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;

public class TreeClassifierTest extends CommonClassifierTest {

    private static final Logger logger = Logger.getLogger(TreeClassifierTest.class);

    @Override
    public Classifier getClassifier() {
        return new J48();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
