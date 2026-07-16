package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.config.ForbiddenWordsStore;
import dev.raidmine.stafftool.config.ModConfig;
import dev.raidmine.stafftool.util.FolderOpener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class SettingsScreen extends Screen {
    private static final int[] PRESETS = {
            0xFFFF8A00, 0xFFFF4D3A,
            0xFFFFB020, 0xFFFF6B00,
            0xFFFF5F7A, 0xFFFF2E63,
            0xFFB36BFF, 0xFF6D5DFF,
            0xFF45B7FF, 0xFF3978FF,
            0xFF36D1A5, 0xFF0EA87A,
            0xFFFFD24A, 0xFFFF8A00,
            0xFFF5F7FA, 0xFF9AA4B2
    };
    private static final String[] FONT_VALUES = {
            "AUTO", "ETUDE_NOIRE", "SEENONIM", "MINECRAFT"
    };
    private static final String[] FONT_LABELS = {
            "Авто — Hemico", "Etude Noire", "Seenonim", "Minecraft Pixel"
    };

    private final Screen parent;
    private Tab tab = Tab.APPEARANCE;
    private TextFieldWidget wordField;
    private TextFieldWidget accentField;
    private TextFieldWidget reasonField;
    private int wordOffset;
    private String status;
    private long statusAt;
    private DragTarget dragTarget = DragTarget.NONE;

    public SettingsScreen(Screen parent) {
        super(Text.literal("RM Tools — настройки"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig config = RaidMineStaffMod.config();
        wordField = field("Добавить слово", 96, "");
        accentField = field("Цвет интерфейса", 7, String.format(Locale.ROOT, "#%06X", config.accentColor & 0xFFFFFF));
        reasonField = field("Формат причины", 64, config.punishmentReasonTemplate);
    }

    private TextFieldWidget field(String label, int maxLength, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, 10, 28, Text.literal(label));
        field.setMaxLength(maxLength);
        field.setText(value);
        field.setDrawsBackground(false);
        field.setEditableColor(UiTheme.TEXT);
        field.setUneditableColor(UiTheme.FAINT);
        return field;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height,
                UiTheme.surface(UiTheme.argb(238, 3, 4, 7)),
                UiTheme.surface(UiTheme.argb(248, 11, 12, 16)));
        Layout l = layout();
        UiTheme.shadow(context, l.x(), l.y(), l.w(), l.h(), 20);
        int uiOutlineAlpha = Math.round(255F * RaidMineStaffMod.config().uiOutlineOpacity);
        if (uiOutlineAlpha > 0) {
            UiTheme.roundedBorder(context, l.x(), l.y(), l.w(), l.h(), 20, 2,
                    UiTheme.withAlpha(UiTheme.accent(), uiOutlineAlpha), UiTheme.BG);
        } else {
            UiTheme.roundedRect(context, l.x(), l.y(), l.w(), l.h(), 20, UiTheme.BG);
        }

        renderHeader(context, l, mouseX, mouseY);
        renderTabs(context, l, mouseX, mouseY);
        switch (tab) {
            case APPEARANCE -> renderAppearance(context, l, mouseX, mouseY);
            case MODERATION -> renderModeration(context, l, mouseX, mouseY);
            case WORDS -> renderWords(context, l, mouseX, mouseY);
        }
        renderStatus(context, l);
    }

    private void renderHeader(DrawContext context, Layout l, int mouseX, int mouseY) {
        UiTheme.logo(context, l.x() + 18, l.y() + 10, 48, 48, 255);
        UiTheme.text(context, textRenderer, "RM Tools", l.x() + 78, l.y() + 17, 16F, UiTheme.TEXT, true);
        UiTheme.text(context, textRenderer, "Настройки интерфейса, модерации и чата", l.x() + 78, l.y() + 39, 9.4F, UiTheme.MUTED, false);
        Rect close = closeRect(l);
        UiTheme.roundedRect(context, close.x(), close.y(), close.w(), close.h(), 10,
                close.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
        UiTheme.icon(context, UiIcon.CLOSE, close.x() + 8, close.y() + 8, 14, UiTheme.MUTED);
    }

    private void renderTabs(DrawContext context, Layout l, int mouseX, int mouseY) {
        int x = l.x() + 18;
        int y = l.y() + 70;
        int w = (l.w() - 44) / 3;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab candidate = Tab.values()[i];
            Rect rect = new Rect(x + i * (w + 4), y, w, 34);
            boolean selected = tab == candidate;
            UiTheme.roundedRect(context, rect.x(), rect.y(), rect.w(), rect.h(), 10,
                    selected ? UiTheme.withAlpha(UiTheme.accent(), 78) : rect.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
            if (selected) context.fill(rect.x() + 12, rect.y() + rect.h() - 2, rect.x() + rect.w() - 12, rect.y() + rect.h(), UiTheme.accent());
            UiTheme.icon(context, candidate.icon, rect.x() + 12, rect.y() + 10, 14, selected ? UiTheme.accent() : UiTheme.FAINT);
            UiTheme.text(context, textRenderer, candidate.label, rect.x() + 34, rect.y() + 11, 9.2F,
                    selected ? UiTheme.TEXT : UiTheme.MUTED, true);
        }
    }

    private void renderAppearance(DrawContext context, Layout l, int mouseX, int mouseY) {
        int contentX = l.x() + 20;
        int contentY = l.y() + 118;
        int contentW = l.w() - 40;
        int leftW = (contentW - 14) / 2;
        int rightX = contentX + leftW + 14;
        int cardH = l.h() - 146;
        card(context, contentX, contentY, leftW, cardH, "Цвет и прозрачность", UiIcon.PALETTE);
        card(context, rightX, contentY, leftW, cardH, "Шрифт интерфейса", UiIcon.FONT);

        UiTheme.text(context, textRenderer, "Готовые темы", contentX + 18, contentY + 46, 9.3F, UiTheme.MUTED, false);
        for (int i = 0; i < PRESETS.length / 2; i++) {
            Rect r = presetRect(contentX, contentY, i);
            int primary = PRESETS[i * 2];
            int secondary = PRESETS[i * 2 + 1];
            UiTheme.roundedRectExact(context, r.x(), r.y(), r.w(), r.h(), 10, primary);
            context.fillGradient(r.x() + r.w() / 2, r.y(), r.x() + r.w(), r.y() + r.h(), primary, secondary);
            if ((RaidMineStaffMod.config().accentColor & 0xFFFFFF) == (primary & 0xFFFFFF)) {
                UiTheme.icon(context, UiIcon.CHECK, r.x() + (r.w() - 14) / 2, r.y() + (r.h() - 14) / 2, 14, UiTheme.TEXT);
            }
        }

        int fieldY = contentY + 154;
        accentField.setX(contentX + 18);
        accentField.setY(fieldY);
        accentField.setWidth(leftW - 86);
        input(context, accentField, "HEX-цвет");
        Rect apply = new Rect(contentX + leftW - 58, fieldY, 40, 28);
        button(context, apply, "OK", apply.contains(mouseX, mouseY), true, UiIcon.CHECK);

        renderSlider(context, "Прозрачность фона", backgroundOpacityTrackRect(contentX, contentY, leftW),
                RaidMineStaffMod.config().uiBackgroundOpacity);
        renderSlider(context, "Прозрачность обводки меню", uiOutlineOpacityTrackRect(contentX, contentY, leftW),
                RaidMineStaffMod.config().uiOutlineOpacity);

        Rect outline = hudOutlineToggleRect(contentX, contentY, leftW);
        toggle(context, outline, "Обводка верхней панели",
                RaidMineStaffMod.config().hudOutlineEnabled, mouseX, mouseY, UiIcon.RESIZE);
        if (RaidMineStaffMod.config().hudOutlineEnabled) {
            renderSlider(context, "Прозрачность обводки панели", hudOutlineOpacityTrackRect(contentX, contentY, leftW),
                    RaidMineStaffMod.config().hudOutlineOpacity);
        } else {
            UiTheme.text(context, textRenderer, "Ползунок появится после включения обводки.",
                    contentX + 18, outline.y() + 42, 8.2F, UiTheme.FAINT, false);
        }

        String activeFont = RaidMineStaffMod.config().fontFamily.equalsIgnoreCase("MINECRAFT")
                ? "Minecraft Pixel" : SmoothAssets.fontName();
        UiTheme.text(context, textRenderer, "Текущий: " + activeFont, rightX + 18, contentY + 46, 9.2F, UiTheme.MUTED, false);
        for (int i = 0; i < FONT_VALUES.length; i++) {
            Rect r = fontRect(rightX, contentY, leftW, i);
            boolean selected = RaidMineStaffMod.config().fontFamily.equalsIgnoreCase(FONT_VALUES[i]);
            UiTheme.roundedRect(context, r.x(), r.y(), r.w(), r.h(), 10,
                    selected ? UiTheme.withAlpha(UiTheme.accent(), 74)
                            : r.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
            UiTheme.icon(context, UiIcon.FONT, r.x() + 10, r.y() + 9, 14,
                    selected ? UiTheme.accent() : UiTheme.FAINT);
            UiTheme.text(context, textRenderer, FONT_LABELS[i], r.x() + 34, r.y() + 10, 9.0F,
                    selected ? UiTheme.TEXT : UiTheme.MUTED, true);
        }
        int infoY = contentY + 66 + ((FONT_VALUES.length + 1) / 2) * 42 + 14;
        UiTheme.text(context, textRenderer, "AUTO использует Hemico. RaidMine Tools всегда остаётся в Hemico.",
                rightX + 18, infoY, 8.2F, UiTheme.FAINT, false);
        UiTheme.text(context, textRenderer, "Шрифты загружены как крупные сглаженные атласы без размытия.",
                rightX + 18, infoY + 15, 8.2F, UiTheme.FAINT, false);
    }

    private void renderSlider(DrawContext context, String label, Rect track, float value) {
        UiTheme.text(context, textRenderer, label, track.x(), track.y() - 24, 9.1F, UiTheme.MUTED, false);
        UiTheme.roundedRect(context, track.x(), track.y(), track.w(), track.h(), 4, UiTheme.argb(190, 48, 52, 62));
        int fillW = Math.round(track.w() * Math.max(0F, Math.min(1F, value)));
        if (fillW > 0) UiTheme.roundedRectExact(context, track.x(), track.y(), fillW, track.h(), 4, UiTheme.accent());
        int knobX = track.x() + fillW - 5;
        knobX = Math.max(track.x() - 1, Math.min(track.x() + track.w() - 9, knobX));
        UiTheme.roundedRectExact(context, knobX, track.y() - 4, 10, 16, 5, UiTheme.TEXT);
        String percent = Math.round(value * 100F) + "%";
        UiTheme.text(context, textRenderer, percent, track.x() + track.w() + 10, track.y() - 1, 9F, UiTheme.TEXT, true);
    }

    private void renderModeration(DrawContext context, Layout l, int mouseX, int mouseY) {
        int x = l.x() + 20;
        int y = l.y() + 118;
        int w = l.w() - 40;
        int half = (w - 14) / 2;
        int rightX = x + half + 14;
        int h = l.h() - 146;
        card(context, x, y, half, h, "Автоматизация", UiIcon.TIMER);
        card(context, rightX, y, half, h, "Правила и уведомления", UiIcon.SHIELD);

        int rowY = y + 46;
        toggle(context, new Rect(x + 18, rowY, half - 36, 34), "AFK Kick → /hub",
                RaidMineStaffMod.config().afkKickEnabled, mouseX, mouseY, UiIcon.TIMER);
        rowY += 42;
        toggle(context, new Rect(x + 18, rowY, half - 36, 34), "Автоматический скриншот",
                RaidMineStaffMod.config().autoScreenshot, mouseX, mouseY, UiIcon.SCREENSHOT);
        rowY += 42;
        toggle(context, new Rect(x + 18, rowY, half - 36, 34), "Панель подсказок",
                RaidMineStaffMod.config().showHintsPanel, mouseX, mouseY, UiIcon.BELL);

        int goalY = y + 188;
        UiTheme.text(context, textRenderer, "Дневная норма онлайна", x + 18, goalY, 9.2F, UiTheme.MUTED, false);
        Rect minus = new Rect(x + 18, goalY + 19, 34, 30);
        Rect plus = new Rect(x + half - 52, goalY + 19, 34, 30);
        button(context, minus, "−", minus.contains(mouseX, mouseY), false, UiIcon.CHEVRON_LEFT);
        button(context, plus, "+", plus.contains(mouseX, mouseY), false, UiIcon.PLUS);
        String goal = RaidMineStaffMod.config().dailyOnlineGoalMinutes + " мин";
        int goalW = UiTheme.textWidth(goal, 12F, true);
        UiTheme.text(context, textRenderer, goal, x + (half - goalW) / 2, goalY + 27, 12F, UiTheme.TEXT, true);
        UiTheme.text(context, textRenderer, "Сброс ежедневно в 00:00 по Москве", x + 18, goalY + 58, 8.4F, UiTheme.FAINT, false);

        rowY = y + 46;
        toggle(context, new Rect(rightX + 18, rowY, half - 36, 34), "Уведомления об упоминании",
                RaidMineStaffMod.config().mentionNotifications, mouseX, mouseY, UiIcon.BELL);
        rowY += 42;
        toggle(context, new Rect(rightX + 18, rowY, half - 36, 34), "Контроль запрещённых слов",
                RaidMineStaffMod.config().forbiddenWordAlerts, mouseX, mouseY, UiIcon.WARN);
        UiTheme.text(context, textRenderer, "Формат причины", rightX + 18, y + 140, 9.2F, UiTheme.MUTED, false);
        reasonField.setX(rightX + 18);
        reasonField.setY(y + 159);
        reasonField.setWidth(half - 36);
        input(context, reasonField, "{rule} — только пункт правил");
        UiTheme.text(context, textRenderer, "AFK Kick работает только в мультиплеере и отправляет в /hub", rightX + 18, y + 207, 8.2F, UiTheme.FAINT, false);
        UiTheme.text(context, textRenderer, "ровно через 4 минуты. После 1 минуты AFK онлайн не считается.", rightX + 18, y + 221, 8.2F, UiTheme.FAINT, false);
    }

    private void renderWords(DrawContext context, Layout l, int mouseX, int mouseY) {
        int x = l.x() + 20;
        int y = l.y() + 118;
        int w = l.w() - 40;
        int h = l.h() - 146;
        card(context, x, y, w, h, "Запрещённые слова", UiIcon.WARN);
        UiTheme.text(context, textRenderer,
                "Список хранится отдельно в config/rm_tools/forbidden_words.json — им можно делиться с другими модераторами.",
                x + 18, y + 43, 8.8F, UiTheme.MUTED, false);

        wordField.setX(x + 18);
        wordField.setY(y + 63);
        wordField.setWidth(Math.min(370, w - 300));
        input(context, wordField, "Введите слово или фразу");
        Rect add = new Rect(wordField.getX() + wordField.getWidth() + 10, wordField.getY(), 84, 28);
        button(context, add, "ДОБАВИТЬ", add.contains(mouseX, mouseY), true, UiIcon.PLUS);
        Rect open = new Rect(x + w - 188, wordField.getY(), 82, 28);
        button(context, open, "ПАПКА", open.contains(mouseX, mouseY), false, UiIcon.FOLDER);
        Rect reload = new Rect(x + w - 96, wordField.getY(), 78, 28);
        button(context, reload, "ОБНОВИТЬ", reload.contains(mouseX, mouseY), false, UiIcon.RELOAD);

        List<String> words = RaidMineStaffMod.config().forbiddenWords;
        int visibleRows = Math.max(1, (h - 112) / 32);
        wordOffset = Math.max(0, Math.min(Math.max(0, words.size() - visibleRows), wordOffset));
        int listY = y + 105;
        if (words.isEmpty()) {
            UiTheme.text(context, textRenderer, "Список пуст. Добавьте первое слово.", x + 18, listY + 8, 9F, UiTheme.FAINT, false);
            return;
        }
        for (int i = 0; i < visibleRows && wordOffset + i < words.size(); i++) {
            int index = wordOffset + i;
            Rect row = new Rect(x + 18, listY + i * 32, w - 36, 26);
            UiTheme.roundedRect(context, row.x(), row.y(), row.w(), row.h(), 9,
                    row.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
            UiTheme.roundedRect(context, row.x() + 9, row.y() + 9, 8, 8, 4, UiTheme.accent());
            UiTheme.text(context, textRenderer, UiTheme.ellipsize(textRenderer, words.get(index), row.w() - 70),
                    row.x() + 26, row.y() + 8, 9.2F, UiTheme.TEXT, true);
            Rect remove = removeRect(row);
            UiTheme.icon(context, UiIcon.TRASH, remove.x() + 5, remove.y() + 5, 13,
                    remove.contains(mouseX, mouseY) ? UiTheme.DANGER : UiTheme.FAINT);
        }
    }

    private void renderStatus(DrawContext context, Layout l) {
        if (status == null || System.currentTimeMillis() - statusAt > 2800L) return;
        int w = Math.min(l.w() - 50, UiTheme.textWidth(status, 9.4F, true) + 30);
        int x = l.x() + (l.w() - w) / 2;
        int y = l.y() + l.h() - 38;
        UiTheme.roundedRect(context, x, y, w, 26, 9, UiTheme.PANEL_2);
        UiTheme.text(context, textRenderer, UiTheme.ellipsize(textRenderer, status, w - 20), x + 10, y + 9,
                9.4F, UiTheme.TEXT, true);
    }

    private void card(DrawContext context, int x, int y, int w, int h, String title, UiIcon icon) {
        UiTheme.roundedRect(context, x, y, w, h, 15, UiTheme.PANEL);
        UiTheme.roundedRect(context, x + 1, y + 1, w - 2, h - 2, 14, UiTheme.argb(245, 18, 20, 26));
        UiTheme.icon(context, icon, x + 16, y + 15, 16, UiTheme.accent());
        UiTheme.text(context, textRenderer, title, x + 42, y + 16, 10.4F, UiTheme.TEXT, true);
    }

    private void input(DrawContext context, TextFieldWidget field, String placeholder) {
        UiTheme.roundedRect(context, field.getX(), field.getY(), field.getWidth(), 28, 9,
                field.isFocused() ? UiTheme.withAlpha(UiTheme.accent(), 105) : UiTheme.BORDER);
        UiTheme.roundedRect(context, field.getX() + 1, field.getY() + 1, field.getWidth() - 2, 26, 8, UiTheme.CARD);
        String value = field.getText();
        UiTheme.text(context, textRenderer, value.isBlank() ? placeholder : value,
                field.getX() + 10, field.getY() + 9, 9.2F, value.isBlank() ? UiTheme.FAINT : UiTheme.TEXT, false);
        if (field.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caret = field.getX() + 10 + UiTheme.textWidth(value, 9.2F, false);
            context.fill(caret, field.getY() + 6, caret + 1, field.getY() + 21, UiTheme.accent());
        }
    }

    private void toggle(DrawContext context, Rect rect, String label, boolean value, int mouseX, int mouseY, UiIcon icon) {
        UiTheme.roundedRect(context, rect.x(), rect.y(), rect.w(), rect.h(), 10,
                rect.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
        UiTheme.icon(context, icon, rect.x() + 10, rect.y() + 10, 14, value ? UiTheme.accent() : UiTheme.FAINT);
        UiTheme.text(context, textRenderer, label, rect.x() + 34, rect.y() + 12, 9F, UiTheme.TEXT, true);
        Rect sw = new Rect(rect.x() + rect.w() - 46, rect.y() + 9, 36, 18);
        UiTheme.roundedRect(context, sw.x(), sw.y(), sw.w(), sw.h(), 9,
                value ? UiTheme.accent() : UiTheme.argb(255, 55, 60, 70));
        int knobX = value ? sw.x() + 20 : sw.x() + 3;
        UiTheme.roundedRect(context, knobX, sw.y() + 3, 12, 12, 6, UiTheme.TEXT);
    }

    private void button(DrawContext context, Rect rect, String label, boolean hovered, boolean accent, UiIcon icon) {
        int bg = accent ? (hovered ? UiTheme.blend(UiTheme.accent(), UiTheme.accent2(), 0.45F) : UiTheme.accent())
                : hovered ? UiTheme.CARD_HOVER : UiTheme.CARD;
        UiTheme.roundedRect(context, rect.x(), rect.y(), rect.w(), rect.h(), 9, bg);
        if (icon != null && rect.w() >= 56) UiTheme.icon(context, icon, rect.x() + 8, rect.y() + 8, 12, accent ? UiTheme.TEXT : UiTheme.MUTED);
        int textX = icon != null && rect.w() >= 56 ? rect.x() + 25 : rect.x() + (rect.w() - UiTheme.textWidth(label, 8F, true)) / 2;
        UiTheme.text(context, textRenderer, label, textX, rect.y() + 10, 8F, UiTheme.TEXT, true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled);
        Layout l = layout();
        double mx = click.x();
        double my = click.y();
        if (closeRect(l).contains(mx, my)) { close(); return true; }
        for (int i = 0; i < Tab.values().length; i++) {
            Rect r = tabRect(l, i);
            if (r.contains(mx, my)) { tab = Tab.values()[i]; focusOnly(null); return true; }
        }

        if (tab == Tab.APPEARANCE) {
            int x = l.x() + 20;
            int y = l.y() + 118;
            int leftW = ((l.w() - 40) - 14) / 2;
            for (int i = 0; i < PRESETS.length / 2; i++) {
                if (presetRect(x, y, i).contains(mx, my)) {
                    RaidMineStaffMod.config().setAccent(PRESETS[i * 2], PRESETS[i * 2 + 1]);
                    accentField.setText(String.format(Locale.ROOT, "#%06X", PRESETS[i * 2] & 0xFFFFFF));
                    showStatus("Цвет интерфейса изменён");
                    return true;
                }
            }
            Rect apply = new Rect(x + leftW - 58, y + 154, 40, 28);
            if (apply.contains(mx, my)) { applyHex(); return true; }
            Rect backgroundTrack = backgroundOpacityTrackRect(x, y, leftW);
            if (backgroundTrack.contains(mx, my)) {
                dragTarget = DragTarget.BACKGROUND;
                setSliderFromMouse(mx, backgroundTrack, dragTarget);
                return true;
            }
            Rect uiOutlineTrack = uiOutlineOpacityTrackRect(x, y, leftW);
            if (uiOutlineTrack.contains(mx, my)) {
                dragTarget = DragTarget.UI_OUTLINE;
                setSliderFromMouse(mx, uiOutlineTrack, dragTarget);
                return true;
            }
            Rect outline = hudOutlineToggleRect(x, y, leftW);
            if (outline.contains(mx, my)) {
                RaidMineStaffMod.config().hudOutlineEnabled ^= true;
                save(RaidMineStaffMod.config().hudOutlineEnabled ? "Обводка панели включена" : "Обводка панели выключена");
                return true;
            }
            Rect hudOutlineTrack = hudOutlineOpacityTrackRect(x, y, leftW);
            if (RaidMineStaffMod.config().hudOutlineEnabled && hudOutlineTrack.contains(mx, my)) {
                dragTarget = DragTarget.HUD_OUTLINE;
                setSliderFromMouse(mx, hudOutlineTrack, dragTarget);
                return true;
            }
            int rightX = x + leftW + 14;
            for (int i = 0; i < FONT_VALUES.length; i++) {
                if (fontRect(rightX, y, leftW, i).contains(mx, my)) {
                    RaidMineStaffMod.config().fontFamily = FONT_VALUES[i];
                    RaidMineStaffMod.config().save();
                    SmoothAssets.reloadFontAtlas();
                    showStatus("Шрифт изменён: " + FONT_LABELS[i]);
                    return true;
                }
            }
            if (accentField.mouseClicked(click, doubled)) { focusOnly(accentField); return true; }
        } else if (tab == Tab.MODERATION) {
            int x = l.x() + 20;
            int y = l.y() + 118;
            int w = l.w() - 40;
            int half = (w - 14) / 2;
            int rightX = x + half + 14;
            Rect afk = new Rect(x + 18, y + 46, half - 36, 34);
            Rect screenshot = new Rect(x + 18, y + 88, half - 36, 34);
            Rect hints = new Rect(x + 18, y + 130, half - 36, 34);
            Rect mention = new Rect(rightX + 18, y + 46, half - 36, 34);
            Rect words = new Rect(rightX + 18, y + 88, half - 36, 34);
            if (afk.contains(mx, my)) { RaidMineStaffMod.config().afkKickEnabled ^= true; save("AFK Kick обновлён"); return true; }
            if (screenshot.contains(mx, my)) { RaidMineStaffMod.config().autoScreenshot ^= true; save("Настройки сохранены"); return true; }
            if (hints.contains(mx, my)) { RaidMineStaffMod.config().showHintsPanel ^= true; save("Настройки сохранены"); return true; }
            if (mention.contains(mx, my)) { RaidMineStaffMod.config().mentionNotifications ^= true; save("Настройки сохранены"); return true; }
            if (words.contains(mx, my)) { RaidMineStaffMod.config().forbiddenWordAlerts ^= true; save("Настройки сохранены"); return true; }
            int goalY = y + 188;
            Rect minus = new Rect(x + 18, goalY + 19, 34, 30);
            Rect plus = new Rect(x + half - 52, goalY + 19, 34, 30);
            if (minus.contains(mx, my)) { RaidMineStaffMod.config().dailyOnlineGoalMinutes = Math.max(15, RaidMineStaffMod.config().dailyOnlineGoalMinutes - 15); save("Норма изменена"); return true; }
            if (plus.contains(mx, my)) { RaidMineStaffMod.config().dailyOnlineGoalMinutes = Math.min(720, RaidMineStaffMod.config().dailyOnlineGoalMinutes + 15); save("Норма изменена"); return true; }
            if (reasonField.mouseClicked(click, doubled)) { focusOnly(reasonField); return true; }
        } else {
            int x = l.x() + 20;
            int y = l.y() + 118;
            int w = l.w() - 40;
            if (wordField.mouseClicked(click, doubled)) { focusOnly(wordField); return true; }
            Rect add = new Rect(wordField.getX() + wordField.getWidth() + 10, wordField.getY(), 84, 28);
            Rect open = new Rect(x + w - 188, wordField.getY(), 82, 28);
            Rect reload = new Rect(x + w - 96, wordField.getY(), 78, 28);
            if (add.contains(mx, my)) { addWord(); return true; }
            if (open.contains(mx, my)) { openWordsFolder(); return true; }
            if (reload.contains(mx, my)) { reloadWords(); return true; }
            List<String> words = RaidMineStaffMod.config().forbiddenWords;
            int visibleRows = Math.max(1, ((l.h() - 146) - 112) / 32);
            int listY = y + 105;
            for (int i = 0; i < visibleRows && wordOffset + i < words.size(); i++) {
                Rect row = new Rect(x + 18, listY + i * 32, w - 36, 26);
                if (removeRect(row).contains(mx, my)) {
                    words.remove(wordOffset + i);
                    RaidMineStaffMod.config().save();
                    showStatus("Слово удалено");
                    return true;
                }
            }
        }
        focusOnly(null);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragTarget != DragTarget.NONE && tab == Tab.APPEARANCE) {
            Layout l = layout();
            int x = l.x() + 20;
            int y = l.y() + 118;
            int leftW = ((l.w() - 40) - 14) / 2;
            Rect track = switch (dragTarget) {
                case BACKGROUND -> backgroundOpacityTrackRect(x, y, leftW);
                case UI_OUTLINE -> uiOutlineOpacityTrackRect(x, y, leftW);
                case HUD_OUTLINE -> hudOutlineOpacityTrackRect(x, y, leftW);
                case NONE -> backgroundOpacityTrackRect(x, y, leftW);
            };
            setSliderFromMouse(click.x(), track, dragTarget);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragTarget != DragTarget.NONE) {
            dragTarget = DragTarget.NONE;
            RaidMineStaffMod.config().save();
            showStatus("Прозрачность сохранена");
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab != Tab.WORDS) return false;
        Layout l = layout();
        int visibleRows = Math.max(1, ((l.h() - 146) - 112) / 32);
        int max = Math.max(0, RaidMineStaffMod.config().forbiddenWords.size() - visibleRows);
        wordOffset = Math.max(0, Math.min(max, wordOffset + (verticalAmount < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (input.isEnter()) {
            if (wordField.isFocused()) { addWord(); return true; }
            if (accentField.isFocused()) { applyHex(); return true; }
            if (reasonField.isFocused()) { saveReason(); return true; }
        }
        if (wordField.isFocused() && wordField.keyPressed(input)) return true;
        if (accentField.isFocused() && accentField.keyPressed(input)) return true;
        if (reasonField.isFocused() && reasonField.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (wordField.isFocused() && wordField.charTyped(input)) return true;
        if (accentField.isFocused() && accentField.charTyped(input)) return true;
        if (reasonField.isFocused() && reasonField.charTyped(input)) return true;
        return super.charTyped(input);
    }

    private void addWord() {
        String value = wordField.getText().trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) { showStatus("Введите слово"); return; }
        if (!RaidMineStaffMod.config().forbiddenWords.contains(value)) RaidMineStaffMod.config().forbiddenWords.add(value);
        RaidMineStaffMod.config().save();
        wordField.setText("");
        showStatus("Слово добавлено");
    }

    private void reloadWords() {
        RaidMineStaffMod.config().forbiddenWords = ForbiddenWordsStore.loadOrCreate(RaidMineStaffMod.config().forbiddenWords);
        RaidMineStaffMod.config().save();
        wordOffset = 0;
        showStatus("Список перечитан из файла");
    }

    private void openWordsFolder() {
        FolderOpener.Result result = FolderOpener.open(ForbiddenWordsStore.directory());
        showStatus(result.message());
    }

    private void setSliderFromMouse(double mouseX, Rect track, DragTarget target) {
        float value = (float) ((mouseX - track.x()) / Math.max(1.0, track.w()));
        value = Math.max(0F, Math.min(1F, value));
        switch (target) {
            case BACKGROUND -> RaidMineStaffMod.config().uiBackgroundOpacity = value;
            case UI_OUTLINE -> RaidMineStaffMod.config().uiOutlineOpacity = value;
            case HUD_OUTLINE -> RaidMineStaffMod.config().hudOutlineOpacity = value;
            case NONE -> { }
        }
    }

    private void applyHex() {
        String raw = accentField.getText().trim().replace("#", "");
        try {
            int primary = Integer.parseInt(raw, 16) & 0xFFFFFF;
            int secondary = darken(primary, 0.72F);
            RaidMineStaffMod.config().setAccent(0xFF000000 | primary, 0xFF000000 | secondary);
            accentField.setText(String.format(Locale.ROOT, "#%06X", primary));
            showStatus("Цвет интерфейса изменён");
        } catch (NumberFormatException exception) {
            showStatus("HEX должен быть вида #FF8A00");
        }
    }

    private void saveReason() {
        String template = reasonField.getText().trim();
        RaidMineStaffMod.config().punishmentReasonTemplate = template.isEmpty() ? "{rule}" : template;
        RaidMineStaffMod.config().save();
        showStatus("Формат причины сохранён");
    }

    private static int darken(int rgb, float factor) {
        int r = Math.round(((rgb >> 16) & 255) * factor);
        int g = Math.round(((rgb >> 8) & 255) * factor);
        int b = Math.round((rgb & 255) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private void focusOnly(TextFieldWidget focused) {
        wordField.setFocused(wordField == focused);
        accentField.setFocused(accentField == focused);
        reasonField.setFocused(reasonField == focused);
    }

    private void save(String message) {
        if (reasonField != null) {
            String template = reasonField.getText().trim();
            RaidMineStaffMod.config().punishmentReasonTemplate = template.isEmpty() ? "{rule}" : template;
        }
        RaidMineStaffMod.config().save();
        showStatus(message);
    }

    private void showStatus(String message) { status = message; statusAt = System.currentTimeMillis(); }

    @Override
    public void close() {
        save("Настройки сохранены");
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) { }
    @Override
    public boolean shouldPause() { return false; }

    private Layout layout() {
        int w = Math.min(920, width - 24);
        int h = Math.min(610, height - 20);
        return new Layout((width - w) / 2, (height - h) / 2, w, h);
    }

    private Rect closeRect(Layout l) { return new Rect(l.x() + l.w() - 48, l.y() + 16, 30, 30); }
    private Rect tabRect(Layout l, int index) {
        int x = l.x() + 18;
        int y = l.y() + 70;
        int w = (l.w() - 44) / 3;
        return new Rect(x + index * (w + 4), y, w, 34);
    }
    private Rect presetRect(int x, int y, int index) {
        int column = index % 4;
        int row = index / 4;
        return new Rect(x + 18 + column * 48, y + 66 + row * 40, 40, 30);
    }
    private Rect fontRect(int x, int y, int width, int index) {
        int column = index % 2;
        int row = index / 2;
        int w = (width - 46) / 2;
        return new Rect(x + 18 + column * (w + 10), y + 66 + row * 42, w, 32);
    }
    private Rect backgroundOpacityTrackRect(int x, int y, int width) {
        return new Rect(x + 18, y + 232, Math.max(120, width - 92), 8);
    }
    private Rect uiOutlineOpacityTrackRect(int x, int y, int width) {
        return new Rect(x + 18, y + 286, Math.max(120, width - 92), 8);
    }
    private Rect hudOutlineToggleRect(int x, int y, int width) {
        return new Rect(x + 18, y + 316, width - 36, 34);
    }
    private Rect hudOutlineOpacityTrackRect(int x, int y, int width) {
        return new Rect(x + 18, y + 388, Math.max(120, width - 92), 8);
    }
    private Rect removeRect(Rect row) { return new Rect(row.x() + row.w() - 30, row.y() + 1, 26, 24); }

    private enum DragTarget { NONE, BACKGROUND, UI_OUTLINE, HUD_OUTLINE }

    private enum Tab {
        APPEARANCE("Интерфейс", UiIcon.PALETTE),
        MODERATION("Модерация", UiIcon.SHIELD),
        WORDS("Запрещённые слова", UiIcon.WARN);
        private final String label;
        private final UiIcon icon;
        Tab(String label, UiIcon icon) { this.label = label; this.icon = icon; }
    }

    private record Layout(int x, int y, int w, int h) { }
    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
}
