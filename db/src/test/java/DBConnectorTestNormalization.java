import data.NGram;
import data.Post;
import data.User;
import db.DBConnector;
import db.DBConnectorToCrawler;
import db.DBConnectorToNormalizer;

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

        // TODO Выбрать нужную БД
        DBConnector.DataBase dbName = DBConnector.DataBase.TEST;

// Заполняем базу тестовыми данными (если пустая)
// ------------------------------------------------------
        // !!! СБРАСЫВАЕМ БАЗУ. НЕ СТОИТ ЭТОГО ДЕЛАТЬ КАЖДЫЙ РАЗ
//        DBConnector.dropInitDatabase(dbName, "Bzw7HPtmHmVVqKvSHe7d");

        DBConnectorToCrawler dbCrawl = new DBConnectorToCrawler(dbName, "DBConnectorTestNormalization");
        for (int i = 0; i < 5; ++i) {
            String username = "username" + i;
            dbCrawl.insertUser(new User(username, null, null, null, null, null, null));
            ArrayList<Post> userPosts = new ArrayList<>();
            for (int j = 0; j < 5; ++j)
                userPosts.add(new Post("SomeTitle", "SomeText", username, Timestamp.valueOf("2015-10-19 08:11:41"),
                        j + new Random().nextLong() % 10000 * 10, 20, new LinkedList<>()));
            dbCrawl.insertPosts(userPosts);
        }
// ------------------------------------------------------

        // Создаем коннектор с правами нормализатора, добавляем идентификатор своей машины в БД
        DBConnectorToNormalizer db = new DBConnectorToNormalizer(dbName, "DBConnectorTestNormalization");

        // Берем из базы список зарезервированных для нас постов
        List<Post> postToNormalize = db.getReservedPosts();

        // Если недообработанных или ранее зарезервированных постов нет ..
        if (postToNormalize.size() == 0) {

            // .. резервируем несколько постов в БД, чтобы никто больше их не обрабатывал, ..
            db.reservePostForNormalizer(5);

            // .. и берем их id, названия и тексты из базы
            postToNormalize = db.getReservedPosts();
        }

        // для каждого поста
        for (Post post : postToNormalize) {

            // Извлекаем юниграммы из поста
            List<NGram> unigrams = new LinkedList<>();
            for (int i = 0; i < 10; ++i)
                // В объект н-граммы входит её текст, сткрока позиций в посте и количество этих позиций
                unigrams.add(new NGram("unigram" + i + new Random().nextInt(20), "1, 2, 10", 3));

            // Добавляем юниграммы в БД
            // (список н-грамм, id поста (внутренний БД), тип n-грамм
            db.insertNGrams(unigrams, post.getId(), DBConnector.NGramType.UNIGRAM);

            // Повторяем то же для диграмм и триграмм
            // ...

            // Помечаем пост обработанным
            // (не для каждого могу найтись н-граммы)
            db.updatePostNormalized(post.getId());
        }
    }
}
