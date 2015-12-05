package posttongram;

import db.DBConnector;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import data.NGram;
import java.io.*;
import java.util.*;


public class Normalizator {

    private static final String TOMITA_FILENAME = "";
    private static final int WATCHDOG_CONST = 60000;
    private static final String INPUT_FILE_NAME = "input.txt";
    private static final String TOMITA_OUTPUT_FILE_NAME = "PrettyOutput.html";
    private static final String TOMITA_MESSAGE_NAME = "NGrams";
    private static final File TOMITA_WORKING_DIR = new File("posttongram/tomitaWorkingFiles");

    private static final File MYSTEM_WORKING_DIR = new File("posttongram/mystem");
    private static final String MYSTEM_FILENAME = "";

    /*
    public static void main(String[] args) throws IOException {

        ///*
        File mystemExecutiveFile = new File(MYSTEM_WORKING_DIR + File.separator + getMystemFileName());
        CommandLine cmdLine = new CommandLine(mystemExecutiveFile);
        cmdLine.addArgument("-l");
        cmdLine.addArgument("-c");
        cmdLine.addArgument("-d");
        cmdLine.addArgument(MYSTEM_WORKING_DIR + File.separator + INPUT_FILE_NAME);
        cmdLine.addArgument(TOMITA_WORKING_DIR + File.separator + INPUT_FILE_NAME);
        execute(cmdLine);

        runTomita("config.proto");
    }
    */

    public static List<NGram> toNGramm(Map<String, String> positionMap) {
        Set<String> keySet = positionMap.keySet();
        List<NGram> nGrams = new LinkedList<>();
        for (String nGramm : keySet) {
            int usescount = positionMap.get(nGramm).split(",").length;
            NGram newNGramm = new NGram(nGramm, positionMap.get(nGramm), usescount);
            nGrams.add(newNGramm);
        }
        return nGrams;
    }

    // processNGrams
    public static Map<String, String> normalizeText(List<String> protoFileNames, DBConnector.NGramType type) throws IOException {
        if (type.equals(DBConnector.NGramType.UNIGRAM)) {
            // here mystem processes 'posttongram/mystem/input.txt' and produces 'posttongram/tomitaWorkingFiles/input.txt'
            runMystem();
        } else {
            rewriteInputWithoutModification();
        }
        List<String> nGramms = new ArrayList<>();
        for (String protoFileName : protoFileNames) {
            // here tomita processes 'input.txt' and produces 'PrettyOutput.html'
            runTomita(protoFileName);
            // here we get nGrams (with repeats) from our 'PrettyOutput.html'
            List<String> nGrams = getNGramms();
            nGramms.addAll(nGrams);
        }
        WordFilter wordFilter = new WordFilter();
        List<String> goodNGrams = wordFilter.normalize(nGramms);
        Map<String, String> nGrammPositions = getPositions(goodNGrams);
        return nGrammPositions;
    }

    public static Map<String, String> getPositions(List<String> strArray) {
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

    public static ArrayList<String> getNGramms() throws IOException {
        File tomitaOutputFile = new File(TOMITA_WORKING_DIR + File.separator + TOMITA_OUTPUT_FILE_NAME);
        ArrayList<String> nGramms = new ArrayList<>();
        Document doc = Jsoup.parse(tomitaOutputFile, "UTF-8");
        Elements tables = doc.select("table");
        for (Element table : tables) {
            Element firstTr = table.getElementsByTag("tr").first();
            if (firstTr.text().equals(TOMITA_MESSAGE_NAME)) {
                Elements links = table.getElementsByTag("tr");
                for (int i = 2; i < links.size(); i++) {
                    nGramms.add(links.get(i).text());
                }
                //links.get()
                //for (Element link : links) {
                //    nGramms.add(link.text());
                //}
            }
        }
        return nGramms;
    }

    public static void saveFileForNormalization(String text) throws FileNotFoundException, UnsupportedEncodingException {
        File inputFile = new File(MYSTEM_WORKING_DIR + File.separator + INPUT_FILE_NAME);
        PrintWriter writer = new PrintWriter(inputFile, "UTF-8");
        writer.println(text);
        writer.close();
    }

    public static String getTomitFileName() {
        if (TOMITA_FILENAME.isEmpty()) {
            String oSName = System.getProperty("os.name").toLowerCase();
            if (oSName.indexOf("linux") >= 0) {
                if (System.getProperty("os.arch").indexOf("64") >= 0) {
                    return "tomita-linux64";
                } else {
                    return "tomita-linux32";
                }
            }
            if (oSName.indexOf("win") >= 0) {
                return "tomitaparser.exe";
            }
            if (oSName.indexOf("mac") >= 0) {
                return "tomita-mac";
            }
            return "tomita-freebsd64";
        } else {
            return TOMITA_FILENAME;
        }
    }

    public static String getMystemFileName() {
        if (MYSTEM_FILENAME.isEmpty()) {
            String oSName = System.getProperty("os.name").toLowerCase();

            if (oSName.indexOf("win") >= 0) {
                return "mystem.exe";
            } else {
                return "mystem";
            }
        } else {
            return MYSTEM_FILENAME;
        }
    }


    // запускает tomita с готовым config.proto
    public static void runTomita(String protoFileName) throws IOException {
        File tomitaExecutiveFile = new File(TOMITA_WORKING_DIR + File.separator + getTomitFileName());
        CommandLine cmdLine = new CommandLine(tomitaExecutiveFile);
        cmdLine.addArgument(TOMITA_WORKING_DIR + File.separator + protoFileName);
        execute(cmdLine);
    }

    // запускает mystem
    public static void runMystem() throws IOException {
        File mystemExecutiveFile = new File(MYSTEM_WORKING_DIR + File.separator + getMystemFileName());
        CommandLine cmdLine = new CommandLine(mystemExecutiveFile);
        cmdLine.addArgument("-l");
        cmdLine.addArgument("-c");
        cmdLine.addArgument("-d");
        cmdLine.addArgument(MYSTEM_WORKING_DIR + File.separator + INPUT_FILE_NAME);
        cmdLine.addArgument(TOMITA_WORKING_DIR + File.separator + INPUT_FILE_NAME);
        execute(cmdLine);
    }

    public static void rewriteInputWithoutModification() throws FileNotFoundException, UnsupportedEncodingException {
        File mystemInputFile = new File(MYSTEM_WORKING_DIR + File.separator + INPUT_FILE_NAME);
        String text = new Scanner(mystemInputFile).useDelimiter("\\Z").next();
        System.out.println(text);
        File inputFile = new File(TOMITA_WORKING_DIR + File.separator + INPUT_FILE_NAME);
        PrintWriter writer = new PrintWriter(inputFile, "UTF-8");
        writer.println(text);
        writer.close();
    }

    public static void execute(CommandLine cmdLine) throws IOException {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(WATCHDOG_CONST);
        executor.setWatchdog(watchdog);
        executor.execute(cmdLine);
    }
}
