package com.interestscsc.normalizer.test;

import com.interestscsc.data.NGram;
import com.interestscsc.data.Post;
import com.interestscsc.db.DBConnector;
import com.interestscsc.db.DBConnectorToNormalizer;
import com.interestscsc.normalizer.Normalizer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NormalizerTest {

    static final int NUMBER_POST_TO_PROCESS = 5;
    static final HashMap<List<String>, DBConnector.NGramType> CONFIG_FILE_N_GRAM_TYPE = new HashMap<>();
    private static final Logger logger = Logger.getLogger(NormalizerTest.class);

    static {
        List<String> unigramProtos = new ArrayList<>();
        unigramProtos.add("config.proto");
        CONFIG_FILE_N_GRAM_TYPE.put(unigramProtos, DBConnector.NGramType.UNIGRAM);
        List<String> digramProtos = new ArrayList<>();
        digramProtos.add("adjNoun.proto");
        digramProtos.add("nounAdj.proto");
        digramProtos.add("nounNoun.proto");
        digramProtos.add("verbAdv.proto");
        CONFIG_FILE_N_GRAM_TYPE.put(digramProtos, DBConnector.NGramType.DIGRAM);
        List<String> trigramProtos = new ArrayList<>();
        trigramProtos.add("trigram.proto");
        CONFIG_FILE_N_GRAM_TYPE.put(trigramProtos, DBConnector.NGramType.TRIGRAM);
    }

    public static void main(String[] args) throws SQLException, IOException {

        DBConnector.DataBase dbName = DBConnector.DataBase.TEST;

        DBConnectorToNormalizer db = new DBConnectorToNormalizer(dbName, System.getProperty("user.name"));

        List<Post> postToNormalize = db.getReservedPosts();

        /**
         * Если недообработанных или ранее зарезервированных постов нет ..
         */
        if (postToNormalize.size() == 0) {

            /**
             * .. резервируем несколько постов в БД, чтобы никто больше их не обрабатывал, ..
             */
            db.reservePostForNormalizer(NUMBER_POST_TO_PROCESS);

            /**
             * .. и берем их id, названия и тексты из базы
             */
            postToNormalize = db.getReservedPosts();
        }

        /**
         * для каждого поста
         */
        for (Post post : postToNormalize) {

            logger.info(post.getText());
            logger.info("processing post with id: " + post.getId());
            String fullText = post.getTitle() + ". " + post.getText();
            /**
             * here we have input file named 'test.txt' for tomita to process it!
             */
            Normalizer.saveFileForNormalization(fullText);


            for (List<String> protoFileName : CONFIG_FILE_N_GRAM_TYPE.keySet()) {
                Map<String, String> wordPosition = Normalizer.normalizeText(protoFileName,
                        CONFIG_FILE_N_GRAM_TYPE.get(protoFileName));
                List<NGram> nGrams = Normalizer.toNGram(wordPosition);

                for (NGram nGram : nGrams) {
                    logger.info(post.getId() + " " + nGram.getText() + " " + nGram.getUsesCnt()
                            + " " + nGram.getUsesStr());
                }
                logger.info("extracted " + CONFIG_FILE_N_GRAM_TYPE.get(protoFileName) + ": " + nGrams.size());
                db.insertNGrams(nGrams, post.getId(), CONFIG_FILE_N_GRAM_TYPE.get(protoFileName));
            }
            db.updatePostNormalized(post.getId());
        }
    }
}
