package posttongram;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WordFilter {

    public static List<String> filter(ArrayList<String> words) {
        List<String> stopWords = getStopWords();
        words.removeAll(stopWords);
        return words;
    }

    public static List<String> normalize(List<String> words){
        List<String> normalizedWords = words.stream().map(p -> normalizeWord(p)).collect(Collectors.toList());
        return normalizedWords;
    }

    // привести слово к нижнему регистру
    public static String normalizeWord(String word) {
        return word.toLowerCase();
    }

    public static List<String> getStopWords() {
        List<String> stopWords = new ArrayList<>();
        stopWords.add("в");
        stopWords.add("у");
        return stopWords;
    }
}

