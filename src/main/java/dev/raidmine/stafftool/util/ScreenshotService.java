package dev.raidmine.stafftool.util;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.chat.UiNotificationCenter;
import dev.raidmine.stafftool.ui.PunishmentScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ScreenshotService {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final long MIN_OPEN_CHAT_MILLIS = 360L;
    private static final long AUTHOR_WAIT_MILLIS = 1500L;
    private static final long ABSOLUTE_FALLBACK_MILLIS = 4200L;
    private static Pending pending;

    private ScreenshotService() { }

    /** Queue evidence capture after the punishment menu has closed. */
    public static void requestAfterChatRender(String player, String reason, Screen chatScreen) {
        if (!RaidMineStaffMod.config().autoScreenshot) return;
        ChatRenderTracker.invalidate();
        pending = new Pending(player, reason, chatScreen, System.currentTimeMillis(), 0);
    }

    /** Returns the already-open chat whenever possible, preserving scroll and SmartChat tabs. */
    public static Screen evidenceScreen(Screen preferred) {
        if (preferred != null) {
            String name = preferred.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            if (preferred instanceof ChatScreen || name.contains("chat")) return preferred;
        }
        return new ChatScreen("", false);
    }

    /** Called from vanilla ChatScreen.render TAIL after the expanded chat was fully drawn. */
    public static void afterChatRendered() {
        Pending request = pending;
        if (request == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof PunishmentScreen) return;
        Screen current = client.currentScreen;
        if (!isChatLike(current)) return;

        int frames = request.renderedFrames() + 1;
        request = request.withRenderedFrames(frames);
        pending = request;
        long age = System.currentTimeMillis() - request.requestedAt();
        boolean newFrame = ChatRenderTracker.hasFrameAfter(current, request.requestedAt());
        boolean authorVisible = newFrame && ChatRenderTracker.hasAuthor(current, request.player());
        if (age < MIN_OPEN_CHAT_MILLIS || frames < 3 || !newFrame) return;
        if (!authorVisible && age < AUTHOR_WAIT_MILLIS) return;
        pending = null;
        captureNow(request.player(), request.reason());
    }

    /** SmartChat path: uses the last frame observed through DrawContext text rendering. */
    public static void tick(MinecraftClient client) {
        Pending request = pending;
        if (request == null || client == null || client.currentScreen instanceof PunishmentScreen) return;
        Screen current = client.currentScreen;
        long age = System.currentTimeMillis() - request.requestedAt();
        if (!isChatLike(current)) {
            if (age > ABSOLUTE_FALLBACK_MILLIS) pending = null;
            return;
        }
        boolean fresh = ChatRenderTracker.hasFreshFrame(current)
                && ChatRenderTracker.hasFrameAfter(current, request.requestedAt());
        boolean authorVisible = fresh && ChatRenderTracker.hasAuthor(current, request.player());
        if (age >= MIN_OPEN_CHAT_MILLIS && fresh && (authorVisible || age >= AUTHOR_WAIT_MILLIS)) {
            pending = null;
            captureNow(request.player(), request.reason());
            return;
        }
        if (age >= ABSOLUTE_FALLBACK_MILLIS) {
            pending = null;
            captureNow(request.player(), request.reason());
        }
    }

    private static boolean isChatLike(Screen screen) {
        if (screen == null) return false;
        return screen instanceof ChatScreen
                || screen.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("chat");
    }

    public static void cancel() {
        pending = null;
    }

    private static void captureNow(String player, String reason) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isChatLike(client.currentScreen)) return;
        String safePlayer = sanitize(player, "player");
        String safeReason = sanitize(reason, "rule");
        String fileName = safePlayer + "_" + safeReason + "_" + FORMAT.format(LocalDateTime.now()) + ".png";
        try {
            ScreenshotRecorder.saveScreenshot(
                    client.runDirectory, fileName, client.getFramebuffer(), 1,
                    text -> client.execute(() -> UiNotificationCenter.info("Скриншот сохранён", fileName))
            );
        } catch (Exception exception) {
            RaidMineStaffMod.LOGGER.error("Could not capture moderation screenshot", exception);
            UiNotificationCenter.info("Ошибка скриншота",
                    exception.getMessage() == null ? "Не удалось сохранить" : exception.getMessage());
        }
    }

    private static String sanitize(String value, String fallback) {
        String safe = value == null ? "" : value.trim().replaceAll("[^A-Za-zА-Яа-я0-9_.-]+", "_");
        safe = safe.replaceAll("_+", "_");
        if (safe.length() > 48) safe = safe.substring(0, 48);
        return safe.isBlank() ? fallback : safe;
    }

    private record Pending(String player, String reason, Screen chatScreen, long requestedAt, int renderedFrames) {
        Pending withRenderedFrames(int frames) {
            return new Pending(player, reason, chatScreen, requestedAt, frames);
        }
    }
}
