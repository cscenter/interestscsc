import data.Post;
import data.Tag;
import data.User;
import db.DBConnector;
import db.DBConnectorToCrawler;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * User: allight
 * Date: 13.10.2015 0:04
 */
public class DBConnectorTestCrawling {

    public static void main(String[] args) throws SQLException, ClassNotFoundException, FileNotFoundException {

        // TODO Выбрать нужную БД
        DBConnector.DataBase dbName = DBConnector.DataBase.TEST;

        // !!! СБРАСЫВАЕМ БАЗУ. НЕ СТОИТ ЭТОГО ДЕЛАТЬ КАЖДЫЙ РАЗ
//        DBConnector.dropInitDatabase(dbName, "Bzw7HPtmHmVVqKvSHe7d");

        // Создаем коннектор с правами краулера, добавляем идентификатор своей машины в БД
        DBConnectorToCrawler db = new DBConnectorToCrawler(dbName, "DBConnectorTestCrawling");

        // Собираем с LJ имена нескольких стартовых пользователей (имитация)
        List<String> rawUsers = new LinkedList<>();
        for (int i = 0; i < 10; ++i)
            rawUsers.add("username" + new Random().nextInt(100));

        // Добавляем стартовые ники в БД
        db.insertRawUsers(rawUsers);

        // Проверяем, есть ли недообработанные пользователи с прошлых сеансов нашего краулера
        // Если такие есть, добавляем в очередь.
        List<String> usersToProceed = db.getUnfinishedRawUsers();

        // Берем из базы список зарезервированных для нас пользователей
        usersToProceed.addAll(db.getReservedRawUsers());

        // Если недообработанных или ранее зарезервированных пользователей нет ..
        if (usersToProceed.size() == 0) {

            // .. резервируем несколько имен пользователей в БД, чтобы никто больше их не обрабатывал, ..
            db.reserveRawUserForCrawler(5);

            // .. и берем их имена из базы
            usersToProceed = db.getReservedRawUsers();
        }

        // Запоминаем имеющиеся регионы. Их немного, не стоит каждый раз лезть за ними в БД.
        HashSet<String> regions = new HashSet<>(db.getRegions());

        // Начинаем обрабатывать пользователей из списка
        for (String username : usersToProceed) {

            // Собираем с LJ список ников друзей пользователя в итерабельныую коллекцию строк (имитация)
            List<String> friends = new LinkedList<>();
            for (int i = 0; i < 5; ++i)
                friends.add("username" + new Random().nextInt(100));

            // Добавляем в БД список ников друзей пользователя
            db.insertRawUsers(friends);

            // Собираем c LJ информацию о пользователе в объект класса User (имитация)
            String[] r = new String[]{"RU", "other", null};
            User user = new User.UserBuilder(username)
                    .setRegion(r[new Random().nextInt(3)])
                    .setDateCreated(Timestamp.valueOf("2015-09-17T13:09:03".replaceFirst("T", " ")))
                    .setDateUpdated(Timestamp.valueOf("2015-09-17T13:09:03".replaceFirst("T", " ")))
                    .setDateFetched(Timestamp.valueOf("2015-09-17T13:09:03".replaceFirst("T", " ")))
                    .setSchools(new LinkedList<>())
                    .build();

            // Если у пользователя есть регион, отсутсвующий в кэше, добавляем его
            if (user.getRegion() != null && !regions.contains(user.getRegion())) {
                db.insertRegion(user.getRegion());
                regions.add(user.getRegion());
            }

            // Добавляем информацию о пользователе в базу
            db.insertUser(user);

            // Если регион нам не подходит (ПОСТАВИТЬ НУЖНОЕ УСЛОВИЕ) - пропускаем сбор постов
            // !!! Сейчас: если регион у пользовател есть, но не 'RU' - собираем только базовую инфу
            if (user.getRegion() != null && !user.getRegion().equals("RU")) {

                // Отмечаем в базе, что вытащили все доспупные посты
                // (т.к. посты этого пользователя нам не интересены)
                db.updateUserFetched(username);
                continue;
            }

            // Собираем c LJ информацию о тэгах пользователя в итерабельныую коллекцию Tag (имитация)
            ArrayList<Tag> userTags = new ArrayList<>();
            if (new Random().nextInt(2) > 0)
                for (int i = 0; i < 10; ++i) // имитация удачно собранной статистики
                    userTags.add(new Tag("tagname" + new Random().nextInt(100), new Random().nextInt(100)));
            else
                for (int i = 0; i < 10; ++i) // имитация не собранной статистики
                    userTags.add(new Tag("tagname" + new Random().nextInt(100), null));

            // Добавляем в базу тэги и статистику их использования данным пользователем
            db.insertTags(userTags, username);

            // Собираем c LJ информацию о постах пользователя в итерабельныую коллекцию Post (имитация)
            ArrayList<Post> userPosts = new ArrayList<>();
            for (int i = 0; i < 10; ++i) {
                //Имитируем случайное кол-во тэгов из списка тэгов пользователя (повторы не страшны)
                List<String> postTags = new LinkedList<>();
                for (int j = new Random().nextInt(10); j > 0; --j)
                    postTags.add(userTags.get(new Random().nextInt(userTags.size())).getName());
                userPosts.add(new Post(
                        "SomeTitle",
                        "SomeText",
                        username,
                        Timestamp.valueOf("2015-10-19 08:11:41"),
                        i + new Random().nextLong() % 10000 * 10,
                        20,
                        postTags
                ));
            }

            // Добавляем в базу посты пользователя и их связь с тэгами
            db.insertPosts(userPosts);

            // Если уверены, что вытащили все выложенные на данный момент посты,
            // ставим пользователю Timestamp на fetched
            db.updateUserFetched(username);
        }

        // Проверяем, что всех успешно обработали
        if (db.getUnfinishedRawUsers().size() == 0)
            System.out.println("One more cycle complete successfully");

        // Возьмем из базы всех пользователей и выведем
        // (сейчас не отображается fetched - его нет в User)
        List<User> allUsers = db.getUsers();

        for (User user : allUsers)
            System.out.println(user.toString() + "\n\n =========== \n\n");
    }
}
