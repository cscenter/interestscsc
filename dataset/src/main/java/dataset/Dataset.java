package dataset;

import data.NGram;
import db.DBConnector;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
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

    private AttributeSelection selector;
    private FastVector attributeVector;
    private Map<String, Integer> totalnGramsListIndexes;
    private List<Long> normalizedIdsTrain;
    private List<Long> normalizedIdsTest;


    public Dataset() {
        selector = null;
    }

    public List<String> getAllnGrammsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<String>();
        for (Long id : normalizedIds) {
            List<NGram> allNGram = db.getAllNGramNames(id);
            for (NGram a : allNGram) {
                ngramsSet.add(a.getText());
            }
        }
        return new ArrayList<String>(ngramsSet);
    }

    public List<String> getAllTagsFromDB(List<Long> normalizedIds, DBConnector db) {
        /*
        Set <String> tags = new HashSet<String>();
        for (Long normalizedId : normalizedIds) {
            //List<String> allTagNames = db.getAllTagNames(normalizedId);
            List<String> allTagNames = db.getAllTagNames(normalizedId);
            tags.addAll(allTagNames);
        }
        //*/
        List<String> tags = new ArrayList<String>();
        tags.add(tag1);
        tags.add(tag2);
        return tags;
    }

    private void setnGramAttributeIndex(List<String> ngramsList) {
        int i = 0;
        totalnGramsListIndexes = new HashMap<String, Integer>();
        for (String nGramm : ngramsList) {
            totalnGramsListIndexes.put(nGramm, i++);
        }
    }

    public Instances getDataset(List<Long> normalizedIds, DBConnector db, List<String> totalnGramsList) throws SQLException, IllegalArgumentException {
        if (attributeVector == null) {
            throw new IllegalArgumentException("No attributes for dataset were provided. Set them using 'public void " +
                    "setAttributes(List<String> attributes, List<String> tags)' before calling this" +
                    "method to provide unique format of dataset.");
        }
        System.out.print("Getting dataset...");
        // Create an empty training set
        int documentCount = normalizedIds.size();
        Instances isTrainingSet = new Instances("Rel", attributeVector, documentCount);
        // Set class index
        isTrainingSet.setClassIndex(totalnGramsList.size()); // нумерация с 0 же, последний элемент - Tag.

        for (Long postId : normalizedIds) {
            List<String> allTagsOfPost = getProperTagName(db, postId);

            List<NGram> allNGram = db.getAllNGramNames(postId);
            if (allNGram.size() < 2) {
                continue;
            }

            for (String tagOfPost : allTagsOfPost) {
                System.out.print("Post " + postId + ":  ");
                Instance iExample = new Instance(1, new double[attributeVector.size()]);
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
                iExample.setValue((Attribute) attributeVector.elementAt(totalnGramsList.size()), tagOfPost);
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

    public void setParametersForLSA(Instances isTrainingSet, double R) throws Exception {
        selector = new AttributeSelection();
        Ranker rank = new Ranker();
        LSA asEvaluation = new LSA();
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

    public static List<Long> getNormalizedIds(DBConnector db) throws SQLException {
        return db.getAllPostNormalizedIds(tag1, tag2);
    }

    public void setAttributes(List<String> attributes, List<String> tags) {
        attributeVector = new FastVector(attributes.size() + 1);
        for (String nGramm : attributes) {
            attributeVector.addElement(new Attribute(nGramm));
        }
        setnGramAttributeIndex(attributes);
        FastVector fvClassVal = new FastVector(tags.size());
        for (String tag : tags) {
            fvClassVal.addElement(tag);
        }
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        attributeVector.addElement(ClassAttribute);
    }

    public void splitToTrainAndTest(List<Long> normalizedIds, double ratio) {
        normalizedIdsTrain = normalizedIds;
        normalizedIdsTest = new ArrayList<Long>();

        // рандомно собираю normalizedIdsTest
        int numberOfPostsInTestingSet = (int)(normalizedIds.size() * ratio);
        //System.out.println(numberOfPostsInTestingSet);
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
