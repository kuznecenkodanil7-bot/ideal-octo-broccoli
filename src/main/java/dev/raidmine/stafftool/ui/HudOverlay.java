package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.chat.UiNotificationCenter;
import dev.raidmine.stafftool.rules.PunishmentType;
import dev.raidmine.stafftool.stats.SessionStats;
import dev.raidmine.stafftool.util.AuthManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class HudOverlay {
    public static final int BASE_WIDTH = 282;
    public static final int BASE_HEIGHT = 42;
    private static volatile boolean editingInteraction;
    private static volatile boolean editingSelected;

    private HudOverlay() { }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HudEditorScreen) return;
        SessionStats stats = renderInternal(context, false);
        if (stats != null) HintSidebarOverlay.render(context, stats);
    }

    public static void renderEditable(DrawContext context) {
        renderInternal(context, true);
    }

    public static void setEditingInteraction(boolean interacting) {
        editingInteraction = interacting;
    }

    public static void setEditingSelected(boolean selected) {
        editingSelected = selected;
        if (!selected) editingInteraction = false;
    }

    public static boolean isEditingSelected() {
        return editingSelected;
    }

    private static SessionStats renderInternal(DrawContext context, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null
                || !RaidMineStaffMod.config().hudEnabled || !AuthManager.canUseMod()) {
            return null;
        }

        SessionStats stats = RaidMineStaffMod.stats();
        Layout layout = layout(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        UiNotificationCenter.Notice notice = editing ? null : UiNotificationCenter.top();
        renderBar(context, layout, stats, notice, editing);
        return stats;
    }

    private static void renderBar(DrawContext context, Layout l, SessionStats stats,
                                  UiNotificationCenter.Notice notice, boolean editing) {
        float transition = notice == null ? 0F : UiNotificationCenter.progress(notice);
        int neutral = UiTheme.argb(247, 12, 14, 18);
        int noticeAccent = notice == null ? UiTheme.accent() : switch (notice.kind()) {
            case VIOLATION -> 0xFFFF6A2A;
            case MENTION -> 0xFFFFB52E;
            case INFO -> UiTheme.SUCCESS;
        };
        int noticeSurface = UiTheme.withAlpha(UiTheme.blend(neutral, noticeAccent, 0.17F), 247);
        int background = UiTheme.blend(neutral, noticeSurface, transition);
        int border = stats.goalReached() && notice == null ? UiTheme.SUCCESS :
                UiTheme.blend(UiTheme.accent(), noticeAccent, transition);
        int radius = Math.max(13, l.height() / 2);

        if (RaidMineStaffMod.config().hudOutlineEnabled) {
            int outlineAlpha = Math.round(255F * RaidMineStaffMod.config().hudOutlineOpacity);
            int thickness = Math.max(1, Math.round(1.6F * l.contentScale()));
            UiTheme.roundedBorder(context, l.x(), l.y(), l.width(), l.height(), radius,
                    thickness, UiTheme.withAlpha(border, outlineAlpha), background);
        } else {
            UiTheme.roundedRect(context, l.x(), l.y(), l.width(), l.height(), radius, background);
        }

        if (editing && editingSelected) {
            int alpha = editingInteraction ? 245 : 155;
            UiTheme.roundedBorder(context, l.x() - 1, l.y() - 1, l.width() + 2, l.height() + 2,
                    radius + 1, 1, UiTheme.withAlpha(UiTheme.accent(), alpha), UiTheme.withAlpha(background, 0));
        }

        if (notice == null) {
            renderStatsContent(context, l, stats, 255, 0);
        } else {
            int oldAlpha = Math.round(255F * (1F - transition));
            int newAlpha = Math.round(255F * transition);
            renderStatsContent(context, l, stats, oldAlpha, -Math.round(7F * transition));
            renderNoticeContent(context, l, notice, newAlpha, Math.round(7F * (1F - transition)), noticeAccent);
        }

        if (editing && editingSelected) renderHandles(context, l, UiTheme.accent());
    }

    private static void renderStatsContent(DrawContext context, Layout l, SessionStats stats,
                                           int alpha, int yOffset) {
        if (alpha <= 2) return;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        float s = l.contentScale();
        int yBase = l.y() + yOffset;
        int cursor = l.x() + px(7, s);

        int logoSize = px(28, s);
        UiTheme.logo(context, cursor, yBase + (l.height() - logoSize) / 2, logoSize, logoSize, alpha);
        cursor += logoSize + px(3, s);

        float brandTop = Math.max(7.2F, 8.8F * s);
        float brandBottom = Math.max(6.7F, 8.1F * s);
        UiTheme.brandText(context, "RaidMine", cursor, yBase + px(8, s), brandTop,
                UiTheme.withAlpha(UiTheme.TEXT, alpha));
        UiTheme.brandText(context, "Tools", cursor, yBase + px(21, s), brandBottom,
                UiTheme.withAlpha(UiTheme.MUTED, alpha));
        cursor += px(48, s);

        context.fill(cursor, yBase + px(7, s), cursor + 1,
                yBase + l.height() - px(7, s), UiTheme.withAlpha(UiTheme.BORDER, Math.round(alpha * 0.42F)));
        cursor += px(5, s);

        cursor = verticalStat(context, tr, cursor, yBase, l, UiIcon.BAN, stats.bans(), UiTheme.DANGER,
                Math.max(stats.pulse(PunishmentType.BAN), stats.pulse(PunishmentType.PERMANENT_BAN)), alpha);
        cursor = verticalStat(context, tr, cursor, yBase, l, UiIcon.MUTE, stats.mutes(), UiTheme.WARNING,
                stats.pulse(PunishmentType.MUTE), alpha);
        cursor = verticalStat(context, tr, cursor, yBase, l, UiIcon.WARN, stats.warns(), UiTheme.accent(),
                stats.pulse(PunishmentType.WARN), alpha);

        int timeColor = stats.goalReached() ? UiTheme.SUCCESS : UiTheme.TEXT;
        int timeW = px(57, s);
        int blockH = Math.min(l.height() - px(8, s), px(32, s));
        int blockY = yBase + (l.height() - blockH) / 2;
        UiTheme.roundedRect(context, cursor, blockY, timeW, blockH,
                Math.max(7, blockH / 3), UiTheme.withAlpha(UiTheme.argb(112, 31, 34, 42), alpha));
        int clock = px(13, s);
        int clockX = cursor + px(5, s);
        int clockY = blockY + (blockH - clock) / 2;
        UiTheme.icon(context, UiIcon.CLOCK, clockX, clockY, clock, UiTheme.withAlpha(timeColor, alpha));
        String time = formatTime(stats.elapsedSeconds());
        float timeSize = Math.max(7.3F, 8.8F * s);
        int timeTextY = blockY + (blockH - Math.round(timeSize * 0.78F)) / 2 - 1;
        UiTheme.text(context, tr, time, cursor + px(21, s), timeTextY,
                timeSize, UiTheme.withAlpha(timeColor, alpha), true);
        cursor += timeW + px(3, s);

        int eyeBox = px(23, s);
        int eyeY = yBase + (l.height() - eyeBox) / 2;
        int neutralBg = UiTheme.withAlpha(UiTheme.argb(92, 32, 35, 43), alpha);
        UiTheme.roundedRect(context, cursor, eyeY, eyeBox, eyeBox, Math.max(7, eyeBox / 3), neutralBg);
        int eyeSize = px(14, s);
        int eyeX = cursor + (eyeBox - eyeSize) / 2;
        int eyeIconY = eyeY + (eyeBox - eyeSize) / 2;
        if (stats.isVanished()) {
            int glowAlpha = Math.round(alpha * 0.30F);
            int glow = UiTheme.withAlpha(0xFFC06CFF, glowAlpha);
            UiTheme.icon(context, UiIcon.EYE, eyeX - 1, eyeIconY, eyeSize, glow);
            UiTheme.icon(context, UiIcon.EYE, eyeX + 1, eyeIconY, eyeSize, glow);
            UiTheme.icon(context, UiIcon.EYE, eyeX, eyeIconY - 1, eyeSize, glow);
            UiTheme.icon(context, UiIcon.EYE, eyeX, eyeIconY + 1, eyeSize, glow);
            UiTheme.icon(context, UiIcon.EYE, eyeX, eyeIconY, eyeSize,
                    UiTheme.withAlpha(0xFFC06CFF, alpha));
        } else {
            UiTheme.icon(context, UiIcon.EYE_OFF, eyeX, eyeIconY, eyeSize,
                    UiTheme.withAlpha(UiTheme.argb(255, 73, 77, 88), alpha));
        }
    }

    private static int verticalStat(DrawContext context, TextRenderer tr, int x, int yBase, Layout l,
                                    UiIcon icon, int value, int accent, float pulse, int alpha) {
        float s = l.contentScale();
        int blockW = px(27, s);
        int blockH = Math.min(l.height() - px(8, s), px(32, s));
        int y = yBase + (l.height() - blockH) / 2;
        int bgAlpha = pulse > 0F ? 46 + Math.round(58F * pulse) : 96;
        bgAlpha = Math.round(bgAlpha * (alpha / 255F));
        int bg = pulse > 0F ? UiTheme.withAlpha(accent, bgAlpha)
                : UiTheme.argb(bgAlpha, 31, 34, 42);
        UiTheme.roundedRect(context, x, y, blockW, blockH, Math.max(7, blockH / 3), bg);
        int iconSize = px(12, s);
        int iconX = x + (blockW - iconSize) / 2;
        int iconY = y + px(3, s);
        UiTheme.icon(context, icon, iconX, iconY, iconSize, UiTheme.withAlpha(accent, alpha));
        String number = Integer.toString(value);
        float numberSize = Math.max(6.8F, 8.0F * s);
        int textW = UiTheme.textWidth(number, numberSize, true);
        int textY = y + blockH - px(11, s);
        UiTheme.text(context, tr, number, x + (blockW - textW) / 2, textY,
                numberSize, UiTheme.withAlpha(UiTheme.TEXT, alpha), true);
        return x + blockW + px(3, s);
    }

    private static void renderNoticeContent(DrawContext context, Layout l,
                                            UiNotificationCenter.Notice notice,
                                            int alpha, int yOffset, int accent) {
        if (alpha <= 2) return;
        float s = l.contentScale();
        int y = l.y() + yOffset;
        int iconSize = px(19, s);
        int iconX = l.x() + px(11, s);
        int iconY = y + (l.height() - iconSize) / 2;
        UiIcon icon = notice.kind() == UiNotificationCenter.Kind.MENTION ? UiIcon.BELL :
                notice.kind() == UiNotificationCenter.Kind.VIOLATION ? UiIcon.WARN : UiIcon.CHECK;
        UiTheme.icon(context, icon, iconX, iconY, iconSize, UiTheme.withAlpha(accent, alpha));
        int textX = iconX + iconSize + px(8, s);
        int maxWidth = l.x() + l.width() - textX - px(10, s);
        String title = UiTheme.ellipsize(MinecraftClient.getInstance().textRenderer, notice.title(), maxWidth);
        String message = UiTheme.ellipsize(MinecraftClient.getInstance().textRenderer, notice.message(), maxWidth);
        UiTheme.text(context, MinecraftClient.getInstance().textRenderer, title,
                textX, y + px(8, s), Math.max(7.7F, 9.1F * s),
                UiTheme.withAlpha(UiTheme.TEXT, alpha), true);
        UiTheme.text(context, MinecraftClient.getInstance().textRenderer, message,
                textX, y + px(22, s), Math.max(6.7F, 7.8F * s),
                UiTheme.withAlpha(UiTheme.MUTED, alpha), false);
    }

    private static void renderHandles(DrawContext context, Layout l, int color) {
        for (Handle handle : l.handles()) {
            Rect r = handle.rect();
            UiTheme.roundedRectExact(context, r.x(), r.y(), r.w(), r.h(),
                    Math.max(3, r.w() / 2), UiTheme.withAlpha(color, 245));
        }
    }

    public static Layout layout(int screenWidth, int screenHeight) {
        float widthScale = RaidMineStaffMod.config().hudWidthScale;
        float heightScale = RaidMineStaffMod.config().hudHeightScale;
        int width = Math.max(244, Math.round(BASE_WIDTH * widthScale));
        int height = Math.max(34, Math.round(BASE_HEIGHT * heightScale));
        int availableX = Math.max(0, screenWidth - width);
        int availableY = Math.max(0, screenHeight - height);
        int x = Math.round(availableX * RaidMineStaffMod.config().hudX);
        int y = Math.round(availableY * RaidMineStaffMod.config().hudY);
        x = Math.max(0, Math.min(availableX, x));
        y = Math.max(0, Math.min(availableY, y));
        float contentScale = Math.max(0.72F, Math.min(width / (float) BASE_WIDTH, height / (float) BASE_HEIGHT));
        return new Layout(x, y, width, height, widthScale, heightScale, contentScale);
    }

    public static void setPosition(int screenWidth, int screenHeight, int x, int y) {
        Layout current = layout(screenWidth, screenHeight);
        int maxX = Math.max(1, screenWidth - current.width());
        int maxY = Math.max(1, screenHeight - current.height());
        RaidMineStaffMod.config().hudX = Math.max(0, Math.min(maxX, x)) / (float) maxX;
        RaidMineStaffMod.config().hudY = Math.max(0, Math.min(maxY, y)) / (float) maxY;
    }

    public static void nudge(int screenWidth, int screenHeight, int dx, int dy) {
        Layout current = layout(screenWidth, screenHeight);
        setPosition(screenWidth, screenHeight, current.x() + dx, current.y() + dy);
        RaidMineStaffMod.config().save();
    }

    public static void setWidthScale(float scale) {
        RaidMineStaffMod.config().hudWidthScale = Math.max(0.72F, Math.min(1.80F, scale));
    }

    public static void setHeightScale(float scale) {
        RaidMineStaffMod.config().hudHeightScale = Math.max(0.72F, Math.min(1.80F, scale));
    }

    public static void setScale(float scale) {
        setWidthScale(scale);
        setHeightScale(scale);
    }

    public static void centerTop() {
        RaidMineStaffMod.config().hudX = 0.5F;
        RaidMineStaffMod.config().hudY = 0.015F;
        RaidMineStaffMod.config().save();
    }

    public static void reset() {
        RaidMineStaffMod.config().hudX = 0.5F;
        RaidMineStaffMod.config().hudY = 0.015F;
        RaidMineStaffMod.config().hudWidthScale = 0.90F;
        RaidMineStaffMod.config().hudHeightScale = 1.00F;
        RaidMineStaffMod.config().save();
    }

    private static int px(int value, float scale) {
        return Math.max(1, Math.round(value * scale));
    }

    private static String formatTime(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, secs)
                : String.format("%02d:%02d", minutes, secs);
    }

    public enum Edge { MOVE, N, S, E, W, NE, NW, SE, SW }
    public record Handle(Edge edge, Rect rect) { }

    public record Layout(int x, int y, int width, int height,
                         float widthScale, float heightScale, float contentScale) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        public Handle[] handles() {
            int size = Math.max(7, Math.round(8 * contentScale));
            int half = size / 2;
            int cx = x + width / 2;
            int cy = y + height / 2;
            return new Handle[]{
                    new Handle(Edge.NW, new Rect(x - half, y - half, size, size)),
                    new Handle(Edge.N, new Rect(cx - half, y - half, size, size)),
                    new Handle(Edge.NE, new Rect(x + width - half, y - half, size, size)),
                    new Handle(Edge.W, new Rect(x - half, cy - half, size, size)),
                    new Handle(Edge.E, new Rect(x + width - half, cy - half, size, size)),
                    new Handle(Edge.SW, new Rect(x - half, y + height - half, size, size)),
                    new Handle(Edge.S, new Rect(cx - half, y + height - half, size, size)),
                    new Handle(Edge.SE, new Rect(x + width - half, y + height - half, size, size))
            };
        }

        public Edge edgeAt(double mouseX, double mouseY) {
            if (editingSelected) {
                for (Handle handle : handles()) {
                    if (handle.rect().contains(mouseX, mouseY)) return handle.edge();
                }
            }
            return contains(mouseX, mouseY) ? Edge.MOVE : null;
        }
    }

    public record Rect(int x, int y, int w, int h) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
}
