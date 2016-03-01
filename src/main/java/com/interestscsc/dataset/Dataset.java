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

/**
 * Created by jamsic on 12.12.15.
 */
public class Dataset {

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

    public Dataset(DBConnector db) throws SQLException {
        selector = null;
        postsNGrams = new HashMap<>();
        tags = db.getTopNormalizedTagNamesByOffset(MIN_NUMBER_POSTS_OF_TAG, MAX_NUMBER_POSTS_OF_TAG);
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

    private void setNGramAttributeIndex(Set<String> ngramsList) {
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
        /**
         * Create an empty training set
         */
        Instances isTrainingSet = new Instances("Rel", attributeVector, normalizedIds.size());
        /**
         * Set class index
         * нумерация с 0, так что последний элемент - Tag.
         */
        isTrainingSet.setClassIndex(totalNGramsList.size());

        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = getProperTagName(db, postId);

            List<NGram> allNGram = postsNGrams.get(postId);
            if (allNGram.size() < 2) {
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
                iExample.setValue((Attribute) attributeVector.elementAt(totalNGramsList.size()), tagOfPost);
                isTrainingSet.add(iExample);
            }
        }
        return isTrainingSet;
    }

    private List<String> getProperTagName(DBConnector db, Long postId) throws SQLException {
        List<String> allTagsOfPost = db.getAllTagNames(postId);
        allTagsOfPost.removeIf(tag -> !tags.contains(tag));
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
        attributeVector = new FastVector(attributes.size() + 1);
        for (String nGram : attributes) {
            attributeVector.addElement(new Attribute(nGram));
        }
        setNGramAttributeIndex(attributes);
        FastVector fvClassVal = new FastVector(tags.size());
        tags.forEach(fvClassVal::addElement);
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        attributeVector.addElement(ClassAttribute);
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
