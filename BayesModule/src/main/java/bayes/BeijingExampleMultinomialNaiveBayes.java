package bayes;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BeijingExampleMultinomialNaiveBayes {

    public static final Map<String, Integer> vocabulary = new HashMap();

    static {
        vocabulary.put("Beijing", 0);
        vocabulary.put("Chinese", 1);
        vocabulary.put("Japan", 2);
        vocabulary.put("Macao", 3);
        vocabulary.put("Shanghai", 4);
        vocabulary.put("Tokyo", 5);
    }

    public static Instance getNextInstance(FastVector fvWekaAttributes, int[] occurrencesCount, String label) {
        Instance iExample1 = new Instance(occurrencesCount.length + 1);
        for (int i = 0; i < occurrencesCount.length; i++) {
            iExample1.setValue((Attribute) fvWekaAttributes.elementAt(i), occurrencesCount[i]);
        }
        //System.out.println(((Attribute) fvWekaAttributes.elementAt(occurrencesCount.length)).isString());
        iExample1.setValue((Attribute) fvWekaAttributes.elementAt(occurrencesCount.length), label);
        return iExample1;
    }

    public static Instance getInstanceForSentence(FastVector fvWekaAttributes, String[] words, String label) {
        int[] occurrencesCount = getOccurrencesCount(words);
        return getNextInstance(fvWekaAttributes, occurrencesCount, label);
    }

    public static int[] getOccurrencesCount(String[] words) {
        int[] result = new int[vocabulary.size()];
        for (String word : words) {
            result[vocabulary.get(word)] += 1;
        }
        return result;
    }

    public static FastVector getBeijingVector() {
        FastVector fvNominalVal = new FastVector(2);
        fvNominalVal.addElement("c"); // about China
        fvNominalVal.addElement("j"); // about Japan
        Attribute ClassAttribute = new Attribute("country", fvNominalVal);
        FastVector fvWekaAttributes = new FastVector(7);
        String[] attributeNames = new String[vocabulary.size()];
        for (String word : vocabulary.keySet()) {
            attributeNames[vocabulary.get(word)] = word;
        }
        for (String attributeName : attributeNames) {
            fvWekaAttributes.addElement(new Attribute(attributeName));
        }
        fvWekaAttributes.addElement(ClassAttribute);
        //System.out.println(((Attribute) fvWekaAttributes.elementAt(6)).isNominal());
        return fvWekaAttributes;
    }

    public static FastVector getBeijingVectorNoClass() {
        FastVector fvWekaAttributes = new FastVector(6);
        String[] attributeNames = new String[vocabulary.size()];
        for (String word : vocabulary.keySet()) {
            attributeNames[vocabulary.get(word)] = word;
        }
        for (String attributeName : attributeNames) {
            fvWekaAttributes.addElement(new Attribute(attributeName));
        }
        //System.out.println(((Attribute) fvWekaAttributes.elementAt(6)).isNominal());
        return fvWekaAttributes;
    }

    public static Instances getBeijingDataset() {
        FastVector attributes = getBeijingVector();
        Instances isTrSet = new Instances("Rel", attributes, 2);
        isTrSet.setClassIndex(6);
        //String[] words = {"Chinese", "Beijing", "Chinese"};
        String[][] wc = {{"Chinese", "Beijing", "Chinese"}, {"Chinese", "Shanghai", "Chinese"}, {"Chinese", "Macao"}};
        for (String[] words : wc) {
            Instance a = getInstanceForSentence(attributes, words, "c");
            isTrSet.add(a);
        }
        String[][] wj = {{"Chinese", "Japan", "Tokyo"}};
        for (String[] words : wj) {
            Instance a = getInstanceForSentence(attributes, words, "j");
            isTrSet.add(a);
        }
        return isTrSet;
    }

    public static void testOnBeijing() throws Exception {

        Instances beijingDataset = getBeijingDataset();
        System.out.println(beijingDataset.toString());


        Classifier cModel = new NaiveBayesMultinomial();
        cModel.buildClassifier(beijingDataset);

        // Test the model
        Evaluation eTest = new Evaluation(beijingDataset);
        eTest.evaluateModel(cModel, beijingDataset);

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        Instance iExample5 = new Instance(6);

        FastVector beijingVector = getBeijingVectorNoClass();

        // Chinese Chinese Chinese Tokyo Japan
        iExample5.setValue((Attribute) beijingVector.elementAt(0), 0);
        iExample5.setValue((Attribute) beijingVector.elementAt(1), 3);
        iExample5.setValue((Attribute) beijingVector.elementAt(2), 1);
        iExample5.setValue((Attribute) beijingVector.elementAt(3), 0);
        iExample5.setValue((Attribute) beijingVector.elementAt(4), 0);
        iExample5.setValue((Attribute) beijingVector.elementAt(5), 1);

        iExample5.setDataset(beijingDataset);

        System.out.println("Chinese Chinese Chinese Tokyo Japan");
        printDistribution(cModel, iExample5);
        System.out.println(cModel.toString());

    }

    private static void printDistribution(final Classifier cModel, final Instance iExample) {
        try {
            double[] fDistribution = cModel.distributionForInstance(iExample);
            System.out.println(fDistribution[0]);
            System.out.println(fDistribution[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception {

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
        // Create the instance
        for (int i = 0; i < 10; i++) {
            Instance iExample = new Instance(4);
            double a = Math.random();
            System.out.print(a + " ");
            iExample.setValue((Attribute) fvWekaAttributes.elementAt(0), a);
            a = Math.random();
            System.out.print(a + " ");
            iExample.setValue((Attribute) fvWekaAttributes.elementAt(1), a);
            //int answer = rn.nextInt(10) + 1;
            int b = rn.nextInt(3);
            System.out.print(values[b] + " ");
            iExample.setValue((Attribute) fvWekaAttributes.elementAt(2), values[b]);
            b = rn.nextInt(2);
            System.out.print(answers[b] + " ");
            iExample.setValue((Attribute) fvWekaAttributes.elementAt(3), answers[b]);

            System.out.println("");
            // add the instance
            isTrainingSet.add(iExample);
        }


        // Create a naïve bayes classifier
        Classifier cModel = new NaiveBayes();
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

        if (eTest == null) {
            return;
        }

        try {
            eTest.evaluateModel(cModel, isTrainingSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        Instance iExample = new Instance(3);
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(0), 1.0);
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(1), 0.5);
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(2), "gray");

        // Get the likelihood of each classes
        // fDistribution[0] is the probability of being “positive”
        // fDistribution[1] is the probability of being “negative”
        iExample.setDataset(isTrainingSet);
        printDistribution(cModel, iExample);

        testOnBeijing();
    }
}
