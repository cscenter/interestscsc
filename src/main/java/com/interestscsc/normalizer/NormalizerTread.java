package com.interestscsc.normalizer;

import com.interestscsc.data.NGram;
import com.interestscsc.data.Post;
import com.interestscsc.db.DBConnector;
import com.interestscsc.db.DBConnectorToNormalizer;
import javafx.util.Pair;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


public class NormalizerTread implements Callable {
    public static final DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;
    /**
     * POSTS_PER_SESSION == 100 - компромисс между слишком частыми запросами
     * к базе и резервированием под поток слишком большого числа постов,
     * которое он не успеет обработать, например, за сутки.
     * <p>
     * TODO Уточнить, определившись со средней скоростью обработки
     */
    public static final int POSTS_PER_SESSION = 100;
    /**
     * SESSIONS_NUMBER == 10 - число последовательных (!) сессий. Ничего
     * существенно не меняющий параметр, служащий, как и в краулере, для
     * управления общим временем работы (зная ~ время работы одной сессии).
     * <p>
     * TODO Возможно, стоит убрать совсем или вынести в конструктор для удобства
     */
    public static final int SESSIONS_NUMBER = 10;

    private static final URL RESOURCE = Main.class.getClassLoader().getResource("normalizer");
    private static final Logger logger = Logger.getLogger(NormalizerTread.class);
    private static final String THREADS_SUB_DIR = "threads";
    private static final String TOMITA_SUB_DIR = "tomita";
    private static final String MYSTEM_SUB_DIR = "mystem";
    private static final String PROTO_TEMPLATE = "protoTemplate.txt";
    private static final Pair<String, String> UNI_PROTO = new Pair<>("config", "mydic");

    private static final Map<DBConnector.NGramType, String[]> N_GRAM_TYPE_TO_PROTO = new HashMap<>();
    private static int liveThreadsInfo = 0;

    static {
        N_GRAM_TYPE_TO_PROTO.put(DBConnector.NGramType.UNIGRAM, new String[]{"config"});
        N_GRAM_TYPE_TO_PROTO.put(DBConnector.NGramType.DIGRAM, new String[]{"adjNoun", "nounAdj", "nounNoun", "verbAdv"});
        N_GRAM_TYPE_TO_PROTO.put(DBConnector.NGramType.TRIGRAM, new String[]{"trigram"});
    }

    private Path resources;
    private String name;

    public NormalizerTread(String name) {
        this.name = name;
        prepareWorkingDir();
    }

    @Override
    public Object call() throws SQLException, IOException {
        try {
            liveThreadsInfo++;

            DBConnectorToNormalizer db = new DBConnectorToNormalizer(dbName, System.getProperty("user.name") + name);
            Normalizer normalizer = new Normalizer(getThreadTomitaDir(), getThreadMystemDir());

            int session = 0;
            while (session < SESSIONS_NUMBER) {
                logger.info(String.format(
                        "=======\tCurrently running %d thread(s). Thread '%s' started session №%d\t======\n",
                        liveThreadsInfo, name, session++));
                List<Post> postToNormalize = db.getReservedPosts();
                if (postToNormalize.size() == 0) {
                    db.reservePostForNormalizer(POSTS_PER_SESSION);
                    postToNormalize = db.getReservedPosts();
                } else if (session > 1)
                    logger.warn(name + ": Seems that some posts wasn't normalized successfully");

                for (Post post : postToNormalize) {
                    logger.info(name + " processing post with id: " + post.getId());
                    normalizer.saveFileForNormalization(post.getTitle() + ". " + post.getText());

                    for (Map.Entry<DBConnector.NGramType, String[]> entry : N_GRAM_TYPE_TO_PROTO.entrySet()) {
                        Map<String, String> wordPosition = normalizer.normalizeText(entry.getValue(), entry.getKey());
                        List<NGram> nGrams = normalizer.toNGram(wordPosition);

                        logger.info(name + " extracted " + entry.getKey() + ": " + nGrams.size());
                        db.insertNGrams(nGrams, post.getId(), entry.getKey());
                    }
                    db.updatePostNormalized(post.getId());
                }
            }
        } finally {
            liveThreadsInfo--;
        }
        return null;
    }

    private File getThreadTomitaDir() {
        return resources.resolve(TOMITA_SUB_DIR).toFile();
    }

    private File getThreadMystemDir() {
        return resources.resolve(MYSTEM_SUB_DIR).toFile();
    }

    private void prepareWorkingDir() {
        try {
            //noinspection ConstantConditions
            Path baseResources = Paths.get(RESOURCE.toURI());
            File srcMystemDir = baseResources.resolve(MYSTEM_SUB_DIR).toFile();
            File srcTomitaDir = baseResources.resolve(TOMITA_SUB_DIR).toFile();
            resources = baseResources.resolve(THREADS_SUB_DIR).resolve(name);
            File dstMystemDir = resources.resolve(MYSTEM_SUB_DIR).toFile();
            File dstTomitaDir = resources.resolve(TOMITA_SUB_DIR).toFile();

            copyDirectoryWithFiles(srcMystemDir, dstMystemDir, null);
            copyDirectoryWithFiles(srcTomitaDir, dstTomitaDir, (dir, name) -> !name.endsWith(".txt"));

            String protoTemplate = new String(Files.readAllBytes(srcTomitaDir.toPath().resolve(PROTO_TEMPLATE)));
            String path = dstTomitaDir.toString().replaceAll("\\\\", "/");

            genProtoBuf(path, UNI_PROTO.getKey(), protoTemplate, UNI_PROTO.getValue());
            for (String name : N_GRAM_TYPE_TO_PROTO.get(DBConnector.NGramType.DIGRAM))
                genProtoBuf(path, name, protoTemplate, name);
            for (String name : N_GRAM_TYPE_TO_PROTO.get(DBConnector.NGramType.TRIGRAM))
                genProtoBuf(path, name, protoTemplate, name);

        } catch (URISyntaxException | NullPointerException | IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Something went wrong while creating Thread Working Dir");
        }
    }

    private void genProtoBuf(String dir, String fileName, String protoTemplate, String dictName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(Paths.get(dir).resolve(fileName + ".proto").toFile());
        writer.print(protoTemplate
                .replaceAll("<dir>", dir)
                .replaceAll("<file>", dictName));
        writer.close();
    }

    private void copyDirectoryWithFiles(File src, File dest, FilenameFilter filter) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        dest.mkdirs();
        File files[] = src.listFiles(filter);
        for (File f : files)
            Files.copy(f.toPath(), dest.toPath().resolve(f.getName()), StandardCopyOption.REPLACE_EXISTING);
    }
}
