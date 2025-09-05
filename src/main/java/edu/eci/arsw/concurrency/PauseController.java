package edu.eci.arsw.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PauseController {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition unpaused = lock.newCondition();
    private final Condition fullyPaused = lock.newCondition();
    private volatile boolean paused = false;
    private int waitingThreads = 0;
    private int totalThreads = 0;

    public void registerThread() {
        lock.lock();
        try {
            totalThreads++;
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        lock.lock();
        try {
            paused = true;
            while (waitingThreads < totalThreads) {
                fullyPaused.awaitUninterruptibly(); // esperamos a que todos se detengan
            }
        } finally {
            lock.unlock();
        }
    }

    public void resume() {
        lock.lock();
        try {
            paused = false;
            unpaused.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean paused() {
        return paused;
    }

    public void awaitIfPaused() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (paused) {
                waitingThreads++;
                if (waitingThreads == totalThreads) {
                    fullyPaused.signal(); // notificar que todos llegaron
                }
                try {
                    unpaused.await();
                } finally {
                    waitingThreads--;
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
