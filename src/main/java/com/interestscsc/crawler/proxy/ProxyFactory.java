package com.interestscsc.crawler.proxy;

import com.interestscsc.crawler.loaders.ProxyChecker;
import com.interestscsc.crawler.loaders.ProxyListLoader;
import com.interestscsc.crawler.parsers.ProxyListParser;
import com.interestscsc.exceptions.NotFoundPageException;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProxyFactory {

    private static final double PERCENT_BROKEN_PROXY_TO_RECHECKING = 0.95;
    private static final Logger logger = Logger.getLogger(ProxyFactory.class);
    private static final String DEFAULT_USER = "mi3ch";
    private static final int MAX_NUMBER_PAGES_ON_SITE_TO_FOUND_PROXIES = 30;
    private static final String RAW_PROXIES_FILE_FULL_PATH = "src" + File.separator +
            "main" + File.separator + "resources" + File.separator +
            "crawler" + File.separator + "proxy" + File.separator +
            "proxies.txt";
    private static final String WORKING_PROXIES_FILE_FULL_PATH = "src" + File.separator +
            "main" + File.separator + "resources" + File.separator +
            "crawler" + File.separator + "proxy" + File.separator +
            "working-proxies.txt";

    private final Random random = new Random();
    private boolean isCheckingNow = false;

    private Set<HttpHost> rawProxies;
    private List<HttpHost> workingProxies;
    private List<HttpHost> brokenProxies;
    private List<String> rawAllUsers;

    public ProxyFactory() {
        rawProxies = new HashSet<>();
        workingProxies = new ArrayList<>();
        brokenProxies = new ArrayList<>();
        rawAllUsers = new ArrayList<>();
        rawAllUsers.add(DEFAULT_USER);
    }

    public void setRawAllUsers(final Set<String> rawAllUsers) {
        this.rawAllUsers.addAll(rawAllUsers);
    }

    public void setBrokenProxies(final HttpHost proxy) {
        logger.info("Proxy: " + proxy + " is not working! It goes to broken list.");
        workingProxies.remove(proxy);
        brokenProxies.add(proxy);
        if (1.f * brokenProxies.size() / rawProxies.size() > PERCENT_BROKEN_PROXY_TO_RECHECKING) {
            logger.info("Broken proxies are too much. Maybe, need to change proxy list?!");
            startCheckingProxy();
        }
    }

    public void clearWorkingProxy() {
        workingProxies.clear();
    }

    public HttpHost getNextProxy() {
        if (workingProxies.isEmpty()) {
            logger.warn("Working proxies ended! Please, wait for result of new checking!");
            startCheckingProxy();
        }

        if (rawProxies.isEmpty()) {
            logger.warn("Program haven't any proxies! Please, add proxies to file or change website!!!");
            return null;
        }

        /**
         *  if working proxy-list is empty, then we should sleeping until new proxy doesn't find
         */
        while (workingProxies.isEmpty()) {
            try {
                logger.info("Haven't any working proxy. Crawler wait 10 sec.");
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                logger.error("Interrupt sleeping. " + e);
            }
        }
        return workingProxies.get(random.nextInt(workingProxies.size()));
    }

    public void setProxy(String hostname, int port) {
        rawProxies.add(new HttpHost(hostname, port));
    }

    public int getCountRawProxies() {
        return rawProxies.size();
    }

    public int getCountWorkingProxies() {
        return workingProxies.size();
    }

    public boolean insertFromFile() {
        logger.info("Start getting proxy-list from file: " + RAW_PROXIES_FILE_FULL_PATH);
        File file = new File(RAW_PROXIES_FILE_FULL_PATH);
        try (BufferedReader bufferedReaderProxy = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {
            String line;
            while ((line = bufferedReaderProxy.readLine()) != null) {
                String[] proxyString = line.split(":");
                rawProxies.add(new HttpHost(proxyString[0], Integer.parseInt(proxyString[1])));
            }
        } catch (IOException e) {
            logger.error("Invalid filename: " + RAW_PROXIES_FILE_FULL_PATH + " or error reading data from the file. " + e);
            return false;
        }
        return !rawProxies.isEmpty();
    }

    public boolean insertFromSite() {
        try {
            logger.info("Start getting proxy-list from site.");
            Calendar calendar = Calendar.getInstance();
            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            int notFoundPagesNumber = 0;
            String response = ProxyListLoader.NOT_FOUND_STATUS_PAGE;
            while (ProxyListLoader.NOT_FOUND_STATUS_PAGE.equals(response)) {
                String dateString = dateFormat.format(calendar.getTime());
                response = new ProxyListLoader().loadData(null, dateString);
                calendar.add(Calendar.DATE, -1);
                if (notFoundPagesNumber++ > MAX_NUMBER_PAGES_ON_SITE_TO_FOUND_PROXIES) {
                    throw new NotFoundPageException(new ProxyListLoader().getUrl());
                }
            }
            rawProxies = ProxyListParser.getProxyList(response);
        } catch (IOException | UnirestException | InterruptedException e) {
            logger.error("Getting proxy-list from site failed. Error: " + e);
            return false;
        } catch (NotFoundPageException e) {
            logger.error(e.getMessage());
            logger.error("Please, change website with proxy-list.");
            return false;
        }
        return !rawProxies.isEmpty();
    }

    private void startCheckingProxy() {
        if (isCheckingNow) {
            logger.info("Please, wait finding new working proxy. A search is working now.");
            return;
        }

        if (rawProxies.isEmpty()) {
            if (!insertFromSite()) {
                if (!insertFromFile()) {
                    logger.warn("Raw proxy-list is empty!");
                    return;
                }
            }
            logger.info("Proxies are added successfully.");
        }

        logger.info("Start new session of checking proxies!");
        logger.info("Clearing file with working proxies.");
        try (FileWriter fileWriter = new FileWriter(WORKING_PROXIES_FILE_FULL_PATH)) {
            fileWriter.write("");
        } catch (IOException e) {
            logger.error("Error clearing file: " + e);
        }

        brokenProxies.clear();
        isCheckingNow = true;
        findWorkingProxy();
    }

    private void findWorkingProxy() {
        ExecutorService service = Executors.newCachedThreadPool();
        rawProxies.parallelStream().forEach(
                proxy -> CompletableFuture.supplyAsync(() -> checkProxy(proxy, getRandomRawNick()), service)
                        .thenAccept(this::writeProxyToFile)
        );

        service.shutdown();
        isCheckingNow = false;
    }

    private ProxyWrapper checkProxy(HttpHost proxy, String nick) {
        boolean response;
        try {
            logger.info("Check proxy: " + proxy.toString());
            response = new ProxyChecker().loadData(
                    new HttpHost(proxy.getHostName(), proxy.getPort()), nick);
        } catch (InterruptedException | UnirestException | IOException e) {
            logger.error("Error checking proxy: " + proxy.toString() + " to find info about user: " + nick + ". " + e);
            brokenProxies.add(proxy);
            return new ProxyWrapper(proxy, false);
        }

        return new ProxyWrapper(proxy, response);
    }

    private synchronized void writeProxyToFile(ProxyWrapper proxyWrapper) {
        HttpHost proxy = proxyWrapper.proxy;

        if (proxyWrapper.isWorking) {
            logger.info("Find new working proxy: " + proxy.toString());
            if (!workingProxies.contains(proxy)) {
                workingProxies.add(proxy);
                try (PrintWriter printWriter = new PrintWriter(
                        new FileWriter(WORKING_PROXIES_FILE_FULL_PATH, true))) {
                    printWriter.println(proxy.getHostName() + ":" + proxy.getPort());
                } catch (IOException e) {
                    logger.error("Error writing to file: " + WORKING_PROXIES_FILE_FULL_PATH + ". " + e);
                }
            }
        } else {
            logger.info("No access to LJ for proxy: " + proxy.toString());
            brokenProxies.add(proxy);
        }
    }

    private String getRandomRawNick() {
        return rawAllUsers.get(random.nextInt(rawAllUsers.size()));
    }

    private static class ProxyWrapper {
        HttpHost proxy;
        boolean isWorking;

        public ProxyWrapper(HttpHost proxy, boolean isWorking) {
            this.proxy = proxy;
            this.isWorking = isWorking;
        }
    }
}
