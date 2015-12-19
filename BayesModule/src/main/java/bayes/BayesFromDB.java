package bayes;

import data.NGram;
import dataset.LSA;
import weka.attributeSelection.*;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.bayes.NaiveBayesMultinomialUpdateable;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import db.DBConnector;

import java.sql.SQLException;
import java.util.*;

public class BayesFromDB {


    //public static String tag1 = "лингвистическое";
    //public static String tag2 = "путешествии";

    public static String tag1 = "детское";
    public static String tag2 = "статистика";

    public static int getWordCountInPost(int postId) {
        return 4;
    }

    public static List<String> getAllnGrammsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<String>();
        for (Long id : normalizedIds) {
            //List<NGram> allNGram = new ArrayList<NGram>();
            List<NGram> allNGram = db.getAllNGramNames(id);
            for (NGram a : allNGram) {
                ngramsSet.add(a.getText());
            }
        }
        List<String> ngramsList = new ArrayList<String>(ngramsSet);
        return ngramsList;
    }

    public static Map getnGramAttributeIndex(List<String> ngramsList) {
        int i = 0;
        Map<String, Integer> nGramToAttributeIndex = new HashMap<String, Integer>();
        for (String nGramm : ngramsList) {
            //attributes.addElement(new Attribute(nGramm.getText()));
            nGramToAttributeIndex.put(nGramm, i++);
        }
        return nGramToAttributeIndex;
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

    public static int getDocumentCountFromDB() {
        return 2;
    }

    public static int getnGrammCountFromDB(String nGramm, int postId) {
        if (postId == 0) {
            if (nGramm.equals("зверь")) {
                return 2;
            }
            if (nGramm.equals("машина")) {
                return 0;
            }
            if (nGramm.equals("собака")) {
                return 2;
            }
        }
        if (postId == 1) {
            if (nGramm.equals("зверь")) {
                return 1;
            }
            if (nGramm.equals("машина")) {
                return 3;
            }
            if (nGramm.equals("собака")) {
                return 0;
            }
        }
        return 0;
    }

    public static String[] getPostTagFromDB(int postId) {
        if (postId == 0) {
            String[] a = {"животные"};
            return a;
        } else {
            String[] a = {"авто"};
            return a;
        }
    }

    public static Instances getDataset(List<Long> normalizedIds, DBConnector db) throws SQLException {
        System.out.print("Getting dataset...");
        List<String> nGramms = getAllnGrammsFromDB(normalizedIds, db);
        FastVector fvWekaAttributes = new FastVector(nGramms.size() + 1);
        ///*
        for (String nGramm : nGramms) {
            fvWekaAttributes.addElement(new Attribute(nGramm));
        }
        //*/
        /*
        for (int i = 0; i < nGramms.size(); i++) {
            fvWekaAttributes.addElement(new Attribute("A" + String.valueOf(i)));
        }
        */

        List<String> allTags = getAllTagsFromDB(normalizedIds, db);

        System.out.println("Number of TAGS\n\n" + allTags.size() + "\n\n");

        FastVector fvClassVal = new FastVector(allTags.size());
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

            ///*
            if (allTagsOfPost.contains(tag1)) {
                allTagsOfPost.clear();
                allTagsOfPost.add(tag1);
                //System.out.println(tag1);
            }
            if (allTagsOfPost.contains(tag2)) {
                allTagsOfPost.clear();
                allTagsOfPost.add(tag2);
                //System.out.println(tag2);
            }
            //*/

            /*
            String mainTag = allTagsOfPost.get(0);
            System.out.println(mainTag);
            allTagsOfPost.clear();
            System.out.println(mainTag);
            allTagsOfPost.add(mainTag);
            //*/

            List<NGram> allNGram = new ArrayList<NGram>();
            allNGram = db.getAllNGramNames(postId);
            if (allNGram.size() < 4) {
                continue;
            }
            Map<String, Integer> ngramMap = new HashMap<String, Integer>();
            for (NGram ngram: allNGram) {
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
                    //int wordCountInPost = getnGrammCountFromDB(nGramms[i], postId);
                    //System.out.println(wordCountInPost);
                    // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                    double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
                    iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), relWordCountInPost);
                    //System.out.print(relWordCountInPost + "   ");
                }
                System.out.println(tagOfPost);
                iExample.setValue((Attribute) fvWekaAttributes.elementAt(nGramms.size()), tagOfPost);
                isTrainingSet.add(iExample);
            }
        }
        return isTrainingSet;
    };

    /*
    public static Instances getDatasetwithFastVector(List<Long> normalizedIds, DBConnector db, FastVector fastVector) throws SQLException {
        System.out.print("Getting dataset...");
        List<String> nGramms = getAllnGrammsFromDB(normalizedIds, db);


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
            }
            if (allTagsOfPost.contains("детское")) {
                allTagsOfPost.clear();
                allTagsOfPost.add("детское");
            }

            List<NGram> allNGram = new ArrayList<NGram>();
            allNGram = db.getAllNGramNames(postId);
            Map<String, Integer> ngramMap = new HashMap<String, Integer>();
            for (NGram ngram: allNGram) {
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
                    //int wordCountInPost = getnGrammCountFromDB(nGramms[i], postId);
                    //System.out.println(wordCountInPost);
                    // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                    double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
                    iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), relWordCountInPost);
                    //System.out.print(relWordCountInPost + "   ");
                }
                System.out.println(tagOfPost);
                iExample.setValue((Attribute) fvWekaAttributes.elementAt(nGramms.size()), tagOfPost);
                isTrainingSet.add(iExample);
            }
        }
        return isTrainingSet;
    }
    */


    public static void testNaiveBayesMultinomial(Instances isTrainingSet) throws SQLException {
        /*
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
            }
            if (allTagsOfPost.contains("детское")) {
                allTagsOfPost.clear();
                allTagsOfPost.add("детское");
            }

            List<NGram> allNGram = new ArrayList<NGram>();
            allNGram = db.getAllNGramNames(postId);
            Map<String, Integer> ngramMap = new HashMap<String, Integer>();
            for (NGram ngram: allNGram) {
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
                    //int wordCountInPost = getnGrammCountFromDB(nGramms[i], postId);
                    //System.out.println(wordCountInPost);
                    // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                    double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
                    iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), relWordCountInPost);
                    //System.out.print(relWordCountInPost + "   ");
                }
                System.out.println(tagOfPost);
                iExample.setValue((Attribute) fvWekaAttributes.elementAt(nGramms.size()), tagOfPost);
                isTrainingSet.add(iExample);
            }
        }
        */

        //Instances isTrainingSet = getDataset(normalizedIds, db);

        Classifier cModel = (Classifier)new NaiveBayesMultinomial();
        try {
            cModel.buildClassifier(isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Test the model
        Evaluation eTest = null;
        try {
            eTest = new Evaluation(isTrainingSet);
            eTest.evaluateModel(cModel, isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        /*
        // зверь
        System.out.println("Text to predict: \"зверь\"");
        Instance iExample = new Instance(nGramms.length);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), 1);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), 0);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), 0);

        iExample.setDataset(isTrainingSet);
        try {
            double[] fDistribution = cModel.distributionForInstance(iExample);
            for (int i = 0; i < allTags.length; i++) {
                System.out.println("Probability that it is " + allTags[i] + " is: " + fDistribution[i]);
            }
            //System.out.println(fDistribution[0]);
            //System.out.println(fDistribution[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // зверь зверь
        System.out.println("Text to predict: \"зверь зверь\"");
        Instance iExample2 = new Instance(nGramms.length);
        iExample2.setValue((Attribute)fvWekaAttributes.elementAt(0), 2);
        iExample2.setValue((Attribute)fvWekaAttributes.elementAt(1), 0);
        iExample2.setValue((Attribute)fvWekaAttributes.elementAt(2), 0);

        iExample2.setDataset(isTrainingSet);
        try {
            double[] fDistribution = cModel.distributionForInstance(iExample2);
            for (int i = 0; i < allTags.length; i++) {
                System.out.println("Probability that it is " + allTags[i] + " is: " + fDistribution[i]);
            }
            //System.out.println(fDistribution[0]);
            //System.out.println(fDistribution[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }


        // собака машина
        System.out.println("Text to predict: \"собака машина\"");
        Instance iExample3 = new Instance(nGramms.length);
        iExample3.setValue((Attribute)fvWekaAttributes.elementAt(0), 0);
        iExample3.setValue((Attribute)fvWekaAttributes.elementAt(1), 1);
        iExample3.setValue((Attribute)fvWekaAttributes.elementAt(2), 1);

        iExample3.setDataset(isTrainingSet);
        try {
            double[] fDistribution = cModel.distributionForInstance(iExample3);
            for (int i = 0; i < allTags.length; i++) {
                System.out.println("Probability that it is " + allTags[i] + " is: " + fDistribution[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        System.out.println(cModel.toString());

    }

    public static void testNaiveBayes(Instances isTrainingSet) throws SQLException {

        Classifier cModel = (Classifier)new NaiveBayes();
        try {
            cModel.buildClassifier(isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Test the model
        Evaluation eTest = null;
        try {
            eTest = new Evaluation(isTrainingSet);
            eTest.evaluateModel(cModel, isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        //System.out.println(cModel.toString());

    }


    public static Instance getInstance(Long normalizedId, DBConnector db, FastVector fvWekaAttributes) throws SQLException {
        List<Long> normalizedIds = new ArrayList<Long>();
        normalizedIds.add(normalizedId);
        //int totalWordCountInPost = getWordCountInPost(postId);
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

        List<NGram> allNGram = new ArrayList<NGram>();
        allNGram = db.getAllNGramNames(normalizedId);
        Map<String, Integer> ngramMap = new HashMap<String, Integer>();
        for (NGram ngram: allNGram) {
            ngramMap.put(ngram.getText(), ngram.getUsesCnt());
        }
        for (int i = 0; i < nGramms.size(); i++) {
            int wordCountInPost = 0;
            if (ngramMap.containsKey(nGramms.get(i))) {
                wordCountInPost = ngramMap.get(nGramms.get(i));
                System.out.print(nGramms.get(i) + " " + wordCountInPost + "   ");
            }
            //int wordCountInPost = getnGrammCountFromDB(nGramms[i], postId);
            //System.out.println(wordCountInPost);
            // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
            double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
            iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), relWordCountInPost);
            //System.out.print(relWordCountInPost + "   ");
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

            List<String> allTags = getAllTagsFromDB(normalizedIds, db);
            FastVector fvClassVal = new FastVector(allTags.size());
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
        Evaluation eTest = null;
        try {
            eTest = new Evaluation(isTrainingSet);
            eTest.evaluateModel(cModel, isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        System.out.println(cModel.toString());

    }

    public static Instances getReducedDataset(Instances isTrainingSet, String method, double R) throws Exception {

        //Object asEvaluation = new Object();
        AttributeSelection selecter = new AttributeSelection();
        Ranker rank = new Ranker();
        Instances newTrainingSet = isTrainingSet;
        if (method.equals("lsa")) {
            LSA asEvaluation = new LSA();
            asEvaluation.setRank(R);
            selecter.setEvaluator(asEvaluation);

            selecter.setSearch(rank);

            selecter.SelectAttributes(isTrainingSet);

            newTrainingSet = selecter.reduceDimensionality(isTrainingSet);
        } else {
            ASEvaluation asEvaluation = new PrincipalComponents();
            selecter.setEvaluator(asEvaluation);

            selecter.setSearch(rank);

            selecter.SelectAttributes(isTrainingSet);

            newTrainingSet = selecter.reduceDimensionality(isTrainingSet);
        }
        //PrincipalComponents pc = new PrincipalComponents();
        //ChiSquaredAttributeEval sc = new ChiSquaredAttributeEval();
        //FilteredAttributeEval fa = new FilteredAttributeEval();
        //LSA lsa = new LSA();
        //LatentSemanticAnalysis lsa2 = new LatentSemanticAnalysis();
        //Ranker rank = new Ranker(); // The default parameters for Ranker and AttributeSelection are
        //appropriate for LSA. You can adjust the LSA options as desired.

        //selecter.setEvaluator(asEvaluation);

        //selecter.setSearch(rank);

        //selecter.SelectAttributes(isTrainingSet);

        //Instances newTrainingSet = selecter.reduceDimensionality(isTrainingSet);
        return  newTrainingSet;
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

        /*
        // Извлекаем из БД список всех н-грамм
        List<String> allNGramNames = db.getAllNGramNames();
        System.out.println("Getting all nGramNames from DB:");
        //for (String nGramName : allNGramNames)
        //    System.out.println("\t" + nGramName);
        System.out.println("\n============\n");

        System.out.print(allNGramNames.size());
        */

        // Извлекаем из БД список всех н-грамм для конкретного поста
        /*
        allNGramNames = db.getAllNGramNames(postId);
        System.out.println("Getting all nGramNames by postId=" +
                postId + " from DB:");
        for (String nGramName : allNGramNames)
            System.out.println("\t" + nGramName);
        System.out.println("\n============\n");
        */

        // Извлекаем из БД список всех тегов
        /*
        List<String> allTagNames = db.getAllTagNames();
        System.out.println("Getting all tagNames from DB:");
        //for (String tagName : allTagNames)
            //System.out.println("\t" + tagName);
        System.out.println("\n============\n");
        //*/

        //System.out.println(allTagNames.size());


        // Извлекаем из БД список всех тегов для конкретного поста
        /*
        allTagNames = db.getAllTagNames(postId);
        System.out.println("Getting all tagNames by postId=" +
                postId + " from DB:");
        for (String tagName : allTagNames)
            System.out.println("\t" + tagName);
        System.out.println("\n============\n");
        //*/


        /*
        List<NGram> allNGram = new ArrayList<NGram>();

        List<Integer> trueProcessedPostId = new ArrayList<Integer>();
        for (int processedPostId = 1; processedPostId < 56; processedPostId++) {
            allNGram = db.getAllNGramNames(processedPostId);
            if (allNGramNames.size() > 0) {
                trueProcessedPostId.add(processedPostId);
            }
        }
        */

       //List<Long> normalizedIds = db.getAllPostNormalizedIds();

        //System.out.println(normalizedIds.size());



        /*
        Set<String> AllProcessedTagsUnique = new HashSet<String>();
        Set<String> AllProcessedNgramsUnique = new HashSet<String>();
        MultiSet<String> AllProcessedTags = new SimpleMultiSet<String>();
        MultiSet<String> AllProcessedNgrams = new SimpleMultiSet<String>();
        for (Integer processedPostId : trueProcessedPostId) {
            AllProcessedTagsUnique.addAll(db.getAllTagNames(processedPostId));
            AllProcessedTags.addAll(db.getAllTagNames(processedPostId));
            AllProcessedNgramsUnique.addAll(db.getAllNGramNames(processedPostId));
            AllProcessedNgrams.addAll(db.getAllNGramNames(processedPostId));
        }

        System.out.println("\n============\n");

        System.out.println(AllProcessedTags.size());
        System.out.println(AllProcessedTagsUnique.size());
        System.out.println(AllProcessedNgrams.size());
        System.out.println(AllProcessedNgramsUnique.size());

        System.out.println("\n============\n");

        Set<String> FrequentTagsUnique = new HashSet<String>();
        Iterator it = AllProcessedTagsUnique.iterator();
        while (it.hasNext()) {
            Object tag = it.next();
            if (AllProcessedTags.count(tag) > 1) {
                FrequentTagsUnique.add((String)tag);
            }
        }

        System.out.println("\n============\n");

        it = FrequentTagsUnique.iterator();
        while (it.hasNext()) {
            Object tag = it.next();
            System.out.println(tag + "   " + AllProcessedTags.count(tag));
        }

        System.out.println("\n============\n");

        Set<String> FrequentNgramsUnique = new HashSet<String>();
        it = AllProcessedNgramsUnique.iterator();
        while (it.hasNext()) {
            Object ngrams = it.next();
            if (AllProcessedNgrams.count(ngrams) > 1) {
                FrequentNgramsUnique.add((String)ngrams);
            }
        }

        System.out.println("\n============\n");

        it = FrequentNgramsUnique.iterator();
        while (it.hasNext()) {
            Object ngrams = it.next();
            System.out.println(ngrams + "   " + AllProcessedNgrams.count(ngrams));
        }

        System.out.println("\n============\n");

        /*
        File inputFile = new File("/home/jamsic/IdeaProjects/WekaBayesFull" + File.separator + "tag.txt");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(inputFile, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        */

        /*
        Set<Long> PostIdWithTagStatistics = new HashSet<Long>();
        Set<Long> PostIdWithTagChildren = new HashSet<Long>();
        Set<Long> PostIdWithTagHistory = new HashSet<Long>();

        for (Long processedPostId : normalizedIds) {
            List<String> a = db.getAllTagNames(processedPostId);
            //if (a.contains("history")) {
            //    PostIdWithTagHistory.add(processedPostId);
            //}
            if (a.contains("детское")) {
                PostIdWithTagChildren.add(processedPostId);
            }
            if (a.contains("статистика")) {
                PostIdWithTagStatistics.add(processedPostId);
            }
        }

        /*
        List<NGram> allNGram = new ArrayList<NGram>();

        List<String> allTagNames = db.getAllTagNames(2);

        allNGram = db.getAllNGramNames(2);

        for (String a : allTagNames) {
            System.out.print(a);
        }
        */

        // Посты с 1 по 50 про статистику и детское.

        List<Long> normalizedIds = new ArrayList<Long>();

        normalizedIds.clear();

        /*
        for (int i = 1; i < 51; i++) {
            normalizedIds.add(new Long(i));
        }
        //*/

        /*
        for (int i = 1; i < 6; i++) {
            normalizedIds.add(new Long(i));
        }
        for (int i = 45; i < 51; i++) {
            normalizedIds.add(new Long(i));
        }
        //*/


        normalizedIds.addAll(db.getAllPostNormalizedIds(tag1, tag2));
        normalizedIds = new ArrayList<Long>(new HashSet<Long>(normalizedIds));
        System.out.println(normalizedIds.size());



        /*
        List<Long> allPostNormalizedIds = db.getAllPostNormalizedIds();
        System.out.println("\nallPostNormalizedIds.size()\n====\n" + allPostNormalizedIds.size());
        int numberOfPosts = 100;
        while (normalizedIds.size() < numberOfPosts) {
            //System.out.println(normalizedIds.size() + " " + numberOfPosts);
            int nextPostNumber = (int) (Math.random() * allPostNormalizedIds.size());
            if (!normalizedIds.contains(allPostNormalizedIds.get(nextPostNumber))) {
                normalizedIds.add(allPostNormalizedIds.get(nextPostNumber));
            }
        }

        System.out.println(normalizedIds.size());
        //*/


        //List<String> nGrammsAll = getAllnGrammsFromDB(normalizedIds, db);

        //System.out.println(nGrammsAll.size());


        Instances isTrainingSet = getDataset(normalizedIds, db);
        //System.out.print(isTrainingSet.toString());
        //System.out.println(isTrainingSet.toSummaryString());

        // assume Instances inputData is your dataset which has already been loaded
        /*
        AttributeSelection selecter = new AttributeSelection();
        PrincipalComponents pc = new PrincipalComponents();
        ChiSquaredAttributeEval sc = new ChiSquaredAttributeEval();
        FilteredAttributeEval fa = new FilteredAttributeEval();
        LSA lsa = new LSA();
        LatentSemanticAnalysis lsa2 = new LatentSemanticAnalysis();
        Ranker rank = new Ranker(); // The default parameters for Ranker and AttributeSelection are
        //appropriate for LSA. You can adjust the LSA options as desired.

        selecter.setEvaluator(lsa2);

        selecter.setSearch(rank);

        selecter.SelectAttributes(isTrainingSet);

        Instances newTrainingSet = selecter.reduceDimensionality(isTrainingSet);
        //*/

        //Instances newTrainingSetpc = getReducedDataset(isTrainingSet, "pc");
        double rank = 0.99999999999;
        Instances newTrainingSet = getReducedDataset(isTrainingSet, "lsa", rank);

        //selecter.numberAttributesSelected();

        //System.out.print(newTrainingSet.toSummaryString());

        //System.out.print(newTrainingSet.toString());

        System.out.println("\n============\n");

        System.out.println("Число постов: " + isTrainingSet.numInstances());

        System.out.println("Число аттрибутов в TrainingSet: " + isTrainingSet.numAttributes());

        System.out.println("Число аттрибутов в newTrainingSet: " + newTrainingSet.numAttributes());

        System.out.println(rank);

        //System.out.println("Число аттрибутов в newTrainingSetpc: " + newTrainingSetpc.numAttributes());

        System.out.println("\n============\n");

        System.out.println("newTrainingSet:");
        //System.out.println(newTrainingSet.toString());

        System.out.println("\n============\n");

        System.out.println(isTrainingSet.toSummaryString());

        //newTrainingSet.na
        System.out.println("Тестируем NaiveBayes на TrainingSet):");
        testNaiveBayes(isTrainingSet);
        System.out.println("Тестируем NaiveBayes на newTrainingSet):");
        testNaiveBayes(newTrainingSet);
        //System.out.println("Тестируем NaiveBayes на newTrainingSetpc):");
        //testNaiveBayes(newTrainingSetpc);

        /*
        System.out.println("pc");
        for (String option : pc.getOptions()) {
            System.out.println(option);
        }
        //System.out.println(pc.getOptions());
        System.out.println(pc.globalInfo());
        System.out.println(pc.listOptions());

        System.out.println("lsa");
        for (String option : lsa.getOptions()) {
            System.out.println(option);
        }
        System.out.println(lsa.globalInfo());
        System.out.println(lsa.listOptions());
        //*/



// the transformed data is now in the outputData object



        //LatentSemanticAnalysis a = new LatentSemanticAnalysis();
        //a.buildEvaluator(isTrainingSet);
        //Instances newTrainingSet = a.transformedData(isTrainingSet);

        //System.out.println(newTrainingSet.toString());

        //testNaiveBayesMultinomial(newTrainingSet);

        //testNaiveBayesMultinomialUpdeatable(normalizedIds, db);


        /*
        normalizedIds.clear();
        for (int i = 1; i < 26; i++) {
            normalizedIds.add(new Long(i));
        }

        List<String> nGrammsChil = getAllnGrammsFromDB(normalizedIds, db);

        normalizedIds.clear();
        for (int i = 26; i < 51; i++) {
            normalizedIds.add(new Long(i));
        }

        List<String> nGrammsStat = getAllnGrammsFromDB(normalizedIds, db);

        System.out.println(nGrammsAll.size());
        System.out.println(nGrammsChil.size());
        System.out.println(nGrammsStat.size());
        System.out.println(nGrammsStat.size() + nGrammsChil.size() - nGrammsAll.size());

        /*
        System.out.println("\nСтатистика\n");

        Iterator it = PostIdWithTagStatistics.iterator();
        while (it.hasNext()) {
            System.out.print(it.next() + "   ");
        }

        System.out.println("\nдетское\n");

        it = PostIdWithTagChildren.iterator();
        while (it.hasNext()) {
            System.out.print(it.next() + "   ");
        }

        System.out.println("\nhistory\n");

        it = PostIdWithTagHistory.iterator();
        while (it.hasNext()) {
            System.out.print(it.next() + "   ");
        }
        */

        /*
        it = AllProcessedTagsUnique.iterator();
        while (it.hasNext()) {
            Object ngrams = it.next();
            System.out.println(ngrams + "   " + AllProcessedTags.count(ngrams));
            writer.println(ngrams + "," + AllProcessedTags.count(ngrams));
        }
        writer.close();
        //*/

        //testNaiveBayesMultinomial();



    }
}
