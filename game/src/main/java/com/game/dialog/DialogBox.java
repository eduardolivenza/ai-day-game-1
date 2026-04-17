package com.game.dialog;

import com.game.GamePanel;
import com.game.GameState;
import com.game.entity.NPC;
import com.game.util.Config;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Renders and manages NPC dialog.
 *
 * State machine:
 *   LOADING_INITIAL  – waiting for first server response     (shows "...")
 *   SHOWING_CHOICES  – NPC message + [1][2][3] choices + [T] type
 *   TYPING           – player composing a custom message
 *   LOADING_RESPONSE – waiting for server response           (shows "...")
 *   SHOWING_FINAL    – final NPC reply, press ENTER to close
 */
public class DialogBox {

    private enum State { IDLE, LOADING_INITIAL, SHOWING_CHOICES, TYPING, LOADING_RESPONSE, SHOWING_FINAL }

    // Whether the pending load should produce new choices (true) or end the conversation (false)
    private boolean loadingForChoices = false;

    private final GamePanel    gp;
    private final GameApiClient api;

    private State       state     = State.IDLE;
    private NPC         activeNpc;
    private String      npcMessage  = "";
    private List<String> choices    = List.of();

    // Text input
    private final StringBuilder typedText = new StringBuilder();
    private int cursorTick = 0;

    // Loading animation
    private int dotTick  = 0;
    private int dotCount = 1;

    // Thread-safe result slot
    private final AtomicReference<GameApiClient.ConversationResult> pending =
            new AtomicReference<>();

    // ── geometry ──────────────────────────────────────────────────────────
    private static final int BOX_X      = 20;
    private static final int BOX_HEIGHT = 190;
    private static final int BOX_W      = GamePanel.SCREEN_WIDTH - 40;
    private static final int BOX_Y      = GamePanel.SCREEN_HEIGHT - BOX_HEIGHT - 10;

    private static final Font NAME_FONT   = new Font("SansSerif", Font.BOLD, 15);
    private static final Font MSG_FONT    = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font CHOICE_FONT = new Font("SansSerif", Font.PLAIN, 12);

    public DialogBox(GamePanel gp, Config config) {
        this.gp  = gp;
        this.api = new GameApiClient(config);
    }

    // ── public API ─────────────────────────────────────────────────────────

    public void startWith(NPC npc) {
        activeNpc  = npc;
        npcMessage = "";
        choices    = List.of();
        typedText.setLength(0);
        pending.set(null);
        state = State.LOADING_INITIAL;
        loadingForChoices = true;
        gp.keyHandler.consumeT();  // clear stale T presses

        CompletableFuture.runAsync(() -> {
            var r = api.startConversation(npc.name, npc.getSystemPrompt(),
                                          npc.conversationHistory);
            pending.set(r);
        });
    }

    // ── update ─────────────────────────────────────────────────────────────

    public void update() {
        dotTick++;
        if (dotTick % 20 == 0) dotCount = (dotCount % 3) + 1;
        cursorTick++;

        switch (state) {
            case LOADING_INITIAL, LOADING_RESPONSE -> pollResult();
            case SHOWING_CHOICES -> handleChoices();
            case TYPING          -> handleTyping();
            case SHOWING_FINAL   -> {
                if (gp.keyHandler.enterPressed) {
                    gp.keyHandler.consumeEnter();
                    close();
                }
            }
        }
    }

    // ── draw ───────────────────────────────────────────────────────────────

    public void draw(Graphics2D g) {
        if (state == State.IDLE) return;

        // Background
        g.setColor(new Color(0, 0, 0, 210));
        g.fillRoundRect(BOX_X, BOX_Y, BOX_W, BOX_HEIGHT, 12, 12);
        g.setColor(new Color(0xd4af37));
        g.drawRoundRect(BOX_X, BOX_Y, BOX_W, BOX_HEIGHT, 12, 12);

        // NPC name
        if (activeNpc != null) {
            g.setColor(activeNpc.shirtColor.brighter());
            g.setFont(NAME_FONT);
            g.drawString(activeNpc.name, BOX_X + 14, BOX_Y + 20);
        }
        g.setColor(new Color(0xd4af37));
        g.drawLine(BOX_X + 10, BOX_Y + 26, BOX_X + BOX_W - 10, BOX_Y + 26);

        switch (state) {
            case LOADING_INITIAL, LOADING_RESPONSE -> drawLoading(g);
            case SHOWING_CHOICES -> drawChoices(g);
            case TYPING          -> drawTyping(g);
            case SHOWING_FINAL   -> drawFinal(g);
        }
    }

    // ── state handlers ─────────────────────────────────────────────────────

    private void pollResult() {
        var r = pending.getAndSet(null);
        if (r == null) return;

        npcMessage = r.npcMessage();
        choices    = r.choices();
        activeNpc.recordAssistant(npcMessage);
        state = (loadingForChoices && !choices.isEmpty()) ? State.SHOWING_CHOICES : State.SHOWING_FINAL;
    }

