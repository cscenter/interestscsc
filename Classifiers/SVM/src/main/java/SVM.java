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
        final String class1 = "history";
        final String class2 = "детское";
//        final String class2 = "личное";

        // Коннектор к базе
        DBConnector db = new DBConnector(DBConnector.DataBase.MAIN);

        // Собираем id всех нормализованных постов
//        List<Long> normalizedPostIds = db.getAllPostNormalizedIds();
        // Временно: .. id нормализованных постов, принадлежащих к одному из указанных классов
        List<Long> normalizedPostIds = db.getAllPostNormalizedIds(class1, class2);

        // Веткор, который будет содержать все наши X+y
        FastVector attributes;

        // Мапа для быстрого поиска позиции нграммы в векторе по её тексту
        // М.б. потом можно улучшить
        Map<String, Integer> nGramToAttributeIndex;

        {
            // Собираем все связанные с вытащенными постами нграммы в Set
            Set<NGram> allNGramms = new HashSet<>();
            // TODO добавить метод выдающий из базы сразу set по списку id
            for (Long id : normalizedPostIds) {
                assert (id != null);
                allNGramms.addAll(db.getAllNGramNames(id));
            }
            // Задаем размер вектора атрибутов, исходя из кол-ва н-грам + 1 (для тега)
            // и заполняем его и мапу данными - текстами н-грамм
            attributes = new FastVector(allNGramms.size() + 1);
            nGramToAttributeIndex = new HashMap<>(allNGramms.size());
            int i = 0;
            for (NGram nGramm : allNGramms) {
                attributes.addElement(new Attribute(nGramm.getText()));
                nGramToAttributeIndex.put(nGramm.getText(), i++);
            }
            // Дебаг: выводим имеющиеся н-граммы в консоль
            System.out.println("all nGrams:");
            for (NGram nGram : allNGramms)
                System.out.print("\t" + nGram.getText());
        }


        {
            // Собираем в мапу все теги, проставленные к данным постам
            Set<String> allTags2 = new HashSet<>();
            // Временно: для простейшего классификатора только два заданных класса
//        for (Long id : normalizedPostIds)
//            allTags2.addAll(db.getAllTagNames(id));
            allTags2.add(class1);
            allTags2.add(class2);
            // Добавляем теги ..
            FastVector classVal = new FastVector(allTags2.size());
            for (String tag : allTags2)
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
        for (Long postId2 : normalizedPostIds) {
            // Забираем все н-граммы и теги, связанные с постом
            List<NGram> allNGramOfPost2 = db.getAllNGramNames(postId2);
            List<String> allTagsOfPost2 = db.getAllTagNames(postId2);

            if (allNGramOfPost2.size() < 2) continue;

            // Для каждой пары тег-пост создаем новый элемент датасета
            for (String tagOfPost : allTagsOfPost2) {
                // Временно: для простейшего классификатора оставляем только теги нужные два тега
                if (!class1.equals(tagOfPost) && !class2.equals(tagOfPost))
                    continue;
                // Узнаем длину поста "в юниграммах"
                // TODO может вызвать проблемы, если использовать не только юниграммы,
                // TODO т.к. является общим делителем (ниже)
                int totalWordCountInPost = db.getPostLength(postId2);
                // Создаем новый элемент датасета на основе вектора атрибутов, заполняем нулями
                Instance example = new Instance(1, new double[attributes.size()]);
                // Для каждой нграммы в посте
                for (NGram nGram : allNGramOfPost2) {
                    // Узнаем количество использований в посте
                    int wordCountInPost = nGram.getUsesCnt();
                    // Добавляем в элемент датасета на соответствующее место относительную частоту
                    double relWordCountInPost = (double) wordCountInPost / (double) totalWordCountInPost;
                    example.setValue(nGramToAttributeIndex.get(nGram.getText()), relWordCountInPost);
                }
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
