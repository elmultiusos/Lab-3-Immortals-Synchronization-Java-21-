package edu.eci.arsw.highlandersim;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;

public final class ControlFrame extends JFrame {

    private ImmortalManager manager;
    private final JTextArea output = new JTextArea(14, 40);
    private final JButton startBtn = new JButton("Start");
    private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
    private final JButton resumeBtn = new JButton("Resume");
    private final JButton stopBtn = new JButton("Stop");

    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 5000, 1));
    private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
    private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
    private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

    // Timer para refrescar la vista en tiempo real (ejecuta en EDT)
    private final javax.swing.Timer refreshTimer;

    public ControlFrame(int count, String fight) {
        setTitle("Highlander Simulator — ARSW");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Count:"));
        countSpinner.setValue(count);
        top.add(countSpinner);
        top.add(new JLabel("Health:"));
        top.add(healthSpinner);
        top.add(new JLabel("Damage:"));
        top.add(damageSpinner);
        top.add(new JLabel("Fight:"));
        fightMode.setSelectedItem(fight);
        top.add(fightMode);
        add(top, BorderLayout.NORTH);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(output), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(startBtn);
        bottom.add(pauseAndCheckBtn);
        bottom.add(resumeBtn);
        bottom.add(stopBtn);
        add(bottom, BorderLayout.SOUTH);

        startBtn.addActionListener(this::onStart);
        pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
        resumeBtn.addActionListener(this::onResume);
        stopBtn.addActionListener(this::onStop);

        // Timer: cada 200 ms refresca la vista si la simulación está corriendo y NO está en pausa.
        refreshTimer = new javax.swing.Timer(200, e -> {
            if (manager == null) {
                return;
            }
            try {
                if (!manager.controller().paused()) {
                    updateLiveView(); // refresco en tiempo real
                }
            } catch (Exception ex) {
                // no interrumpimos el timer por excepciones
            }
        });

        pack();
        setLocationByPlatform(true);
        setVisible(true);
    }

    private void onStart(ActionEvent e) {
        safeStop();
        int n = (Integer) countSpinner.getValue();
        int health = (Integer) healthSpinner.getValue();
        int damage = (Integer) damageSpinner.getValue();
        String fight = (String) fightMode.getSelectedItem();
        manager = new ImmortalManager(n, fight, health, damage);
        manager.start();
        // iniciar timer para refresco en tiempo real
        refreshTimer.start();
        output.setText("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n"
                .formatted(n, health, damage, fight));
    }

    private void onPauseAndCheck(ActionEvent e) {
        if (manager == null) {
            return;
        }
        // Parar refresco en tiempo real: vamos a pausar la simulación y mostrar snapshot
        refreshTimer.stop();

        // Pausamos la simulación (bloquea hasta que todos los hilos estén en awaitIfPaused)
        manager.pause();
        manager.pause();
        // Ahora hacemos snapshot consistente
        List<Immortal> pop = manager.populationSnapshot();
        long sum = 0;
        StringBuilder sb = new StringBuilder();
        for (Immortal im : pop) {
            int h = im.getHealth();
            sum += h;
            sb.append(String.format("%-14s : %5d%n", im.name(), h));
        }
        sb.append("--------------------------------\n");
        sb.append("Total Health: ").append(sum).append('\n');
        sb.append("Score (fights): ").append(manager.scoreBoard().totalFights()).append('\n');
        output.setText(sb.toString());
    }

    private void onResume(ActionEvent e) {
        if (manager == null) {
            return;
        }
        manager.resume();

        // Volver a arrancar el refresco en tiempo real para que la GUI muestre actividad
        refreshTimer.start();

        // Mensaje corto en la UI para confirmar reanudado
        output.setText("Simulation resumed...\n");
    }

    private void onStop(ActionEvent e) {
        safeStop();
    }

    private void safeStop() {
        if (manager != null) {
            // parar timer
            refreshTimer.stop();
            manager.stop();
            manager = null;
            // mostrar mensaje en la interfaz
            output.setText("Simulation stopped.\n");
        }
    }

    private void updateLiveView() {
        // Se llama desde EDT por el javax.swing.Timer
        if (manager == null) {
            return;
        }
        List<Immortal> pop = manager.populationSnapshot();
        long sum = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Live view (press Pause & Check for consistent snapshot)\n");
        sb.append("--------------------------------\n");
        for (Immortal im : pop) {
            int h = im.getHealth();
            sum += h;
            sb.append(String.format("%-14s : %5d%n", im.name(), h));
        }
        sb.append("--------------------------------\n");
        sb.append("Total Health: ").append(sum).append('\n');
        sb.append("Score (fights): ").append(manager.scoreBoard().totalFights()).append('\n');
        output.setText(sb.toString());
    }

    public static void main(String[] args) {
        int count = Integer.getInteger("count", 8);
        String fight = System.getProperty("fight", "ordered");
        SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
    }
}
