/**
 * Created by jamsic on 26.09.15.
 */
package posttongram;

import db.DBConnector;
import data.Post;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import data.NGram;

import java.io.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class TomitaExecutor {

    private static final int WATCHDOG_CONST = 60000;
    static final String TOMITA_INPUT_FILE_NAME = "input.txt";
    static final String TOMITA_OUTPUT_FILE_NAME = "PrettyOutput.html";
    private static final int ID_INDEX = 1;
    private static final int TITLE_INDEX = 5;
    private static final int TEXT_INDEX = 6;
    private static final String BASE_DIR = System.getProperty("user.dir")  + File.separator + getTomitFileName();
    private static final String TOMITA_MESSAGE_NAME = "NGrams";
    private static final File testDir = new File("posttongram/tomitaWorkingFiles");

    static final HashMap<String, String> configFileNgrammType = new HashMap();
    static {
        configFileNgrammType.put("config1.proto", "Unigram");
        configFileNgrammType.put("config2.proto", "Bigram");
        configFileNgrammType.put("config3.proto", "Trigram");
    }


    public static void main(String[] args) throws Exception {
        DBConnector db = new DBConnector("DBConnectorTestNormalization");

        //Statement st = dbConnector.getConnection().createStatement();

        //ResultSet rs = st.executeQuery("SELECT * FROM Post");
        LinkedList<Post> unprocessedPosts = db.getPostsToNormalize(1);

        for (Post post : unprocessedPosts) {
            long textId = post.getId();
            System.out.println("id: " + textId);
            String title = post.getTitle();
            System.out.println("title: " + title);
            String newText = post.getText();
            String fullText = new String(title + ". " + newText);
            TomitaExecutor tomitaExec = new TomitaExecutor();
            // here we have input file named 'test.txt' for tomita to process it!
            tomitaExec.saveFileForTomita(fullText);

            for (String protoFileName : configFileNgrammType.keySet()) {
                String ngrammType = configFileNgrammType.get(protoFileName);
                //System.out.println(a + " " + configFilesToNgramms.get(a));
                Map<String, String> wordsCount = tomitaExec.runTomitaOnText(protoFileName);
                List<NGram> nGramms = tomitaExec.toNGramm(wordsCount, textId);

                ///*
                for (NGram nGramm : nGramms) {
                    System.out.println(post.getId() + " " + nGramm.getText() + " " + nGramm.getUsesCnt()
                            + " " + nGramm.getUsesStr());
                }
                //*/
                System.out.println(nGramms.size());
            }

            //Map<String, String> wordsPositions = tomitaExec.runTomitaOnText(fullText);
            //NGramm[] nGramms = tomitaExec.toNGramm(wordsPositions, textId);

            /*
            for (NGramm nGramm : nGramms) {
                System.out.println(nGramm.getPostId() + " " + nGramm.getnGramm() + " " + nGramm.getPositions()
                        + " " + nGramm.getCountOccurences());
            }
            //*/


        }

        //rs.close();

        //st.close();
    }
    //*/

    public static List<NGram> toNGramm(Map<String, String> positionMap, long idText) {
        Set<String> keySet = positionMap.keySet();
        NGram[] nGramms = new NGram[keySet.size()];
        List<NGram> nGrams = new LinkedList<NGram>();
        int i = 0;
        for (String nGramm : keySet) {
            int usescount = positionMap.get(nGramm).split(",").length;
            NGram newNGramm = new NGram(nGramm, positionMap.get(nGramm), usescount);
            //nGramms[i] = newNGramm;
            nGrams.add(newNGramm);
            //i++;
        }
        return nGrams;
    }

    private static void printCount(Map<String, Integer> countMap) {
        Set<String> keySet = countMap.keySet();
        for (String string : keySet) {
            System.out.println(string + " : " + countMap.get(string));
        }
    }


    // processNGrams
    public static Map<String, String> runTomitaOnText(String protoFileName) {
        // here we have input file named 'test.txt' for tomita to process it!
        //saveFileForTomita(text);
        // here tomita processes 'test.txt' and produces 'PrettyOutput.html'
        runTomita(protoFileName);
        // here we get nGrams (with repeats) from our 'PrettyOutput.html'
        ArrayList<String> nGramms = getNGramms();

        WordFilter wordFilter = new WordFilter();
        ArrayList<String> goodNGrams = wordFilter.filter(nGramms);

        //Map<String, Integer> nGrammCount = countOccurences(goodNGrams);
        Map<String, String> nGrammPositions = getPositions(goodNGrams);

        return nGrammPositions;
    }

    public static Map<String, String> getPositions(ArrayList<String> strArray) {
        Map<String, String> positionMap = new HashMap<String, String>();
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

    /*
    public static Map<String, Integer> countOccurences(ArrayList<String> strArray) {
        Map<String, Integer> countMap = new HashMap<String, Integer>();
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
    */

    public static ArrayList<String> getNGramms() {
        File tomitaOutputFile = new File(testDir + File.separator + TOMITA_OUTPUT_FILE_NAME);
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
            File inputFile = new File(testDir + File.separator + TOMITA_INPUT_FILE_NAME);
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
    public static void runTomita(String protoFileName) {
        // из других директорий не запускается, не знаю почему(
        //System.out.println(BASE_DIR);
        //System.out.print("f");
        System.out.println(protoFileName);

        /** simulates a PDF print job */
        final File acroRd32Script = new File(testDir + File.separator + getTomitFileName());
        System.out.println(acroRd32Script.getPath());

        CommandLine cmdLine = new CommandLine(acroRd32Script);
        cmdLine.addArgument(testDir + File.separator + protoFileName);
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