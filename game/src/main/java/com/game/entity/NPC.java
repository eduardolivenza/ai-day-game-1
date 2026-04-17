package com.game.entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NPC extends Entity {

    public final String name;
    public final Color shirtColor;

    /** Conversation history sent to the API. Each entry: {role, content}. */
    public final List<Map<String, String>> conversationHistory = new ArrayList<>();

    /** Whether this NPC has a "!" indicator above them (player nearby). */
    public boolean playerNearby = false;

    private int exclamationTick = 0;

    public NPC(String name, int spawnCol, int spawnRow, Color shirtColor) {
        this.name       = name;
        this.shirtColor = shirtColor;
        this.worldX       = spawnCol * TILE;
        this.worldY       = spawnRow * TILE;
        this.speed        = 0; // NPCs are stationary in base version
        generateSprites();
    }

    @Override
    public void update() {
        exclamationTick++;
    }

    @Override
    public void draw(Graphics2D g) {
        super.draw(g);
        if (playerNearby) drawExclamation(g);
    }

    private void drawExclamation(Graphics2D g) {
        // Bobbing "!" above the NPC
        int bob = (int)(Math.sin(exclamationTick * 0.12) * 3);
        int x = worldX + 18;
        int y = worldY - 20 + bob;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(x - 2, y - 2, 16, 22, 6, 6);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(Color.YELLOW);
        g.drawString("!", x + 4, y + 15);
    }

    /** Adds an assistant message to this NPC's conversation history. */
    public void recordAssistant(String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", "assistant");
        m.put("content", content);
        conversationHistory.add(m);
    }

    /** Adds a user message to this NPC's conversation history. */
    public void recordUser(String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", "user");
        m.put("content", content);
        conversationHistory.add(m);
    }

    // ── sprite generation ────────────────────────────────────────────────────

    private void generateSprites() {
        Color pantsColor = new Color(0x3a3a6a);
        Color skinColor  = new Color(0xFFDBAA);
        Color hairColor  = deriveHairColor(shirtColor);

        sprites = new BufferedImage[4][3];
        for (int dir = 0; dir < 4; dir++)
            for (int frame = 0; frame < 3; frame++)
                sprites[dir][frame] = buildSprite(shirtColor, pantsColor, skinColor, hairColor, dir, frame);
    }

    private Color deriveHairColor(Color shirt) {
        // Pick a contrasting hair color
        float[] hsb = Color.RGBtoHSB(shirt.getRed(), shirt.getGreen(), shirt.getBlue(), null);
        if (hsb[2] > 0.7f) return new Color(0x5c3d1e); // dark brown
        return new Color(0xd4c090);                      // light blonde
    }

    private static BufferedImage buildSprite(Color shirt, Color pants, Color skin,
                                              Color hair, int dir, int frame) {
        BufferedImage img = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        switch (dir) {
            case DIR_DOWN  -> drawFront(g, shirt, pants, skin, hair, frame);
            case DIR_UP    -> drawBack (g, shirt, pants, skin, hair, frame);
            case DIR_LEFT  -> drawSide (g, shirt, pants, skin, hair, frame, true);
            case DIR_RIGHT -> drawSide (g, shirt, pants, skin, hair, frame, false);
        }
        g.dispose();
        return img;
    }

    // ── shared draw helpers (also used by Player) ────────────────────────────

    public static void drawFront(Graphics2D g, Color shirt, Color pants,
                                  Color skin, Color hair, int frame) {
        // Hair
        g.setColor(hair);
        g.fillOval(14, 2, 20, 16);
        // Face
        g.setColor(skin);
        g.fillOval(15, 5, 18, 15);
        // Eyes
        g.setColor(new Color(0x222222));
        g.fillRect(19, 10, 3, 3);
        g.fillRect(26, 10, 3, 3);
        // Mouth
        g.setColor(new Color(0xaa5540));
        g.drawLine(21, 16, 27, 16);
        // Neck
        g.setColor(skin);
        g.fillRect(21, 19, 6, 3);
        // Shirt/body
        g.setColor(shirt);
        g.fillRoundRect(12, 21, 24, 13, 4, 4);
        // Arms (offset by walk frame)
        g.setColor(skin);
        int lAY = 22 + (frame == 1 ? -2 : frame == 2 ? 2 : 0);
        int rAY = 22 + (frame == 1 ? 2  : frame == 2 ? -2 : 0);
        g.fillRoundRect(5, lAY, 7, 10, 3, 3);
        g.fillRoundRect(36, rAY, 7, 10, 3, 3);
        // Pants
        g.setColor(pants);
        int lLY = 34 + (frame == 1 ? -2 : frame == 2 ? 2 : 0);
        int rLY = 34 + (frame == 1 ? 2  : frame == 2 ? -2 : 0);
        g.fillRoundRect(13, lLY, 9, 12, 3, 3);
        g.fillRoundRect(26, rLY, 9, 12, 3, 3);
        // Shoes
        g.setColor(new Color(0x4a2d0f));
        g.fillRoundRect(11, lLY + 10, 11, 5, 3, 3);
        g.fillRoundRect(24, rLY + 10, 11, 5, 3, 3);
    }

    public static void drawBack(Graphics2D g, Color shirt, Color pants,
                                 Color skin, Color hair, int frame) {
        g.setColor(hair);
        g.fillOval(14, 2, 20, 17);
        g.setColor(shirt);
        g.fillRoundRect(12, 21, 24, 13, 4, 4);
        g.setColor(skin);
        int lAY = 22 + (frame == 1 ? -2 : frame == 2 ? 2 : 0);
        int rAY = 22 + (frame == 1 ? 2  : frame == 2 ? -2 : 0);
        g.fillRoundRect(5,  lAY, 7, 10, 3, 3);
        g.fillRoundRect(36, rAY, 7, 10, 3, 3);
        g.setColor(pants);
        int lLY = 34 + (frame == 1 ? -2 : frame == 2 ? 2 : 0);
        int rLY = 34 + (frame == 1 ? 2  : frame == 2 ? -2 : 0);
        g.fillRoundRect(13, lLY, 9, 12, 3, 3);
        g.fillRoundRect(26, rLY, 9, 12, 3, 3);
        g.setColor(new Color(0x4a2d0f));
        g.fillRoundRect(11, lLY + 10, 11, 5, 3, 3);
        g.fillRoundRect(24, rLY + 10, 11, 5, 3, 3);
    }

    public static void drawSide(Graphics2D g, Color shirt, Color pants,
                                 Color skin, Color hair, int frame, boolean facingLeft) {
        // Hair
        g.setColor(hair);
        g.fillOval(14, 2, 20, 16);
        // Head
        g.setColor(skin);
        g.fillOval(facingLeft ? 16 : 12, 5, 16, 14);
        // Eye
        g.setColor(new Color(0x222222));
        g.fillRect(facingLeft ? 18 : 26, 10, 2, 2);
        // Neck
        g.setColor(skin);
        g.fillRect(21, 19, 6, 3);
        // Shirt
        g.setColor(shirt);
        g.fillRoundRect(14, 21, 20, 13, 4, 4);
        // Front arm
        g.setColor(skin);
        int armY = 22 + (frame == 1 ? -3 : frame == 2 ? 3 : 0);
        if (facingLeft) g.fillRoundRect(6,  armY, 7, 10, 3, 3);
        else            g.fillRoundRect(35, armY, 7, 10, 3, 3);
        // Pants
        g.setColor(pants);
        int lLY = 34 + (frame == 1 ? -2 : frame == 2 ? 2 : 0);
        int rLY = 34 + (frame == 1 ? 2  : frame == 2 ? -2 : 0);
        g.fillRoundRect(14, lLY, 9, 12, 3, 3);
        g.fillRoundRect(25, rLY, 9, 12, 3, 3);
        // Shoes
        g.setColor(new Color(0x4a2d0f));
        int shoeX = facingLeft ? 10 : 14;
        g.fillRoundRect(shoeX, lLY + 10, 14, 5, 3, 3);
        g.fillRoundRect(shoeX + 2, rLY + 10, 12, 5, 3, 3);
    }
}
