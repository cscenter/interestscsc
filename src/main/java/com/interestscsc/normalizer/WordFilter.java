package com.interestscsc.normalizer;

import java.util.Collections;
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
        List<String> normalizedWords = words.stream()
                .map(WordFilter::normalizeWord).collect(Collectors.toList());
        normalizedWords = filter(normalizedWords);
        return normalizedWords;
    }

    /**
     * привести слово к нижнему регистру
     */
    public static String normalizeWord(String word) {
        return word.toLowerCase();
    }

    public static Set<String> getStopWords() {
        Set<String> stopWords = new HashSet<>();
        String[] stopWordsArray = {
                "в", "нет", "до", "эта", "ли", "с", "уж", "той", "тут", "той",
                "пот", "ком", "один", "второй", "втора", "ува", "впоследствии",
                "гот", "другой", "том", "кв"
        };
        Collections.addAll(stopWords, stopWordsArray);
        return stopWords;
    }
}

