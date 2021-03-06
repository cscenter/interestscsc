package com.interestscsc.normalizer;

import com.interestscsc.data.NGram;
import com.interestscsc.db.DBConnector;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Normalizer приводит посты из базы данных к списку н-грамм,
 * прогоняя тексты последовательно через mystem и tomita.
 * <p>
 * Инструкция по применению:
 * 1. Скачать себе ветку master
 * 2. Открыть проект в Intellij Idea (так, чтобы главной папкой была interestscsc(interestscsc-master)) и,
 * нажав правой кнопкой мышки на pom.xml выбрать +Add as Maven Project
 * 3. Назначить SetupSDK (оно само попросится)
 * 4. Скачать соответствующий операционной системе файл mystem (Версия 3.0) https://tech.yandex.ru/mystem/
 * и распаковать в src/main/resources/normalizer/mystem
 * 5. Скачать соответствующий операционной системе файл tomita https://tech.yandex.ru/tomita/ и распаковать
 * в src/main/resources/normalizer/tomita
 * 6. Попробовать запустить src/test/java/com/interestscsc/normalizer/test/NormalizerrTest.java
 * <p>
 * Возможные трудности:
 * Может неправильно определить имена исполняемых файлов mystem и tomita, потому что они зависят от
 * операционной системы, а у меня, к сожалению, нет возможности протестить их все, поэтому в этом случае их
 * можно указать явно в параметрах (только названия, без директорий)
 * <p>
 * private static final String TOMITA_FILENAME = "";
 * private static final String MYSTEM_FILENAME = "";
 * (например, private static final String TOMITA_FILENAME = "tomita-linux64";
 * <p>
 * в src/main/java/com/interestscsc/normalizer/Normalizer.java, а потом рассказать об ошибке мне. В остальном
 * должно работать.
 * <p>
 * Обо всех ошибках сразу сообщайте, любые комментарии к коду и структуре модуля приветствуются!
 */

public class Normalizer {

    private static final String TOMITA_FILENAME = "";
    private static final int WATCHDOG_CONST = 60000;
    private static final String INPUT_FILE_NAME = "input.txt";
    private static final String TOMITA_OUTPUT_FILE_NAME = "PrettyOutput.html";
    private static final String TOMITA_MESSAGE_NAME = "NGrams";
    private static final String MYSTEM_FILENAME = "";
    private static final String PROTO_EXTENSION = ".proto";

    private final File tomitaDir;
    private final File mystemDir;

    public Normalizer(File tomitaDir, File mystemDir) {
        this.tomitaDir = tomitaDir;
        this.mystemDir = mystemDir;
    }

    public List<NGram> toNGram(Map<String, String> positionMap) {
        Set<String> keySet = positionMap.keySet();
        List<NGram> nGrams = new LinkedList<>();
        for (String nGram : keySet) {
            int usesCount = positionMap.get(nGram).split(",").length;
            NGram newNGram = new NGram(nGram, positionMap.get(nGram), usesCount);
            nGrams.add(newNGram);
        }
        return nGrams;
    }

    /**
     * processes NGrams
     */
    public Map<String, String> normalizeText(String protoFileNames[], DBConnector.NGramType type)
            throws IOException {
        if (type.equals(DBConnector.NGramType.UNIGRAM)) {
            /**
             * here mystem processes 'src/main/resources/normalizer/mystem/input.txt'
             * and produces 'src/main/resources/normalizer/tomita/input.txt'
             */
            runMystem();
        } else {
            rewriteInputWithoutModification();
        }
        List<String> nGrams = new ArrayList<>();
        for (String protoFileName : protoFileNames) {
            /**
             * here tomita processes 'input.txt' and produces 'PrettyOutput.html'
             */
            runTomita(protoFileName);
            /**
             * here we get nGrams (with repeats) from our 'PrettyOutput.html'
             */
            nGrams.addAll(getNGrams());
        }
        List<String> goodNGrams = WordFilter.normalize(nGrams);
        return getPositions(goodNGrams);
    }

    public Map<String, String> getPositions(List<String> strArray) {
        Map<String, String> positionMap = new HashMap<>();
        for (int i = 0; i < strArray.size(); i++) {
            if (!positionMap.containsKey(strArray.get(i))) {
                positionMap.put(strArray.get(i), Integer.toString(i));
            } else {
                String positions = positionMap.get(strArray.get(i));
                positions = positions + "," + Integer.toString(i);
                positionMap.put(strArray.get(i), positions);
            }
        }
        return positionMap;
    }

    public ArrayList<String> getNGrams() throws IOException {
        File tomitaOutputFile = new File(tomitaDir + File.separator + TOMITA_OUTPUT_FILE_NAME);
        ArrayList<String> nGrams = new ArrayList<>();
        Document doc = Jsoup.parse(tomitaOutputFile, "UTF-8");
        Elements tables = doc.select("table");
        for (Element table : tables) {
            Element firstTr = table.getElementsByTag("tr").first();
            if (firstTr.text().equals(TOMITA_MESSAGE_NAME)) {
                Elements links = table.getElementsByTag("tr");
                for (int i = 2; i < links.size(); i++)
                    nGrams.add(links.get(i).text());
            }
        }
        return nGrams;
    }

    public void saveFileForNormalization(String text) throws FileNotFoundException, UnsupportedEncodingException {
        File inputFile = new File(mystemDir + File.separator + INPUT_FILE_NAME);
        PrintWriter writer = new PrintWriter(inputFile, "UTF-8");
        writer.println(text);
        writer.close();
    }

    public String getTomitaFileName() {
        if (TOMITA_FILENAME.isEmpty()) {
            String oSName = System.getProperty("os.name").toLowerCase();
            if (oSName.contains("linux")) {
                if (System.getProperty("os.arch").contains("64")) {
                    return "tomita-linux64";
                } else {
                    return "tomita-linux32";
                }
            }
            if (oSName.contains("win")) {
                return "tomitaparser.exe";
            }
            if (oSName.contains("mac")) {
                return "tomita-mac";
            }
            return "tomita-freebsd64";
        } else {
            return TOMITA_FILENAME;
        }
    }

    public String getMystemFileName() {
        if (MYSTEM_FILENAME.isEmpty()) {
            String oSName = System.getProperty("os.name").toLowerCase();

            if (oSName.contains("win")) {
                return "mystem.exe";
            } else {
                return "mystem";
            }
        } else {
            return MYSTEM_FILENAME;
        }
    }


    /**
     * запускает tomita с готовым config.proto
     */
    public void runTomita(String protoFileName) throws IOException {
        File tomitaExecutiveFile = new File(tomitaDir + File.separator + getTomitaFileName());
        CommandLine cmdLine = new CommandLine(tomitaExecutiveFile);
        cmdLine.addArgument(tomitaDir + File.separator + protoFileName + PROTO_EXTENSION);
        execute(cmdLine);
    }

    /**
     * запускает mystem
     */
    public void runMystem() throws IOException {
        File mystemExecutiveFile = new File(mystemDir + File.separator + getMystemFileName());
        CommandLine cmdLine = new CommandLine(mystemExecutiveFile);
        cmdLine.addArgument("-l");
        cmdLine.addArgument("-c");
        cmdLine.addArgument("-d");
        cmdLine.addArgument(mystemDir + File.separator + INPUT_FILE_NAME);
        cmdLine.addArgument(tomitaDir + File.separator + INPUT_FILE_NAME);
        execute(cmdLine);
    }

    public void rewriteInputWithoutModification() throws IOException {
        Path mystem = mystemDir.toPath().resolve(INPUT_FILE_NAME);
        Path tomita = tomitaDir.toPath().resolve(INPUT_FILE_NAME);
        Files.copy(mystem, tomita, StandardCopyOption.REPLACE_EXISTING);
    }

    public void execute(CommandLine cmdLine) throws IOException {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(WATCHDOG_CONST);
        executor.setWatchdog(watchdog);
        executor.execute(cmdLine);
    }
}
