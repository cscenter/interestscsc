package com.interestscsc.normalizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class WordFilter {
    private static final String STOP_WORDS_RESOURCE_PATH = "normalizer/stopwords/RU.txt";
    private static final String STOP_WORDS_ENCODING = "UTF-8";
    private static HashSet<String> stopWords = null;

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
        if (stopWords != null)
            return stopWords;

        URL url = WordFilter.class.getClassLoader()
                .getResource(STOP_WORDS_RESOURCE_PATH);
        Scanner scanner;
        try {
            //noinspection ConstantConditions
            scanner = new Scanner(new File(
                    new URI(url.toString()).getPath()), STOP_WORDS_ENCODING
            );
        } catch (NullPointerException | FileNotFoundException | URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException("resource '" + STOP_WORDS_RESOURCE_PATH +
                    "' could not be found or the invoker doesn't have adequate " +
                    "privileges to get the resource");
        }
        LinkedList<String> wordList = new LinkedList<>();
        while (scanner.hasNext())
            wordList.add(scanner.next());

        return stopWords = new HashSet<>(wordList);
    }
}

