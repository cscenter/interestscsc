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

    static final HashMap<String, String> configFileNgrammType = new HashMap();
    static {
        configFileNgrammType.put("config1.proto", "Unigram");
        configFileNgrammType.put("config2.proto", "Bigram");
        configFileNgrammType.put("config3.proto", "Trigram");
    }

    public static List<NGram> toNGramm(Map<String, String> positionMap) {
        Set<String> keySet = positionMap.keySet();
        List<NGram> nGrams = new LinkedList<>();
        int i = 0;
        for (String nGramm : keySet) {
            int usescount = positionMap.get(nGramm).split(",").length;
            NGram newNGramm = new NGram(nGramm, positionMap.get(nGramm), usescount);
            nGrams.add(newNGramm);
        }
        return nGrams;
    }

    // processNGrams
    public static Map<String, String> runTomitaOnText(String protoFileName) {
        // here tomita processes 'test.txt' and produces 'PrettyOutput.html'
        runTomita(protoFileName);
        // here we get nGrams (with repeats) from our 'PrettyOutput.html'
        ArrayList<String> nGramms = getNGramms();
        WordFilter wordFilter = new WordFilter();
        List<String> goodNGrams = wordFilter.normalize(wordFilter.filter(nGramms));
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

    public static ArrayList<String> getNGramms() {
        File tomitaOutputFile = new File(TOMITA_WORKING_DIR + File.separator + TOMITA_OUTPUT_FILE_NAME);
        ArrayList<String> nGramms = new ArrayList<>();
        Document doc = null;
        try {
            doc = Jsoup.parse(tomitaOutputFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void saveFileForTomita(String text) {
        PrintWriter writer = null;
        try {
            File inputFile = new File(TOMITA_WORKING_DIR + File.separator + TOMITA_INPUT_FILE_NAME);
            writer = new PrintWriter(inputFile, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        writer.println(text);
        writer.close();
    }

    public static String getTomitFileName() {
        if (!TOMITA_FILENAME.isEmpty()) {
            return TOMITA_FILENAME;
        } else {
            String oSName = System.getProperty("os.name");
            if (oSName.equals("Linux")) {
                // разрядность?
                return "tomita-linux64";
            }
            if (oSName.equals("Windows")) {
                return "tomita-win32";
            }
            if (oSName.equals("Mac")) {
                return "tomita-mac";
            }
            return "tomita-freebsd";
        }
    }


    // запускает tomita с готовым config.proto
    public static void runTomita(String protoFileName) {
        File tomitaExecutiveFile = new File(TOMITA_WORKING_DIR + File.separator + getTomitFileName());

        CommandLine cmdLine = new CommandLine(tomitaExecutiveFile);
        cmdLine.addArgument(TOMITA_WORKING_DIR + File.separator + protoFileName);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(WATCHDOG_CONST);
        executor.setWatchdog(watchdog);
        try {
            executor.execute(cmdLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

