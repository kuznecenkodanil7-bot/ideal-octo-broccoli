package dev.raidmine.stafftool.mixin;

import dev.raidmine.stafftool.ui.ChatHoverRenderer;
import dev.raidmine.stafftool.util.ChatRenderTracker;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Shadow @Final private int mouseX;
    @Shadow @Final private int mouseY;
    @Inject(
            method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V",
            at = @At("HEAD")
    )
    private void rmtools$trackChatText(TextRenderer renderer, OrderedText text,
                                       int x, int y, int color, boolean shadow,
                                       CallbackInfo ci) {
        DrawContext context = (DrawContext) (Object) this;
        ChatRenderTracker.observe(context, renderer, text, x, y);
        net.minecraft.client.gui.screen.Screen current = net.minecraft.client.MinecraftClient.getInstance().currentScreen;
        ChatRenderTracker.claimHover(context, current, mouseX, mouseY).ifPresent(hit -> {
            context.setCursor(net.minecraft.client.gui.cursor.StandardCursors.POINTING_HAND);
            ChatHoverRenderer.render(context, hit);
        });
    }
}
