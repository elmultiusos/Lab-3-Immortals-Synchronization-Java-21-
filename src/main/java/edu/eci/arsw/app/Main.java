package edu.eci.arsw.app;
/*
import edu.eci.arsw.demos.DeadlockDemo;
import edu.eci.arsw.demos.OrderedTransferDemo;
import edu.eci.arsw.demos.TryLockTransferDemo;

public final class Main {
  private Main() {}
  public static void main(String[] args) throws Exception {
    String mode = System.getProperty("mode", "ui");
    switch (mode) {
      case "demos" -> {
        String demo = System.getProperty("demo", "2");
        switch (demo) {
          case "1" -> DeadlockDemo.run();
          case "2" -> OrderedTransferDemo.run();
          case "3" -> TryLockTransferDemo.run();
          default -> System.out.println("Use -Ddemo=1|2|3");
        }
      }
      case "immortals", "ui" -> {
        int n = Integer.getInteger("count", 8);
        String fight = System.getProperty("fight", "ordered");
        javax.swing.SwingUtilities.invokeLater(
          () -> new edu.eci.arsw.highlandersim.ControlFrame(n, fight)
        );
      }
      default -> System.out.println("Use -Dmode=immortals|demos|ui");
    }
  }
}
*/

import edu.eci.arsw.core.BlackListServer;
import edu.eci.arsw.core.HostBlackListValidator;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Crear una lista simulada de servidores de lista negra
        int totalServers = 100;
        List<BlackListServer> servers = new ArrayList<>();
        for (int i = 0; i < totalServers; i++) {
            servers.add(new BlackListServer());
        }

        // Crear el validador
        HostBlackListValidator validator = new HostBlackListValidator(servers);

        // Probar la búsqueda con una IP y 10 hilos
        String ipToCheck = "192.168.1.1";
        int numThreads = 10;
        List<Integer> found = validator.checkHost(ipToCheck, numThreads);

        System.out.println("Servidores en lista negra para IP " + ipToCheck + ":");
        System.out.println(found);
        System.out.println("Total encontrados: " + found.size());
    }
}