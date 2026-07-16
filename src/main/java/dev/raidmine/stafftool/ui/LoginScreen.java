package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.util.AuthManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class LoginScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget passwordField;
    private String status = "";
    private boolean error;
    private long openedAt;

    public LoginScreen(Screen parent) {
        super(Text.literal("RM Tools — вход"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        passwordField = new TextFieldWidget(textRenderer, 0, 0, 10, 46, Text.literal("Пароль"));
        passwordField.setMaxLength(32);
        passwordField.setDrawsBackground(false);
        passwordField.setEditableColor(UiTheme.TEXT);
        passwordField.setUneditableColor(UiTheme.FAINT);
        passwordField.setFocused(true);
        openedAt = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height,
                UiTheme.surface(UiTheme.argb(249, 2, 3, 5)),
                UiTheme.surface(UiTheme.argb(251, 8, 9, 13)));

        Layout l = layout();
        float appear = UiTheme.easeOutBack(Math.min(1F, (System.currentTimeMillis() - openedAt) / 280F));
        int animatedY = l.panelY() + Math.round((1F - appear) * 16F);
        updatePasswordBounds(l, animatedY);

        UiTheme.shadow(context, l.panelX(), animatedY, l.panelW(), l.panelH(), 24);
        UiTheme.roundedRect(context, l.panelX(), animatedY, l.panelW(), l.panelH(), 24, UiTheme.BG);
        UiTheme.roundedRect(context, l.panelX() + 1, animatedY + 1, l.panelW() - 2, l.panelH() - 2,
                23, UiTheme.argb(248, 10, 11, 15));
        context.fillGradient(l.panelX() + 18, animatedY + 1,
                l.panelX() + l.panelW() - 18, animatedY + 4,
                UiTheme.accent(), UiTheme.accent2());

        renderHeader(context, l, animatedY);
        renderAccount(context, l, animatedY);
        renderPassword(context, l, animatedY, mouseX, mouseY);
        renderLoginButton(context, l, animatedY, mouseX, mouseY);
        renderStatus(context, l, animatedY);
    }

    private void renderHeader(DrawContext context, Layout l, int panelY) {
        int logoW = 150;
        int logoH = 125;
        int logoX = l.panelX() + (l.panelW() - logoW) / 2;
        int logoY = panelY + 22;
        UiTheme.logo(context, logoX, logoY, logoW, logoH, 255);

        String title = "RM Tools";
        int titleX = l.panelX() + (l.panelW() - UiTheme.textWidth(title, 18F, true)) / 2;
        UiTheme.text(context, textRenderer, title, titleX, panelY + 153, 18F, UiTheme.TEXT, true);

        String subtitle = "Вход для персонала RaidMine";
        int subtitleX = l.panelX() + (l.panelW() - UiTheme.textWidth(subtitle, 10F, false)) / 2;
        UiTheme.text(context, textRenderer, subtitle, subtitleX, panelY + 178,
                10F, UiTheme.MUTED, false);
    }

    private void renderAccount(DrawContext context, Layout l, int panelY) {
        String username = AuthManager.currentSessionName(MinecraftClient.getInstance());
        boolean allowed = AuthManager.isAllowedUsername(username);
        Rect card = l.accountRect(panelY);

        UiTheme.roundedRect(context, card.x(), card.y(), card.w(), card.h(), 13, UiTheme.CARD);
        UiTheme.roundedRect(context, card.x() + 1, card.y() + 1, card.w() - 2, card.h() - 2,
                12, UiTheme.argb(244, 22, 24, 31));
        UiTheme.icon(context, UiIcon.USER, card.x() + 15, card.y() + 15, 18,
                allowed ? UiTheme.accent() : UiTheme.DANGER);
        UiTheme.text(context, textRenderer, "Логин", card.x() + 45, card.y() + 9,
                9F, UiTheme.FAINT, false);
        UiTheme.text(context, textRenderer,
                username.isBlank() ? "Ник не найден" : username,
                card.x() + 45, card.y() + 25,
                12F, allowed ? UiTheme.TEXT : UiTheme.DANGER, true);
        UiTheme.text(context, textRenderer, "определён автоматически",
                card.x() + card.w() - 142, card.y() + 20,
                9F, UiTheme.FAINT, false);
    }

    private void renderPassword(DrawContext context, Layout l, int panelY, int mouseX, int mouseY) {
        Rect field = l.passwordRect(panelY);
        boolean focused = passwordField.isFocused();
        boolean hovered = field.contains(mouseX, mouseY);

        UiTheme.text(context, textRenderer, "Пароль", field.x(), field.y() - 19,
                10F, UiTheme.MUTED, true);

        if (focused || hovered) {
            UiTheme.glow(context, field.x(), field.y(), field.w(), field.h(), 13, UiTheme.accent());
        }
        UiTheme.roundedRect(context, field.x(), field.y(), field.w(), field.h(), 13,
                focused ? UiTheme.withAlpha(UiTheme.accent(), 110) : UiTheme.BORDER);
        UiTheme.roundedRect(context, field.x() + 1, field.y() + 1, field.w() - 2, field.h() - 2,
                12, focused ? UiTheme.argb(255, 27, 25, 23) : UiTheme.argb(255, 20, 22, 28));

        UiTheme.icon(context, UiIcon.SHIELD, field.x() + 15, field.y() + 14, 18,
                focused ? UiTheme.accent() : UiTheme.FAINT);
        renderPasswordValue(context, field);
    }

    private void renderPasswordValue(DrawContext context, Rect field) {
        String raw = passwordField.getText();
        String masked = "*".repeat(Math.max(0, raw.length()));
        int textX = field.x() + 47;
        int textY = field.y() + 17;

        if (raw.isBlank()) {
            UiTheme.text(context, textRenderer, "Введите пароль", textX, textY,
                    11F, UiTheme.FAINT, false);
        } else {
            UiTheme.text(context, textRenderer, masked, textX, textY - 1,
                    13F, UiTheme.TEXT, true);
        }

        if (passwordField.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = textX + UiTheme.textWidth(masked, 13F, true) + 2;
            context.fill(caretX, textY - 3, caretX + 1, textY + 14, UiTheme.accent());
        }
    }

    private void renderLoginButton(DrawContext context, Layout l, int panelY, int mouseX, int mouseY) {
        String username = AuthManager.currentSessionName(MinecraftClient.getInstance());
        boolean allowed = AuthManager.isAllowedUsername(username);
        Rect button = l.loginRect(panelY);
        boolean hovered = allowed && button.contains(mouseX, mouseY);

        if (allowed) {
            UiTheme.glow(context, button.x(), button.y(), button.w(), button.h(), 13, UiTheme.accent());
        }
        int buttonColor = !allowed
                ? UiTheme.argb(170, 46, 49, 58)
                : hovered
                ? UiTheme.blend(UiTheme.accent(), UiTheme.accent2(), 0.45F)
                : UiTheme.accent();
        UiTheme.roundedRect(context, button.x(), button.y(), button.w(), button.h(), 13, buttonColor);

        String label = allowed ? "ВОЙТИ" : "ДОСТУП ЗАКРЫТ";
        int labelX = button.x() + (button.w() - UiTheme.textWidth(label, 11F, true)) / 2;
        UiTheme.text(context, textRenderer, label, labelX, button.y() + 15,
                11F, allowed ? UiTheme.TEXT : UiTheme.FAINT, true);
    }

    private void renderStatus(DrawContext context, Layout l, int panelY) {
        String username = AuthManager.currentSessionName(MinecraftClient.getInstance());
        if (!AuthManager.isAllowedUsername(username)) {
            status = "Этот ник не добавлен в список персонала";
            error = true;
        }
        if (status == null || status.isBlank()) return;

        int color = error ? UiTheme.DANGER : UiTheme.SUCCESS;
        int textX = l.panelX() + (l.panelW() - UiTheme.textWidth(status, 9F, false)) / 2;
        UiTheme.text(context, textRenderer, status, textX, panelY + l.panelH() - 27,
                9F, color, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Layout l = layout();
        updatePasswordBounds(l, l.panelY());
        if (passwordField.mouseClicked(click, doubled)) {
            return true;
        }

        Rect button = l.loginRect(l.panelY());
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && button.contains(click.x(), click.y())) {
            attemptLogin();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            attemptLogin();
            return true;
        }
        return (passwordField != null && passwordField.keyPressed(input)) || super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return (passwordField != null && passwordField.charTyped(input)) || super.charTyped(input);
    }

    private void attemptLogin() {
        String username = AuthManager.currentSessionName(MinecraftClient.getInstance());
        if (!AuthManager.isAllowedUsername(username)) {
            status = "Доступ запрещён для аккаунта " + (username.isBlank() ? "—" : username);
            error = true;
            return;
        }
        if (passwordField.getText().isBlank()) {
            status = "Введите пароль в поле выше";
            error = true;
            passwordField.setFocused(true);
            return;
        }
        if (AuthManager.login(passwordField.getText())) {
            error = false;
            status = "Вход выполнен";
            MinecraftClient.getInstance().setScreen(new WelcomeScreen(parent, username));
        } else {
            error = true;
            status = "Неверный пароль";
            passwordField.setText("");
            passwordField.setFocused(true);
        }
    }

    private void updatePasswordBounds(Layout l, int panelY) {
        Rect field = l.passwordRect(panelY);
        passwordField.setDimensionsAndPosition(field.w(), field.h(), field.x(), field.y());
    }

    private Layout layout() {
        int panelW = Math.min(500, width - 32);
        int panelH = Math.min(440, height - 20);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        return new Layout(panelX, panelY, panelW, panelH);
    }

    @Override
    public void close() {
        if (AuthManager.canUseMod()) {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Layout(int panelX, int panelY, int panelW, int panelH) {
        Rect accountRect(int animatedPanelY) {
            return new Rect(panelX + 36, animatedPanelY + 207, panelW - 72, 52);
        }

        Rect passwordRect(int animatedPanelY) {
            return new Rect(panelX + 36, animatedPanelY + 292, panelW - 72, 48);
        }

        Rect loginRect(int animatedPanelY) {
            return new Rect(panelX + 36, animatedPanelY + 356, panelW - 72, 46);
        }
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
