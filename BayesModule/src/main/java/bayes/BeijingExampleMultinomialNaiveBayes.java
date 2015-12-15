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

public class BeijingExampleMultinomialNaiveBayes {

    public static Instance getNextInstance(FastVector fvWekaAttributes, int[] occurrencesCount, String label) {
        Instance iExample1 = new Instance(occurrencesCount.length + 1);
        for (int i = 0; i < occurrencesCount.length; i++) {
            iExample1.setValue((Attribute)fvWekaAttributes.elementAt(i), occurrencesCount[i]);
        }
        iExample1.setValue((Attribute)fvWekaAttributes.elementAt(occurrencesCount.length), label);
        return iExample1;
    }

    public static void testOnBeijing() {
        Attribute Attribute1 = new Attribute("Beijing"); // Beijing
        Attribute Attribute2 = new Attribute("Chinese"); // Chinese
        Attribute Attribute3 = new Attribute("Japan"); // Japan
        Attribute Attribute4 = new Attribute("Macao"); // Macao
        Attribute Attribute5 = new Attribute("Shanghai"); // Shanghai
        Attribute Attribute6 = new Attribute("Tokyo"); // Tokyo
        FastVector fvNominalVal = new FastVector(2);
        fvNominalVal.addElement("c"); // about China
        fvNominalVal.addElement("j"); // about Japan
        Attribute ClassAttribute = new Attribute("country", fvNominalVal);
        FastVector fvWekaAttributes = new FastVector(7);
        fvWekaAttributes.addElement(Attribute1);
        fvWekaAttributes.addElement(Attribute2);
        fvWekaAttributes.addElement(Attribute3);
        fvWekaAttributes.addElement(Attribute4);
        fvWekaAttributes.addElement(Attribute5);
        fvWekaAttributes.addElement(Attribute6);
        fvWekaAttributes.addElement(ClassAttribute);

        Instances isTrSet = new Instances("Rel", fvWekaAttributes, 2);
        isTrSet.setClassIndex(6);


        {
            // Chinese Beijing Chinese
            int[] occurrencescount = {1, 2, 0, 0, 0, 0};
            String label = "c";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
        }


        {
            ///*
            // Chinese Chinese Shanghai
            int[] occurrencescount = {0, 2, 0, 0, 1, 0};
            String label = "c";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
            //*/
        }

        {
            // Chinese Macao
            int[] occurrencescount = {0, 1, 0, 1, 0, 0};
            String label = "c";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
        }


        {
            // Tokyo Japan Chinese
            int[] occurrencescount = {0, 1, 1, 0, 0, 1};
            String label = "j";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
        }


        Classifier cModel = (Classifier)new NaiveBayesMultinomial();
        try {
            cModel.buildClassifier(isTrSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Test the model
        Evaluation eTest = null;
        try {
            eTest = new Evaluation(isTrSet);
            eTest.evaluateModel(cModel, isTrSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print the result à la Weka explorer:
        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        Instance iExample5 = new Instance(6);
        // Chinese Chinese Chinese Tokyo Japan
        iExample5.setValue((Attribute)fvWekaAttributes.elementAt(0), 0);
        iExample5.setValue((Attribute)fvWekaAttributes.elementAt(1), 3);
        iExample5.setValue((Attribute)fvWekaAttributes.elementAt(2), 1);
        iExample5.setValue((Attribute)fvWekaAttributes.elementAt(3), 0);
        iExample5.setValue((Attribute)fvWekaAttributes.elementAt(4), 0);
        iExample5.setValue((Attribute)fvWekaAttributes.elementAt(5), 1);

        iExample5.setDataset(isTrSet);

        System.out.println("Chinese Chinese Chinese Tokyo Japan");
        try {
            double[] fDistribution = cModel.distributionForInstance(iExample5);
            System.out.println(fDistribution[0]);
            System.out.println(fDistribution[1]);
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

        testOnBeijing();

        //testNaiveBayesMultinomial();

    }
}
