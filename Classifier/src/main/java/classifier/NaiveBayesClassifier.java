package classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;

public class NaiveBayesClassifier {

    public static Classifier trainClassifier(Instances isTrainingSet) throws Exception {
        Classifier cModel = new NaiveBayes();
        cModel.buildClassifier(isTrainingSet);
        return cModel;
    }

    public static String validateClassifier(Classifier cModel, Instances isTrainingSet) throws Exception {
        Evaluation eTest = new Evaluation(isTrainingSet);
        eTest.evaluateModel(cModel, isTrainingSet);

        return eTest.toSummaryString();
    }

    public static String testClassifier(Instances isTrainingSet) throws Exception {

        Classifier cModel = new NaiveBayes();
        cModel.buildClassifier(isTrainingSet);

        // Test the model
        Evaluation eTest = new Evaluation(isTrainingSet);
        eTest.evaluateModel(cModel, isTrainingSet);

        return eTest.toSummaryString();
    }
}
