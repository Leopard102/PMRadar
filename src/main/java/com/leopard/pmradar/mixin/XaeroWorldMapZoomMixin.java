package com.leopard.pmradar.mixin;

import com.leopard.pmradar.client.WorldMapRadarLegendOverlay;
import com.leopard.pmradar.client.StormOverlayData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;
import xaero.map.graphics.CustomRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;

@Mixin(value = GuiMap.class, remap = false)
public abstract class XaeroWorldMapZoomMixin {
    @Shadow
    private double scale;

    @Shadow
    private float[] colourBuffer;

    @Shadow
    public abstract void drawObjectOnMap(PoseStack matrixStack, VertexConsumer guiLinearBuffer, double x, double z, float angle, double sc, float offX, float offY, int textureX, int textureY, int w, int h, int filter);

    private int pmradar$renderMouseX;
    private int pmradar$renderMouseY;
    @Shadow private int mouseDownPosX;
    @Shadow private int mouseCheckPosX;
    @Shadow private long prevMouseCheckTimeNano;
    private boolean pmradar$mapMouseHeld;
    private int pmradar$dualModeMapElementPanel;
    private boolean pmradar$duplicatingDualModeMapObject;
    private final BufferSource pmradar$panelObjectBuffers = MultiBufferSource.immediate(new ByteBufferBuilder(4096));

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0,
            remap = false
    )
    private int pmradar$captureRenderMouseX(int mouseX) {
        this.pmradar$renderMouseX = mouseX;
        return mouseX;
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1,
            require = 0,
            remap = false
    )
    private int pmradar$hideRenderMouseYBehindRadarTools(int mouseY) {
        this.pmradar$renderMouseY = mouseY;
        return WorldMapRadarLegendOverlay.xaeroRenderMouseY((Screen) (Object) this, this.pmradar$renderMouseX, mouseY);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void pmradar$beginDualModeCapture(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        WorldMapRadarLegendOverlay.drawDualModeBackground((Screen) (Object) this, guiGraphics);
        WorldMapRadarLegendOverlay.beginXaeroWorldMapRender((Screen) (Object) this);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 5),
            require = 0,
            remap = false
    )
    private void pmradar$renderRadarLayer(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        WorldMapRadarLegendOverlay.translateDualModeMainMapElements(guiGraphics);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/graphics/ImprovedFramebuffer;bindDefaultFramebuffer(Lnet/minecraft/client/Minecraft;)V",
                    shift = At.Shift.AFTER
            ),
            require = 0,
            remap = false
    )
    private void pmradar$clipMainMapToBottomHalf(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        WorldMapRadarLegendOverlay.enableDualModeMainMapScissor();
    }

    @ModifyArgs(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/GuiMap;renderTexturedModalRect(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFIIFFFFFFFF)V"),
            require = 0,
            remap = false
    )
    private void pmradar$moveMainMapTextureToBottomHalf(Args args) {
        if (!WorldMapRadarLegendOverlay.shouldTransformXaeroDualModeMap()) {
            return;
        }

        float y = args.get(3);
        args.set(3, y + WorldMapRadarLegendOverlay.dualModeMainMapBlitYOffset(this.scale));
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/radar/tracker/PlayerTrackerMapElementRenderer;update(Lnet/minecraft/client/Minecraft;)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0,
            remap = false
    )
    private void pmradar$captureCleanDualModeBaseMap(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (StormOverlayData.isDisplayEnabled() && StormOverlayData.isDualModeEnabled()) {
            WorldMapRadarLegendOverlay.captureDualModeSourceMap();
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/Minecraft;F)Lxaero/map/element/HoveredMapElementHolder;"
            ),
            require = 0,
            remap = false
    )
    private HoveredMapElementHolder<?, ?> pmradar$renderCurrentXaeroMapElementsInDualPanels(
            MapElementRenderHandler mapElementRenderHandler,
            GuiMap guiMap,
            GuiGraphics guiGraphics,
            BufferSource renderTypeBuffers,
            MultiTextureRenderTypeRendererProvider rendererProvider,
            double cameraX,
            double cameraZ,
            int screenWidth,
            int screenHeight,
            double screenSizeBasedScale,
            double scale,
            double playerDimDiv,
            double mousePosX,
            double mousePosZ,
            float brightness,
            boolean cave,
            HoveredMapElementHolder<?, ?> viewed,
            Minecraft minecraft,
            float partialTicks
    ) {
        WorldMapRadarLegendOverlay.drawStationLabelsBeforeXaeroIcons((Screen) (Object) this, guiGraphics);

        HoveredMapElementHolder<?, ?> result;
        if (StormOverlayData.isDisplayEnabled() && StormOverlayData.isDualModeEnabled()) {
            if (minecraft == null || minecraft.getWindow() == null) {
                result = mapElementRenderHandler.render(
                        guiMap,
                        guiGraphics,
                        renderTypeBuffers,
                        rendererProvider,
                        cameraX,
                        cameraZ,
                        screenWidth,
                        screenHeight,
                        screenSizeBasedScale,
                        scale,
                        playerDimDiv,
                        mousePosX,
                        mousePosZ,
                        brightness,
                        cave,
                        viewed,
                        minecraft,
                        partialTicks
                );
            } else {
                WorldMapRadarLegendOverlay.disableDualModeMainMapScissor();
                WorldMapRadarLegendOverlay.renderDualModeMapsBeforeXaeroElements((Screen) (Object) this, guiGraphics);

                int width = minecraft.getWindow().getGuiScaledWidth();
                int height = minecraft.getWindow().getGuiScaledHeight();
                int upperBottom = WorldMapRadarLegendOverlay.dualModeGuiPanelHeight();
                int lowerTop = WorldMapRadarLegendOverlay.dualModeLowerGuiPanelTop();
                HoveredMapElementHolder<?, ?> upperViewed = null;
                HoveredMapElementHolder<?, ?> lowerViewed = null;

                // Each panel needs Xaero's full provider/render pass, including per-entity transforms.
                WorldMapRadarLegendOverlay.beginDualModeCurrentXaeroMapElementRender();
                try {
                    this.pmradar$dualModeMapElementPanel = 1;
                    guiGraphics.enableScissor(0, 0, width, upperBottom);
                    guiGraphics.pose().pushPose();
                    try {
                        WorldMapRadarLegendOverlay.translateXaeroElementsToUpperPanel(guiGraphics.pose());
                        guiGraphics.pose().translate(0.0D, 0.0D, WorldMapRadarLegendOverlay.dualModeXaeroRadarElementZOffset());
                        upperViewed = mapElementRenderHandler.render(
                                guiMap,
                                guiGraphics,
                                renderTypeBuffers,
                                rendererProvider,
                                cameraX,
                                cameraZ,
                                screenWidth,
                                screenHeight,
                                screenSizeBasedScale,
                                scale,
                                playerDimDiv,
                                mousePosX,
                                mousePosZ,
                                brightness,
                                cave,
                                viewed,
                                minecraft,
                                partialTicks
                        );
                        guiGraphics.flush();
                        renderTypeBuffers.endBatch();
                    } finally {
                        guiGraphics.pose().popPose();
                        guiGraphics.disableScissor();
                    }

                    this.pmradar$dualModeMapElementPanel = 2;
                    WorldMapRadarLegendOverlay.prepareDualModeCurrentXaeroMapElementRenderPass();
                    guiGraphics.enableScissor(0, lowerTop, width, Math.min(height, lowerTop + upperBottom));
                    guiGraphics.pose().pushPose();
                    try {
                        guiGraphics.pose().translate(0.0D, 0.0D, WorldMapRadarLegendOverlay.dualModeXaeroRadarElementZOffset());
                        lowerViewed = mapElementRenderHandler.render(
                                guiMap,
                                guiGraphics,
                                renderTypeBuffers,
                                rendererProvider,
                                cameraX,
                                cameraZ,
                                screenWidth,
                                screenHeight,
                                screenSizeBasedScale,
                                scale,
                                playerDimDiv,
                                mousePosX,
                                mousePosZ,
                                brightness,
                                cave,
                                viewed,
                                minecraft,
                                partialTicks
                        );
                        guiGraphics.flush();
                        renderTypeBuffers.endBatch();
                    } finally {
                        guiGraphics.pose().popPose();
                        guiGraphics.disableScissor();
                    }
                } finally {
                    this.pmradar$dualModeMapElementPanel = 0;
                    WorldMapRadarLegendOverlay.endDualModeCurrentXaeroMapElementRender();
                }

                result = lowerViewed != null ? lowerViewed : upperViewed;
            }
        } else {
            result = mapElementRenderHandler.render(
                    guiMap,
                    guiGraphics,
                    renderTypeBuffers,
                    rendererProvider,
                    cameraX,
                    cameraZ,
                    screenWidth,
                    screenHeight,
                    screenSizeBasedScale,
                    scale,
                    playerDimDiv,
                    mousePosX,
                    mousePosZ,
                    brightness,
                    cave,
                    viewed,
                    minecraft,
                    partialTicks
            );
        }

        return result;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
                    ordinal = 2,
                    shift = At.Shift.AFTER
            ),
            require = 0,
            remap = false
    )
    private void pmradar$captureCleanDualModeMap(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (StormOverlayData.isDisplayEnabled() && StormOverlayData.isDualModeEnabled()) {
            if (!WorldMapRadarLegendOverlay.isDualModeSourceMapReady()) {
                WorldMapRadarLegendOverlay.captureDualModeSourceMap();
            }
        }

        WorldMapRadarLegendOverlay.disableDualModeMainMapScissor();
        WorldMapRadarLegendOverlay.renderRadarLayer((Screen) (Object) this, guiGraphics);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
                    ordinal = 3,
                    shift = At.Shift.AFTER
            ),
            require = 0,
            remap = false
    )
    private void pmradar$drawControlsAfterXaeroOverlays(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        WorldMapRadarLegendOverlay.drawPersistentControlsBeforeXaeroPopups(
                (Screen) (Object) this, guiGraphics, this.pmradar$renderMouseX, this.pmradar$renderMouseY
        );
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lxaero/map/misc/Misc;getMouseY(Lnet/minecraft/client/Minecraft;Z)D", ordinal = 0),
            require = 0,
            remap = false
    )
    private double pmradar$adjustDualModeRenderMouseY(Minecraft minecraft, boolean vivecraft) {
        return WorldMapRadarLegendOverlay.xaeroRenderRawMouseY(
                (Screen) (Object) this,
                this.pmradar$renderMouseX,
                Misc.getMouseY(minecraft, vivecraft),
                this.pmradar$mapMouseHeld || this.mouseDownPosX != -1
        );
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lxaero/map/misc/Misc;getMouseX(Lnet/minecraft/client/Minecraft;Z)D", ordinal = 0),
            require = 0,
            remap = false
    )
    private double pmradar$freezeMapMouseXOverTools(Minecraft minecraft, boolean vivecraft) {
        return WorldMapRadarLegendOverlay.xaeroRenderRawMouseX((Screen) (Object) this,
                this.pmradar$renderMouseX, Misc.getMouseX(minecraft, vivecraft), Misc.getMouseY(minecraft, vivecraft));
    }

    @Inject(
            method = "mouseClicked(DDI)Z",
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void pmradar$beginDualModeMapMousePress(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (button == 0) {
            this.pmradar$mapMouseHeld = true;
            this.mouseCheckPosX = -1;
            this.prevMouseCheckTimeNano = -1L;
        }
        WorldMapRadarLegendOverlay.beginDualModeMapMousePress(button, Misc.getMouseY(Minecraft.getInstance(), SupportMods.vivecraft));
    }

    @Redirect(
            method = "mouseClicked(DDI)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/misc/Misc;getMouseY(Lnet/minecraft/client/Minecraft;Z)D"),
            require = 0,
            remap = false
    )
    private double pmradar$adjustDualModeMouseClickedY(Minecraft minecraft, boolean vivecraft) {
        return WorldMapRadarLegendOverlay.dualModeAdjustedRawMouseY(Misc.getMouseY(minecraft, vivecraft));
    }

    @Redirect(
            method = "mouseReleased(DDI)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/misc/Misc;getMouseY(Lnet/minecraft/client/Minecraft;Z)D"),
            require = 0,
            remap = false
    )
    private double pmradar$adjustDualModeMouseReleasedY(Minecraft minecraft, boolean vivecraft) {
        return WorldMapRadarLegendOverlay.dualModeAdjustedRawMouseY(Misc.getMouseY(minecraft, vivecraft));
    }

    @Inject(
            method = "mouseReleased(DDI)Z",
            at = @At("RETURN"),
            require = 0,
            remap = false
    )
    private void pmradar$endDualModeMapMousePress(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (button == 0) {
            this.pmradar$mapMouseHeld = false;
        }
        WorldMapRadarLegendOverlay.endDualModeMapMousePress(button);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("RETURN"),
            require = 0,
            remap = false
    )
    private void pmradar$ensureMainMapScissorDisabled(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        this.pmradar$dualModeMapElementPanel = 0;
        WorldMapRadarLegendOverlay.disableDualModeMainMapScissor();
    }

    @Inject(
            method = "drawObjectOnMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDFDFFIIIII)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void pmradar$captureDualModeXaeroMapObject(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            double x,
            double z,
            float angle,
            double scale,
            float offsetX,
            float offsetY,
            int textureX,
            int textureY,
            int width,
            int height,
            int filter,
            CallbackInfo callbackInfo
    ) {
        if (!WorldMapRadarLegendOverlay.shouldTransformXaeroDualModeMap()
                || this.pmradar$dualModeMapElementPanel != 0
                || this.pmradar$duplicatingDualModeMapObject) {
            return;
        }

        double duplicateYOffset = WorldMapRadarLegendOverlay.dualModeTopMapElementLocalYOffset(this.scale);
        if (duplicateYOffset == 0.0D) {
            return;
        }

        this.pmradar$duplicatingDualModeMapObject = true;
        int[] previousScissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, previousScissor);
        boolean wasScissored = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int framebufferWidth = Minecraft.getInstance().getWindow().getWidth();
        int framebufferHeight = Minecraft.getInstance().getWindow().getHeight();
        int panelHeight = Math.min(540, framebufferHeight / 2);
        try {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            for (int panel = 0; panel < 2; panel++) {
                GL11.glScissor(0, panel == 0 ? framebufferHeight - panelHeight : 0,
                        framebufferWidth, panelHeight);
                matrixStack.pushPose();
                try {
                    if (panel == 0) {
                        WorldMapRadarLegendOverlay.translateXaeroElementsToUpperPanel(matrixStack);
                    }
                    VertexConsumer panelConsumer = this.pmradar$panelObjectBuffers.getBuffer(CustomRenderTypes.GUI_BILINEAR);
                    boolean playerArrow = textureY == 0
                            && ((textureX == 0 && width == 26 && height == 28)
                            || (textureX == 26 && width == 54 && height == 13));
                    Matrix4f pose = matrixStack.last().pose();
                    Matrix4f projection = RenderSystem.getProjectionMatrix();
                    double projectedY = projection.m11() * (pose.m01() * x + pose.m11() * z + pose.m31())
                            + projection.m31();
                    double screenY = (1.0D - projectedY) * framebufferHeight * 0.5D;
                    boolean aboveScreen = panel == 0 && screenY < 0.0D;
                    boolean belowScreen = panel == 1 && screenY > framebufferHeight;
                    double pixelsPerLocalY = -projection.m11() * pose.m11() * framebufferHeight * 0.5D;
                    if (playerArrow && (aboveScreen || belowScreen) && pixelsPerLocalY != 0.0D) {
                        double edgeY = aboveScreen ? 0.0D : framebufferHeight;
                        double edgeZ = z + (edgeY - screenY) / pixelsPerLocalY;
                        float edgeAngle = aboveScreen ? 180.0F : 0.0F;
                        if (textureX == 26) {
                            float normalizedAngle = (angle % 360.0F + 360.0F) % 360.0F;
                            if (normalizedAngle > 0.0F && normalizedAngle < 180.0F) {
                                edgeAngle = aboveScreen ? 135.0F : 45.0F;
                            } else if (normalizedAngle > 180.0F) {
                                edgeAngle = aboveScreen ? 225.0F : 315.0F;
                            }
                        }
                        // Xaero's own off-screen sprite; the shared divider is deliberately not an edge.
                        this.drawObjectOnMap(matrixStack, panelConsumer, x, edgeZ,
                                edgeAngle, scale,
                                27.0F, 13.0F, 26, 0, 54, 13, filter);
                    } else {
                        this.drawObjectOnMap(matrixStack, panelConsumer,
                                x, z, angle, scale, offsetX, offsetY, textureX, textureY, width, height, filter);
                    }
                    this.pmradar$panelObjectBuffers.endBatch();
                } finally {
                    matrixStack.popPose();
                }
            }
        } finally {
            GL11.glScissor(previousScissor[0], previousScissor[1], previousScissor[2], previousScissor[3]);
            if (!wasScissored) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
            this.pmradar$duplicatingDualModeMapObject = false;
        }
        callbackInfo.cancel();
    }

    @Inject(
            method = "drawArrowOnMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDFD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void pmradar$captureDualModePlayerArrow(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            double x,
            double z,
            float angle,
            double scale,
            CallbackInfo callbackInfo
    ) {
        if (WorldMapRadarLegendOverlay.captureDualModePlayerArrow(
                false,
                x,
                z,
                angle,
                scale,
                this.colourBuffer[0],
                this.colourBuffer[1],
                this.colourBuffer[2],
                this.colourBuffer[3]
        )) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "drawFarArrowOnMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDFD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void pmradar$captureDualModeFarPlayerArrow(
            PoseStack matrixStack,
            VertexConsumer vertexConsumer,
            double x,
            double z,
            float angle,
            double scale,
            CallbackInfo callbackInfo
    ) {
        if (WorldMapRadarLegendOverlay.captureDualModePlayerArrow(
                true,
                x,
                z,
                angle,
                scale,
                this.colourBuffer[0],
                this.colourBuffer[1],
                this.colourBuffer[2],
                this.colourBuffer[3]
        )) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V"),
            require = 0,
            remap = false
    )
    private void pmradar$drawXaeroCenteredLabel(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            int x,
            int y,
            int color,
            float red,
            float green,
            float blue,
            float alpha,
            VertexConsumer vertexConsumer
    ) {
        if (StormOverlayData.isDisplayEnabled() && StormOverlayData.isDualModeEnabled()) {
            if (WorldMapRadarLegendOverlay.captureDualModeXaeroLabel((Screen) (Object) this, text, x, y)) {
                return;
            }

            MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, font, text, x, y, color, red, green, blue, alpha, vertexConsumer);
            return;
        }

        if (isZoomText(text) && StormOverlayData.isDisplayEnabled()) {
            y -= 27;
        }

        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, font, text, x, y, color, red, green, blue, alpha, vertexConsumer);
    }

    private static boolean isCoordinateText(String text) {
        if (text == null || !text.startsWith("X: ")) {
            return false;
        }

        return text.contains(" Z:");
    }

    private static boolean isZoomText(String text) {
        if (text == null || text.length() < 2 || text.length() > 10 || !text.endsWith("x")) {
            return false;
        }

        boolean hasDigit = false;
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                hasDigit = true;
                continue;
            }

            if (c != '.') {
                return false;
            }
        }

        return hasDigit;
    }
}
