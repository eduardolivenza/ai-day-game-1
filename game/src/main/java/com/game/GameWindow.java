package com.game;

import javax.swing.*;

public class GameWindow extends JFrame {

    public GameWindow() {
        setTitle("AI Day — The Legend Begins");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);

        panel.startGameLoop();
    }
}
