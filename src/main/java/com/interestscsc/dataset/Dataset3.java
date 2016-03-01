package com.interestscsc.dataset;

import com.interestscsc.data.NGram;
import com.interestscsc.db.DBConnector;
import meka.classifiers.multilabel.BCC;
import meka.core.Result;
import org.apache.log4j.Logger;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.LatentSemanticAnalysis;
import weka.attributeSelection.Ranker;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static meka.classifiers.multilabel.Evaluation.testClassifier;

//MULTILABEL DATASET
/**
 * Created by jamsic on 12.12.15.
 */
public class Dataset3 {

    private final static int MIN_NUMBER_POSTS_OF_TAG = 150;
    private final static int MAX_NUMBER_POSTS_OF_TAG = 1000000000;
    private static final Logger logger = Logger.getLogger(Dataset.class);

    private static List<String> tags;

    private AttributeSelection selector;
    private FastVector attributeVector;
    private Map<Long, List<NGram>> postsNGrams;
    private Map<String, Integer> totalNGramsListIndexes;
    private List<Long> normalizedIdsTrain;
    private List<Long> normalizedIdsTest;

    public Dataset3(DBConnector db) throws SQLException {
        selector = null;
        postsNGrams = new HashMap<>();
        tags = db.getTopNormalizedTagNamesByOffset(MIN_NUMBER_POSTS_OF_TAG, MAX_NUMBER_POSTS_OF_TAG);
        logger.info("Number of popular tags: " + tags.size());
    }

    public Dataset3(DBConnector db, List<String> wantedTags) throws SQLException {
        selector = null;
        postsNGrams = new HashMap<>();
        tags = wantedTags;
        logger.info("Number of popular tags: " + tags.size());
    }

