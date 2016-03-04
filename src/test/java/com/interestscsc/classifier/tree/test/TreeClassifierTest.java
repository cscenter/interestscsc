package com.interestscsc.classifier.tree.test;

import com.interestscsc.classifier.AbstractClassifier;
import com.interestscsc.classifier.CommonClassifierTest;
import com.interestscsc.classifier.tree.TreeClassifier;
import org.apache.log4j.Logger;

public class TreeClassifierTest extends CommonClassifierTest {

    private static final Logger logger = Logger.getLogger(TreeClassifierTest.class);

    @Override
    public AbstractClassifier getClassifier() {
        return new TreeClassifier();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
