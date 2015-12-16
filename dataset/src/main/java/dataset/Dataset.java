package dataset;

import data.NGram;
import db.DBConnector;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by jamsic on 12.12.15.
 */
public class Dataset {

    public static String tag1 = "детское";
    public static String tag2 = "статистика";

    private AttributeSelection selecter;
    private FastVector attributeVector;
    private Map<String, Integer> totalnGramsListIndexes;
    private List<Long> normalizedIdsTrain;
    private List<Long> normalizedIdsTest;


    public Dataset() {
        this.selecter = null;
    }

    public static List<String> getAllnGrammsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<String>();
        for (Long id : normalizedIds) {
            List<NGram> allNGram = db.getAllNGramNames(id);
            for (NGram a : allNGram) {
                ngramsSet.add(a.getText());
            }
        }
        List<String> ngramsList = new ArrayList<String>(ngramsSet);
        return ngramsList;
    }

    public static List<String> getAllTagsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        /*
        Set <String> tags = new HashSet<String>();
        for (Long normalizedId : normalizedIds) {
            //List<String> allTagNames = db.getAllTagNames(normalizedId);
            List<String> allTagNames = db.getAllTagNames(normalizedId);
            tags.addAll(allTagNames);
        }
        //*/
        List tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        return new ArrayList<String>(tags);
    }

    private void setnGramAttributeIndex(List<String> ngramsList) {
        int i = 0;
        totalnGramsListIndexes = new HashMap<String, Integer>();
        for (String nGramm : ngramsList) {
            totalnGramsListIndexes.put(nGramm, i++);
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

            List<NGram> allNGram = db.getAllNGramNames(postId);
            if (allNGram.size() < 2) {
                continue;
            }
            Map<String, Integer> ngramMap = new HashMap<String, Integer>();
            for (NGram ngram: allNGram) {
                ngramMap.put(ngram.getText(), ngram.getUsesCnt());
            }

            for (String tagOfPost : allTagsOfPost) {
                System.out.print("Post " + postId + ":  ");
                Instance iExample = new Instance(1, new double[this.attributeVector.size()]);
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

    private static List<String> getProperTagName(DBConnector db, Long postId) throws SQLException {
        List<String> allTagsOfPost = db.getAllTagNames(postId);
        if (allTagsOfPost.contains(tag1)) {
            allTagsOfPost.clear();
            allTagsOfPost.add(tag1);
        }
        if (allTagsOfPost.contains(tag2)) {
            allTagsOfPost.clear();
            allTagsOfPost.add(tag2);
        }
        return allTagsOfPost;
    }

    ;

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

    public void setParametersForLSA(Instances isTrainingSet, double R) throws Exception {
        this.selecter = new AttributeSelection();
        Ranker rank = new Ranker();
        LSA asEvaluation = new LSA();
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

    public static List<Long> getNormalizedIds(DBConnector db) throws SQLException {
        List<Long> normalizedIds = db.getAllPostNormalizedIds(tag1, tag2);
        return normalizedIds;
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
        this.normalizedIdsTrain = normalizedIds;
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

    public static void main(String[] args) throws Exception {

        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;

        DBConnector db = new DBConnector(dbName);

        // нужно подумать, как набирать датасет
        List<Long> normalizedIds = getNormalizedIds(db);

        Dataset dataset = new Dataset();

        // 0.1 - количество теста относительно всего сета, т. е. 1/10 test, 9/10 - train
        dataset.splitToTrainAndTest(normalizedIds, 0.1);
        List<Long> normalizedIdsTrain = dataset.getNormalizedIdsTrain();
        List<Long> normalizedIdsTest = dataset.getNormalizedIdsTest();

        List<String> allTags = getAllTagsFromDB(normalizedIds, db);
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
        validateClassifier(classifier, isTestingSet);


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
    }
}
