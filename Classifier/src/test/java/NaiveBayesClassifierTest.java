import classifier.NaiveBayesClassifier;
import dataset.Dataset;
import db.DBConnector;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import weka.classifiers.Classifier;
import weka.core.Instances;

import java.sql.SQLException;
import java.util.List;


public class NaiveBayesClassifierTest {
    private DBConnector db;
    private Dataset dataset;
    private static final Logger logger = Logger.getLogger(NaiveBayesClassifierTest.class);

    @Before
    public void setUp() {
        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;
        db = new DBConnector(dbName);
        dataset = new Dataset();
    }

    @Test
    public void testNaiveBayesClassifier() {

        List<Long> normalizedIds = null;
        try {
            normalizedIds = Dataset.getNormalizedIds(db);
        } catch (SQLException e) {
            logger.error("Getting normalized posts from DB failed. " + e);
        }
        Assert.assertNotNull(normalizedIds);

        // 0.1 - количество теста относительно всего сета, т. е. 1/10 test, 9/10 - train
        dataset.splitToTrainAndTest(normalizedIds, 0.1);
        List<Long> normalizedIdsTrain = dataset.getNormalizedIdsTrain();
        List<Long> normalizedIdsTest = dataset.getNormalizedIdsTest();

        Instances isTrainingSet = null;
        Instances isTestingSet = null;
        try {
            List<String> allTags = dataset.getAllTagsFromDB(normalizedIds, db);
            List<String> allnGrammsFromDB = dataset.getAllnGrammsFromDB(normalizedIdsTrain, db);

            dataset.setAttributes(allnGrammsFromDB, allTags);

            isTrainingSet = dataset.getDataset(normalizedIdsTrain, db, allnGrammsFromDB);
            isTestingSet = dataset.getDataset(normalizedIdsTest, db, allnGrammsFromDB);

        } catch (SQLException e) {
            logger.error("Working with DB failed. " + e);
        }

        Assert.assertNotNull(isTrainingSet);
        Assert.assertNotNull(isTestingSet);

        logger.info("Число постов: " + isTrainingSet.numInstances());
        logger.info("Число аттрибутов в TrainingSet: " + isTrainingSet.numAttributes());

        try {
            Classifier classifier = NaiveBayesClassifier.trainClassifier(isTrainingSet);
            logger.info("Ошибка на исходном множестве:");
            NaiveBayesClassifier.validateClassifier(classifier, isTrainingSet);
            logger.info("Ошибка на тестовом множестве:");
            NaiveBayesClassifier.validateClassifier(classifier, isTestingSet);

        } catch (Exception e) {
            logger.error("Working with classifier failed. " + e);
        }

        // With LSA
        try {
            dataset.setParametersForLSA(isTrainingSet, 0.9999);
            Instances newTrainingSet = dataset.getLSAReducedDataset(isTrainingSet);
            logger.info("Число аттрибутов в newTrainingSet: " + newTrainingSet.numAttributes());
            Instances newTestingSet = dataset.getLSAReducedDataset(isTestingSet);
            logger.info(newTrainingSet.equalHeaders(newTestingSet));


            Classifier naiveBayes2 = NaiveBayesClassifier.trainClassifier(newTrainingSet);
            logger.info("ошибка на исходном множестве:");
            NaiveBayesClassifier.validateClassifier(naiveBayes2, newTrainingSet);
            logger.info("ошибка на тестовом множестве:");
            NaiveBayesClassifier.validateClassifier(naiveBayes2, newTestingSet);
        } catch (Exception e) {
            logger.error("Working with classifier with LSA failed. " + e);
        }
    }

    @After
    public void setOut() {
        logger.info("End of test: " + NaiveBayesClassifierTest.class);
    }

}
