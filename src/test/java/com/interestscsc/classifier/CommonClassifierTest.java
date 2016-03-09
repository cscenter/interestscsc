package com.interestscsc.classifier;

/**
 * Created by Maxim on 05.03.2016.
 * Updated by Maxim on 09.03.2016.
 */

import com.interestscsc.dataset.Dataset;
import com.interestscsc.db.DBConnector;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public abstract class CommonClassifierTest {
    /**
     *  Parameters to playing
     */

    /**
     * Number iteration of cv
     */
    private final static int NUMBER_ITERATION_OF_CV = 5;

    /**
     * Ration of testing set: 0.2 means that train = 80%, test = 20%
     */
    private final static double CV_RATIO_TEST = 0.2;

    /**
     * Getting tags from db: start popular tag index and limit of popular tags
     */
    private final static int START_POSITION_OF_POPULAR_TAGS = 200;
    private final static int LIMIT_NUMBER_OF_POPULAR_TAGS = 5;

    private DBConnector db;
    private Dataset dataset;

    private Instances trainingSet;
    private Instances testingSet;

    private static Logger logger;

    @Before
    public void setUp() throws SQLException {
        BasicConfigurator.configure();

        logger = getLogger();
        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;
        db = new DBConnector(dbName);
        dataset = new Dataset();

        dataset.setTagListByOffset(db, START_POSITION_OF_POPULAR_TAGS, LIMIT_NUMBER_OF_POPULAR_TAGS);
    }

    @Test
    public void testClassifier() {

        Instances instances = getNormalizedInstances();

        Assert.assertNotNull(instances);

        /**
         *  CV and classification
         */
        for (int i = 0; i < NUMBER_ITERATION_OF_CV; ++i) {
            logger.info("Cross-Validation " + (i + 1));
            getTrainAndTestSet(instances);

            try {
                ClassifierBuilder classifierBuilder = new ClassifierBuilder(getClassifier());
                classifierBuilder.buildClassifier(trainingSet);

                Evaluation eTrain = classifierBuilder.validateClassifier(trainingSet);
                logger.info("Result onto Training Set:");
                logger.info("Summary string:");
                logger.info(eTrain.toSummaryString());
                logger.info("Class Details string:");
                logger.info(eTrain.toClassDetailsString());
                logger.info("Matrix string:");
                logger.info(eTrain.toMatrixString());

                Evaluation eTest = classifierBuilder.validateClassifier(testingSet);
                logger.info("Result onto Testing Set: " + eTest.pctCorrect());
                logger.info("Summary string:");
                logger.info(eTest.toSummaryString());
                logger.info("Class Details string:");
                logger.info(eTest.toClassDetailsString());
                logger.info("Matrix string:");
                logger.info(eTest.toMatrixString());

            } catch (Exception e) {
                logger.error("Working with classifier failed. " + e);
            }
        }


        /**
         *  CV and classification with LSA
         */
        double rankLSA = 0.9;
        logger.info("Start using LSA with rank " + rankLSA);
        try {
            dataset.setParametersForLSA(trainingSet, rankLSA);
            Instances newTrainingSet = dataset.getLSAReducedDataset(trainingSet);
            logger.info("Число аттрибутов в newTrainingSet: " + newTrainingSet.numAttributes());
            Instances newTestingSet = dataset.getLSAReducedDataset(testingSet);
            logger.info("Число аттрибутов в newTestingSet: " + newTestingSet.numAttributes());
            logger.info(newTrainingSet.equalHeaders(newTestingSet));


            ClassifierBuilder classifierBuilder = new ClassifierBuilder(getClassifier());
            classifierBuilder.buildClassifier(newTrainingSet);
            logger.info("ошибка на исходном множестве:");
            Evaluation eTrain = classifierBuilder.validateClassifier(newTrainingSet);
            logger.info("Result onto Training Set:");
            logger.info("Summary string:");
            logger.info(eTrain.toSummaryString());
            logger.info("Class Details string:");
            logger.info(eTrain.toClassDetailsString());
            logger.info("Matrix string:");
            logger.info(eTrain.toMatrixString());

            Evaluation eTest = classifierBuilder.validateClassifier(newTestingSet);
            logger.info("Result onto Testing Set: " + eTest.pctCorrect());
            logger.info("Summary string:");
            logger.info(eTest.toSummaryString());
            logger.info("Class Details string:");
            logger.info(eTest.toClassDetailsString());
            logger.info("Matrix string:");
            logger.info(eTest.toMatrixString());
        } catch (Exception e) {
            logger.error("Working with classifier with LSA failed. " + e);
        }
    }

    private Instances getNormalizedInstances() {

        /**
         *  Getting normalizes posts id for tags
         */
        List<Long> normalizedPostIds = null;
        try {
            normalizedPostIds = dataset.getNormalizedPostIds(db);
        } catch (SQLException e) {
            logger.error("Getting normalized posts from DB failed. " + e);
        }
        Assert.assertNotNull(normalizedPostIds);
        logger.info("Finish getting posts.");
        logger.info("Number of normalized posts: " + normalizedPostIds.size());

        /**
         *  Getting features = allNGrams
         */
        Set<String> allnGrammsFromDB = null;
        try {
            allnGrammsFromDB = dataset.getAllnGramsNamesFromDB(normalizedPostIds, db);
            logger.info("Finish getting of nGram. Number of nGramms: " + allnGrammsFromDB.size());
            dataset.setAttributes(allnGrammsFromDB);
        } catch (SQLException e) {
            logger.error("Working with DB failed. " + e);
        }

        Assert.assertNotNull(allnGrammsFromDB);

        /**
         *  Getting instances from dataset
         */
        Instances instances = null;
        try {
            instances = dataset.getDataset(normalizedPostIds, db);
        } catch (SQLException | IllegalArgumentException e) {
            logger.error("Getting dataset failed. " + e);
        }

        return instances;
    }

    /**
     * Splitting instances on Training and Testing set
     */
    private void getTrainAndTestSet(final Instances instances) {
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

    public abstract Classifier getClassifier();

    public abstract Logger getLogger();
}
