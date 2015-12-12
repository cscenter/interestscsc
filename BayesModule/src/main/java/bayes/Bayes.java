package bayes;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Random;

public class Bayes {

    public static int getWordCountInPost(int postId) {
        return 4;
    }

    public static String[] getAllnGrammsFromDB() {
        String[] a = {"зверь", "машина", "собака"};
        return a;
    }

    public static String[] getAllTagsFromDB() {
        String[] a = {"животные", "авто"};
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

    public static void testNaiveBayesMultinomial() {
        String[] nGramms = getAllnGrammsFromDB();
        FastVector fvWekaAttributes = new FastVector(nGramms.length + 1);
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
        int documentCount = getDocumentCountFromDB();
        Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, documentCount);
        // Set class index
        isTrainingSet.setClassIndex(nGramms.length); // нумерация с 0 же, последний элемент - Tag.

        System.out.println("Word:   " + nGramms[0] + " " + nGramms[1] + " " + nGramms[2]);
        for (int postId = 0; postId < documentCount; postId++) {
            String[] allTagsOfPost = getPostTagFromDB(postId);
            for (String tagOfPost : allTagsOfPost) {
                System.out.print("Post " + postId + ":  ");
                int totalWordCountInPost = getWordCountInPost(postId);
                Instance iExample = new Instance(nGramms.length + 1);
                for (int i = 0; i < nGramms.length; i++) {
                    int wordCountInPost = getnGrammCountFromDB(nGramms[i], postId);
                    //System.out.println(wordCountInPost);
                    // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                    double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
                    iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), relWordCountInPost);
                    System.out.print(relWordCountInPost + "   ");
                }
                System.out.println(tagOfPost);
                iExample.setValue((Attribute) fvWekaAttributes.elementAt(nGramms.length), tagOfPost);
                isTrainingSet.add(iExample);
            }
        }

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

        System.out.println(cModel.toString());


    }


    public static void main(String[] args) {

        System.out.println("Тут просто наивный Байес, тренированный на рандомных параметрах, ничего интересного");
        String[] values = {"blue", "gray", "black"};
        String[] answers = {"positive", "negative"};

        // Declare two numeric attributes
        Attribute Attribute1 = new Attribute("firstNumeric");
        Attribute Attribute2 = new Attribute("secondNumeric");

        // Declare a nominal attribute along with its values
        FastVector fvNominalVal = new FastVector(3);
        fvNominalVal.addElement("blue");
        fvNominalVal.addElement("gray");
        fvNominalVal.addElement("black");
        Attribute Attribute3 = new Attribute("aNominal", fvNominalVal);

        // Declare the class attribute along with its values
        FastVector fvClassVal = new FastVector(2);
        fvClassVal.addElement("positive");
        fvClassVal.addElement("negative");
        Attribute ClassAttribute = new Attribute("theClass", fvClassVal);

        // Declare the feature vector
        FastVector fvWekaAttributes = new FastVector(4);
        fvWekaAttributes.addElement(Attribute1);
        fvWekaAttributes.addElement(Attribute2);
        fvWekaAttributes.addElement(Attribute3);
        fvWekaAttributes.addElement(ClassAttribute);

        // Create an empty training set
        Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
        // Set class index
        isTrainingSet.setClassIndex(3);

        Random rn = new Random();
        double a = 0;
        int b = 0;
        // Create the instance
        for (int i = 0; i < 10; i++) {
            Instance iExample = new Instance(4);
            a = Math.random();
            System.out.print(a + " ");
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), a);
            a = Math.random();
            System.out.print(a + " ");
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), a);
            //int answer = rn.nextInt(10) + 1;
            b = rn.nextInt(3);
            System.out.print(values[b] + " ");
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), values[b]);
            b = rn.nextInt(2);
            System.out.print(answers[b] + " ");
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(3), answers[b]);

            System.out.println("");
            // add the instance
            isTrainingSet.add(iExample);
        }


        // Create a naïve bayes classifier
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            eTest.evaluateModel(cModel, isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        // Get the confusion matrix
        double[][] cmMatrix = eTest.confusionMatrix();


        //FastVector fvWekaAttributes2 = new FastVector(3);
        //fvWekaAttributes2.addElement(Attribute1);
        //fvWekaAttributes2.addElement(Attribute2);
        // fvWekaAttributes2.addElement(Attribute3);

        //Instances isTestingSet = new Instances("Rel", fvWekaAttributes2, 1);

        Instance iExample = new Instance(3);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), 1.0);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), 0.5);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), "gray");

        // add the instance
        //isTrainingSet.add(iExample);


        // Get the likelihood of each classes
        // fDistribution[0] is the probability of being “positive”
        // fDistribution[1] is the probability of being “negative”
        iExample.setDataset(isTrainingSet);
        try {
            double[] fDistribution = cModel.distributionForInstance(iExample);
            System.out.println(fDistribution[0]);
            System.out.println(fDistribution[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Обычный Байес закончился");

        testNaiveBayesMultinomial();

    }
}
