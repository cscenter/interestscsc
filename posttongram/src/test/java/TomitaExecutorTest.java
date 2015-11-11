import data.NGram;
import data.Post;
import db.DBConnector;
import posttongram.TomitaExecutor;
import posttongram.WordFilter;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by jamsic on 11.11.15.
 */
public class TomitaExecutorTest {

    static final int NUMBER_POST_TO_PROCESS = 1;
    static final HashMap<String, Integer> configFileNgrammType = new HashMap();
    static {
        configFileNgrammType.put("config1.proto", 1);
        configFileNgrammType.put("config2.proto", 2);
        configFileNgrammType.put("config3.proto", 3);
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException, FileNotFoundException {

        // TODO Не запускать на рабочей БД
        // Создаем коннектор, добавляем идентификатор своей машины в БД
        DBConnector db = new DBConnector("DBConnectorTestNormalization");

        // !!! СБРАСЫВАЕМ БАЗУ. НЕ СТОИТ ЭТОГО ДЕЛАТЬ КАЖДЫЙ РАЗ
//        db.dropInitDatabase("DBConnectorTestNormalization", "Bzw7HPtmHmVVqKvSHe7d");

        /*
        // Заполняем базу тестовыми данными (если пустая)
        for (int i = 0; i < 5; ++i) {
            String username = "username" + i;
            db.insertUser(new User(username, null, null, null, null, null, null));
            ArrayList<Post> userPosts = new ArrayList<>();
            for (int j = 0; j < 5; ++j)
                userPosts.add(new Post("SomeTitle", "SomeText", username, Timestamp.valueOf("2015-10-19 08:11:41"),
                        j + new Random().nextInt(10000) * 10, 20, new LinkedList<>()));
            db.insertPosts(userPosts);
        }
        */

        // Извлекаем из БД несколько постов для последующей обработки
        LinkedList<Post> unprocessedPosts = db.getPostsToNormalize(NUMBER_POST_TO_PROCESS);

        // для каждого поста
        for (Post post : unprocessedPosts) {

            System.out.println(post.getText());

            long textId = post.getId();
            System.out.println("id: " + textId);
            String title = post.getTitle();
            System.out.println("title: " + title);
            String newText = post.getText();
            String fullText = new String(title + ". " + newText);
            TomitaExecutor tomitaExec = new TomitaExecutor();
            // here we have input file named 'test.txt' for tomita to process it!
            tomitaExec.saveFileForTomita(fullText);

            for (String protoFileName : configFileNgrammType.keySet()) {
                int ngrammType = configFileNgrammType.get(protoFileName);
                //System.out.println(a + " " + configFilesToNgramms.get(a));
                Map<String, String> wordsCount = tomitaExec.runTomitaOnText(protoFileName);
                List<NGram> nGramms = tomitaExec.toNGramm(wordsCount, textId);

                ///*
                for (NGram nGramm : nGramms) {
                    System.out.println(post.getId() + " " + nGramm.getText() + " " + nGramm.getUsesCnt()
                            + " " + nGramm.getUsesStr());
                }
                //*/
                System.out.println(nGramms.size());
                db.insertNGrams(nGramms, post.getId(), ngrammType);
            }
            db.updatePostNormalized(post.getId());
            /*
            // Извлекаем юниграммы из поста
            LinkedList<NGram> unigrams = new LinkedList<>();
            for (int i = 0; i < 10; ++i)
                // В объект н-граммы входит её текст, сткрока позиций в посте и количество этих позиций
                unigrams.add(new NGram("unigram" + i + new Random().nextInt(20), "1, 2, 10", 3));

            // Добавляем юниграммы в БД
            // (список н-грамм, id поста (внутренний БД),
            // тип n-грамм, [1..3] , где 1 - unigram, 2 - digram, 3 - trigram.
            db.insertNGrams(unigrams, post.getId(), 1);

            // Повторяем то же для диграмм и триграмм
            // ...

            // Помечаем пост обработанным
            // (не для каждого могу найтись н-граммы)
            db.updatePostNormalized(post.getId());
            */
        }
    }
}
