package com.game.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean up, down, left, right;
    public boolean enterPressed;   // ENTER or SPACE — game-world interact / dialog confirm
    public boolean confirmPressed; // ENTER only — submit typed text in dialog
    public boolean escPressed;
    public boolean tPressed;                    // open typing mode in dialog
    public boolean[] numPressed = new boolean[4]; // indices 1–3 used

    // Last typed character (consumed by DialogBox each frame)
    private volatile char lastTyped = 0;

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP,    KeyEvent.VK_W -> up    = true;
            case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> down  = true;
            case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> left  = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE -> enterPressed = true;
            case KeyEvent.VK_ENTER -> { enterPressed = true; confirmPressed = true; }
            case KeyEvent.VK_ESCAPE -> escPressed = true;
            case KeyEvent.VK_T      -> tPressed   = true;
            case KeyEvent.VK_1 -> numPressed[1] = true;
            case KeyEvent.VK_2 -> numPressed[2] = true;
            case KeyEvent.VK_3 -> numPressed[3] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP,    KeyEvent.VK_W -> up    = false;
            case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> down  = false;
            case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> left  = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = false;
        }
    }

    /**
     * Captures every typed character including backspace (\b) and printable chars.
     * ENTER / ESC are handled via keyPressed flags, not here.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        // Store printable chars and backspace; ignore ENTER/ESC (handled elsewhere)
        if (c != KeyEvent.CHAR_UNDEFINED && c != '\n' && c != '\r' && c != 27) {
            lastTyped = c;
        }
    }

    /** Returns the last typed character and clears it. Returns 0 if nothing new. */
    public char consumeTyped() {
        char c = lastTyped;
        lastTyped = 0;
        return c;
    }

    public void consumeEnter()          { enterPressed = false; confirmPressed = false; }
    public void consumeConfirm()        { confirmPressed = false; }
    public void consumeEsc()            { escPressed   = false; }
    public void consumeT()              { tPressed     = false; }
    public void consumeNum(int n)       { if (n >= 1 && n <= 3) numPressed[n] = false; }
}
