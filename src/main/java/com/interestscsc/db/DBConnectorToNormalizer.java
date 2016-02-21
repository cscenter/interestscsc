package com.interestscsc.db;

import com.interestscsc.data.NGram;
import com.interestscsc.data.Post;

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
            int i = 0;
            insertNormalizer.setString(++i, normalizerName);
            tryUpdateTransaction(insertNormalizer, normalizerName, "normalizer");
            i = 0;
            selectNormalizer.setString(++i, normalizerName);
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
                "BEGIN; " +
                "LOCK Post IN SHARE UPDATE EXCLUSIVE MODE; " +
                "UPDATE Post p " +
                "SET normalizer_id = ? " +
                "FROM ( " +
                "    WITH max_r AS ( " +
                "      SELECT count(*)-1 FROM PostToNormalizeRanked " +
                "    ), " +
                "    rand AS ( " +
                "      SELECT 1 + (random() * (SELECT * FROM max_r)) :: INTEGER AS row_number " +
                "      FROM generate_series(1, ?) " +
                "      GROUP BY row_number " +
                "    ) " +
                "    SELECT id " +
                "    FROM PostToNormalizeRanked " +
                "    JOIN rand USING(row_number) " +
                "     ) free " +
                "WHERE p.id = free.id; " +
                "COMMIT;";
        try (
                Connection con = getConnection();
                PreparedStatement reservePosts = con.prepareStatement(reservePostsString)
        ) {
            int i = 0;
            reservePosts.setInt(++i, normalizerId);
            reservePosts.setInt(++i, reserveNum);
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
            int i = 0;
            selectPosts.setInt(++i, normalizerId);
            ResultSet rs = tryQueryTransaction(selectPosts, "Post");
            if (rs != null)
                while (rs.next()) {
                    i = 0;
                    result.add(new Post(
                            rs.getLong(++i),
                            rs.getString(++i),
                            rs.getString(++i)));
                }
        }
        return result;
    }

    public int updatePostNormalized(long postId) throws SQLException {
        int rowsAffected = 0;
        String updateNormalizedString =
                "UPDATE Post SET normalized = TRUE WHERE id = ?;";

        try (
                Connection con = getConnection();
                PreparedStatement updateNormalized = con.prepareStatement(updateNormalizedString)
        ) {
            int i = 0;
            updateNormalized.setLong(++i, postId);
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
    @SuppressWarnings("SqlResolve")
    public int insertNGrams(Iterable<NGram> ngrams, long postId, NGramType nGramType) throws SQLException {
        int rowsAffected = 0;
        String insertNGramString = "INSERT INTO " + nGramType.getTableName() + " (text) VALUES (?);";
        String insertNGramToPostString = "INSERT INTO " + nGramType.getTableToPostName() + " " +
                "(ngram_id, post_id, uses_str, uses_cnt) VALUES ( " +
                "(SELECT id FROM " + nGramType.getTableName() + " WHERE text = ?), ?, ?, ?);";

        try (
                Connection con = getConnection();
                PreparedStatement insertNGram = con.prepareStatement(insertNGramString);
                PreparedStatement insertNGramToPost = con.prepareStatement(insertNGramToPostString)
        ) {
            for (NGram ngram : ngrams) {
                int i = 0;
                insertNGram.setString(++i, ngram.getText());
                rowsAffected += tryUpdateTransaction(insertNGram, ngram.getText(), nGramType.getTableName());
            }

            for (NGram ngram : ngrams) {
                int i = 0;
                insertNGramToPost.setString(++i, ngram.getText());
                insertNGramToPost.setLong(++i, postId);
                insertNGramToPost.setString(++i, ngram.getUsesStr());
                insertNGramToPost.setInt(++i, ngram.getUsesCnt());
                rowsAffected += tryUpdateTransaction(insertNGramToPost, "postId<->" + ngram.getText(),
                        nGramType.getTableToPostName());
            }
        }
        return rowsAffected;
    }
}
