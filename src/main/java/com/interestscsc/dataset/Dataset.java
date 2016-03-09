package com.interestscsc.dataset;

import com.interestscsc.data.NGram;
import com.interestscsc.db.DBConnector;
import org.apache.log4j.Logger;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.LatentSemanticAnalysis;
import weka.attributeSelection.Ranker;
import weka.core.*;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


public class Dataset {

    private final static int MIN_NUMBER_POSTS_OF_TAG = 150;
    private final static int MAX_NUMBER_POSTS_OF_TAG = 1000000000;
    private static final Logger logger = Logger.getLogger(Dataset.class);
    private final static int minAllowedNGramCount = 2;

    private List<String> tags;

    private AttributeSelection selector;
    private ArrayList attributeVector;
    private Map<Long, List<NGram>> postsNGrams;
    private Map<String, Integer> totalNGramsListIndexes;
    private List<Long> normalizedIdsTrain;
    private List<Long> normalizedIdsTest;

    public static final String prefix = "Tag_";

    public Dataset(DBConnector db) throws SQLException {
        selector = null;
        postsNGrams = new HashMap<>();
        tags = db.getTopNormalizedTagNamesByOffset(MIN_NUMBER_POSTS_OF_TAG, MAX_NUMBER_POSTS_OF_TAG);
        logger.info("Number of popular tags: " + tags.size());
    }

    public Dataset(List<String> wantedTags) throws SQLException {
        selector = null;
        postsNGrams = new HashMap<>();
        tags = wantedTags;
        logger.info("Number of popular tags: " + tags.size());
    }

    public Set<String> getAllNGramsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<>();
        for (Long id : normalizedIds) {
            List<NGram> allNGram = db.getAllNGramNames(id, DBConnector.NGramType.UNIGRAM);
            logger.info("Number of nGram by post_id " + id + " : " + allNGram.size());
            ngramsSet.addAll(allNGram.stream().map(NGram::getText).collect(Collectors.toSet()));
            postsNGrams.put(id, allNGram.stream().distinct().collect(Collectors.toList()));
        }
        logger.info("Finish getting set of nGram");
        return ngramsSet;
    }

    private void setNGramAttributeIndex(List<String> ngramsList) {
        int i = 0;
        totalNGramsListIndexes = new HashMap<>();
        for (String nGram : ngramsList) {
            totalNGramsListIndexes.put(nGram, i++);
        }
    }

    public Instances getDataset(List<Long> normalizedIds, DBConnector db, Set<String> totalNGramsList) throws SQLException, IllegalArgumentException {
        if (attributeVector == null) {
            throw new IllegalArgumentException("No attributes for dataset were provided. Set them using 'public void " +
                    "setAttributes(List<String> attributes, List<String> tags)' before calling this" +
                    "method to provide unique format of dataset.");
        }
        logger.info("Getting dataset...");
        Instances isTrainingSet = new Instances("Rel", attributeVector, normalizedIds.size());
        /**
         * Set class index
         * нумерация с 0, так что последний элемент - Tag.
         */
        isTrainingSet.setClassIndex(totalNGramsList.size());

        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = getProperTagName(db, postId);

            List<NGram> allNGram = postsNGrams.get(postId);
            if (allNGram.size() < minAllowedNGramCount) {
                continue;
            }

            for (String tagOfPost : allTagsOfPost) {
                logger.info("Post " + postId + ":  ");
                Instance iExample = new DenseInstance(1, new double[attributeVector.size()]);
                allNGram.stream().filter(nGram -> totalNGramsListIndexes.containsKey(nGram.getText())).forEach(nGram -> {
                    /**
                     * Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                     */
                    iExample.setValue(totalNGramsListIndexes.get(nGram.getText()), (double) nGram.getUsesCnt());
                });
                iExample.setValue((Attribute) attributeVector.get(totalNGramsList.size()), tagOfPost);
                //attributeVector.get()
                isTrainingSet.add(iExample);
            }
        }
        return isTrainingSet;
    }

    public Instances getMultilabelDataset(List<Long> normalizedIds, DBConnector db) throws SQLException, IllegalArgumentException {
        if (attributeVector == null) {
            throw new IllegalArgumentException("No attributes for dataset were provided. Set them using 'public void " +
                    "setAttributes(List<String> attributes, List<String> tags)' before calling this" +
                    "method to provide unique format of dataset.");
        }
        logger.info("Getting Multilabeldataset...");
        /**
         * Create an empty training set
         */
        Instances isTrainingSet = new Instances("Rel", attributeVector, normalizedIds.size());
        /**
         * сначала должны идти теги, ставим номер класса равным количеству тегов.
         */
        isTrainingSet.setClassIndex(this.tags.size());

        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = getProperTagName(db, postId);
            allTagsOfPost = assertTagNames(allTagsOfPost);

            List<NGram> allNGram = postsNGrams.get(postId);
            if (allNGram.size() < minAllowedNGramCount) {
                continue;
            }

            logger.info("Post " + postId + ":  ");
            Instance iExample = new DenseInstance(1, new double[attributeVector.size()]);

            for (String tag: allTagsOfPost) {
                /**
                 * ставим 1, если тег есть, 0 по дефолту будет
                 */
                Attribute g = (Attribute) attributeVector.get(totalNGramsListIndexes.get(tag));
                iExample.setValue((Attribute) this.attributeVector.get(totalNGramsListIndexes.get(tag)), "1");
            }

            for (NGram nGram : allNGram) {
                if (totalNGramsListIndexes.containsKey(nGram.getText())) {
                    iExample.setValue(totalNGramsListIndexes.get(nGram.getText()), (double)nGram.getUsesCnt());
                }
            }

            isTrainingSet.add(iExample);
        }
        return isTrainingSet;
    }

    private List<String> getProperTagName(DBConnector db, Long postId) throws SQLException {
        List<String> allTagsOfPost = db.getAllTagNames(postId);
        allTagsOfPost.removeIf(tag -> !tags.contains(tag));
        //allTagsOfPost = assertTagNames(allTagsOfPost);
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

    public void setAttributes(Set<String> attributes) {
        attributeVector = new ArrayList(attributes.size() + 1);
        for (String nGram : attributes) {
            attributeVector.add(new Attribute(nGram));
        }
        List<String> allFeatures = new ArrayList<String>();
        allFeatures.addAll(attributes);
        setNGramAttributeIndex(allFeatures);
        ArrayList fvClassVal = new ArrayList(tags.size());
        tags.forEach(fvClassVal::add);
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        attributeVector.add(ClassAttribute);
    }

    public void setMultilabelAttributes(Set<String> attributes) {
        attributeVector = new ArrayList(attributes.size() + tags.size());
        /**
         * какие значения могут принимать столбцы с тегами (1 - соответствует тегу, 0 - не соответствует
          */
        List<String> allowedValuesForTags = new ArrayList<String>();
        allowedValuesForTags.add("0");
        allowedValuesForTags.add("1");
        List<String> assertedTags = assertTagNames(tags);
        for (String tag : assertedTags) {
            attributeVector.add(new Attribute(tag, allowedValuesForTags));
        }
        for (String nGram : attributes) {
            attributeVector.add(new Attribute(nGram));
        }
        List<String> allFeatures = new ArrayList<String>();
        allFeatures.addAll(assertedTags);
        allFeatures.addAll(attributes);
        setNGramAttributeIndex(allFeatures);
    }

    /**
     * Чтобы имена тегов не путались с именами нграмм, добавляем в начало префикс
      */
    public static List<String> assertTagNames(List<String> list) {
        return list.stream().map(entry -> prefix + entry).collect(Collectors.toList());
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
}
