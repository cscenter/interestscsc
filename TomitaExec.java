/**
 * Created by jamsic on 26.09.15.
 */

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class TomitaExec {

    private static final String PROTO_FILE = "config.proto";
    private static final int WATCHDOG_CONST = 60000;
    static final String TOMITA_INPUT_FILE_NAME = "test.txt";
    static final String TOMITA_OUTPUT_FILE_NAME = "PrettyOutput.html";
    private static final int ID_INDEX = 1;
    private static final int TITLE_INDEX = 5;
    private static final int TEXT_INDEX = 6;
    private static final String BASE_DIR = System.getProperty("user.dir")  + File.separator + getTomitFileName();
    private static final String TOMITA_MESSAGE_NAME = "Word";


    public static void main(String[] args) throws Exception {
        DBConnector dbConnector = new DBConnector();
        dbConnector.setConnectionParams("localhost", "5432", "interests", "interests", "12345");
        dbConnector.connect();

        Statement st = dbConnector.getConnection().createStatement();

        ResultSet rs = st.executeQuery("SELECT * FROM Post");
        while (rs.next()) {
            String textId = rs.getString(ID_INDEX);
            System.out.println("id: " + textId);
            String title = rs.getString(TITLE_INDEX);
            System.out.println("title: " + title);
            String newText = rs.getString(TEXT_INDEX);
            String fullText = new String(title + ". " + newText);
            TomitaExec tomitaExec = new TomitaExec();
            HashMap<String, Integer> wordsCount = tomitaExec.runTomitaOnText(fullText);
            NGramm[] nGramms = tomitaExec.toNGramm(wordsCount, textId);

            ///*
            for (NGramm nGramm : nGramms) {
                System.out.println(nGramm.getTextId() + " " + nGramm.getnGramm() + " " + nGramm.getCountOccurences());
            }
            //*/


        }

        rs.close();

        st.close();
    }
    //*/

    public static NGramm[] toNGramm(HashMap<String, Integer> countMap, String idText) {
        Set<String> keySet = countMap.keySet();
        NGramm[] nGramms = new NGramm[keySet.size()];
        int i = 0;
        for (String nGramm : keySet) {
            NGramm newNGramm = new NGramm(idText, nGramm, countMap.get(nGramm));
            nGramms[i] = newNGramm;
            i++;
        }
        return nGramms;
    }

    private static void printCount(HashMap<String, Integer> countMap) {
        Set<String> keySet = countMap.keySet();
        for (String string : keySet) {
            System.out.println(string + " : " + countMap.get(string));
        }
    }


    // processNGramms
    public static HashMap<String, Integer> runTomitaOnText(String text) {
        // here we have input file named 'test.txt' for tomita to eat it!
        saveFileForTomita(text);
        // here tomita ate 'test.txt' and produced 'PrettyOutput.html'
        runTomita();
        // here we get nGrams (with repeats) from our 'PrettyOutput.html'
        ArrayList<String> nGramms = getNGramms();

        WordFilter wordFilter = new WordFilter();
        ArrayList<String> goodNGrams = wordFilter.filter(nGramms);

        HashMap<String, Integer> nGrammCount = countOccurences(goodNGrams);

        return nGrammCount;
    }

    public static HashMap<String, Integer> countOccurences(ArrayList<String> strArray) {
        HashMap<String, Integer> countMap = new HashMap<String, Integer>();
        for (String string : strArray) {
            if (!countMap.containsKey(string)) {
                countMap.put(string, 1);
            } else {
                Integer count = countMap.get(string);
                count = count + 1;
                countMap.put(string, count);
            }
        }
        return countMap;
    }

    public static ArrayList<String> getNGramms() {
        File tomitaOutputFile = new File(TOMITA_OUTPUT_FILE_NAME);
        ArrayList<String> nGramms = new ArrayList<String>();
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
            writer = new PrintWriter(TOMITA_INPUT_FILE_NAME, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        writer.println(text);
        writer.close();
    }

    public static String getTomitFileName() {
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


    // запускает tomita с готовым config.proto
    public static void runTomita() {
        // из других директорий не запускается, не знаю почему(
        System.out.println(BASE_DIR);

        CommandLine cmdLine = new CommandLine(BASE_DIR);
        cmdLine.addArgument(PROTO_FILE);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(WATCHDOG_CONST);
        executor.setWatchdog(watchdog);
        try {
            int exitValue = executor.execute(cmdLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


class NGramm {
    private String textId;
    private String nGramm;
    private int countOccurences;

    NGramm() {

    }

    NGramm(final String textId, final String nGramm, final int countOccurences) {
        this.textId = textId;
        this.nGramm = nGramm;
        this.countOccurences = countOccurences;
    }

    String getTextId() {
        return this.textId;
    }

    String getnGramm() {
        return this.nGramm;
    }

    int getCountOccurences() {
        return this.countOccurences;
    }

    void setTextId(final String textId) {
        this.textId = textId;
    }

    void setnGramm(final String nGramm) {
        this.nGramm = nGramm;
    }

    void setCountOccurences(final int countOccurences) {
        this.countOccurences = countOccurences;
    }
}
