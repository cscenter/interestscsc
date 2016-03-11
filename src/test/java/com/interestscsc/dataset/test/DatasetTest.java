package com.interestscsc.dataset.test;

import com.interestscsc.classifier.Estimator;
import com.interestscsc.dataset.Dataset;
import com.interestscsc.db.DBConnector;
import meka.classifiers.multilabel.BCC;
import meka.core.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.core.*;
import java.util.*;
import static meka.classifiers.multilabel.Evaluation.testClassifier;

/**
 * Проверка жизнеспособности класса
 */
public class DatasetTest {

    private final static int MAX_COUNT = 10;
    private List<String> allTags;
    private DBConnector db;
    private Instances isTrainingSet;
    private Instances isTestingSet;
    private Instances isTrainingMultiSet;
    private Instances isTestingMultiSet;

    @Before
    public void setUp() {
        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;
        db = new DBConnector(dbName);

        allTags = new ArrayList<String>();
        allTags.add("политика");
        allTags.add("депутат");
        allTags.add("Россия");
    }

    @Test
    public void testGettingDatasets() throws Exception {

        Dataset dataset = new Dataset(allTags);
        List<Long> normalizedIds = dataset.getNormalizedIds(db);
        Set<String> allNGramsFromDB  = dataset.getAllNGramsFromDB(normalizedIds, db);
        dataset.setMultilabelAttributes(allNGramsFromDB);

        dataset.splitToTrainAndTest(normalizedIds, 0.1);
        List<Long> normalizedIdsTrain = dataset.getNormalizedIdsTrain();
        List<Long> normalizedIdsTest = dataset.getNormalizedIdsTest();

        isTrainingMultiSet = dataset.getMultilabelDataset(normalizedIdsTrain, db);
        isTestingMultiSet = dataset.getMultilabelDataset(normalizedIdsTest, db);
        System.out.println(isTestingMultiSet.toString());

        Dataset dataset2 = new Dataset(allTags);
        normalizedIds = dataset2.getNormalizedIds(db);
        allNGramsFromDB  = dataset2.getAllNGramsFromDB(normalizedIds, db);
        dataset2.setAttributes(allNGramsFromDB);

        dataset2.splitToTrainAndTest(normalizedIds, 0.1);
        normalizedIdsTrain = dataset2.getNormalizedIdsTrain();
        normalizedIdsTest = dataset2.getNormalizedIdsTest();

        isTrainingSet = dataset2.getDataset(normalizedIdsTrain, db, allNGramsFromDB);
        isTestingSet = dataset2.getDataset(normalizedIdsTest, db, allNGramsFromDB);
        System.out.println(isTestingSet.toString());

    };

    @After
    public void testUsingDatasets() throws Exception {


        NaiveBayes naiveBayes = new NaiveBayes();
        naiveBayes.buildClassifier(isTrainingSet);

        Evaluation eTrain = new Evaluation(isTrainingSet);
        eTrain.evaluateModel(naiveBayes, isTrainingSet);
        System.out.print(eTrain.toSummaryString());

        Evaluation eTest = new Evaluation(isTestingSet);
        eTest.evaluateModel(naiveBayes, isTestingSet);
        System.out.print(eTest.toSummaryString());

        BCC bcc = new BCC();
        bcc.setClassifier(new NaiveBayes());
        bcc.buildClassifier(isTrainingMultiSet);
        Result trainResult = testClassifier(bcc, isTrainingMultiSet);
        Result testResult = testClassifier(bcc, isTestingMultiSet);

        double a[][] = trainResult.allPredictions();
        for (int i = 0; i < Integer.min(a.length, MAX_COUNT); i++) {
            for (int j = 0; j < a[i].length; j++) {
                System.out.print(a[i][j] + "  ");
            }
            System.out.println();
        }

        Estimator estimator = new Estimator();
        System.out.println("Estimating training dataset");
        estimator.estimate(trainResult, isTrainingMultiSet, 0, allTags.size());
        double trainAcc = estimator.getAccuracy();
        double trainFMeasure = estimator.getFMeasure();
        System.out.println("Accuracy        " + trainAcc);
        System.out.println("FMeasure        " + trainFMeasure);
        System.out.println("Precision       " + estimator.getPrecision());
        System.out.println("Recall          " + estimator.getRecall());
        System.out.println("FN              " + estimator.getFalseNegative());
        System.out.println("FP              " + estimator.getFalsePositive());
        System.out.println("TN              " + estimator.getTrueNegative());
        System.out.println("TP              " + estimator.getTruePositive());

        estimator = new Estimator();
        System.out.println("Estimating testing dataset");
        estimator.estimate(testResult, isTestingMultiSet, 0, allTags.size());
        double testAcc = estimator.getAccuracy();
        double testFMeasure = estimator.getFMeasure();
        System.out.println("Accuracy        " + testAcc);
        System.out.println("FMeasure        " + testFMeasure);
        System.out.println("Precision       " + estimator.getPrecision());
        System.out.println("Recall          " + estimator.getRecall());
        System.out.println("FN              " + estimator.getFalseNegative());
        System.out.println("FP              " + estimator.getFalsePositive());
        System.out.println("TN              " + estimator.getTrueNegative());
        System.out.println("TP              " + estimator.getTruePositive());

    }

}
