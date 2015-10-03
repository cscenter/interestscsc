/**
 * Created by jamsic on 26.09.15.
 */

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TomitaExec {

    public static void main(String[] args) {

        String inputFile = "test.txt";
        String outputFile = "testit";
        PrepareProtoFile(inputFile, outputFile);
        runTomita();

        WordFilter a = new WordFilter();
        String[] strArray = {"а", "н", "у", "с", "в"};
        List<String> GoodWords = a.filter(strArray);
        for( String oneItem : GoodWords ) {
            System.out.println(oneItem);
        }

    }

    // запускает tomita с готовым config.proto
    public static void runTomita() {
        // из других директорий не запускается, не знаю почему(
        final String BASE_DIR = System.getProperty("user.dir")  + File.separator + "tomita-linux64";

        CommandLine cmdLine = new CommandLine(BASE_DIR);
        cmdLine.addArgument("config.proto");
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        try {
            int exitValue = executor.execute(cmdLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // возвращает строку, что должна быть в config.proto
    public static String getProtoText(String inputFile, String outputFile) {
        String ProtoString = "encoding \"utf8\"; // указываем кодировку, в которой написан конфигурационный файл\n" +
                "TTextMinerConfig {\n" +
                "  Dictionary = \"mydic.gzt\"; // путь к корневому словарю\n" +
                "  PrettyOutput = \"" + outputFile + ".html\"; // путь к файлу с отладочным выводом в удобном для чтения виде\n" +
                "  Input = {\n" +
                "    File = \""+ inputFile +"\"; // путь к входному файлу\n" +
                "  }\n" +
                "  Articles = [\n" +
                "    { Name = \"наша_первая_грамматика\" } // название статьи в корневом словаре,\n" +
                "                                          // которая содержит запускаемую грамматику\n" +
                "  ]\n" +
                "Facts = [\n" +
                "    { Name = \"Word\" }\n" +
                "]\n" +
                "Output = {\n" +
                "    File = \"" + outputFile + ".txt\";\n" +
                "    Format = text;        // можно использовать следующие форматы:\n" +
                "                          // proto (Google Protobuf), xml, text\n" +
                "}\n" +
                "}";
        return ProtoString;
    }

    // записывает config.proto
    public static void PrepareProtoFile(String inputFile, String outputFile) {
        String ProtoString = getProtoText(inputFile, outputFile);
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("config.proto"), "utf-8"))) {
            try {
                writer.write(ProtoString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
class TomitaHTMLParser {

    public static String parse(String OutputFileName) {
        final String BASE_DIR = System.getProperty("user.dir")  + File.separator + OutputFileName;
        return "";
    }
}
*/

// Будущий фильтр для стоп-слов
class WordFilter {

    public static List<String> filter(String[] Words) {
        String [] StopWords = getStopWords();
        List<String> GoodWords = new ArrayList<String>();
        for (String Word : Words) {
            String NormWord = normalize(Word);
            if (!isIn(NormWord, StopWords)) {
                GoodWords.add( NormWord );
            }
        }
        return GoodWords;
    }

    // привести слово к нижнему регистру
    public static String normalize(String Word) {
        return Word.toLowerCase();
    }

    public static boolean isIn(String item, String[] strArray) {
        for (String str : strArray) {
            if (str.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getStopWords() {
        String[] a = {"в", "у"};
        return a;
    }
}