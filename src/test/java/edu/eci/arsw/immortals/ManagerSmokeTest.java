package edu.eci.arsw.immortals;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import edu.eci.arsw.concurrency.PauseController;

final class ManagerSmokeTest {

    @Test
    void startsAndStops() throws Exception {
        var m = new ImmortalManager(8, "ordered", 100, 10);
        m.start();
        Thread.sleep(50);
        m.pause();
        long sum = m.totalHealth();
        m.resume();
        m.stop();
        assertTrue(sum > 0);
    }

    @Test
    void fightLogicIsCorrect() throws Exception {
        int initialHealth = 100;
        int damage = 10;

        var controller = new PauseController();
        var scoreBoard = new ScoreBoard();

        Immortal a = new Immortal("A", initialHealth, damage, List.of(), scoreBoard, controller);
        Immortal b = new Immortal("B", initialHealth, damage, List.of(), scoreBoard, controller);

        // Usamos reflexión para invocar fightNaive directamente, ya que es un metodo privado
        var method = Immortal.class.getDeclaredMethod("fightNaive", Immortal.class);
        method.setAccessible(true);

        method.invoke(a, b); // A ataca a B

        assertEquals(initialHealth - damage, b.getHealth(),
                "El oponente debe perder exactamente M de salud");
        assertEquals(initialHealth + damage / 2, a.getHealth(),
                "El atacante debe ganar exactamente M/2 de salud");
        assertEquals(1, scoreBoard.totalFights(),
                "Debe registrarse una pelea en el marcador");
    }

    @Test
    void healthDoesNotChangeWhilePaused() throws Exception {
        int n = 10;
        int initialHealth = 100;
        int damage = 10;

        var manager = new ImmortalManager(n, "ordered", initialHealth, damage);
        manager.start();

        // Dejamos que peleen un poco
        Thread.sleep(200);

        // Pausamos (aquí se bloquea hasta que TODOS estén detenidos)
        manager.pause();

        // Medimos dos veces seguidas la salud total
        long total1 = manager.totalHealth();
        Thread.sleep(200); // esperamos un rato extra
        long total2 = manager.totalHealth();

        // Reanudamos y detenemos
        manager.resume();
        manager.stop();

        // Validamos que la salud no haya cambiado mientras estaba pausado
        assertEquals(total1, total2,
                "La salud no debe cambiar mientras los hilos están pausados");
    }
}
