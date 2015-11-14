package posttongram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WordFilter {

    public static List<String> filter(ArrayList<String> words) {
        List<String> stopWords = getStopWords();
        words.removeAll(stopWords);
        return words;
    }

    public static List<String> normalize(List<String> words){
        List<String> normalizedWords = new ArrayList<>();
        for (String word : words) {
            normalizedWords.add(normalizeWord(word));
        }
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

