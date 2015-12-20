import data.NGram;
import data.Post;
import data.Tag;
import data.User;
import db.DBConnector;
import db.DBConnectorToCrawler;
import db.DBConnectorToNormalizer;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * User: allight
 * Date: 13.10.2015 0:04
 */

public class DBConnectorTestLearning {

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws SQLException {

        // TODO Выбрать нужную БД
        DBConnector.DataBase dbName = DBConnector.DataBase.TEST;

// Заполняем базу тестовыми данными (если пустая)
// ------------------------------------------------------
        // !!! СБРАСЫВАЕМ БАЗУ. НЕ СТОИТ ЭТОГО ДЕЛАТЬ КАЖДЫЙ РАЗ
//        DBConnector.dropInitDatabase(dbName, "Bzw7HPtmHmVVqKvSHe7d");

        DBConnectorToCrawler dbCrawl = new DBConnectorToCrawler(dbName, "DBConnectorTestLearning");
        for (int i = 0; i < 5; ++i) {
            String username = "username" + i;
            dbCrawl.insertUser(new User.UserBuilder(username).setSchools(new LinkedList<>()).build());
            ArrayList<Tag> userTags = new ArrayList<>();
            for (int j = 0; j < 5; ++j)
                userTags.add(new Tag("tagname" + new Random().nextInt(100), null));
            dbCrawl.insertTags(userTags, username);
            ArrayList<Post> userPosts = new ArrayList<>();
            for (int j = 0; j < 5; ++j) {
                List<String> postTags = new LinkedList<>();
                for (int k = new Random().nextInt(10); k > 0; --k)
                    postTags.add(userTags.get(new Random().nextInt(userTags.size())).getName());
                userPosts.add(new Post("SomeTitle", "SomeText", username, Timestamp.valueOf("2015-10-19 08:11:41"),
                        j + new Random().nextLong() % 10000 * 10, 20, postTags));
            }
            dbCrawl.insertPosts(userPosts);
        }
        DBConnectorToNormalizer dbNorm = new DBConnectorToNormalizer(dbName, "DBConnectorTestLearning");
        dbNorm.reservePostForNormalizer(5);
        List<Post> unprocessedPosts = dbNorm.getReservedPosts();
        for (Post post : unprocessedPosts) {
            List<NGram> unigrams = new LinkedList<>();
            for (int i = 0; i < 10; ++i)
                unigrams.add(new NGram("unigram" + i + new Random().nextInt(20), "1, 2, 10", 3));
            dbNorm.insertNGrams(unigrams, post.getId(), DBConnector.NGramType.UNIGRAM);
            dbNorm.updatePostNormalized(post.getId());
        }
// ------------------------------------------------------

        // Создаем коннектор без прав записи в базу
        DBConnector db = new DBConnector(dbName);

        // Возьмем из базы список тегов, находящихся в некотором диапазане
        // "очков" популярности
        // по произведению количесва содержащих их постов
        // на .
        final long min_score = 2000L;
        final long max_score = 50000L;
        List<String> preferredTags = db.getTopNormalizedTagNames(min_score, max_score);
        System.out.println("Getting most popular in normalized posts tags from DB " +
                "with score between " + min_score + " and " + max_score + ":");
        for (String tag : preferredTags)
            System.out.println("\t" + tag);
        System.out.println("\n============\n");

        // Возьмем из базы список тегов, встречающихся
        // в нормализованных постах с определенной частотой
        final int offset_top_positions = 300;
        final int limit_top_positions = 2;
        preferredTags = db.getTopNormalizedTagNames(offset_top_positions, limit_top_positions);
        System.out.println("Getting most popular in normalized posts tags from DB " +
                "with offset on top positions " + offset_top_positions + " and limit" + limit_top_positions + ":");
        for (String tag : preferredTags)
            System.out.println("\t" + tag);
        System.out.println("\n============\n");

        // Возьмем из базы список id всех нормализованных постов,
        // имеющих хоть один из нужных нам тегов
        //noinspection UnusedAssignment
        List<Long> normalizedIds = db.getAllPostNormalizedIds(preferredTags);

        // .. или возьмем из базы список id всех нормализованных постов
        normalizedIds = db.getAllPostNormalizedIds();

        // Если такие нашлись, возьмем один из них для примера
        long postId;
        if(!normalizedIds.isEmpty())
            postId = normalizedIds.get(0);
        else throw new IllegalStateException("DB doesn't contain any normalizes post," +
                " further testing is pointless. Fill DB and retry.");

        // Извлекаем из БД количество, например диграм, для конкретного поста
        int nGramNum = db.getNGramCount(postId, DBConnector.NGramType.DIGRAM);
        System.out.println("\nGetting number of nGrams with n=1 and " +
                "post_id=" + postId + " from DB:");
        System.out.println("\t" + nGramNum);
        System.out.println("\n============\n");

        // Извлекаем из БД количество постов
        int postNum = db.getPostCount();
        System.out.println("Getting number of posts from DB:");
        System.out.println("\t" + postNum);
        System.out.println("\n============\n");

        // Извлекаем из БД количество нормализованных постов
        int postNormalizedNum = db.getPostNormalizedCount();
        System.out.println("Getting number of normalized posts from DB:");
        System.out.println("\t" + postNormalizedNum);
        System.out.println("\n============\n");


        // Извлекаем из БД список всех н-грамм
        List<String> allNGramNames = db.getAllNGramNames();
        System.out.println("Getting all nGramNames from DB:");
        for (String nGramName : allNGramNames)
            System.out.println("\t" + nGramName);
        System.out.println("\n============\n");

        // Извлекаем из БД список всех н-грамм для списка постов
        List<Long> prefferedPosts =
                normalizedIds.subList(0, Math.min(3,normalizedIds.size()));
        List<String> allNGramNamesForPosts = db.getAllNGramNames(prefferedPosts);
        System.out.println("Getting all nGrams for multiple posts from DB:");
        for (String nGram : allNGramNamesForPosts)
            System.out.println("\t" + nGram);
        System.out.println("\n============\n");

        // Извлекаем из БД список всех, например триграм, для конкретного поста
        List<NGram> nGrams = db.getAllNGramNames(postId, DBConnector.NGramType.TRIGRAM);
        System.out.println("Getting all triGrams by postId=" +
                postId + " from DB:");
        for (NGram nGram : nGrams)
            System.out.println("\t" + nGram.getText() + "\t" + nGram.getUsesCnt());
        System.out.println("\n============\n");

        // Извлекаем из БД список всех всех, например биграм, для списка постов
        allNGramNamesForPosts =
                db.getAllNGramNames(prefferedPosts, DBConnector.NGramType.DIGRAM);
        System.out.println("Getting all diGrams for multiple posts from DB:");
        for (String nGram : allNGramNamesForPosts)
            System.out.println("\t" + nGram);
        System.out.println("\n============\n");

        // Извлекаем из БД список всех н-грамм для конкретного поста
        List<NGram> allNGrams = db.getAllNGrams(postId);
        System.out.println("Getting all nGrams by postId=" +
                postId + " from DB:");
        for (NGram nGram : allNGrams)
            System.out.println("\t" + nGram.getText() + "\t" + nGram.getUsesCnt());
        System.out.println("\n============\n");

        // Извлекаем из БД список всех н-грамм для списка постов
        Map<Long,List<NGram>> allNGramsForPosts = db.getAllNGrams(prefferedPosts);
        System.out.println("Getting all nGrams for every post in a list with size = " +
                prefferedPosts.size() + "from DB:");
        for (Long postID : prefferedPosts) {
            System.out.println("\tnGrams for postID = " + postID);
            for (NGram nGram : allNGramsForPosts.get(postID))
                System.out.println("\t\t" + nGram.getText() + "\t" + nGram.getUsesCnt());
        }
        System.out.println("\n============\n");

        // Извлекаем из БД список всех тегов
        List<String> allTagNames = db.getAllTagNames();
        System.out.println("Getting all tagNames from DB:");
        for (String tagName : allTagNames)
            System.out.println("\t" + tagName);
        System.out.println("\n============\n");

        // Извлекаем из БД список всех тегов для конкретного userLJ
        String userLJNick = "username0";
        allTagNames = db.getAllTagNames(userLJNick);
        System.out.println("Getting all tagNames by userLJ_nick=" +
                userLJNick + " from DB:");
        for (String tagName : allTagNames)
            System.out.println("\t" + tagName);
        System.out.println("\n============\n");

        // Извлекаем из БД список всех тегов для конкретного поста
        allTagNames = db.getAllTagNames(postId);
        System.out.println("Getting all tagNames by postId=" +
                postId + " from DB:");
        for (String tagName : allTagNames)
            System.out.println("\t" + tagName);
        System.out.println("\n============\n");

        // Извлекаем из БД список всех тегов для набора постов
        List<Long> listOfPostIDs = normalizedIds
                .subList(0,Math.min(4,normalizedIds.size()));
        allTagNames = db.getAllTagNames(listOfPostIDs);
        System.out.println("Getting all tagNames by list of postIDs from DB:");
        for (String tagName : allTagNames)
            System.out.println("\t" + tagName);
        System.out.println("\n============\n");


        // Извлекаем из БД количество слов в конкретном посте
        int postLength = db.getPostLength(postId);
        System.out.println("Getting number of words in post with postId=" +
                postId + " from DB:");
        System.out.println("\t" + postLength);
        System.out.println("\n============\n");

        // Извлекаем из БД количество уникальных слов в конкретном посте
        int postUniqueWordCount = db.getPostUniqueWordCount(postId);
        System.out.println("Getting number of unique words in post with " +
                "postId=" + postId + " from DB:");
        System.out.println("\t" + postUniqueWordCount);
        System.out.println("\n============\n");
    }
}