    public Set<String> getAllNGramsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<>();
        for (Long id : normalizedIds) {
            List<NGram> allNGram = db.getAllNGramNames(id, DBConnector.NGramType.UNIGRAM);
            //logger.info("Number of nGram by post_id " + id + " : " + allNGram.size());
            ngramsSet.addAll(allNGram.stream().map(NGram::getText).collect(Collectors.toSet()));
            postsNGrams.put(id, allNGram.stream().distinct().collect(Collectors.toList()));
        }
        logger.info("Finish getting set of nGram");
        return ngramsSet;
    }

    // CHANGED to List, cause order matters!
    private void setNGramAttributeIndex(List<String> ngramsList) {
        int i = 0;
        totalNGramsListIndexes = new HashMap<>();
        for (String nGram : ngramsList) {
            totalNGramsListIndexes.put(nGram, i);
            i++;
        }
    }

    // CHANGED
    public Instances getDataset(List<Long> normalizedIds, DBConnector db) throws SQLException, IllegalArgumentException {
        if (attributeVector == null) {
            throw new IllegalArgumentException("No attributes for dataset were provided. Set them using 'public void " +
                    "setAttributes(List<String> attributes, List<String> tags)' before calling this" +
                    "method to provide unique format of dataset.");
        }
        logger.info("Getting dataset...");
        /**
         * Create an empty training set
         */
        Instances isTrainingSet = new Instances("Rel", attributeVector, normalizedIds.size());
        /**
         * Set class index
         * нумерация с 0, так что последний элемент - Tag.
         */
        //isTrainingSet.setClassIndex(totalNGramsList.size());
        /**
         * Теперь нумерация с 0, сначала должны идти теги, помечаем последний элемент.
         */
        isTrainingSet.setClassIndex(this.tags.size());

        // датасет ограничен, чтобы можно было посмотреть целиком, все строки под "to delete" нужно будет удалить
        // to delete
        int maxCount = 10;
        // to delete
        int count = 0;
        for (Long postId : normalizedIds) {
            // to delete
            if (count > maxCount) {
                break;
            }
            // to delete
            count++;
            List<String> allTagsOfPost = getProperTagName(db, postId);

            List<NGram> allNGram = postsNGrams.get(postId);
            if (allNGram.size() < 2) {
                continue;
            }

            //for (String tagOfPost : allTagsOfPost) {
                logger.info("Post " + postId + ":  ");
                Instance iExample = new DenseInstance(1, new double[attributeVector.size()]);

                // АААА Что это???!!!
                //allNGram.stream().filter(nGram -> totalNGramsListIndexes.containsKey(nGram.getText())).forEach(nGram -> {
                    /**
                     * Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                     */
                //    iExample.setValue(totalNGramsListIndexes.get(nGram.getText()), (double) nGram.getUsesCnt());
                //});

                //iExample.setValue((Attribute) attributeVector.elementAt(totalNGramsList.size()), tagOfPost);

                for (String tag: allTagsOfPost) {
                    // ставим 1, если тег есть, 0 по дефолту будет
                    //System.out.println("tag " + tag);
                    //System.out.println("index of " + totalNGramsListIndexes.get(tag));
                    Attribute g = (Attribute) attributeVector.elementAt(totalNGramsListIndexes.get(tag));
                    //System.out.println("is nominal " + g.isNominal());
                    iExample.setValue((Attribute) this.attributeVector.elementAt(totalNGramsListIndexes.get(tag)), "1");
                    //System.out.print(tag + " ");
                }

                //добавляем нграммы
                for (NGram nGram : allNGram) {
                    if (totalNGramsListIndexes.containsKey(nGram.getText())) {
                        iExample.setValue(totalNGramsListIndexes.get(nGram.getText()), (double)nGram.getUsesCnt());
                        //System.out.print(nGram.getText() + " ");
                    }
                }

                isTrainingSet.add(iExample);
            }
        //}
        return isTrainingSet;
    }

    private List<String> getProperTagName(DBConnector db, Long postId) throws SQLException {
        List<String> allTagsOfPost = db.getAllTagNames(postId);
        allTagsOfPost.removeIf(tag -> !tags.contains(tag));

        // added
        allTagsOfPost = assertTagNames(allTagsOfPost, "Tag_");

        return allTagsOfPost;
    }

    public void setParametersForLSA(Instances isTrainingSet, double R) throws Exception {
        selector = new AttributeSelection();
        Ranker rank = new Ranker();
        LatentSemanticAnalysis asEvaluation = new LatentSemanticAnalysis();
        asEvaluation.setMaximumAttributeNames(Integer.MAX_VALUE);
        asEvaluation.setRank(R);
        selector.setEvaluator(asEvaluation);
        selector.setSearch(rank);
        selector.SelectAttributes(isTrainingSet);
    }

    public Instances getLSAReducedDataset(Instances set) throws Exception {
        if (selector == null) {
            throw new Exception("No parameters for LSA were set. Set them using 'public void " +
                    "setParametersForLSA(Instances isTrainingSet, double R)' before calling this" +
                    "method.");
        }
        return selector.reduceDimensionality(set);
    }

    public List<Long> getNormalizedIds(DBConnector db) throws SQLException {
        return db.getAllPostNormalizedIds(tags);
    }

    // CHANGED
    public void setAttributes(Set<String> attributes) {
        attributeVector = new FastVector(attributes.size() + tags.size());
        // какие значения могут принимать столбцы с тегами (1 - соответствует тегу, 0 - не соответствует
        List<String> allowedValuesForTags = new ArrayList<String>();
        allowedValuesForTags.add("0");
        allowedValuesForTags.add("1");
        // добавляю теги
        for (String tag : assertTagNames(tags, "Tag_")) {
            //System.out.print("putting to dataset" + tag);
            Attribute a = new Attribute(tag, allowedValuesForTags);
            //System.out.println(" attribute is nominal " + a.isNominal());
            attributeVector.addElement(a);
        }
        // добавляю нграммы
        for (String nGram : attributes) {
            // new Attribute(nGram) значит, что числовой (по умолчанию)
            attributeVector.addElement(new Attribute(nGram));
        }
        // allFeatures -- полный набор атрибутов (теги+нграммы)
        List<String> allFeatures = new ArrayList<String>();
        allFeatures.addAll(assertTagNames(tags, "Tag_"));
        allFeatures.addAll(attributes);
        //System.out.println(allFeatures.containsAll(tags));
        setNGramAttributeIndex(allFeatures);
        //FastVector fvClassVal = new FastVector(tags.size());
        //tags.forEach(fvClassVal::addElement);
        //Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        //attributeVector.addElement(ClassAttribute);
    }

    // Чтобы имена тегов не путались с именами нграмм, добавляем в начало префикс
    // Где лучше хранить preffix? Эффективно ли делать это в функции?
    public static List<String> assertTagNames(List<String> list, String preffix) {
        List<String> newList = new ArrayList<String>();
        for (String entry : list) {
            newList.add(preffix + entry);
        }
        return newList;
    }

    public void splitToTrainAndTest(List<Long> normalizedIds, double ratio) {
        normalizedIdsTrain = normalizedIds;
        normalizedIdsTest = new ArrayList<>();

        /**
         * рандомно собираю normalizedIdsTest
         */
        int numberOfPostsInTestingSet = (int) (normalizedIds.size() * ratio);
        while (normalizedIdsTest.size() < numberOfPostsInTestingSet) {
            int nextPostNumber = (int) (Math.random() * normalizedIds.size());
            if (!normalizedIdsTest.contains(normalizedIds.get(nextPostNumber))) {
                normalizedIdsTest.add(normalizedIds.get(nextPostNumber));
            }
        }
        normalizedIdsTrain.removeAll(normalizedIdsTest);
    }

    public List<Long> getNormalizedIdsTrain() {
        return normalizedIdsTrain;
    }

    public List<Long> getNormalizedIdsTest() {
        return normalizedIdsTest;
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
        Dataset3 dataset3 = new Dataset3(db, allTags);
        //dataset3.
        List<Long> normalizedIds = dataset3.getNormalizedIds(db);
        Set<String> allNGramsFromDB  = dataset3.getAllNGramsFromDB(normalizedIds, db);
        dataset3.setAttributes(allNGramsFromDB);
        dataset3.splitToTrainAndTest(normalizedIds, 0.1);
        List<Long> normalizedIdsTrain = dataset3.getNormalizedIdsTrain();
        List<Long> normalizedIdsTest = dataset3.getNormalizedIdsTest();

        Instances isTrainingSet = null;
        Instances isTestingSet = null;

        isTrainingSet = dataset3.getDataset(normalizedIdsTrain, db);
        isTestingSet = dataset3.getDataset(normalizedIdsTest, db);
        System.out.println(isTrainingSet.toString());
        //System.out.print(isTrainingSet.a);
        //System.out.print(isTrainingSet.toString());

        BCC bcc = new BCC();
        bcc.setClassifier(new NaiveBayes());
        bcc.buildClassifier(isTrainingSet);
        Result trainResult = testClassifier(bcc, isTrainingSet);
        double a[][] = trainResult.allPredictions();
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                System.out.print(a[i][j] + "  ");
            }
            System.out.println();
        }
        //System.out.println(bcc.toString());
        //trainResult.

    };

}
