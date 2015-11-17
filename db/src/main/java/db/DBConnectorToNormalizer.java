package db;

import data.NGram;
import data.Post;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;


/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

@SuppressWarnings("Duplicates")
public class DBConnectorToNormalizer extends DBConnector {
    private Integer normalizerId;

    public DBConnectorToNormalizer(DataBase dataBase, String normalizerName) throws SQLException {
        super(dataBase);
        setNewNormalizerName(normalizerName);
    }

    public void setNewNormalizerName(String normalizerName) throws SQLException {
        if (checkTable("normalizer"))
            this.normalizerId = getNormalizerId(normalizerName);
        else
            System.err.println("WARNING: There is no table \"normalizer\" in current DB. " +
                    "Maybe you'll need to recreate DB with method <code>dropInitDatabase()</code>, " +
                    "and then manually set you normalizer name with method <code>setNewNormalizerName(..)</code>");
    }


    private Integer getNormalizerId(String normalizerName) throws SQLException {
        String insertNormalizerString =
                "INSERT INTO normalizer (name) VALUES (?);";
        String selectNormalizerString = "SELECT id FROM normalizer WHERE name = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement insertNormalizer = con.prepareStatement(insertNormalizerString);
                PreparedStatement selectNormalizer = con.prepareStatement(selectNormalizerString)
        ) {
            insertNormalizer.setString(1, normalizerName);
            tryUpdateTransaction(insertNormalizer, normalizerName, "normalizer");
            selectNormalizer.setString(1, normalizerName);
            ResultSet rs = tryQueryTransaction(selectNormalizer, "normalizer");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            return rs.getInt("id");
        }
    }


    public int reservePostForNormalizer(int reserveNum) throws SQLException {
        if (reserveNum <= 0) throw new IllegalArgumentException("Argument reserveNum must be greater than 0.");
        int rowsAffected = 0;
        String reservePostsString =
                "UPDATE Post p SET normalizer_id = ? " +
                        "FROM ( " +
                        "       SELECT id FROM Post p " +
                        "       WHERE p.normalizer_id IS NULL AND NOT p.normalized " +
                        "       LIMIT ? FOR UPDATE " +
                        "     ) free " +
                        "WHERE p.id = free.id;";
        try (
                Connection con = getConnection();
                PreparedStatement reservePosts = con.prepareStatement(reservePostsString)
        ) {
            reservePosts.setInt(1, normalizerId);
            reservePosts.setInt(2, reserveNum);
            rowsAffected += tryUpdateTransaction(reservePosts, "normalizerId = " + normalizerId, "Post");
        }
        return rowsAffected;
    }


    public List<Post> getReservedPosts() throws SQLException {
        List<Post> result = new LinkedList<>();
        String selectPostsString = "SELECT id, title, text FROM Post " +
                "WHERE normalizer_id = ? AND NOT normalized";
        try (
                Connection con = getConnection();
                PreparedStatement selectPosts = con.prepareStatement(selectPostsString)
        ) {
            selectPosts.setInt(1, normalizerId);
            ResultSet rs = tryQueryTransaction(selectPosts, "Post");
            if (rs != null)
                while (rs.next())
                    result.add(new Post(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("text")));
        }
        return result;
    }


    public int updatePostNormalized(long postId) throws SQLException {                                                                     //TODO
        int rowsAffected = 0;
        String updateNormalizedString =
                "UPDATE Post SET normalized = TRUE WHERE id = ?;";

        try (
                Connection con = getConnection();
                PreparedStatement updateNormalized = con.prepareStatement(updateNormalizedString)
        ) {
            updateNormalized.setLong(1, postId);
            rowsAffected += tryUpdateTransaction(updateNormalized, "Post_id = " + postId, "Post");
        }
        return rowsAffected;
    }


    /**
     * Поочередно добавляет в БД n-граммы из любого итерабельного контейнера.
     * Информация о посте добавляемых n-грамм уже должна быть в базе
     *
     * @param ngrams    - итерабельный контейнер с n-граммами в строках.
     * @param postId    - внутренний id поста в БД, выдается в объекте Post,
     *                  в методе getUnprocessedPosts(int num).
     * @param nGramType - тип добавляемых n-грамм. [1..3]
     *                  1 - unigram, 2 - digram, 3 - trigram.
     * @return кол-во добавленных записей
     */
    public int insertNGrams(Iterable<NGram> ngrams, long postId, NGramType nGramType) throws SQLException {
        int rowsAffected = 0;
        String insertNGramString = "INSERT INTO " + nGramType.getTableName() + " (text) VALUES (?);";
        String insertNGramToPostString = "INSERT INTO " + nGramType.getTableName() + "ToPost " +
                "(ngram_id, post_id, uses_str, uses_cnt) VALUES ( " +
                "(SELECT id FROM " + nGramType.getTableName() + " WHERE text = ?), ?, ?, ?);";

        try (
                Connection con = getConnection();
                PreparedStatement insertNGram = con.prepareStatement(insertNGramString);
                PreparedStatement insertNGramToPost = con.prepareStatement(insertNGramToPostString)
        ) {
            for (NGram ngram : ngrams) {
                insertNGram.setString(1, ngram.getText());
                rowsAffected += tryUpdateTransaction(insertNGram, ngram.getText(), nGramType.getTableName());
            }

            for (NGram ngram : ngrams) {
                insertNGramToPost.setString(1, ngram.getText());
                insertNGramToPost.setLong(2, postId);
                insertNGramToPost.setString(3, ngram.getUsesStr());
                insertNGramToPost.setInt(4, ngram.getUsesCnt());
                rowsAffected += tryUpdateTransaction(insertNGramToPost, "postId<->" + ngram.getText(),
                        nGramType.getTableName() + "ToPost");
            }
        }
        return rowsAffected;
    }
}
