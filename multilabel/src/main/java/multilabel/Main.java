package multilabel;

import meka.classifiers.multilabel.BCC;
import meka.core.Result;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.functions.SMO;
import weka.core.*;

import static meka.classifiers.multilabel.Evaluation.testClassifier;

/**
 * Created by jamsic on 12.12.15.
 */
public class Main {

    public static Instance getNextInstance(FastVector fvWekaAttributes, int[] occurrencescount, String label) {
        Instance iExample1 = new DenseInstance(occurrencescount.length);
        for (int i = 0; i < occurrencescount.length; i++) {
            //System.out.println("i " + i);
            if (i < 2) {
                System.out.println(i);
                iExample1.setValue((Attribute) fvWekaAttributes.elementAt(i), Integer.toString(occurrencescount[i]));
            } else {
                iExample1.setValue((Attribute) fvWekaAttributes.elementAt(i), occurrencescount[i]);
            }
        }
        //iExample1.setValue((Attribute)fvWekaAttributes.elementAt(occurrencescount.length), label);
        return iExample1;
    }

    public static Instances testOnBeijing() {
        Attribute Attribute1 = new Attribute("Beijing"); // Beijing
        Attribute Attribute2 = new Attribute("Chinese"); // Chinese
        Attribute Attribute3 = new Attribute("Japan"); // Japan
        Attribute Attribute4 = new Attribute("Macao"); // Macao
        Attribute Attribute5 = new Attribute("Shanghai"); // Shanghai
        Attribute Attribute6 = new Attribute("Tokyo"); // Tokyo
        Attribute Attribute7 = new Attribute("c"); // about China
        Attribute Attribute8 = new Attribute("j"); // about Japan
        FastVector fvNominalVal = new FastVector(2);
        fvNominalVal.addElement("0"); // about China
        fvNominalVal.addElement("1"); // about Japan
        Attribute ClassAttribute1 = new Attribute("c", fvNominalVal);
        Attribute ClassAttribute2 = new Attribute("j", fvNominalVal);
        FastVector fvWekaAttributes = new FastVector(8);
        fvWekaAttributes.addElement(ClassAttribute1);
        fvWekaAttributes.addElement(ClassAttribute2);
        fvWekaAttributes.addElement(Attribute3);
        fvWekaAttributes.addElement(Attribute4);
        fvWekaAttributes.addElement(Attribute5);
        fvWekaAttributes.addElement(Attribute6);
        fvWekaAttributes.addElement(Attribute1);
        fvWekaAttributes.addElement(Attribute2);
        //fvWekaAttributes.addElement(ClassAttribute);

        Instances isTrSet = new Instances("Rel", fvWekaAttributes, 4);
        //isTrSet.setClassIndex(0);
        isTrSet.setClassIndex(2);


        {
            // Chinese Beijing Chinese
            int[] occurrencescount = {1, 1, 0, 0, 0, 0, 1, 2};
            String label = "c";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
        }


        {
            ///*
            // Chinese Chinese Shanghai
            int[] occurrencescount = {0, 1, 0, 0, 1, 0, 0, 2};
            String label = "c";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
            //*/
        }

        {
            // Chinese Macao
            int[] occurrencescount = {1, 0, 0, 1, 0, 0, 0, 1};
            String label = "c";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
        }


        {
            // Tokyo Japan Chinese
            int[] occurrencescount = {1, 1, 1, 0, 0, 1, 0, 1};
            String label = "j";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
        }

        /*
        {
            // Tokyo Japan Chinese
            int[] occurrencescount = {0, 0, 1, 0, 0, 1, 0, 1};
            String label = "j";
            Instance iExample = getNextInstance(fvWekaAttributes, occurrencescount, label);
            isTrSet.add(iExample);
        }
        */


        return isTrSet;

        /*
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
        */

    }

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
            String[] a = {"животные", "авто"};
            return a;
        } else {
            String[] a = {"авто", "животные"};
            return a;
        }
    }

    public static int getTagCountFromDB(String a, int postId) {
        if (a.equals("животные")) {
            if (postId == 0) return 1;
            if (postId == 1) return 1;
            if (postId == 2) return 0;
            if (postId == 3) return 1;
        } else {
            if (postId == 0) return 1;
            if (postId == 1) return 1;
            if (postId == 2) return 1;
            if (postId == 3) return 1;
        }
        return 0;
    }

    public static Instances testNaiveBayesMultinomial() {
        String[] nGramms = getAllnGrammsFromDB();
        FastVector fvWekaAttributes = new FastVector(nGramms.length + 1);
        for (String nGramm : nGramms) {
            fvWekaAttributes.addElement(new Attribute(nGramm));
        }

        String[] allTags = getAllTagsFromDB();
        /*
        FastVector fvClassVal = new FastVector(allTags.length);
        for (String tag : allTags) {
            fvClassVal.addElement(tag);
        }
        Attribute ClassAttribute = new Attribute("Tag", fvClassVal);
        fvWekaAttributes.addElement(ClassAttribute);
        */
        for (String tag : allTags) {
            fvWekaAttributes.addElement(new Attribute(tag));
        }

        // Create an empty training set
        int documentCount = getDocumentCountFromDB();
        Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, documentCount);
        // Set class index
        for (int i = 0; i < allTags.length; i++) {
            isTrainingSet.setClassIndex(nGramms.length + i); // нумерация с 0 же, последний элемент - Tag.
        }

        System.out.println("Word:   " + nGramms[0] + " " + nGramms[1] + " " + nGramms[2]);
        for (int postId = 0; postId < documentCount; postId++) {
            String[] allTagsOfPost = getPostTagFromDB(postId);
            //for (String tagOfPost : allTagsOfPost) {
                System.out.print("Post " + postId + ":  ");
                int totalWordCountInPost = getWordCountInPost(postId);
                Instance iExample = new DenseInstance(nGramms.length + allTagsOfPost.length);
                for (int i = 0; i < nGramms.length; i++) {
                    int wordCountInPost = getnGrammCountFromDB(nGramms[i], postId);
                    //System.out.println(wordCountInPost);
                    // Attention! На вход подаются АБСОЛЮТНЫЕ ЧАСТОТЫ
                    double relWordCountInPost = (double) wordCountInPost; // / (double) totalWordCountInPost;
                    iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), relWordCountInPost);
                    System.out.print(relWordCountInPost + "   ");
                }
            for (int i = nGramms.length; i < nGramms.length + allTagsOfPost.length; i++) {
                iExample.setValue((Attribute) fvWekaAttributes.elementAt(i), getTagCountFromDB(allTagsOfPost[i - nGramms.length], postId));
            }
                //System.out.println(tagOfPost);

                isTrainingSet.add(iExample);
           // }
        }
        return isTrainingSet;

        /*

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
        Instance iExample = new DenseInstance(nGramms.length);
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
        Instance iExample2 = new DenseInstance(nGramms.length);
        //Instance d = new SparseInstance()
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
        Instance iExample3 = new DenseInstance(nGramms.length);
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
        */


    }

    public static void main(String[] args) throws Exception {
        Instances f = testNaiveBayesMultinomial();
        Instances beijing = testOnBeijing();
        System.out.println(f.toString());
        BCC bcc = new BCC();

        bcc.buildClassifier(f);

        //Evaluation eTest = new Evaluation(f);
        //eTest.evaluateModel(bcc, f);

        // Print the result à la Weka explorer:
        //String strSummary = eTest.toSummaryString();
        //System.out.println(strSummary);

        System.out.println(beijing.toString());
        BCC bcc2 = new BCC();
        for (String h : bcc2.getOptions()) {
            System.out.println(h);
        }
        bcc2.setClassifier(new SMO());
        bcc2.buildClassifier(beijing);

        Result result = testClassifier(bcc2, beijing);
        System.out.println(result);
        System.out.println(result.size());
        double [][] s = result.allPredictions();
        for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < s[i].length; j++) {
                System.out.print(s[i][j] + " ");
            }
            System.out.print("\n");
        }
        System.out.println(result.allPredictions());
        //result

        //Evaluation eTest = new Evaluation(beijing);
        //eTest.evaluateModel(bcc, beijing);

        //Print the result à la Weka explorer:
        //String strSummary = eTest.toSummaryString();
        //System.out.println(eTest.correct());
        //System.out.println(strSummary);

        int[] a = {0, 1, 1, 0, 0, 2};
        Instance b = beijing.instance(2);

        int numClasses = 2;
        for (int i = 0; i < a.length; i++) {
            b.setValue(b.attribute(i + numClasses), a[i]);
        }
        //b.setValue(b.attribute(0), 0);
        System.out.println(b.toString());

        double[] distributionForInstance = bcc2.distributionForInstance(b);
        for (double fff: distributionForInstance) {
            System.out.print(fff + "   ");
        }


        Attribute Atribute1 = new Attribute("Beijing", 0); // Beijing
        Attribute Atribute2 = new Attribute("Chinese", 0); // Chinese
        Attribute Atribute3 = new Attribute("Japan", 0); // Japan
        Attribute Atribute4 = new Attribute("Macao", 0); // Macao
        Attribute Atribute5 = new Attribute("Shanghai", 0); // Shanghai
        Attribute Atribute6 = new Attribute("Tokyo", 0); // Tokyo
        //Attribute Attribute7 = new Attribute("c"); // about China
        //Attribute Attribute8 = new Attribute("j"); // about Japan
        //FastVector fvNominalVal = new FastVector(2);
        //fvNominalVal.addElement("c"); // about China
        //fvNominalVal.addElement("j"); // about Japan
        //Attribute ClassAttribute = new Attribute("country", fvNominalVal);
        FastVector fvWekaAttributes2 = new FastVector();
        fvWekaAttributes2.addElement(Atribute1);
        fvWekaAttributes2.addElement(Atribute2);
        fvWekaAttributes2.addElement(Atribute3);
        fvWekaAttributes2.addElement(Atribute4);
        fvWekaAttributes2.addElement(Atribute5);
        fvWekaAttributes2.addElement(Atribute6);


        //fvWekaAttributes.addElement(Attribute7);
        //fvWekaAttributes.addElement(Attribute8);

        ///*
        //int[] a = {0, 1, 1, 0, 0, 1};
        //Instance iExample = getNextInstance(fvWekaAttributes2, a, "f");
        //Instance s = getNextInstance(fvWekaAttributes, a, "c");
        //Instance iExample2 = new DenseInstance(a.length);
        //System.out.print(iExample2.numAttributes());
        //Instance iExample = getNextInstance(fvWekaAttributes2, a, "f");
        //System.out.print(iExample2.attribute(0));
        //iExample2.setValue((Attribute) fvWekaAttributes2.elementAt(0), a[0]);
//        iExample2.setValue((Attribute) fvWekaAttributes2.elementAt(0), 0);
        //iExample2.setValue((Attribute) fvWekaAttributes2.elementAt(1), a[1]);
        //iExample2.setValue((Attribute) fvWekaAttributes2.elementAt(2), a[2]);
        //iExample2.setValue((Attribute) fvWekaAttributes2.elementAt(3), a[3]);
        //iExample2.setValue((Attribute) fvWekaAttributes2.elementAt(4), a[4]);
        //iExample2.setValue((Attribute) fvWekaAttributes2.elementAt(5), a[5]);
        //*/

        //iExample2.setDataset(beijing);
        //System.out.println(bcc2.distributionForInstance(iExample2));
       // String[] g = {"-t", "beijing"};

    }
}
