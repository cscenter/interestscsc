import data.NGram;
import data.Post;
import db.DBConnector;
import db.DBConnectorToNormalizer;
import org.apache.log4j.Logger;
import posttongram.Normalizator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NormalizatorTest {

    static final int NUMBER_POST_TO_PROCESS = 5;
    static final HashMap<String, DBConnector.NGramType> configFileNgrammType = new HashMap();
    private static final Logger logger = Logger.getLogger(java.util.logging.Logger.class);
    static {
        configFileNgrammType.put("config.proto", DBConnector.NGramType.UNIGRAM);
        //configFileNgrammType.put("config2.proto", DBConnector.NGramType.DIGRAM);
        //configFileNgrammType.put("config3.proto", DBConnector.NGramType.TRIGRAM);
    }

    public static void main(String[] args) throws SQLException, IOException {

        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;

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


            for (String protoFileName : configFileNgrammType.keySet()) {
                Map<String, String> wordPosition = Normalizator.normalizeText(protoFileName);
                List<NGram> nGramms = Normalizator.toNGramm(wordPosition);

                ///*
                for (NGram nGramm : nGramms) {
                    logger.info(post.getId() + " " + nGramm.getText() + " " + nGramm.getUsesCnt()
                            + " " + nGramm.getUsesStr());
                }
                //*/
                logger.info("extracted " + configFileNgrammType.get(protoFileName) + ": " + nGramms.size());
                db.insertNGrams(nGramms, post.getId(), configFileNgrammType.get(protoFileName));
            };
            db.updatePostNormalized(post.getId());
        }
    }
}
