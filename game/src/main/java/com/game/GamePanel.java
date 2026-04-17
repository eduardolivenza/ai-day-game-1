package com.game;

import com.game.dialog.DialogBox;
import com.game.entity.NPC;
import com.game.entity.Player;
import com.game.input.KeyHandler;
import com.game.map.MapManager;
import com.game.map.VaultManager;
import com.game.tile.TileManager;
import com.game.util.Config;
import com.game.util.SaveManager;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {

    // ── screen constants ──────────────────────────────────────────────────
    public static final int TILE_SIZE     = 48;
    public static final int SCREEN_COLS   = 16;
    public static final int SCREEN_ROWS   = 12;
    public static final int SCREEN_WIDTH  = TILE_SIZE * SCREEN_COLS;  // 768
    public static final int SCREEN_HEIGHT = TILE_SIZE * SCREEN_ROWS;  // 576

    // ── camera ─────────────────────────────────────────────────────────
    public int cameraX = 0;
    public int cameraY = 0;

    // ── game state ──────────────────────────────────────────────────────
    public GameState gameState = GameState.PLAYING;

    // ── components ──────────────────────────────────────────────────────
    public final KeyHandler   keyHandler;
    public final TileManager  tileManager;
    public final MapManager   mapManager;
    public final VaultManager vaultManager;
    public final Player       player;
    public final DialogBox    dialogBox;
    public final SaveManager  saveManager;

    private Thread gameThread;

    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        keyHandler  = new KeyHandler();
        tileManager = new TileManager();

        addKeyListener(keyHandler);
        setFocusable(true);

        Config config = new Config();
        mapManager   = new MapManager(this, config);
        vaultManager = new VaultManager();
        player       = new Player(this);
        dialogBox    = new DialogBox(this, config);
        saveManager  = new SaveManager(this);

        saveManager.loadGame();

        // Save on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(saveManager::saveGame));
    }

    public void startGameLoop() {
        gameThread = new Thread(this);
        gameThread.setDaemon(true);
        gameThread.start();
    }

    // ── game loop (fixed timestep, ~60 FPS) ──────────────────────────────

    @Override
    public void run() {
        double interval = 1_000_000_000.0 / 60;
        double delta    = 0;
        long   last     = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / interval;
            last   = now;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }

            try { Thread.sleep(1); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ── update ─────────────────────────────────────────────────────────

    private void update() {
        switch (gameState) {
            case PLAYING -> {
                player.update();
                for (NPC npc : mapManager.getCurrentMap().getNpcs())
                    npc.update();
                updateCamera();
            }
            case DIALOG      -> dialogBox.update();
            case TRANSITION  -> mapManager.updateTransition();
            case VAULT_FOUND -> {
                if (keyHandler.enterPressed) {
                    keyHandler.consumeEnter();
                    gameState = GameState.PLAYING;
                }
            }
        }
    }

    public void openVault() {
        for (NPC npc : mapManager.getNpcRegistry().values())
            npc.conversationHistory.clear();
        vaultManager.randomize();
        gameState = GameState.VAULT_FOUND;
    }

    // ── rendering ──────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,   RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // World-space rendering (shifted by camera)
        g2.translate(-cameraX, -cameraY);
        mapManager.draw(g2);
        if ("overworld".equals(mapManager.getCurrentMap().getId()))
            drawVaultChest(g2);
        // Draw NPCs then player (player on top)
        for (NPC npc : mapManager.getCurrentMap().getNpcs())
            npc.draw(g2);
        player.draw(g2);
        g2.translate(cameraX, cameraY);

        // Screen-space UI
        drawHUD(g2);
        if (gameState == GameState.DIALOG)
            dialogBox.draw(g2);
        if (gameState == GameState.TRANSITION)
            mapManager.drawTransition(g2);
        if (gameState == GameState.VAULT_FOUND)
            drawVaultFoundOverlay(g2);
    }

    private void drawHUD(Graphics2D g) {
        // Map name badge
        String mapName = switch (mapManager.getCurrentMap().getId()) {
            case "house1"    -> "Brom's Inn";
            case "house2"    -> "Aelara's Study";
            case "house3"    -> "Empty house";
            default          -> "The Village of Elmswick";
        };
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(mapName);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(SCREEN_WIDTH / 2 - tw / 2 - 8, 8, tw + 16, 22, 8, 8);
        g.setColor(new Color(0xd4af37));
        g.drawString(mapName, SCREEN_WIDTH / 2 - tw / 2, 24);

        // Bottom interaction hint (NPC takes priority over vault)
        String hint = null;
        for (NPC npc : mapManager.getCurrentMap().getNpcs()) {
            if (npc.playerNearby && gameState == GameState.PLAYING) {
                hint = "Press ENTER to talk to " + npc.name;
                break;
            }
        }
        if (hint == null && vaultManager.playerNearby && gameState == GameState.PLAYING)
            hint = "Press ENTER to open the ancient vault";
        if (hint != null) {
            int hw = g.getFontMetrics().stringWidth(hint);
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRoundRect(SCREEN_WIDTH / 2 - hw / 2 - 8, SCREEN_HEIGHT - 36, hw + 16, 22, 8, 8);
            g.setColor(Color.WHITE);
            g.drawString(hint, SCREEN_WIDTH / 2 - hw / 2, SCREEN_HEIGHT - 20);
        }
    }

    // ── vault rendering ────────────────────────────────────────────────

    private void drawVaultChest(Graphics2D g) {
        int T  = TileManager.T;
        var loc = vaultManager.getCurrent();
        int x  = loc.col() * T + 8;
        int y  = loc.row() * T + 12;

        // Glow when player is nearby
        if (vaultManager.playerNearby) {
            g.setColor(new Color(255, 215, 0, 55));
            g.fillRoundRect(x - 4, y - 4, 40, 44, 10, 10);
        }
        // Chest body
        g.setColor(new Color(0x7B5E3A));
        g.fillRoundRect(x, y + 10, 32, 22, 4, 4);
        // Chest lid
        g.setColor(new Color(0x9C7A50));
        g.fillRoundRect(x, y, 32, 14, 4, 4);
        // Dark outline
        g.setColor(new Color(0x3A2510));
        g.drawRoundRect(x, y + 10, 32, 22, 4, 4);
        g.drawRoundRect(x, y, 32, 14, 4, 4);
        // Metal bands
        g.drawLine(x + 1,  y + 24, x + 31, y + 24);
        g.drawLine(x + 15, y,      x + 15, y + 32);
        // Golden lock
        g.setColor(new Color(0xD4AF37));
        g.fillOval(x + 12, y + 12, 8, 6);
        g.setColor(new Color(0x8A6A10));
        g.drawOval(x + 12, y + 12, 8, 6);
    }

    private void drawVaultFoundOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        int bx = 80, by = 165, bw = SCREEN_WIDTH - 160, bh = 230;
        g.setColor(new Color(20, 14, 2, 235));
        g.fillRoundRect(bx, by, bw, bh, 16, 16);
        g.setColor(new Color(0xD4AF37));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(bx, by, bw, bh, 16, 16);
        g.setStroke(new BasicStroke(1));

        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        String title = "The Ancient Vault Found!";
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0xD4AF37));
        g.drawString(title, SCREEN_WIDTH / 2 - fm.stringWidth(title) / 2, by + 44);
        g.setColor(new Color(0x8A6A10));
        g.drawLine(bx + 14, by + 54, bx + bw - 14, by + 54);

        // Body
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        String[] lines = {
            "The vault opens to reveal ancient secrets within...",
            "But as quickly as it opened, it seals once more.",
            "",
            "A new vault has appeared somewhere in the village.",
            "Speak with the villagers — they may know where to look."
        };
        int lineY = by + 80;
        for (String line : lines) {
            g.drawString(line, bx + 20, lineY);
            lineY += 24;
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.GRAY);
        g.drawString("[ ENTER ] Continue", SCREEN_WIDTH / 2 - 60, by + bh - 14);
    }

    // ── camera ─────────────────────────────────────────────────────────

    private void updateCamera() {
        int mapW = mapManager.getCurrentMap().getWidth()  * TILE_SIZE;
        int mapH = mapManager.getCurrentMap().getHeight() * TILE_SIZE;
        int tx   = player.worldX - SCREEN_WIDTH  / 2 + TILE_SIZE / 2;
        int ty   = player.worldY - SCREEN_HEIGHT / 2 + TILE_SIZE / 2;
        cameraX  = Math.max(0, Math.min(tx, mapW - SCREEN_WIDTH));
        cameraY  = Math.max(0, Math.min(ty, mapH - SCREEN_HEIGHT));
    }
}
