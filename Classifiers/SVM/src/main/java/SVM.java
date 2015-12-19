import data.NGram;
import db.DBConnector;
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
    public static void testSVM() throws Exception {
        // Временно: два класса из базы, на которых работаем
        // Классы выбраны, т.к. на данный момент (04.12.15) они представленны
        // в базе большим количеством нормализованных постов, а также
        // т.к. представляют пересекающиеся множества:
        // Всего 51 норм.постов с такими тэгами: 31 с одним, 27 с другим
        // к 8 постам проставленны оба тега - это и вызывает трудности
        // у любого бинарного классификатора.
        // Если в кач-ве второго класса выбрать "личное",
        // SMO сможет 100% распознать все посты, что логично.

        List<String> classes = new LinkedList<>();
        classes.add("history");
        classes.add("детское");

//        final String class1 = "history";
//        final String class2 = "детское";
//        final String class2 = "личное";

        // Коннектор к базе
        DBConnector db = new DBConnector(DBConnector.DataBase.MAIN);

        // Собираем id всех нормализованных постов
//        List<Long> normalizedPostIds = db.getAllPostNormalizedIds();
        // Временно: .. id нормализованных постов, принадлежащих к одному из указанных классов
        List<Long> normalizedPostIds = db.getAllPostNormalizedIds(classes);

        // Веткор, который будет содержать все наши X+y
        FastVector attributes;

        // Мапа для быстрого поиска позиции нграммы в векторе по её тексту
        // М.б. потом можно улучшить
        Map<String, Integer> nGramNameToAttributeIndex;
        Map<String, Integer> classNameToAttributeIndex;

        {
            // Собираем все связанные с вытащенными постами нграммы в Set
            Set<String> allNGramsNames = new HashSet<>();
            allNGramsNames.addAll(db.getAllNGramNames(normalizedPostIds));

            // Задаем размер вектора атрибутов, исходя из кол-ва н-грам + количество классов + 1 (для тега)
            // и заполняем его и мапу данными - текстами н-грамм и названиями классов
            attributes = new FastVector(allNGramsNames.size() + classes.size() + 1);
            int i = 0;
            nGramNameToAttributeIndex = new HashMap<>(allNGramsNames.size());
            for (String nGramName : allNGramsNames) {
                attributes.addElement(new Attribute(nGramName));
                nGramNameToAttributeIndex.put(nGramName, i++);
            }
            classNameToAttributeIndex = new HashMap<>(classes.size());
            for (String className : classes) {
                attributes.addElement(new Attribute(className));
                classNameToAttributeIndex.put(className, i++);
            }

            // Дебаг: выводим имеющиеся н-граммы в консоль
            System.out.println("all nGrams:");
            for (String nGramName : allNGramsNames)
                System.out.print("\t" + nGramName);
        }


        {
            // Собираем в мапу все теги, проставленные к данным постам
            Set<String> allTags = new HashSet<>();
            // Временно: для простейшего классификатора только два заданных класса
//        for (Long id : normalizedPostIds)
//            allTags.addAll(db.getAllTagNames(id));
            allTags.addAll(classes);
            // Добавляем теги ..
            FastVector classVal = new FastVector(allTags.size());
            for (String tag : allTags)
                classVal.addElement(tag);
            // .. в общий вектор атрибутов в качестве атрибута возможных класса
            Attribute classAttribute = new Attribute("Tag", classVal);
            attributes.addElement(classAttribute);
        }
        // Создаем на основе вектора атрибутов пустой тренировочный набор
        // TODO немного смысла так задавать размер, он м.б. больше.
        Instances trainingSet = new Instances("Rel", attributes, normalizedPostIds.size());
        trainingSet.setClassIndex(attributes.size() - 1);

        // Для каждого нормализованного поста:
        for (Long postId : normalizedPostIds) {
            // Забираем все н-граммы и теги, связанные с постом
            List<NGram> allNGramOfPost = db.getAllNGramNames(postId);
            List<String> allTagsOfPost = db.getAllTagNames(postId);

            // отсеиваем слишкрм короткие посты
            if (allNGramOfPost.size() < 2) continue;

            // Для каждой пары тег-пост создаем новый элемент датасета
            for (String tagOfPost : allTagsOfPost) {
                // Временно: для простейшего классификатора оставляем только нужные теги
                if (!classes.contains(tagOfPost))
                    continue;
                // Узнаем длину поста "в юниграммах"
                // TODO может вызвать проблемы, если использовать не только юниграммы,
                // TODO т.к. является общим делителем (ниже)
                int totalWordCountInPost = db.getPostLength(postId);
                // Создаем новый элемент датасета на основе вектора атрибутов, заполняем нулями
                Instance example = new Instance(1, new double[attributes.size()]);
                // Для каждой нграммы в посте
                for (NGram nGram : allNGramOfPost) {
                    // Узнаем количество использований в посте
                    int wordCountInPost = nGram.getUsesCnt();
                    // Добавляем в элемент датасета на соответствующее место относительную частоту
                    double relWordCountInPost = (double) wordCountInPost / (double) totalWordCountInPost;
                    example.setValue(nGramNameToAttributeIndex.get(nGram.getText()), relWordCountInPost);
                }
                // Устанавливаем нужный класс в атрибут
                example.setValue(classNameToAttributeIndex.get(tagOfPost),1);
                // Устанавливаем элементу датасета нужный класс
                example.setValue((Attribute) attributes.elementAt(trainingSet.classIndex()), tagOfPost);
                // Добавляем в тренировочный датасет
                trainingSet.add(example);
            }
        }

        // Дебаг: полученный датасет
//        System.out.println(trainingSet.toString());
//        System.out.println(trainingSet.toSummaryString());

        // Создаем классификатор на основе тренировочного датасета
//        Classifier model = new LibSVM(); // Верно распознает 54 процента на history+детское
        Classifier model = new SMO();   // Верно распознает 80 процента на history+детское
        model.buildClassifier(trainingSet);

        // Классифицируем датасет
        // TODO здесь должен быть тестовый набор, например часть cross-validation
        // TODO сейчас это лишь показыват, удалось ли полностью разделить датасет
        Evaluation test = new Evaluation(trainingSet);
        test.evaluateModel(model, trainingSet);

        // Дебаг: результаты распознавания
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