    private void handleChoices() {
        // Numbered choice → end conversation
        for (int i = 1; i <= 3; i++) {
            if (gp.keyHandler.numPressed[i]) {
                gp.keyHandler.consumeNum(i);
                int idx = i - 1;
                if (idx < choices.size()) {
                    sendReply(choices.get(idx), false); // includeChoices=false → SHOWING_FINAL
                }
                return;
            }
        }
        // T → open typing input
        if (gp.keyHandler.tPressed) {
            gp.keyHandler.consumeT();
            gp.keyHandler.consumeTyped(); // discard the 't' that triggered this
            typedText.setLength(0);
            cursorTick = 0;
            state = State.TYPING;
        }
    }

    private void handleTyping() {
        // Consume typed characters
        char c = gp.keyHandler.consumeTyped();
        if (c != 0) {
            if (c == '\b') {
                if (typedText.length() > 0)
                    typedText.deleteCharAt(typedText.length() - 1);
            } else if (c >= 32 && c < 127 && typedText.length() < 100) {
                typedText.append(c);
            }
        }

        // ESC → back to choices
        if (gp.keyHandler.escPressed) {
            gp.keyHandler.consumeEsc();
            state = State.SHOWING_CHOICES;
            return;
        }

        // ENTER → submit (only if something typed; SPACE must not trigger this)
        if (gp.keyHandler.confirmPressed && typedText.length() > 0) {
            gp.keyHandler.consumeConfirm();
            sendReply(typedText.toString().trim(), true); // includeChoices=true → loop back
        }
    }

    private void sendReply(String playerMessage, boolean includeChoices) {
        activeNpc.recordUser(playerMessage);
        loadingForChoices = includeChoices;
        state = State.LOADING_RESPONSE;
        pending.set(null);

        CompletableFuture.runAsync(() -> {
            var r = api.continueConversation(
                    activeNpc.getSystemPrompt(),
                    activeNpc.conversationHistory,
                    playerMessage,
                    includeChoices);
            pending.set(r);
        });
    }

    // ── draw helpers ──────────────────────────────────────────────────────

    private void drawLoading(Graphics2D g) {
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(MSG_FONT);
        g.drawString("...".substring(0, dotCount), BOX_X + 14, BOX_Y + 55);
    }

    private void drawChoices(Graphics2D g) {
        g.setFont(MSG_FONT);
        g.setColor(Color.WHITE);
        drawWrapped(g, npcMessage, BOX_X + 14, BOX_Y + 50, BOX_W - 28, 66);

        g.setFont(CHOICE_FONT);
        int baseY = BOX_Y + 124;
        for (int i = 0; i < choices.size() && i < 3; i++) {
            g.setColor(new Color(0xffdd80));
            g.drawString("[" + (i + 1) + "] " + choices.get(i), BOX_X + 14, baseY + i * 17);
        }
        // Type option
        g.setColor(new Color(0x80ddff));
        g.drawString("[T] Type your own message...", BOX_X + 14, baseY + 51);
    }

    private void drawTyping(Graphics2D g) {
        g.setFont(MSG_FONT);
        g.setColor(Color.WHITE);
        drawWrapped(g, npcMessage, BOX_X + 14, BOX_Y + 50, BOX_W - 28, 50);

        // Input box
        int inputY = BOX_Y + 110;
        g.setColor(new Color(20, 20, 40, 220));
        g.fillRoundRect(BOX_X + 10, inputY, BOX_W - 20, 36, 6, 6);
        g.setColor(new Color(0x80ddff));
        g.drawRoundRect(BOX_X + 10, inputY, BOX_W - 20, 36, 6, 6);

        String text = typedText.toString();
        boolean showCursor = (cursorTick / 30) % 2 == 0;
        g.setFont(MSG_FONT);
        g.setColor(Color.WHITE);
        g.drawString(text + (showCursor ? "|" : " "), BOX_X + 18, inputY + 23);

        g.setFont(CHOICE_FONT);
        g.setColor(Color.GRAY);
        g.drawString("ENTER to send  •  ESC to cancel", BOX_X + 14, BOX_Y + BOX_HEIGHT - 10);
    }

    private void drawFinal(Graphics2D g) {
        g.setFont(MSG_FONT);
        g.setColor(Color.WHITE);
        drawWrapped(g, npcMessage, BOX_X + 14, BOX_Y + 50, BOX_W - 28, 110);
        g.setFont(CHOICE_FONT);
        g.setColor(Color.GRAY);
        g.drawString("[ ENTER ] Close", BOX_X + BOX_W - 115, BOX_Y + BOX_HEIGHT - 10);
    }

    // ── utilities ──────────────────────────────────────────────────────────

    private void close() {
        state     = State.IDLE;
        activeNpc = null;
        gp.gameState = GameState.PLAYING;
    }

    /** Word-wraps text within maxWidth pixels, up to maxHeight pixels tall. */
    private static void drawWrapped(Graphics2D g, String text,
                                    int x, int y, int maxWidth, int maxHeight) {
        FontMetrics fm  = g.getFontMetrics();
        int lineH       = fm.getHeight();
        String[] words  = text.split(" ");
        StringBuilder line = new StringBuilder();
        int curY = y;

        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
                if (curY - y + lineH > maxHeight) break;
                g.drawString(line.toString(), x, curY);
                curY += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty() && curY - y + lineH <= maxHeight)
            g.drawString(line.toString(), x, curY);
    }
}
