import classifier.NaiveBayesClassifier;
import dataset.Dataset;
import db.DBConnector;
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


public class NaiveBayesClassifierTest {

    private final static int NUMBER_ITERATION_OF_CV = 100;

    private DBConnector db;
    private Dataset dataset;
    private static final Logger logger = Logger.getLogger(NaiveBayesClassifierTest.class);

    @Before
    public void setUp() throws SQLException {
        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;
        db = new DBConnector(dbName);
        dataset = new Dataset(db);
    }

    @Test
    public void testNaiveBayesClassifier() {

        List<Long> normalizedIds = null;
        try {
            normalizedIds = dataset.getNormalizedIds(db);
        } catch (SQLException e) {
            logger.error("Getting normalized posts from DB failed. " + e);
        }
        Assert.assertNotNull(normalizedIds);
        logger.info("Finish getting posts.");
        logger.info("Number of normalized posts: " + normalizedIds.size());

        Set<String> allnGrammsFromDB = null;
        try {
            allnGrammsFromDB = dataset.getAllnGrammsFromDB(normalizedIds, db);
            logger.info("Finish getting of nGram.");
            dataset.setAttributes(allnGrammsFromDB);

        } catch (SQLException e) {
            logger.error("Working with DB failed. " + e);
        }

        Assert.assertNotNull(allnGrammsFromDB);

        for (int i = 0; i < NUMBER_ITERATION_OF_CV; ++i) {
            // 0.1 - количество теста относительно всего сета, т. е. 1/10 test, 9/10 - train
            dataset.splitToTrainAndTest(normalizedIds, 0.1);
            List<Long> normalizedIdsTrain = dataset.getNormalizedIdsTrain();
            List<Long> normalizedIdsTest = dataset.getNormalizedIdsTest();

            Instances isTrainingSet = null;
            Instances isTestingSet = null;
            try {
                logger.info("Training set: ");
                isTrainingSet = dataset.getDataset(normalizedIdsTrain, db, allnGrammsFromDB);
                logger.info("Testing set: ");
                isTestingSet = dataset.getDataset(normalizedIdsTest, db, allnGrammsFromDB);

            } catch (SQLException | IllegalArgumentException e) {
                logger.error("Getting dataset failed. " + e);
            }

            Assert.assertNotNull(isTrainingSet);
            Assert.assertNotNull(isTestingSet);

            logger.info("Число постов: " + isTrainingSet.numInstances());
            logger.info("Число аттрибутов в TrainingSet: " + isTrainingSet.numAttributes());

            try {
                Classifier classifier = NaiveBayesClassifier.trainClassifier(isTrainingSet);

                Evaluation eTrain = NaiveBayesClassifier.validateClassifier(classifier, isTrainingSet);
                logger.info("Ошибка на исходном множестве:");
                logger.info(eTrain.toSummaryString());

                Evaluation eTest = NaiveBayesClassifier.validateClassifier(classifier, isTestingSet);
                logger.info("Ошибка на тестовом множестве:");
                logger.info(eTest.toSummaryString());
            } catch (Exception e) {
                logger.error("Working with classifier failed. " + e);
            }
        }


        dataset.splitToTrainAndTest(normalizedIds, 0.1);
        List<Long> normalizedIdsTrain = dataset.getNormalizedIdsTrain();
        List<Long> normalizedIdsTest = dataset.getNormalizedIdsTest();

        Instances isTrainingSet = null;
        Instances isTestingSet = null;
        try {
            logger.info("Training set: ");
            isTrainingSet = dataset.getDataset(normalizedIdsTrain, db, allnGrammsFromDB);
            logger.info("Testing set: ");
            isTestingSet = dataset.getDataset(normalizedIdsTest, db, allnGrammsFromDB);

        } catch (SQLException | IllegalArgumentException e) {
            logger.error("Getting dataset failed. " + e);
        }

        Assert.assertNotNull(isTrainingSet);
        Assert.assertNotNull(isTestingSet);

        logger.info("Число постов: " + isTrainingSet.numInstances());
        logger.info("Число аттрибутов в TrainingSet: " + isTrainingSet.numAttributes());

        // With LSA
        try {
            dataset.setParametersForLSA(isTrainingSet, 0.9);
            Instances newTrainingSet = dataset.getLSAReducedDataset(isTrainingSet);
            logger.info("Число аттрибутов в newTrainingSet: " + newTrainingSet.numAttributes());
            Instances newTestingSet = dataset.getLSAReducedDataset(isTestingSet);
            logger.info(newTrainingSet.equalHeaders(newTestingSet));


            Classifier naiveBayes2 = NaiveBayesClassifier.trainClassifier(newTrainingSet);
            logger.info("ошибка на исходном множестве:");
            logger.info(NaiveBayesClassifier.validateClassifier(naiveBayes2, newTrainingSet));
            logger.info("ошибка на тестовом множестве:");
            logger.info(NaiveBayesClassifier.validateClassifier(naiveBayes2, newTestingSet));
        } catch (Exception e) {
            logger.error("Working with classifier with LSA failed. " + e);
        }
    }

    @After
    public void setOut() {
        logger.info("End of test: " + NaiveBayesClassifierTest.class);
    }

}
