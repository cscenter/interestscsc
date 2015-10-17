package db;

import java.util.ArrayList;

/**
 * Created by jamsic on 17.10.15.
 */
public class WordFilter {

    public static ArrayList<String> filter(ArrayList<String> words) {
        String [] stopWords = getStopWords();
        ArrayList<String> goodWords = new ArrayList<String>();
        for (String word : words) {
            String normWord = normalize(word);
            if (!isIn(normWord, stopWords)) {
                goodWords.add(normWord);
            }
        }
        return goodWords;
    }

    // привести слово к нижнему регистру
    public static String normalize(String word) {
        return word.toLowerCase();
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

