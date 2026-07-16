package dev.raidmine.stafftool.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Records the coordinates of text exactly as a chat screen renders it.
 * This is the SmartChat compatibility path: custom scale, translation and
 * stretched chat layouts are taken from DrawContext's live matrix instead of
 * being reconstructed from vanilla chat settings.
 */
public final class ChatRenderTracker {
    private static final int TEXT_HEIGHT = 9;
    private static final List<ChatSelectionHelper.Hit> LAST_HITS = new ArrayList<>();
    private static boolean collecting;
    private static Screen screen;
    private static DrawContext frameContext;
    private static long completedAt;
    private static long frameStartedAt;
    private static DrawContext hoverClaimContext;
    private static ChatSelectionHelper.Hit hoverClaimedHit;

    private ChatRenderTracker() {
    }

    public static void begin(Screen currentScreen) {
        collecting = true;
        screen = currentScreen;
        frameContext = null;
        frameStartedAt = System.currentTimeMillis();
        hoverClaimContext = null;
        hoverClaimedHit = null;
        LAST_HITS.clear();
    }

    /** Invalidates coordinates captured before a screen transition. */
    public static void invalidate() {
        collecting = false;
        frameContext = null;
        screen = null;
        completedAt = 0L;
        frameStartedAt = 0L;
        hoverClaimContext = null;
        hoverClaimedHit = null;
        LAST_HITS.clear();
    }

    public static void finish(Screen currentScreen) {
        if (screen == currentScreen) {
            completedAt = System.currentTimeMillis();
        }
        collecting = false;
    }

    public static void observe(DrawContext context, TextRenderer renderer, OrderedText text, int x, int y) {
        if (context == null || renderer == null || text == null) return;
        if (!collecting) {
            Screen current = MinecraftClient.getInstance().currentScreen;
            String name = current == null ? "" : current.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            if (current == null || !name.contains("chat")) return;
            if (frameContext != context || screen != current) {
                LAST_HITS.clear();
                frameContext = context;
                screen = current;
                frameStartedAt = System.currentTimeMillis();
                hoverClaimContext = null;
                hoverClaimedHit = null;
            }
        } else if (frameContext != context) {
            frameContext = context;
            frameStartedAt = System.currentTimeMillis();
            hoverClaimContext = null;
            hoverClaimedHit = null;
        }

        StringBuilder raw = new StringBuilder();
        List<Piece> pieces = new ArrayList<>();
        final float[] cursor = {x};
        text.accept((index, style, codePoint) -> {
            String character = new String(Character.toChars(codePoint));
            int start = raw.length();
            raw.append(character);
            float width = renderer.getTextHandler().getWidth(OrderedText.styled(codePoint, style));
            pieces.add(new Piece(start, raw.length(), cursor[0], cursor[0] + width));
            cursor[0] += width;
            return true;
        });

        Optional<NicknameResolver.AuthorMatch> resolved = NicknameResolver.resolveAuthorMatch(raw.toString());
        if (resolved.isEmpty()) return;
        NicknameResolver.AuthorMatch author = resolved.get();

        float left = Float.MAX_VALUE;
        float right = Float.MIN_VALUE;
        for (Piece piece : pieces) {
            if (piece.end() > author.start() && piece.start() < author.end()) {
                left = Math.min(left, piece.left());
                right = Math.max(right, piece.right());
            }
        }
        if (left == Float.MAX_VALUE || right <= left) return;

        Matrix3x2f matrix = new Matrix3x2f(context.getMatrices());
        Vector2f a = matrix.transformPosition(new Vector2f(left, y));
        Vector2f b = matrix.transformPosition(new Vector2f(right, y));
        Vector2f c = matrix.transformPosition(new Vector2f(left, y + TEXT_HEIGHT));
        Vector2f d = matrix.transformPosition(new Vector2f(right, y + TEXT_HEIGHT));
        int screenLeft = Math.round(Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)));
        int screenRight = Math.round(Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)));
        int screenTop = Math.round(Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)));
        int screenBottom = Math.round(Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)));
        if (screenRight <= screenLeft || screenBottom <= screenTop) return;

        LAST_HITS.add(new ChatSelectionHelper.Hit(
                author.name(),
                raw.substring(author.start(), author.end()),
                screenLeft,
                screenTop,
                screenRight,
                screenBottom
        ));
        completedAt = System.currentTimeMillis();
    }


    /** Returns a hovered author once per rendered frame, so the fill is drawn exactly behind the glyphs. */
    public static Optional<ChatSelectionHelper.Hit> claimHover(DrawContext context, Screen currentScreen,
                                                                double mouseX, double mouseY) {
        if (context == null || currentScreen == null || context != frameContext || currentScreen != screen) {
            return Optional.empty();
        }
        Optional<ChatSelectionHelper.Hit> found = find(currentScreen, mouseX, mouseY);
        if (found.isEmpty()) return Optional.empty();
        ChatSelectionHelper.Hit hit = found.get();
        if (hoverClaimContext == context && hit.equals(hoverClaimedHit)) return Optional.empty();
        hoverClaimContext = context;
        hoverClaimedHit = hit;
        return Optional.of(hit);
    }

    public static boolean hasFrameAfter(Screen currentScreen, long timestamp) {
        return currentScreen != null && currentScreen == screen
                && completedAt >= timestamp && frameStartedAt >= timestamp;
    }

    public static boolean hasFreshFrame(Screen currentScreen) {
        return currentScreen != null && currentScreen == screen
                && System.currentTimeMillis() - completedAt <= 900L;
    }

    public static boolean hasAuthor(Screen currentScreen, String nickname) {
        if (!hasFreshFrame(currentScreen) || nickname == null || nickname.isBlank()) return false;
        for (ChatSelectionHelper.Hit hit : LAST_HITS) {
            if (hit.nickname().equalsIgnoreCase(nickname)) return true;
        }
        return false;
    }

    public static Optional<ChatSelectionHelper.Hit> find(Screen currentScreen, double mouseX, double mouseY) {
        if (currentScreen == null || currentScreen != screen) return Optional.empty();
        if (System.currentTimeMillis() - completedAt > 2_000L) return Optional.empty();

        ChatSelectionHelper.Hit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ChatSelectionHelper.Hit hit : LAST_HITS) {
            int marginX = 1;
            int marginY = 1;
            if (mouseX < hit.left() - marginX || mouseX > hit.right() + marginX
                    || mouseY < hit.top() - marginY || mouseY > hit.bottom() + marginY) {
                continue;
            }
            double centerX = (hit.left() + hit.right()) * 0.5D;
            double centerY = (hit.top() + hit.bottom()) * 0.5D;
            double distance = Math.abs(mouseX - centerX) + Math.abs(mouseY - centerY) * 1.8D;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = hit;
            }
        }
        return Optional.ofNullable(best);
    }

    private record Piece(int start, int end, float left, float right) {
    }
}
