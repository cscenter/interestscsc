import data.NGram;
import data.Post;
import data.User;
import db.DBConnector;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * User: allight
 * Date: 13.10.2015 0:04
 */

public class DBConnectorTestNormalization {

    public static void main(String[] args) throws SQLException, ClassNotFoundException, FileNotFoundException {

        // TODO Не запускать на рабочей БД
        // Создаем коннектор, добавляем идентификатор своей машины в БД
        DBConnector db = new DBConnector("DBConnectorTestNormalization");

        // !!! СБРАСЫВАЕМ БАЗУ. НЕ СТОИТ ЭТОГО ДЕЛАТЬ КАЖДЫЙ РАЗ
//        db.dropInitDatabase("DBConnectorTestNormalization", "Bzw7HPtmHmVVqKvSHe7d");

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

        // Извлекаем из БД несколько постов для последующей обработки
        List<Post> unprocessedPosts = db.getPostsToNormalize(5);

        // для каждого поста
        for (Post post : unprocessedPosts) {

            // Извлекаем юниграммы из поста
            List<NGram> unigrams = new LinkedList<>();
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
        }
    }
}
