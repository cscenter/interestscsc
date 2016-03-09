package com.interestscsc.dataset.test;

import com.interestscsc.classifier.Estimator;
import com.interestscsc.dataset.Dataset;
import com.interestscsc.db.DBConnector;
import meka.classifiers.multilabel.BCC;
import meka.core.Result;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.*;
import java.util.*;
import static meka.classifiers.multilabel.Evaluation.testClassifier;

/**
 * Проверка жизнеспособности класса
 */
public class TestDataset {

    public final static int MAX_COUNT = 10;

    public static void main(String[] args) throws Exception {

        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;

        DBConnector db = new DBConnector(dbName);

        List<String> allTags = new ArrayList<String>();
        allTags.add("зарубежная архитектура");
        allTags.add("музеи");
        Dataset dataset = new Dataset(allTags);
        List<Long> normalizedIds = dataset.getNormalizedIds(db);
        Set<String> allNGramsFromDB  = dataset.getAllNGramsFromDB(normalizedIds, db);
        dataset.setMultilabelAttributes(allNGramsFromDB);
        dataset.splitToTrainAndTest(normalizedIds, 0.1);
        List<Long> normalizedIdsTrain = dataset.getNormalizedIdsTrain();

        Instances isTrainingSet = null;

        isTrainingSet = dataset.getMultilabelDataset(normalizedIdsTrain, db);
        System.out.println(isTrainingSet.toString());

        ///*
        //BCC bcc = new BCC();
        /*
        bcc.setClassifier(new NaiveBayes());
        bcc.buildClassifier(isTrainingSet);
        Result trainResult = testClassifier(bcc, isTrainingSet);
        */

        Dataset dataset2 = new Dataset(allTags);
        normalizedIds = dataset2.getNormalizedIds(db);
        allNGramsFromDB  = dataset2.getAllNGramsFromDB(normalizedIds, db);
        dataset2.setAttributes(allNGramsFromDB);
        dataset2.splitToTrainAndTest(normalizedIds, 0.1);
        normalizedIdsTrain = dataset2.getNormalizedIdsTrain();
        Instances isTrainingSet2 = dataset2.getDataset(normalizedIdsTrain, db, allNGramsFromDB);
        System.out.println(isTrainingSet2.toString());

        NaiveBayes naiveBayes = new NaiveBayes();
        naiveBayes.buildClassifier(isTrainingSet2);
        Evaluation eTest = new Evaluation(isTrainingSet2);
        eTest.evaluateModel(naiveBayes, isTrainingSet2);
        System.out.print(eTest.toSummaryString());

        /*
        double a[][] = trainResult.allPredictions();
        for (int i = 0; i < Integer.min(a.length, MAX_COUNT); i++) {
            for (int j = 0; j < a[i].length; j++) {
                System.out.print(a[i][j] + "  ");
            }
            System.out.println();
        }
        */

        //Estimator estimator = new Estimator();
    };

}
