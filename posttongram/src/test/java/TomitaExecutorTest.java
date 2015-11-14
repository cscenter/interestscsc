import data.NGram;
import data.Post;
import db.DBConnector;
import org.apache.log4j.Logger;
import posttongram.TomitaExecutor;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TomitaExecutorTest {

    static final int NUMBER_POST_TO_PROCESS = 1;
    static final HashMap<String, Integer> configFileNgrammType = new HashMap();
    private static final Logger logger = Logger.getLogger(java.util.logging.Logger.class);
    static {
        configFileNgrammType.put("config1.proto", 1);
        configFileNgrammType.put("config2.proto", 2);
        configFileNgrammType.put("config3.proto", 3);
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException, FileNotFoundException {

        // TODO Не запускать на рабочей БД
        // Создаем коннектор, добавляем идентификатор своей машины в БД
        DBConnector db = new DBConnector("DBConnectorTestNormalization");

        // Извлекаем из БД несколько постов для последующей обработки
        List<Post> unprocessedPosts = db.getPostsToNormalize(NUMBER_POST_TO_PROCESS);

        // для каждого поста
        for (Post post : unprocessedPosts) {

            logger.info(post.getText());
            logger.info("processing post with id: " + post.getId());
            String fullText = new String(post.getTitle() + ". " + post.getText());
            // here we have input file named 'test.txt' for tomita to process it!
            TomitaExecutor.saveFileForTomita(fullText);

            for (String protoFileName : configFileNgrammType.keySet()) {
                int ngrammType = configFileNgrammType.get(protoFileName);
                Map<String, String> wordsCount = TomitaExecutor.runTomitaOnText(protoFileName);
                List<NGram> nGramms = TomitaExecutor.toNGramm(wordsCount);

                //*/
                for (NGram nGramm : nGramms) {
                    logger.info(post.getId() + " " + nGramm.getText() + " " + nGramm.getUsesCnt()
                            + " " + nGramm.getUsesStr());
                }
                //*/
                logger.info("extracted " + ngrammType + ": " + nGramms.size());
                db.insertNGrams(nGramms, post.getId(), ngrammType);
            }
            db.updatePostNormalized(post.getId());
        }
    }
}

