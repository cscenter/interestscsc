import data.NGram;
import data.Post;
import data.Tag;
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

public class DBConnectorTestLearning {

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws SQLException, ClassNotFoundException, FileNotFoundException {

        // Создаем коннектор, добавляем идентификатор своей машины в БД
        DBConnector db = new DBConnector("DBConnectorTestLearning");

        // TODO Не запускать на рабочей БД
        // !!! СБРАСЫВАЕМ БАЗУ. НЕ СТОИТ ЭТОГО ДЕЛАТЬ КАЖДЫЙ РАЗ
//        db.dropInitDatabase("DBConnectorTestLearning", "Bzw7HPtmHmVVqKvSHe7d");

// Заполняем базу тестовыми данными (если пустая)
// ------------------------------------------------------
        for (int i = 0; i < 5; ++i) {
            String username = "username" + i;
            db.insertUser(new User(username, null, null, null, null, null, null));
            ArrayList<Tag> userTags = new ArrayList<>();
            for (int j = 0; j < 5; ++j)
                userTags.add(new Tag("tagname" + new Random().nextInt(100), null));
            db.insertTags(userTags, username);
            ArrayList<Post> userPosts = new ArrayList<>();
            for (int j = 0; j < 5; ++j) {
                List<String> postTags = new LinkedList<>();
                for (int k = new Random().nextInt(10); k > 0; --k)
                    postTags.add(userTags.get(new Random().nextInt(userTags.size())).getName());
                userPosts.add(new Post("SomeTitle", "SomeText", username, Timestamp.valueOf("2015-10-19 08:11:41"),
                        j + new Random().nextInt(10000) * 10, 20, postTags));
            }
            db.insertPosts(userPosts);
        }
        LinkedList<Post> unprocessedPosts = db.getPostsToNormalize(5);
        for (Post post : unprocessedPosts) {
            LinkedList<NGram> unigrams = new LinkedList<>();
            for (int i = 0; i < 10; ++i)
                unigrams.add(new NGram("unigram" + i + new Random().nextInt(20), "1, 2, 10", 3));
            db.insertNGrams(unigrams, post.getId(), 1);
            db.updatePostNormalized(post.getId());
        }
// ------------------------------------------------------


        // Возьмем для примера один из обработанных постов
        long postId = unprocessedPosts.getFirst().getId();


        // Извлекаем из БД количество n-грамм для конкретного поста и n = 1
        int nGramNum = db.getNGramCount(postId, 1);
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
        LinkedList<String> allNGramNames = db.getAllNGramNames();
        System.out.println("Getting all nGramNames from DB:");
        for (String nGramName : allNGramNames)
            System.out.println("\t" + nGramName);
        System.out.println("\n============\n");

        // Извлекаем из БД список всех н-грамм для конкретного поста
        allNGramNames = db.getAllNGramNames(postId);
        System.out.println("Getting all nGramNames by postId=" +
                postId + " from DB:");
        for (String nGramName : allNGramNames)
            System.out.println("\t" + nGramName);
        System.out.println("\n============\n");


        // Извлекаем из БД список всех тегов
        LinkedList<String> allTagNames = db.getAllTagNames();
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
