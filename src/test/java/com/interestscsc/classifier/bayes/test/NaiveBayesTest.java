package com.interestscsc.classifier.bayes.test;

import com.interestscsc.classifier.bayes.NaiveBayes;
import com.interestscsc.classifier.AbstractClassifier;
import com.interestscsc.dataset.Dataset;
import com.interestscsc.db.DBConnector;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;


public class NaiveBayesTest {

    // Parameters to playing
    // number iteration of cv
    private final static int NUMBER_ITERATION_OF_CV = 30;
    // test set ration: 0.2 means that train = 80%, test = 20%
    private final static double CV_RATIO_TEST = 0.2;

    // getting tags from db: minScore and maxScore
    private final static long MIN_NUMBER_POSTS_OF_TAG = 80;
    private final static long MAX_NUMBER_POSTS_OF_TAG = 100;
    // getting tags from db: start popular tag index and limit of popular tags
    private final static int START_POSITION_OF_POPULAR_TAGS = 2;
    private final static int LIMIT_NUMBER_OF_POPULAR_TAGS = 5;

    private DBConnector db;
    private Dataset dataset;

    private Instances trainingSet;
    private Instances testingSet;

    private static final Logger logger = Logger.getLogger(NaiveBayesClassifierTest.class);

    @Before
    public void setUp() throws SQLException {
        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;
        db = new DBConnector(dbName);
        dataset = new Dataset();
        // 3 methods to set tags
        dataset.setTagList(db, START_POSITION_OF_POPULAR_TAGS, LIMIT_NUMBER_OF_POPULAR_TAGS);
    }

    @Test
    public void testNaiveBayesClassifier() {

        Instances instances = getNormalizedInstances();

        Assert.assertNotNull(instances);

        // CV and classification
        for (int i = 0; i < NUMBER_ITERATION_OF_CV; ++i) {
            logger.info("Cross-Validation " + (i + 1));
            getDataset(instances);

            try {
                AbstractClassifier classifier = new NaiveBayesClassifier();
                Classifier cModel = classifier.trainClassifier(trainingSet);

                Evaluation eTrain = classifier.validateClassifier(cModel, trainingSet);
                logger.info("Result onto Training Set:");
                logger.info(eTrain.toSummaryString());

                Evaluation eTest = classifier.validateClassifier(cModel, testingSet);
                logger.info("Result onto Testing Set: " + eTest.pctCorrect());
                logger.info(eTest.toSummaryString());

            } catch (Exception e) {
                logger.error("Working with classifier failed. " + e);
            }
        }


        // With LSA
        try {
            dataset.setParametersForLSA(trainingSet, 0.9999);
            Instances newTrainingSet = dataset.getLSAReducedDataset(trainingSet);
            logger.info("Число аттрибутов в newTrainingSet: " + newTrainingSet.numAttributes());
            Instances newTestingSet = dataset.getLSAReducedDataset(testingSet);
            logger.info("Число аттрибутов в newTestingSet: " + newTestingSet.numAttributes());
            logger.info(newTrainingSet.equalHeaders(newTestingSet));


            AbstractClassifier classifier = new NaiveBayesClassifier();
            Classifier naiveBayes = classifier.trainClassifier(newTrainingSet);
            logger.info("ошибка на исходном множестве:");
            Evaluation eTrain = classifier.validateClassifier(naiveBayes, newTrainingSet);
            logger.info("Result onto Training Set:");
            logger.info(eTrain.toSummaryString());

            Evaluation eTest = classifier.validateClassifier(naiveBayes, newTestingSet);
            logger.info("Result onto Testing Set: " + eTest.pctCorrect());
            logger.info(eTest.toSummaryString());
        } catch (Exception e) {
            logger.error("Working with classifier with LSA failed. " + e);
        }
    }

    private Instances getNormalizedInstances() {

        // getting normalizes posts id for tags
        List<Long> normalizedPostIds = null;
        try {
            normalizedPostIds = dataset.getNormalizedPostIds(db);
        } catch (SQLException e) {
            logger.error("Getting normalized posts from DB failed. " + e);
        }
        Assert.assertNotNull(normalizedPostIds);
        logger.info("Finish getting posts.");
        logger.info("Number of normalized posts: " + normalizedPostIds.size());

        // getting features = allNGrams
        Set<String> allnGrammsFromDB = null;
        try {
            allnGrammsFromDB = dataset.getAllnGramsNamesFromDB(normalizedPostIds, db);
            logger.info("Finish getting of nGram. Number of nGramms: " + allnGrammsFromDB.size());
            dataset.setAttributes(allnGrammsFromDB);
        } catch (SQLException e) {
            logger.error("Working with DB failed. " + e);
        }

        Assert.assertNotNull(allnGrammsFromDB);

        // getting dataset
        Instances instances = null;
        try {
            instances = dataset.getDataset(normalizedPostIds, db);
        } catch (SQLException | IllegalArgumentException e) {
            logger.error("Getting dataset failed. " + e);
        }

        return instances;
    }

    private void getDataset(final Instances instances) {
        dataset.splitToTrainAndTest(instances, CV_RATIO_TEST);

        logger.info("Training set: ");
        trainingSet = dataset.getInstancesTrain();
        Assert.assertNotNull(trainingSet);
        logger.info("Число постов: " + trainingSet.numInstances());
        logger.info("Число аттрибутов в TrainingSet: " + trainingSet.numAttributes());

        logger.info("Testing set: ");
        testingSet = dataset.getInstancesTest();
        Assert.assertNotNull(testingSet);
        logger.info("Число постов: " + testingSet.numInstances());
        logger.info("Число аттрибутов в TestingSet: " + testingSet.numAttributes());
    }

    @After
    public void setOut() {
        logger.info("End of test: " + NaiveBayesClassifierTest.class);
    }

}
