package bayes;

import data.NGram;
import data.Post;
import db.DBConnectorToNormalizer;
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
import db.DBConnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;

public class BayesFromDB {


    public static int getWordCountInPost(int postId) {
        return 4;
    }

    public static List<String> getAllnGrammsFromDB(List<Long> normalizedIds, DBConnector db) throws SQLException {
        Set<String> ngramsSet = new HashSet<String>();
        for (Long id : normalizedIds) {
            List<NGram> allNGram = new ArrayList<NGram>();
            allNGram = db.getAllNGramNames(id);
            for (NGram a : allNGram) {
                ngramsSet.add(a.getText());
            }
        }
        List<String> ngramsList = new ArrayList<String>(ngramsSet);
        return ngramsList;
    }

    public static String[] getAllTagsFromDB() {
        String[] a = {"статистика", "детское"};
        return a;
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

        System.out.println(cModel.toString());

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
        ///*
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


        //List<String> nGrammsAll = getAllnGrammsFromDB(normalizedIds, db);


        Instances isTrainingSet = getDataset(normalizedIds, db);
        //System.out.print(isTrainingSet.toString());
        //System.out.println(isTrainingSet.toSummaryString());

        // assume Instances inputData is your dataset which has already been loaded
        AttributeSelection selecter = new AttributeSelection();
        LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
        Ranker rank = new Ranker(); // The default parameters for Ranker and AttributeSelection are
        //appropriate for LSA. You can adjust the LSA options as desired.

        selecter.setEvaluator(lsa);

        selecter.setSearch(rank);

// the methods in the next two lines can throw exceptions, so you have to
        //deal with those appropriately selecter.SelectAttributes(inputData);

        selecter.SelectAttributes(isTrainingSet);

        Instances newTrainingSet = selecter.reduceDimensionality(isTrainingSet);
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
