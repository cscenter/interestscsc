package com.interestscsc.classifier.svm;

import com.interestscsc.data.NGram;
import com.interestscsc.db.DBConnector;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;

/**
 * Created by allight
 */

public class SVM {

    private static final DBConnector.DataBase DB_NAME = DBConnector.DataBase.MAIN;
    private static final int MAX_TOTAL_NGRAMS_NUM = 2000;
    private static final double LEARN_PART_SIZE = 0.5;

    /**
     * Запятая в имени класса гарантирует неповторяемость среди прочих тегов,
     * по крайней мере для системы livejournal.com
     */
    private static final String NULL_CLASS = "<NULL(,)>";

    /**
     * TODO: Сейчас главная проблема в том, как снизить влияние атрибутов-классов
     */
    public static void testSVM() throws Exception {

        /**
         * Временно: несколько классов из базы, на которых работаем
         * Классы выбраны как представляют сильно пересекающиеся множества:
         * Посты с одним из этих тегов часто имеют также проставленный
         * другой из этой группы - это и вызывает трудности
         * у любого бинарного классификатора.
         */
        List<String> classes = new LinkedList<>();
        classes.add("политика");
        classes.add("россия");
        classes.add("власть");
        classes.add("украина");
        classes.add("история");

        DBConnector db = new DBConnector(DB_NAME);

        /**
         * Собираем id всех нормализованных постов
         * В фмнале: List<Long> normalizedPostIds = db.getAllPostNormalizedIds();
         * Временно: id нормализованных постов, принадлежащих к одному из указанных классов
         */
        List<Long> normalizedPostIds = db.getAllPostNormalizedIds(classes);

        System.out.println(String.format("Fetched %d normalized post IDs", normalizedPostIds.size()));

        /**
         * Вектор, который будет содержать все наши X+y
         */
        FastVector attributes;

        /**
         * Мапа для быстрого поиска позиции нграммы в векторе по её тексту
         */
        Map<String, Integer> nGramNameToAttributeIndex;
        Map<String, Integer> classNameToAttributeIndex;

        {
            /**
             * Собираем наиболее полезные (по if-idf) нграммы,
             * из связанных с вытащенными постами, в Set
             */
            Set<String> allNGramsNames = new HashSet<>();
            allNGramsNames.addAll(db.getTFIDFTopNGramNamesByTotalLimit(classes, MAX_TOTAL_NGRAMS_NUM));

            System.out.println(String.format("Fetched %d nGram names", allNGramsNames.size()));

            /**
             * Задаем размер вектора атрибутов, исходя из кол-ва н-грам + кол-ва классов + 1 (для тега)
             * и заполняем его и мапу данными - текстами н-грамм и названиями классов
             */
            attributes = new FastVector(allNGramsNames.size() + classes.size() + 1);
            int i = 0;
            nGramNameToAttributeIndex = new HashMap<>(allNGramsNames.size());
            for (String nGramName : allNGramsNames) {
                attributes.addElement(new Attribute(nGramName));
                nGramNameToAttributeIndex.put(nGramName, i++);
            }
            classNameToAttributeIndex = new HashMap<>(classes.size());
            for (String className : classes) {
                attributes.addElement(new Attribute("class_" + className));
                classNameToAttributeIndex.put(className, i++);
            }
        }

        {
            /**
             * Собираем в сет все теги, проставленные к данным постам + пустой тег
             */
            Set<String> allTags = new HashSet<>();
            /**
             * В финале:
             *    for (Long id : normalizedPostIds)
             *        allTags.addAll(db.getAllTagNames(id));
             * Временно: для простейшего классификатора только заданные классы
             */
            allTags.addAll(classes);
            allTags.add(NULL_CLASS);

            /**
             * Добавляем теги в общий вектор атрибутов в качестве одного
             * атрибута (возможных классов)
             */
            FastVector classVal = new FastVector(allTags.size());
            for (String tag : allTags)
                classVal.addElement(tag);
            Attribute classAttribute = new Attribute("Tag", classVal);
            attributes.addElement(classAttribute);
        }

        /**
         * Создаем на основе вектора атрибутов пустой тренировочный и тестовый наборы
         * TODO немного смысла так задавать размер, реально он больше.
         */
        Instances trainingSet = new Instances("Rel", attributes, normalizedPostIds.size());
        trainingSet.setClassIndex(attributes.size() - 1);

        Instances testSet = new Instances("Rel", attributes, normalizedPostIds.size());
        testSet.setClassIndex(attributes.size() - 1);


        {
            /**
             * Извлекаем все н-граммы и теги, связанные с постами,
             * а так же длины постов
             */
            Map<Long, List<NGram>> postToNGrams = db.getAllNGrams(normalizedPostIds);
            Map<Long, List<String>> postToTags = db.getAllTags(normalizedPostIds);
            Map<Long, Integer> postToLength = db.getPostLength(normalizedPostIds);

            System.out.println(String.format("Fetched %d nGram lists by postIDs", postToNGrams.size()));
            System.out.println(String.format("Fetched %d tag lists by postIDs", postToTags.size()));
            System.out.println(String.format("Fetched %d post lengths by postIDs", postToLength.size()));

            /**
             * Для каждого нормализованного поста:
             */
            for (Long postId : normalizedPostIds) {

                /**
                 * находим списки н-грамм и тегов, отсеивая слишком короткие посты
                 */
                List<NGram> allNGramOfPost = postToNGrams.get(postId);
                if (allNGramOfPost.size() < 2) continue;
                List<String> allTagsOfPost = postToTags.get(postId);

                /**
                 * Случайным образом определяем, в обучающую или тестовую выборку
                 * пойдет пост
                 */
                boolean learnSet = new Random().nextFloat() < LEARN_PART_SIZE;

                /**
                 * Для каждого класса
                 */
                for (String tag : classes) {

                    /**
                     * Узнаем длину поста "в юниграммах"
                     * TODO может вызвать проблемы, если использовать не только юниграммы,
                     * TODO т.к. является общим делителем (ниже)
                     */
                    int totalWordCountInPost = postToLength.get(postId);

                    /**
                     * Создаем новый элемент датасета на основе вектора атрибутов, заполняем нулями
                     */
                    Instance example = new Instance(1, new double[attributes.size()]);

                    /**
                     * Для каждой нграммы в посте
                     */
                    for (NGram nGram : allNGramOfPost) {
                        Integer index = nGramNameToAttributeIndex.get(nGram.getText());

                        /**
                         * Если index не найден - н-грамма не в топе TF-IDF, пропускаем
                         */
                        if (index == null) continue;

                        /**
                         * Узнаем количество использований в посте и относительную частоту
                         */
                        int wordCountInPost = nGram.getUsesCnt();
                        double relWordCountInPost = (double) wordCountInPost / (double) totalWordCountInPost;

                        example.setValue(index, relWordCountInPost);
                    }

                    /**
                     * Устанавливаем нужный класс в атрибут
                     */
                    String tagOfPost = NULL_CLASS;
                    if(allTagsOfPost.contains(tag)) {
                        tagOfPost = tag;
                        // TODO Оптимизировать в мапу по class -> post_id
                    }
                    example.setValue(classNameToAttributeIndex.get(tag), 1);

                    /**
                     * Устанавливаем новому элементу датасета нужный класс и добавляем
                     * в тренировочный или тестовый датасет
                     */
                    if (learnSet) {
                        example.setValue((Attribute) attributes.elementAt(trainingSet.classIndex()), tagOfPost);
                        trainingSet.add(example);
                    } else {
                        example.setValue((Attribute) attributes.elementAt(testSet.classIndex()), tagOfPost);
                        testSet.add(example);
                    }
                }
            }
        }

        /**
         * Создаем классификатор на основе тренировочного датасета
         * LibSVM() или SMO()
         */
        Classifier model = new SMO();
        model.buildClassifier(trainingSet);

        /**
         * Классифицируем датасет
         */
        Evaluation learning = new Evaluation(trainingSet);
        Evaluation test = new Evaluation(testSet);

        learning.evaluateModel(model, trainingSet);
        test.evaluateModel(model, testSet);

        System.out.println(learning.toSummaryString());
        System.out.println(test.toSummaryString());
    }

    public static void main(String[] args) {
        try {
            testSVM();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
