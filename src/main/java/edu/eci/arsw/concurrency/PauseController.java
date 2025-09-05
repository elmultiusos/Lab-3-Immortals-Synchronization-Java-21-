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

    /**
     * Registrar un hilo que usará este controlador. Debe llamarse una vez por
     * cada hilo antes de que pueda usar awaitIfPaused.
     */
    public void registerThread() {
        lock.lock();
        try {
            totalThreads++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Desregistrar un hilo que ya no usará este controlador. Debe llamarse una
     * vez por cada hilo que haya llamado a registerThread.
     */
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

    /**
     * Reanudar todos los hilos que estén esperando en awaitIfPaused.
     */
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

    /**
     * Si el controlador está en pausa, el hilo que llame a este método se
     * bloqueará hasta que se reanude. Si no está en pausa, el método retorna
     * inmediatamente.
     *
     * @throws InterruptedException si el hilo es interrumpido mientras espera.
     */
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
