import db.DBConnector;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

/**
 * User: allight
 * Date: 19.12.2015 09:11
 */

public class DBConnectorTestStatistics {
    // Задать нужное количество тегов из топа по количеству употреблений
    private static final int NUMBER_OF_TOP_TAGS_TO_GET_STATISTIC_FOR = 100;

    public static void main(String[] args)
            throws SQLException, FileNotFoundException, UnsupportedEncodingException {

        DBConnector.DataBase dbName = DBConnector.DataBase.MAIN;
        DBConnector db = new DBConnector(dbName);

        ArrayList<Date> weekList = new ArrayList<>();
        ArrayList<String> tagList = new ArrayList<>();

        int[][] res = db.getTagByWeekStatisticsPerYear(
                weekList, tagList, NUMBER_OF_TOP_TAGS_TO_GET_STATISTIC_FOR
        );

        PrintWriter writer = new PrintWriter("weekly-tag-statistic.txt", "UTF-8");

        writer.print("X\t");
        for (Date week : weekList)
            writer.print(week + "\t");
        writer.println();
        int tadIt = 0;
        for (int[] resI : res) {
            writer.print(tagList.get(tadIt++) + "\t");
            for (int resIJ : resI)
                writer.print(resIJ + "\t");
            writer.println();
        }
        writer.close();
    }
}