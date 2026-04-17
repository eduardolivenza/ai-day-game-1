package com.game;

import javax.swing.SwingUtilities;

public class Main {
    static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
