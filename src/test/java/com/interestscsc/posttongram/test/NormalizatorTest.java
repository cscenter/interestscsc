package com.interestscsc.posttongram.test;

import com.interestscsc.data.NGram;
import com.interestscsc.data.Post;
import com.interestscsc.db.DBConnector;
import com.interestscsc.db.DBConnectorToNormalizer;
import org.apache.log4j.Logger;
import com.interestscsc.posttongram.Normalizator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NormalizatorTest {

    static final int NUMBER_POST_TO_PROCESS = 5;
    static final HashMap<List<String>, DBConnector.NGramType> configFileNgrammType = new HashMap();
    private static final Logger logger = Logger.getLogger(NormalizatorTest.class);

    static {
        List<String> unigramProtos = new ArrayList<>();
        unigramProtos.add("config.proto");
        configFileNgrammType.put(unigramProtos, DBConnector.NGramType.UNIGRAM);
        List<String> digramProtos = new ArrayList<>();
        digramProtos.add("adjNoun.proto");
        digramProtos.add("nounAdj.proto");
        digramProtos.add("nounNoun.proto");
        digramProtos.add("verbAdv.proto");
        configFileNgrammType.put(digramProtos, DBConnector.NGramType.DIGRAM);
        List<String> trigramProtos = new ArrayList<>();
        trigramProtos.add("trigram.proto");
        configFileNgrammType.put(trigramProtos, DBConnector.NGramType.TRIGRAM);
    }

    public static void main(String[] args) throws SQLException, IOException {

        DBConnector.DataBase dbName = DBConnector.DataBase.TEST;

        DBConnectorToNormalizer db = new DBConnectorToNormalizer(dbName, System.getProperty("user.name"));

        List<Post> postToNormalize = db.getReservedPosts();

        // Если недообработанных или ранее зарезервированных постов нет ..
        if (postToNormalize.size() == 0) {

            // .. резервируем несколько постов в БД, чтобы никто больше их не обрабатывал, ..
            db.reservePostForNormalizer(NUMBER_POST_TO_PROCESS);

            // .. и берем их id, названия и тексты из базы
            postToNormalize = db.getReservedPosts();
        }

        // для каждого поста
        for (Post post : postToNormalize) {

            logger.info(post.getText());
            logger.info("processing post with id: " + post.getId());
            String fullText = new String(post.getTitle() + ". " + post.getText());
            // here we have input file named 'test.txt' for tomita to process it!
            Normalizator.saveFileForNormalization(fullText);


            for (List protoFileName : configFileNgrammType.keySet()) {
                Map<String, String> wordPosition = Normalizator.normalizeText(protoFileName,
                        configFileNgrammType.get(protoFileName));
                List<NGram> nGramms = Normalizator.toNGramm(wordPosition);

                ///*
                for (NGram nGramm : nGramms) {
                    logger.info(post.getId() + " " + nGramm.getText() + " " + nGramm.getUsesCnt()
                            + " " + nGramm.getUsesStr());
                }
                //*/
                logger.info("extracted " + configFileNgrammType.get(protoFileName) + ": " + nGramms.size());
                db.insertNGrams(nGramms, post.getId(), configFileNgrammType.get(protoFileName));
            }
            ;
            db.updatePostNormalized(post.getId());
        }
    }
}
