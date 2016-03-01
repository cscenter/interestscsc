package com.interestscsc.dataset;

import com.interestscsc.data.NGram;
import com.interestscsc.db.DBConnector;
import meka.classifiers.multilabel.BCC;
import meka.core.Result;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.LatentSemanticAnalysis;
import weka.attributeSelection.Ranker;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.*;

import java.sql.SQLException;
import java.util.*;

import static meka.classifiers.multilabel.Evaluation.testClassifier;

/**
 * Created by jamsic on 12.12.15.
 */
public class Dataset2 {

    public static String tag1 = "детское";
    public static String tag2 = "статистика";

    //public static String[] tags = {};

    private AttributeSelection selecter;
    private FastVector attributeVector;
    private Map<String, Integer> totalnGramsListIndexes;
    private List<Long> normalizedIdsTrain;
    private List<Long> normalizedIdsTest;
    private List<String> tags;


    public Dataset2() {
        this.selecter = null;
    }

    public static List<String> getAllnGrammsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<String>();
        for (Long id : normalizedIds) {
            List<NGram> allNGram = null;
            //List<NGram> allNGram = db.getAllNGramNames(id);
            for (NGram a : allNGram) {
                ngramsSet.add(a.getText());
            }
        }
        List<String> ngramsList = new ArrayList<String>(ngramsSet);
        return ngramsList;
    }

    /*
    public static List<String> getAllTagsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        /*
        Set <String> tags = new HashSet<String>();
        for (Long normalizedId : normalizedIds) {
            //List<String> allTagNames = db.getAllTagNames(normalizedId);
            List<String> allTagNames = db.getAllTagNames(normalizedId);
            tags.addAll(allTagNames);
        }
        //*/
    /*
        List tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        return tags;
    }
    */

    ///*
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    //*/

    private void setnGramAttributeIndex(List<String> ngramsList) {
        int i = 0;
        this.totalnGramsListIndexes = new HashMap<String, Integer>();
        for (String nGramm : ngramsList) {
            this.totalnGramsListIndexes.put(nGramm, i++);
        }
    }

    public Instances getDataset(List<Long> normalizedIds, DBConnector db, List<String> totalnGramsList) throws Exception {
        if (this.attributeVector == null) {
            throw new Exception("No attributes for dataset were provided. Set them using 'public void " +
                    "setAttributes(List<String> attributes, List<String> tags)' before calling this" +
                    "method to provide unique format of dataset.");
        }
        System.out.print("Getting dataset...");
        // Create an empty training set
        int documentCount = normalizedIds.size();
        Instances isTrainingSet = new Instances("Rel", this.attributeVector, documentCount);
        // Set class index
        isTrainingSet.setClassIndex(totalnGramsList.size()); // нумерация с 0 же, последний элемент - Tag.

        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = getProperTagName(db, postId);

            List<NGram> allNGram = null;
            //List<NGram> allNGram = db.getAllNGramNames(postId);
            if (allNGram.size() < 2) {
                continue;
            }
            Map<String, Integer> ngramMap = new HashMap<String, Integer>();
            for (NGram ngram: allNGram) {
                ngramMap.put(ngram.getText(), ngram.getUsesCnt());
            }

            for (String tagOfPost : allTagsOfPost) {
                System.out.print("Post " + postId + ":  ");
                Instance iExample = new DenseInstance(1, new double[this.attributeVector.size()]);
                for (NGram nGram : allNGram) {
                    if (totalnGramsListIndexes.containsKey(nGram.getText())) {
                        int wordCountInPost = nGram.getUsesCnt();
                        // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                        double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
                        iExample.setValue(totalnGramsListIndexes.get(nGram.getText()), relWordCountInPost);
                        System.out.print(nGram.getText() + " ");
                    }
                }
                System.out.println(tagOfPost);
                iExample.setValue((Attribute) this.attributeVector.elementAt(totalnGramsList.size()), tagOfPost);
                isTrainingSet.add(iExample);
            }
        }
        return isTrainingSet;
    }

    public Instances getMultiLabelDataset(List<Long> normalizedIds, DBConnector db, List<String> totalnGramsList) throws Exception {
        if (this.attributeVector == null) {
            throw new Exception("No attributes for dataset were provided. Set them using 'public void " +
                    "setAttributes(List<String> attributes, List<String> tags)' before calling this" +
                    "method to provide unique format of dataset.");
        }
        // сколько постов с более чем 1 классом
        int multiLabelNum = 0;
        //System.out.println("Getting dataset...");
        // Create an empty training set
        int documentCount = normalizedIds.size();
        Instances isTrainingSet = new Instances("Rel", this.attributeVector, documentCount);
        // Set class index
        //System.out.print("this.attributeVector.size() " + this.attributeVector.size());
        isTrainingSet.setClassIndex(this.tags.size()); // нумерация с 0 же, последний элемент - Tag.

        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = getProperTagName(db, postId);
            if (allTagsOfPost.size() > 1) {
                multiLabelNum += 1;
            }

            List<NGram> allNGram = null;
            //List<NGram> allNGram = db.getAllNGramNames(postId);
            if (allNGram.size() < 2) {
                continue;
            }

            //System.out.println("this.tags.size() + allNGram.size() " + this.tags.size() + allNGram.size());

            //List<String> allFeatures = new ArrayList<String>();
            //allFeatures.addAll(allTagsOfPost);
            //allFeatures.addAll(allNGram);

            Map<String, Integer> ngramMap = new HashMap<String, Integer>();
            for (NGram ngram: allNGram) {
                ngramMap.put(ngram.getText(), ngram.getUsesCnt());
            }

            //for (String tagOfPost : allTagsOfPost) {
            System.out.print(" " + postId);
            Instance iExample = new DenseInstance(1, new double[this.attributeVector.size()]);
            //iExample.
            /*
            for (String tag : assertTagNames(this.tags, "Tag_")) {
                System.out.print(tag + " is 0; ");
                iExample.setValue((Attribute) this.attributeVector.elementAt(totalnGramsListIndexes.get(tag)), "0");
            }
            //*/

            ///*
            for (String tag: allTagsOfPost) {
                iExample.setValue((Attribute) this.attributeVector.elementAt(totalnGramsListIndexes.get(tag)), "1");
                //System.out.print(tag + " ");
            }
            //*/

            for (NGram nGram : allNGram) {
                if (totalnGramsListIndexes.containsKey(nGram.getText())) {
                    int wordCountInPost = nGram.getUsesCnt();
                    // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                    double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
                    iExample.setValue(totalnGramsListIndexes.get(nGram.getText()), relWordCountInPost);
                    //System.out.print(nGram.getText() + " ");
                }
            }
                //iExample.setValue((Attribute) this.attributeVector.elementAt(totalnGramsList.size()), tagOfPost);
            isTrainingSet.add(iExample);
            //}
        }
        System.out.println("\n multiLabelNum: " + multiLabelNum);
        return isTrainingSet;
    }

    private List<String> getProperTagName(DBConnector db, Long postId) throws SQLException {
        List<String> allTagsOfPost = db.getAllTagNames(postId);
        // TO DO retain
        List<String> properTagsOfPost = new ArrayList<String>();
        for (String tag : this.tags) {
            if (allTagsOfPost.contains(tag)) {
                properTagsOfPost.add(tag);
            }
        }
        properTagsOfPost = assertTagNames(properTagsOfPost, "Tag_");
        return properTagsOfPost;
    }


    /*
    public static void testClassifier(Instances isTrainingSet) throws Exception {

        Classifier cModel = new NaiveBayes();
        cModel.buildClassifier(isTrainingSet);

        // Test the model
        Evaluation eTest = new Evaluation(isTrainingSet);
        eTest.evaluateModel(cModel, isTrainingSet);

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        //System.out.println(cModel.toString());

    }
    */

    /*
    public static void validateClassifier(Classifier cModel, Instances isTrainingSet) throws Exception {
        Evaluation eTest = new Evaluation(isTrainingSet);
        eTest.evaluateModel(cModel, isTrainingSet);

        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);
    }

    public static Classifier trainClassifier(Instances isTrainingSet) throws Exception {
        Classifier cModel = new NaiveBayes();
        cModel.buildClassifier(isTrainingSet);
        return cModel;
    }
    */

    public void setParametersForLSA(Instances isTrainingSet, double R) throws Exception {
        this.selecter = new AttributeSelection();
        Ranker rank = new Ranker();
        LatentSemanticAnalysis asEvaluation = new LatentSemanticAnalysis();
        asEvaluation.setMaximumAttributeNames(Integer.MAX_VALUE);
        asEvaluation.setRank(R);
        this.selecter.setEvaluator(asEvaluation);
        this.selecter.setSearch(rank);
        this.selecter.SelectAttributes(isTrainingSet);
    }

    public Instances getLSAReducedDataset(Instances set) throws Exception {
        if (this.selecter == null) {
            throw new Exception("No parameters for LSA were set. Set them using 'public void " +
                    "setParametersForLSA(Instances isTrainingSet, double R)' before calling this" +
                    "method.");
        }
        Instances newTrainingSet = this.selecter.reduceDimensionality(set);
        return  newTrainingSet;
    }

    public List<Long> getNormalizedIds(DBConnector db) throws SQLException {
        List<Long> normalizedIds = db.getAllPostNormalizedIds(this.tags);
        return normalizedIds;
    }

    public void setAttributesForMultiLabel(List<String> ngrams, List<String> tags) {
        this.attributeVector = new FastVector(ngrams.size() + tags.size());
        List<String> allFeatures = new ArrayList<String>();
        allFeatures.addAll(assertTagNames(tags, "Tag_"));
        allFeatures.addAll(ngrams);
        List<String> a = new ArrayList<String>();
        a.add("0");
        a.add("1");
        for (String tag : assertTagNames(tags, "Tag_")) {
            attributeVector.addElement(new Attribute(tag, a));
        }
        for (String feature : ngrams) {
            attributeVector.addElement(new Attribute(feature));
        }
        setnGramAttributeIndex(allFeatures);
        //System.out.print("ngrams.size() " + ngrams.size() + "     " + this.attributeVector.size());
    }

    public void setAttributes(List<String> attributes, List<String> tags) {
        this.attributeVector = new FastVector(attributes.size() + 1);
        for (String nGramm : attributes) {
            attributeVector.addElement(new Attribute(nGramm));
        }
        setnGramAttributeIndex(attributes);
        FastVector fvClassVal = new FastVector(tags.size());
        for (String tag : tags) {
            fvClassVal.addElement(tag);
        }
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        this.attributeVector.addElement(ClassAttribute);
    }

    public void splitToTrainAndTest(List<Long> normalizedIds, double ratio) {
        this.normalizedIdsTrain = new ArrayList<Long>(normalizedIds);
        this.normalizedIdsTest = new ArrayList<Long>();

        // рандомно собираю normalizedIdsTest
        int numberOfPostsInTestingSet = (int)(normalizedIds.size() * ratio);
        //System.out.println(numberOfPostsInTestingSet);
        while (this.normalizedIdsTest.size() < numberOfPostsInTestingSet) {
            int nextPostNumber = (int) (Math.random() * normalizedIds.size());
            if (!this.normalizedIdsTest.contains(normalizedIds.get(nextPostNumber))) {
                this.normalizedIdsTest.add(normalizedIds.get(nextPostNumber));
            }
        }
        this.normalizedIdsTrain.removeAll(this.normalizedIdsTest);
        //System.out.println(this.normalizedIdsTrain.size() + "!!!" + this.normalizedIdsTest.size());
    }

    public List<Long> getNormalizedIdsTrain() {
        return this.normalizedIdsTrain;
    }

    public List<Long> getNormalizedIdsTest() {
        return this.normalizedIdsTest;
    }

    public static List<String> assertTagNames(List<String> list, String preffix) {
        List<String> newList = new ArrayList<String>();
        for (String entry : list) {
            newList.add(preffix + entry);
        }
        return newList;
    }

    public static double estimateClassification(Result result, Instances dataset, List<String> tags) throws Exception {
        double [][] s = result.allPredictions();
        double sumWin = 0;
        for (int j = 0; j < dataset.size(); j++) {
            for (int i = 0; i < tags.size(); i++) {
                //System.out.print(s[j][i]);
                //System.out.print(dataset.get(j).value(i));
                //System.out.print("   ");
                if (s[j][i] == dataset.get(j).value(i) && s[j][i] == 1.0) {
                    sumWin += 1;
                    //System.out.println(" " + i + "   " + j + " " + sumWin);
                    break;
                }
            }
            //System.out.println("");
        }

        //System.out.println(sumWin);
        if (dataset.size() < 1) {
            throw new Exception("dataset.size() < 1");
        }
        return sumWin / dataset.size();
    }

    public static double mean(List<Double> list) {
        double sum = 0;
        for (double a: list) {
            sum += a;
        }
        return sum / list.size();
    }

    public static void main(String[] args) throws Exception {

        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;

        DBConnector db = new DBConnector(dbName);

        // нужно подумать, как набирать датасет

        //Dataset dataset = new Dataset();

        // 0.1 - количество теста относительно всего сета, т. е. 1/10 test, 9/10 - train
        //dataset.splitToTrainAndTest(normalizedIds, 0.1);
        //List<Long> normalizedIdsTrain = dataset.getNormalizedIdsTrain();
        //List<Long> normalizedIdsTest = dataset.getNormalizedIdsTest();

        List<String> allTags = new ArrayList<String>();
        //allTags.add("психология");
        //allTags.add("памятники");
        //allTags.add("стихи");
        allTags.add("зарубежная архитектура");
        allTags.add("музеи");

        //allTags = assertTagNames(allTags, "Tag_");

        /*
        List<String>  allnGrammsFromDB = getAllnGrammsFromDB(normalizedIdsTrain, db);

        dataset.setAttributes(allnGrammsFromDB, allTags);

        Instances isTrainingSet = dataset.getDataset(normalizedIdsTrain, db, allnGrammsFromDB);
        Instances isTestingSet = dataset.getDataset(normalizedIdsTest, db, allnGrammsFromDB);

        System.out.println("Число постов: " + isTrainingSet.numInstances());

        System.out.println("Число аттрибутов в TrainingSet: " + isTrainingSet.numAttributes());

        Classifier classifier = trainClassifier(isTrainingSet);
        System.out.println("ошибка на исходном множестве:");
        validateClassifier(classifier, isTrainingSet);
        System.out.println("ошибка на тестовом множестве:");
        validatemultiLabelNumClassifier(classifier, isTestingSet);


        dataset.setParametersForLSA(isTrainingSet, 0.9999);
        Instances newTrainingSet = dataset.getLSAReducedDataset(isTrainingSet);
        System.out.println("Число аттрибутов в newTrainingSet: " + newTrainingSet.numAttributes());
        Instances newTestingSet = dataset.getLSAReducedDataset(isTestingSet);
        System.out.println(newTrainingSet.equalHeaders(newTestingSet));

        Classifier naiveBayes2 = trainClassifier(newTrainingSet);
        System.out.println("ошибка на исходном множестве:");
        validateClassifier(naiveBayes2, newTrainingSet);
        System.out.println("ошибка на тестовом множестве:");
        validateClassifier(naiveBayes2, newTestingSet);
        */


        Dataset2 data = new Dataset2();
        data.setTags(allTags);
        List<Long> normalizedIds = data.getNormalizedIds(db);
        System.out.println("Total number of posts: " + normalizedIds.size());

        int repeatNumber = 1;

        List<Double> CVpercents = new ArrayList<Double>();
        //CVpercents.add(0.5);
        //CVpercents.add(0.3);
        CVpercents.add(0.2);
        //CVpercents.add(0.1);
        //CVpercents.add(0.05);
        //Estimator estimator = new Estimator();

        List<Double> measureOnTrainingSet = new ArrayList<Double>();
        List<Double> measureOnTestingSet = new ArrayList<Double>();

        for (double percent: CVpercents) {
            List<Double> measureOnTrainingSetCV = new ArrayList<Double>();
            List<Double> measureOnTestingSetCV = new ArrayList<Double>();
            for (int i = 0; i < repeatNumber; i++) {
                data.splitToTrainAndTest(normalizedIds, percent);
                List<Long> normalizedIdsTrain = data.getNormalizedIdsTrain();
                List<Long> normalizedIdsTest = data.getNormalizedIdsTest();
                List<String> allnGrammsFromDB = getAllnGrammsFromDB(normalizedIdsTrain, db);
                data.setAttributesForMultiLabel(allnGrammsFromDB, allTags);
                System.out.println("Getting training set " + normalizedIdsTrain.size());
                Instances isTrainingSet = data.getMultiLabelDataset(normalizedIdsTrain, db, allnGrammsFromDB);
                System.out.println("Getting testing set " + normalizedIdsTest.size());
                Instances isTestingSet = data.getMultiLabelDataset(normalizedIdsTest, db, allnGrammsFromDB);

                BCC bcc = new BCC();
                bcc.setClassifier(new NaiveBayes());
                bcc.buildClassifier(isTrainingSet);

                Result trainResult = testClassifier(bcc, isTrainingSet);
                Result testResult = testClassifier(bcc, isTestingSet);

                measureOnTrainingSetCV.add(estimateClassification(trainResult, isTrainingSet, allTags));
                measureOnTestingSetCV.add(estimateClassification(testResult, isTestingSet, allTags));
                //estimator.estimate(trainResult, isTrainingSet, 0, allTags.size());
                //double a = ((double)estimator.getTruePositive()) / isTrainingSet.size();
                //System.out.println("~~~~~on training set  " + a);
                //estimator.estimate(testResult, isTestingSet, 0, allTags.size());
                //a = ((double)estimator.getTruePositive()) / isTestingSet.size();
                //System.out.println("~~~~~on testing set  " + a);
            }
            System.out.println("");
            System.out.println(percent + ":");
            System.out.println("measureOnTrainingSetCV");
            for (double trainingMeasure : measureOnTrainingSetCV) {
                System.out.print(trainingMeasure + "   ");
            }
            System.out.println("");
            System.out.println("measureOnTestingSetCV");
            for (double testingMeasure : measureOnTestingSetCV) {
                System.out.print(testingMeasure + "   ");
            }
            System.out.println("");

            measureOnTrainingSet.add(mean(measureOnTrainingSetCV));
            measureOnTestingSet.add(mean(measureOnTestingSetCV));
        }

        System.out.println("CV percents:");
        for (double percent : CVpercents) {
            System.out.print(percent + "   ");
        }
        System.out.println("");
        System.out.println("On training set:");
        for (double measure : measureOnTrainingSet) {
            System.out.print(measure + "   ");
        }
        System.out.println("");
        System.out.println("On testing set:");
        for (double measure : measureOnTestingSet) {
            System.out.print(measure + "   ");
        }
        System.out.println("");

        /*
        System.out.println("TN " + estimator.getTrueNegative());
        System.out.println("TP " + estimator.getTruePositive());
        System.out.println("FN " + estimator.getFalseNegative());
        System.out.println("FP " + estimator.getFalsePositive());
        System.out.println(estimator.getAccuracy());
        System.out.println(estimator.getPrecision());
        System.out.println(estimator.getRecall());
        System.out.println(estimator.getFMeasure());
        */

        /*
        data.splitToTrainAndTest(normalizedIds, 0.2);
        List<Long> normalizedIdsTrain = data.getNormalizedIdsTrain();
        System.out.println(normalizedIdsTrain.size());
        List<Long> normalizedIdsTest = data.getNormalizedIdsTest();
        System.out.println(normalizedIdsTest.size());
        List<String>  allnGrammsFromDB = getAllnGrammsFromDB(normalizedIdsTrain, db);
        System.out.println(allnGrammsFromDB.size());

        data.setAttributesForMultiLabel(allnGrammsFromDB, allTags);
        //Instances isTrainingSet = data.getMultiLabelDataset(normalizedIdsTrain, db, allnGrammsFromDB);
        Instances isTestingSet = data.getMultiLabelDataset(normalizedIdsTest, db, allnGrammsFromDB);
        System.out.println("===");
        System.out.println(isTestingSet.numAttributes());
        System.out.println(isTestingSet.size());
        //System.out.println(isTestingSet.equalHeaders(isTrainingSet));
        System.out.println("Число постов: " + isTestingSet.toString());
        */

        /*
        BCC bcc = new BCC();
        bcc.setClassifier(new NaiveBayes());
        bcc.buildClassifier(isTestingSet);

        Result resultBCC = testClassifier(bcc, isTestingSet);
        System.out.println(resultBCC);
        System.out.println(resultBCC.size());
        double [][] s = resultBCC.allPredictions();
        for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < s[i].length; j++) {
                System.out.print(s[i][j] + " ");
            }
            System.out.print("\n");
        }
        System.out.println(resultBCC.allPredictions());
        //*/

        /*
        double sumWin = 0;
        for (int j = 0; j < isTestingSet.size(); j++) {
            for (int i = 0; i < data.tags.size(); i++) {
                System.out.print(s[j][i]);
                System.out.print(isTestingSet.get(j).value(i));
                System.out.print("   ");
                if (s[j][i] == isTestingSet.get(j).value(i) && s[j][i] == 1.0) {
                    sumWin += 1;
                    System.out.println(" " + i + "   " + j + " " + sumWin);
                    break;
                }
            }
            System.out.println("");
        }

        System.out.println(sumWin);
        System.out.println(sumWin / isTestingSet.size());

        System.out.println(estimateClassification(resultBCC, isTestingSet, data.tags));
        //System.out.print(isTestingSet.get(2).value(0));

        //System.out.print(isTrainingSet.trainCV(2, 0));
        */

    }
}
