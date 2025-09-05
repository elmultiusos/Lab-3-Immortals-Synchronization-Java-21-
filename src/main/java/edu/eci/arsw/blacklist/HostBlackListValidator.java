package edu.eci.arsw.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HostBlackListValidator {
    private final List<BlackListServer> servers;
    public static final int BLACK_LIST_ALARM_COUNT = 5; // ajusta según tu consigna

    public HostBlackListValidator(List<BlackListServer> servers) {
        this.servers = servers;
    }

    public List<Integer> checkHost(String ipAddress, int nThreads) throws InterruptedException {
        List<Integer> blackListOccurrences = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger alarmCounter = new AtomicInteger(0);

        Thread[] threads = new Thread[nThreads];
        int serversPerThread = servers.size() / nThreads;

        for (int i = 0; i < nThreads; i++) {
            final int start = i * serversPerThread;
            final int end = (i == nThreads - 1) ? servers.size() : start + serversPerThread;

            threads[i] = new Thread(() -> {
                for (int j = start; j < end && alarmCounter.get() < BLACK_LIST_ALARM_COUNT; j++) {
                    if (servers.get(j).isInBlackList(ipAddress)) {
                        int count = alarmCounter.incrementAndGet();
                        blackListOccurrences.add(j);

                        if (count >= BLACK_LIST_ALARM_COUNT) {
                            break; // Parada temprana
                        }
                    }
                }
            });
        }

        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();

        return blackListOccurrences;
    }
}