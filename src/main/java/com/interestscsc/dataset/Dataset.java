package com.interestscsc.dataset;

import com.interestscsc.data.NGram;
import com.interestscsc.db.DBConnector;
import org.apache.log4j.Logger;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.LatentSemanticAnalysis;
import weka.attributeSelection.Ranker;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jamsic on 12.12.15.
 */
public class Dataset {

    // if number of posts will be larger, we get OutOfMemoryException: Heap size
    private final static int MAX_POST_NUMBER = 2300;

    private static List<String> tags;

    private AttributeSelection selector;
    private FastVector attributeVector;
    private Map<Long, List<NGram>> postsNGrams;
    private Map<String, Integer> totalnGramsListIndexes;
    private Instances instancesTrain;
    private Instances instancesTest;
    private static final Logger logger = Logger.getLogger(Dataset.class);

    public Dataset() throws SQLException {
        selector = null;
    }

    public void setTagList(DBConnector db, int minScore, int maxScore) throws SQLException {
        tags = db.getTopNormalizedTagNames(minScore, maxScore);
        logger.info("Number of tags: " + tags.size());
    }

    public void setTagList(DBConnector db, long offset, long limit) throws SQLException {
        tags = db.getTopNormalizedTagNames(offset, limit);
        logger.info("Number of tags: " + tags.size());
    }

    public void setTagList(List<String> inputTags) throws SQLException {
        tags = inputTags;
        logger.info("Number of tags: " + tags.size());
    }

    public Set<String> getAllnGramsNamesFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        postsNGrams = db.getAllNGrams(normalizedIds, DBConnector.NGramType.UNIGRAM);
        //postsNGrams = db.getAllNGrams(normalizedIds);

        Set<String> nGramsSet = new HashSet<>();
        postsNGrams.values().forEach(nGramsList ->
                nGramsSet.addAll(nGramsList.stream().map(NGram::getText).collect(Collectors.toSet()))
        );
        return nGramsSet;
    }

    public Instances getDataset(List<Long> normalizedIds, DBConnector db) throws SQLException, IllegalArgumentException {
        if (attributeVector == null) {
            throw new IllegalArgumentException("No attributes for dataset were provided. Set them using 'public void " +
                    "setAttributes(List<String> attributes, List<String> tags)' before calling this" +
                    "method to provide unique format of dataset.");
        }
        logger.info("Getting dataset...");
        // Create an empty training set
        Instances instances = new Instances("Rel", attributeVector, normalizedIds.size());
        // Set class index
        instances.setClassIndex(attributeVector.size() - 1); // нумерация с 0 же, последний элемент - Tag.
        Map<Long, Integer> postsLength = db.getPostLength(normalizedIds);

        int numberPost = 0;
        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = getProperTagName(db, postId);

            List<NGram> allNGram = postsNGrams.get(postId);
            if (allNGram == null || allNGram.size() < 2) {
                continue;
            }

            for (String tagOfPost : allTagsOfPost) {
                logger.info("Post" + numberPost++ + " : " + postId);
                Instance iExample = new Instance(1, new double[attributeVector.size()]);
                allNGram.forEach(nGram ->
                        iExample.setValue(totalnGramsListIndexes.get(nGram.getText()), (double) nGram.getUsesCnt() / (double) postsLength.get(postId))
                );
                logger.info("Answer: " + tagOfPost);
                iExample.setValue((Attribute) attributeVector.elementAt(instances.classIndex()), tagOfPost);
                instances.add(iExample);
            }
        }
        return instances;
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

    public List<Long> getNormalizedPostIds(DBConnector db) throws SQLException {
        List<Long> posts = db.getAllPostNormalizedIds(tags);
        while (posts.size() > MAX_POST_NUMBER) {
            posts.remove(new Random().nextInt(posts.size()));
        }
        return posts;
    }

    public void setAttributes(final Set<String> attributes) {
        attributeVector = new FastVector(attributes.size() + 1);
        totalnGramsListIndexes = new HashMap<>(attributes.size());
        int i = 0;
        for (String nGramm : attributes) {
            attributeVector.addElement(new Attribute(nGramm));
            totalnGramsListIndexes.put(nGramm, i++);
        }

        FastVector fvClassVal = new FastVector(tags.size());
        tags.forEach(fvClassVal::addElement);
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        attributeVector.addElement(ClassAttribute);
    }

    public void splitToTrainAndTest(final Instances instances, double ratioTest) {
        Instances tmpInstances = instances.resample(new Random());

        int numberOfPostsInTestingSet = (int) (instances.numInstances() * ratioTest);
        int numberOfPostsInTrainingSet = tmpInstances.numInstances() - numberOfPostsInTestingSet;

        instancesTrain = new Instances("Rel", attributeVector, numberOfPostsInTrainingSet);
        instancesTrain.setClassIndex(attributeVector.size() - 1);

        instancesTest = new Instances("Rel", attributeVector, numberOfPostsInTestingSet);
        instancesTest.setClassIndex(attributeVector.size() - 1);
        for (int i = 0; i < tmpInstances.numInstances(); ++i) {
            if (i < numberOfPostsInTrainingSet) {
                instancesTrain.add(tmpInstances.instance(i));
            } else {
                instancesTest.add(tmpInstances.instance(i));
            }
        }
    }

    public Instances getInstancesTrain() {
        return instancesTrain;
    }

    public Instances getInstancesTest() {
        return instancesTest;
    }
}
