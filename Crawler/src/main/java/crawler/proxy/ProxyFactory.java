package crawler.proxy;

import com.mashape.unirest.http.exceptions.UnirestException;
import crawler.loaders.BaseLoader;
import crawler.loaders.UserInfoLoader;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProxyFactory {
    private Set<HttpHost> rawProxies;
    private List<HttpHost> workingProxies;
    final Random random = new Random();
    private static final Logger logger = Logger.getLogger(ProxyFactory.class);

    private final String PATH_TO_FILE_WITH_PROXY = "Crawler" + File.separator + "src" + File.separator +
            "main" + File.separator + "resources" + File.separator;

    public ProxyFactory() {
        rawProxies = new HashSet<>();
        workingProxies = new LinkedList<>();
    }

    public HttpHost getNextProxy() {
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

    public void insertFromFile(String fileName, boolean working) {
        File file = new File(PATH_TO_FILE_WITH_PROXY + fileName);
        try (BufferedReader bufferedReaderProxy = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {
            String line;
            while ((line = bufferedReaderProxy.readLine()) != null) {
                String[] proxyString = line.split(":");
                if (working) {
                    workingProxies.add(new HttpHost(proxyString[0], Integer.parseInt(proxyString[1])));
                }else {
                    rawProxies.add(new HttpHost(proxyString[0], Integer.parseInt(proxyString[1])));
                }
            }
        } catch (IOException e) {
            logger.error("Invalid filename: " + fileName + " or error reading data from the file. " + e);
        }
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
            for (HttpHost proxy: workingProxies) {
                bufferedWriterProxy.write(proxy.getHostName() + ":" + proxy.getPort() + "\n");
            }
        } catch (IOException e) {
            logger.error("Error writing to file: " + fileName + ". " + e);
        }
    }

    private synchronized void checkProxy(final HttpHost proxy, final String nick) {
        String response;
        try {
            logger.info("Check proxy: " + proxy.toString());
            response = new UserInfoLoader().loadData(new HttpHost(proxy.getHostName(), proxy.getPort()), nick);
        } catch (RuntimeException | InterruptedException | UnirestException | IOException e) {
            logger.error("Error checking proxy: " + proxy.toString() + " to find info about user: " + nick + ". " + e);
            Thread.currentThread().interrupt();
            return;
        }

        if (!BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            logger.info("Find new working proxy: " + proxy.toString());
            if (!workingProxies.contains(proxy)) {
                workingProxies.add(proxy);
            }
        }
        else {
            logger.info("No access to LJ for proxy: " + proxy.toString());
        }
    }
}
