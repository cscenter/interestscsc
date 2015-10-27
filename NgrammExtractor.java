package db;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

/**
 * Created by jamsic on 15.10.15.
 */
public class NgrammExtractor {

    public static void main(String[] args) throws Exception {
        DBConnector dbConnector = new DBConnector();
        dbConnector.setConnectionParams("localhost", "5432", "interests", "interests", "12345");
        dbConnector.connect();

        Statement st = dbConnector.getConnection().createStatement();

        ResultSet rs = st.executeQuery("SELECT * FROM Post");
        while (rs.next()) {
            String textId = rs.getString(1);
            System.out.println("id: " + textId);
            String title = rs.getString(5);
            System.out.println("title: " + title);
            String newText = rs.getString(6);
            String fullText = new String(title + ". " + newText);
            TomitaExecutor tomitaExec = new TomitaExecutor();
            HashMap<String, String> wordsCount = tomitaExec.runTomitaOnText(fullText);
            NGramm[] nGramms = tomitaExec.toNGramm(wordsCount, textId);

            ///*
            for (NGramm nGramm : nGramms) {
                System.out.println(nGramm.getPostId() + " " + nGramm.getnGramm() + " " + nGramm.getCountOccurences());
            }
            //*/

        }
        rs.close();

        st.close();
    }

}
