package com.interestscsc.crawler.proxy;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.interestscsc.crawler.loaders.ProxyLoader;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProxyFactory {

    final static double PERCENT_BROKEN_PROXY_TO_RECHECKING = 0.85;

    private Set<HttpHost> rawProxies;
    private List<HttpHost> workingProxies;
    private List<HttpHost> brokenProxies;
    private Set<String> rawAllUsers;
    private final Random random = new Random();
    private static final Logger logger = Logger.getLogger(ProxyFactory.class);

    private static final HttpHost defaultWorkingProxy = new HttpHost("24.246.127.180", 8080);
    private static final String defaultUser = "mi3ch";

    private static final String RAW_PROXIES_FILE = "proxies.txt";
    private static final String WORKING_PROXIES_FILE = "working-proxies.txt";
    private final String PATH_TO_FILE_WITH_PROXY = "Crawler" + File.separator + "src" + File.separator +
            "main" + File.separator + "resources" + File.separator;

    public ProxyFactory() {
        rawProxies = new HashSet<>();
        workingProxies = new ArrayList<>();
        workingProxies.add(defaultWorkingProxy);
        brokenProxies = new ArrayList<>();
        rawAllUsers = new HashSet<>();
        rawAllUsers.add(defaultUser);
    }

    public void setRawAllUsers(final Set<String> rawAllUsers) {
        this.rawAllUsers = rawAllUsers;
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

    public HttpHost getNextProxy() {
        if (workingProxies.isEmpty()) {
            logger.warn("Working proxies ended! Please, wait for result of new checking!");
            startCheckingProxy();
        }

        // if working proxy-list is empty, then we should sleeping until new proxy doesn't find
        while (workingProxies.isEmpty()) {
            try {
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

    public void insertFromFile(String fileName) {
        File file = new File(PATH_TO_FILE_WITH_PROXY + fileName);
        try (BufferedReader bufferedReaderProxy = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {
            String line;
            while ((line = bufferedReaderProxy.readLine()) != null) {
                String[] proxyString = line.split(":");
                rawProxies.add(new HttpHost(proxyString[0], Integer.parseInt(proxyString[1])));
            }
        } catch (IOException e) {
            logger.error("Invalid filename: " + fileName + " or error reading data from the file. " + e);
        }
    }

    public void startCheckingProxy() {
        if (rawProxies.isEmpty()) {
            insertFromFile(WORKING_PROXIES_FILE);
            insertFromFile(RAW_PROXIES_FILE);
        }
        logger.info("Start new session of checking proxies!");
        brokenProxies.clear();
        Thread proxyThread = new Thread(() -> {
            findWorkingProxy(rawAllUsers);
        });
        proxyThread.start();
    }

    public void findWorkingProxy(final Set<String> rawUsers) {
        ExecutorService service = Executors.newCachedThreadPool();
        Iterator<String> iterator = rawUsers.iterator();
        rawProxies.parallelStream().forEach(
                proxy -> service.execute(
                        () -> checkProxy(proxy, iterator.next()))
        );

        service.shutdown();
        while (!service.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("Interrupt error");
            }
        }

        logger.info("All thread was finished.");
        writeWorkingProxiesToFile("working-proxies.txt");
    }

    private void writeWorkingProxiesToFile(final String fileName) {

        File file = new File(PATH_TO_FILE_WITH_PROXY + fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.info("Error creating new file: " + fileName);
            }
        }

        try (BufferedWriter bufferedWriterProxy = new BufferedWriter(new FileWriter(file.getAbsoluteFile()))) {
            for (HttpHost proxy : workingProxies) {
                bufferedWriterProxy.write(proxy.getHostName() + ":" + proxy.getPort() + "\n");
            }
        } catch (IOException e) {
            logger.error("Error writing to file: " + fileName + ". " + e);
        }
    }

    private synchronized void checkProxy(final HttpHost proxy, final String nick) {
        int response;
        try {
            logger.info("Check proxy: " + proxy.toString());
            response = new ProxyLoader().loadData(new HttpHost(proxy.getHostName(), proxy.getPort()), nick);
        } catch (RuntimeException | InterruptedException | UnirestException | IOException e) {
            logger.error("Error checking proxy: " + proxy.toString() + " to find info about user: " + nick + ". " + e);
            brokenProxies.add(proxy);
            Thread.currentThread().interrupt();
            return;
        }

        if (response == 0) {
            logger.info("Find new working proxy: " + proxy.toString());
            if (!workingProxies.contains(proxy)) {
                workingProxies.add(proxy);
            }
        } else {
            logger.info("No access to LJ for proxy: " + proxy.toString());
            brokenProxies.add(proxy);
        }
    }
}