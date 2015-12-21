package com.interestscsc.bayes;

import com.interestscsc.data.NGram;
import com.interestscsc.db.DBConnector;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.LatentSemanticAnalysis;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.bayes.NaiveBayesMultinomialUpdateable;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.sql.SQLException;
import java.util.*;

public class BayesFromDB {

    public static List<String> getAllnGrammsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<String>();
        for (Long id : normalizedIds) {
            List<NGram> allNGram = db.getAllNGramNames(id, DBConnector.NGramType.UNIGRAM);
            for (NGram a : allNGram) {
                ngramsSet.add(a.getText());
            }
        }
        return new ArrayList<String>(ngramsSet);
    }

    public static String[] getAllTagsFromDB() {
        return new String[]{"статистика", "детское"};
    }

    public static Instances getDataset(List<Long> normalizedIds, DBConnector db) throws SQLException {
        System.out.print("Getting dataset...");
        List<String> nGramms = getAllnGrammsFromDB(normalizedIds, db);
        FastVector fvWekaAttributes = new FastVector(nGramms.size() + 1);
        for (String nGramm : nGramms) {
            fvWekaAttributes.addElement(new Attribute(nGramm));
        }

        String[] allTags = getAllTagsFromDB();
        FastVector fvClassVal = new FastVector(allTags.length);
        for (String tag : allTags) {
            fvClassVal.addElement(tag);
        }
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        fvWekaAttributes.addElement(ClassAttribute);

        // Create an empty training set
        int documentCount = normalizedIds.size();
        Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, documentCount);
        // Set class index
        isTrainingSet.setClassIndex(nGramms.size()); // нумерация с 0 же, последний элемент - Tag.

        //System.out.println("Word:   " + nGramms[0] + " " + nGramms[1] + " " + nGramms[2]);
        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = db.getAllTagNames(postId);

            if (allTagsOfPost.contains("статистика")) {
                allTagsOfPost.clear();
                allTagsOfPost.add("статистика");
                System.out.println("статистика");
            }
            if (allTagsOfPost.contains("детское")) {
                allTagsOfPost.clear();
                allTagsOfPost.add("детское");
                System.out.println("детское");
            }

            List<NGram> allNGram = db.getAllNGramNames(postId, DBConnector.NGramType.UNIGRAM);
            Map<String, Integer> ngramMap = new HashMap<String, Integer>();
            for (NGram ngram : allNGram) {
                ngramMap.put(ngram.getText(), ngram.getUsesCnt());
            }

            for (String tagOfPost : allTagsOfPost) {
                System.out.print("Post " + postId + ":  ");
                //int totalWordCountInPost = getWordCountInPost(postId);
                Instance iExample = new Instance(nGramms.size() + 1);
                for (int i = 0; i < nGramms.size(); i++) {
                    int wordCountInPost = 0;
                    if (ngramMap.containsKey(nGramms.get(i))) {
                        wordCountInPost = ngramMap.get(nGramms.get(i));
                        System.out.print(nGramms.get(i) + " " + wordCountInPost + "   ");
                    }
                    iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), wordCountInPost);
                }
                System.out.println(tagOfPost);
                iExample.setValue((Attribute) fvWekaAttributes.elementAt(nGramms.size()), tagOfPost);
                isTrainingSet.add(iExample);
            }
        }
        return isTrainingSet;
    }

    ;


    public static void testNaiveBayesMultinomial(Instances isTrainingSet) throws SQLException {


        Classifier cModel = new NaiveBayesMultinomial();
        try {
            cModel.buildClassifier(isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Test the model
        try {
            Evaluation eTest = new Evaluation(isTrainingSet);
            eTest.evaluateModel(cModel, isTrainingSet);
            // Print the result à la Weka explorer:
            String strSummary = eTest.toSummaryString();
            System.out.println(strSummary);

            System.out.println(cModel.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    public static void testNaiveBayes(Instances isTrainingSet) throws SQLException {

        Classifier cModel = new NaiveBayes();
        try {
            cModel.buildClassifier(isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Test the model
        try {
            Evaluation eTest = new Evaluation(isTrainingSet);
            eTest.evaluateModel(cModel, isTrainingSet);
            // Print the result à la Weka explorer:
            String strSummary = eTest.toSummaryString();
            System.out.println(strSummary);

            System.out.println(cModel.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    public static Instance getInstance(Long normalizedId, DBConnector db, FastVector fvWekaAttributes) throws SQLException {
        List<Long> normalizedIds = new ArrayList<Long>();
        normalizedIds.add(normalizedId);
        List<String> nGramms = getAllnGrammsFromDB(normalizedIds, db);
        Instance iExample = new Instance(nGramms.size() + 1);
        List<String> allTagsOfPost = db.getAllTagNames(normalizedId);

        String tagOfPost = null;
        if (allTagsOfPost.contains("статистика")) {
            tagOfPost = "статистика";
        }
        if (allTagsOfPost.contains("детское")) {
            tagOfPost = "детское";
        }

        List<NGram> allNGram = db.getAllNGramNames(normalizedId, DBConnector.NGramType.UNIGRAM);
        Map<String, Integer> ngramMap = new HashMap<String, Integer>();
        for (NGram ngram : allNGram) {
            ngramMap.put(ngram.getText(), ngram.getUsesCnt());
        }
        for (int i = 0; i < nGramms.size(); i++) {
            int wordCountInPost = 0;
            if (ngramMap.containsKey(nGramms.get(i))) {
                wordCountInPost = ngramMap.get(nGramms.get(i));
                System.out.print(nGramms.get(i) + " " + wordCountInPost + "   ");
            }
            iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), wordCountInPost);
        }
        System.out.println(tagOfPost);
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(nGramms.size()), tagOfPost);
        return iExample;
    }

    public static FastVector getFastVector(List<Long> normalizedIds, DBConnector db) throws SQLException {
        List<String> nGramms = getAllnGrammsFromDB(normalizedIds, db);
        FastVector fvWekaAttributes = new FastVector(nGramms.size() + 1);
        for (String nGramm : nGramms) {
            fvWekaAttributes.addElement(new Attribute(nGramm));
        }

        String[] allTags = getAllTagsFromDB();
        FastVector fvClassVal = new FastVector(allTags.length);
        for (String tag : allTags) {
            fvClassVal.addElement(tag);
        }
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        fvWekaAttributes.addElement(ClassAttribute);
        return fvWekaAttributes;
    }

    public static void testNaiveBayesMultinomialUpdeatable(List<Long> normalizedIds, DBConnector db) throws SQLException {
        List<Long> firstTen = new ArrayList<Long>();
        for (int i = 0; i < 10; i++) {
            firstTen.add(normalizedIds.get(i));
        }
        Instances isTrainingSet = getDataset(firstTen, db);

        FastVector fastVector = getFastVector(normalizedIds, db);

        NaiveBayesMultinomialUpdateable cModel = new NaiveBayesMultinomialUpdateable();
        try {
            cModel.buildClassifier(isTrainingSet);
            for (int i = 10; i < 51; i++) {
                Instance next = getInstance(normalizedIds.get(i), db, fastVector);
                cModel.updateClassifier(next);
            }
            //cModel.updateClassifier(isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Test the model
        try {
            Evaluation eTest = new Evaluation(isTrainingSet);
            eTest.evaluateModel(cModel, isTrainingSet);
            // Print the result à la Weka explorer:
            String strSummary = eTest.toSummaryString();
            System.out.println(strSummary);

            System.out.println(cModel.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    public static void main(String[] args) throws Exception {

        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;

        DBConnector db = new DBConnector(dbName);

        //DBConnectorToNormalizer dbNorm = new DBConnectorToNormalizer(dbName, "DBConnectorTestLearning");
        //List<Post> unprocessedPosts = dbNorm.getReservedPosts();
        // Возьмем для примера один из обработанных постов

        // Извлекаем из БД количество постов
        int postNum = db.getPostCount();
        System.out.println("Getting number of posts from DB:");
        System.out.println("\t" + postNum);
        System.out.println("\n============\n");

        // Извлекаем из БД количество нормализованных постов
        int postNormalizedNum = db.getPostNormalizedCount();
        System.out.println("Getting number of normalized posts from DB:");
        System.out.println("\t" + postNormalizedNum);
        System.out.println("\n============\n");

        // Посты с 1 по 50 про статистику и детское
        List<Long> normalizedIds = new ArrayList<Long>();

        normalizedIds.clear();
        for (int i = 1; i < 51; i++) {
            normalizedIds.add((long) i);
        }
        Instances isTrainingSet = getDataset(normalizedIds, db);
        // assume Instances inputData is your dataset which has already been loaded
        AttributeSelection selector = new AttributeSelection();
        LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
        Ranker rank = new Ranker(); // The default parameters for Ranker and AttributeSelection are
        //appropriate for LSA. You can adjust the LSA options as desired.

        selector.setEvaluator(lsa);

        selector.setSearch(rank);

// the methods in the next two lines can throw exceptions, so you have to
        //deal with those appropriately selecter.SelectAttributes(inputData);

        selector.SelectAttributes(isTrainingSet);

        Instances newTrainingSet = selector.reduceDimensionality(isTrainingSet);
        //selecter.numberAttributesSelected();

        //System.out.print(newTrainingSet.toSummaryString());

        System.out.println("\n============\n");

        System.out.println("Число аттрибутов в TrainingSet: " + isTrainingSet.numAttributes());

        System.out.println("Число аттрибутов в newTrainingSet: " + newTrainingSet.numAttributes());

        System.out.println("\n============\n");

        System.out.println("newTrainingSet:");
        System.out.println(newTrainingSet.toString());

        System.out.println("\n============\n");

        //newTrainingSet.na
        System.out.println("Тестируем NaiveBayes на TrainingSet):");
        testNaiveBayes(isTrainingSet);
        System.out.println("Тестируем NaiveBayes на newTrainingSet):");
        testNaiveBayes(newTrainingSet);

    }
}
