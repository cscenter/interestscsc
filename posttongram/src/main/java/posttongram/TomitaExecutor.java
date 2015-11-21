package posttongram;

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


public class TomitaExecutor {

    private static final String TOMITA_FILENAME = "";
    private static final int WATCHDOG_CONST = 60000;
    private static final String TOMITA_INPUT_FILE_NAME = "input.txt";
    private static final String TOMITA_OUTPUT_FILE_NAME = "PrettyOutput.html";
    private static final String TOMITA_MESSAGE_NAME = "NGrams";
    private static final File TOMITA_WORKING_DIR = new File("posttongram/tomitaWorkingFiles");

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
    public static Map<String, String> runTomitaOnText(String protoFileName) throws IOException {
        // here tomita processes 'test.txt' and produces 'PrettyOutput.html'
        runTomita(protoFileName);
        // here we get nGrams (with repeats) from our 'PrettyOutput.html'
        ArrayList<String> nGramms = getNGramms();
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
                Elements links = table.getElementsByTag("a");
                for (Element link : links) {
                    nGramms.add(link.text());
                }
            }
        }
        return nGramms;
    }

    public static void saveFileForTomita(String text) throws FileNotFoundException, UnsupportedEncodingException {
        File inputFile = new File(TOMITA_WORKING_DIR + File.separator + TOMITA_INPUT_FILE_NAME);
        PrintWriter writer = new PrintWriter(inputFile, "UTF-8");
        writer.println(text);
        writer.close();
    }

    public static String getTomitFileName() {
        if (TOMITA_FILENAME.isEmpty()) {
            String oSName = System.getProperty("os.name");
            if (oSName.indexOf("Linux") >= 0) {
                // разрядность?
                if (System.getProperty("os.arch").indexOf("64") >= 0) {
                    return "tomita-linux64";
                } else {
                    return "tomita-linux32";
                }
            }
            if (oSName.indexOf("Windows") >= 0) {
                return "tomitaparser.exe";
            }
            if (oSName.indexOf("Mac") >= 0) {
                return "tomita-mac";
            }
            return "tomita-freebsd64";
        } else {
            return TOMITA_FILENAME;
        }
    }


    // запускает tomita с готовым config.proto
    public static void runTomita(String protoFileName) throws IOException {
        File tomitaExecutiveFile = new File(TOMITA_WORKING_DIR + File.separator + getTomitFileName());
        CommandLine cmdLine = new CommandLine(tomitaExecutiveFile);
        cmdLine.addArgument(TOMITA_WORKING_DIR + File.separator + protoFileName);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(WATCHDOG_CONST);
        executor.setWatchdog(watchdog);
        executor.execute(cmdLine);
    }
}
