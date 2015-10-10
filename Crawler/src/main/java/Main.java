import crawler.Crawler;

public class Main {
    public static void main(String[] args) {

        String startUser = "mi3ch";
        Crawler ljCrawler = new Crawler();
        ljCrawler.crawl(startUser);
    }
}
