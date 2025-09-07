package edu.eci.arsw.immortals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.eci.arsw.concurrency.PauseController;

public final class ImmortalManager implements AutoCloseable {

    private final List<Immortal> population = new CopyOnWriteArrayList<>();
    private final List<Future<?>> futures = new ArrayList<>();
    private final PauseController controller = new PauseController();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    private ExecutorService exec;

    private final String fightMode;
    private final int initialHealth;
    private final int damage;

    public ImmortalManager(int n, String fightMode) {
        this(n, fightMode, Integer.getInteger("health", 100), Integer.getInteger("damage", 10));
    }

    /**
     * Crea un nuevo ImmortalManager con n inmortales, cada uno con la salud
     * inicial y el daño especificados. El modo de pelea puede ser "naive" u
     * "ordered". Ademas se le asigna un registrador de pausas para controlar la
     * ejecucion de los hilos.
     */
    public ImmortalManager(int n, String fightMode, int initialHealth, int damage) {
        this.fightMode = fightMode;
        this.initialHealth = initialHealth;
        this.damage = damage;
        for (int i = 0; i < n; i++) {
            controller.registerThread();
            population.add(new Immortal("Immortal-" + i, initialHealth, damage, population, scoreBoard, controller));
        }
    }

    public synchronized void start() {
        if (exec != null) {
            stop();
        }
        exec = Executors.newVirtualThreadPerTaskExecutor();
        for (Immortal im : population) {
            futures.add(exec.submit(im));
        }
    }

    public void pause() {
        controller.pause();
    }

    public void resume() {
        controller.resume();
    }

    public void stop() {
        if (exec == null) {
            return;
        }

        // Indicar a todos los inmortales que deben detenerse
        for (Immortal im : population) {
            im.stop();
        }

        // Reanudar todos los hilos pausados para que puedan salir del awaitIfPaused
        controller.resume();

        // Apagar el executor de forma ordenada
        exec.shutdown();
        try {
            // Esperar hasta 5 segundos para que todos los hilos terminen
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                // Si no terminan en tiempo, forzar apagado
                exec.shutdownNow();
            }
        } catch (InterruptedException e) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
        exec = null;
    }

    public int aliveCount() {
        int c = 0;
        for (Immortal im : population) {
            if (im.isAlive()) {
                c++;
            }
        }
        return c;
    }

    public long totalHealth() {
        long sum = 0;
        for (Immortal im : population) {
            sum += im.getHealth();
        }
        return sum;
    }

    public List<Immortal> populationSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(population));
    }

    public ScoreBoard scoreBoard() {
        return scoreBoard;
    }

    public PauseController controller() {
        return controller;
    }

    @Override
    public void close() {
        stop();
    }
}
