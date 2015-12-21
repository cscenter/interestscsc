package posttongram;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WordFilter {

    public static List<String> filter(List<String> words) {
        Set<String> stopWords = getStopWords();
        words.removeAll(stopWords);
        return words;
    }

    public static List<String> normalize(List<String> words) {
        List<String> normalizedWords = words.stream().map(p -> normalizeWord(p)).collect(Collectors.toList());
        normalizedWords = filter(normalizedWords);
        return normalizedWords;
    }

    // привести слово к нижнему регистру
    public static String normalizeWord(String word) {
        return word.toLowerCase();
    }

    public static Set<String> getStopWords() {
        Set<String> stopWords = new HashSet<>();
        stopWords.add("в");
        stopWords.add("нет");
        stopWords.add("до");
        stopWords.add("эта");
        stopWords.add("ли");
        stopWords.add("с");
        stopWords.add("уж");
        stopWords.add("той");
        stopWords.add("тут");
        stopWords.add("той");
        stopWords.add("пот");
        stopWords.add("ком");
        stopWords.add("один");
        stopWords.add("второй");
        stopWords.add("втора");
        stopWords.add("ува");
        stopWords.add("впоследствии");
        stopWords.add("гот");
        stopWords.add("другой");
        stopWords.add("том");
        stopWords.add("кв");
        return stopWords;
    }
}

