package com.leopard.pmradar.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.util.ColorTables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ScreenEvent;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorldMapRadarLegendOverlay {
    private static final String XAERO_WORLD_MAP_SCREEN = "xaero.map.gui.GuiMap";
    private static final int DEFAULT_RADAR_TEXTURE_SIZE = 100;
    private static final int MIN_RADAR_TEXTURE_SIZE = 32;
    private static final int MAX_RADAR_TEXTURE_SIZE = 200;
    private static final float REFLECTIVITY_LEGEND_MIN_DBZ = 3.0F;
    private static final float REFLECTIVITY_LEGEND_MAX_DBZ = 80.0F;
    private static final float VELOCITY_LEGEND_MAX_MPH = 245.0F;
    private static final float CORRELATION_MIN = 0.208F;
    private static final float CORRELATION_MAX = 1.048F;
    private static final double RADAR_BLIND_SPOT_RADIUS_BLOCKS = 48.0D;
    private static final int RADAR_TOOLS_BUTTON_SIZE = 24;
    private static final int RADAR_TOOLS_BOTTOM_MARGIN = 4;
    private static final int BOTTOM_CONTROLS_PADDING = 6;
    private static final int BOTTOM_CONTROLS_STORM_BAR_HEIGHT = 15;
    private static final String DISPLAY_BUTTON_TEXT = "RADAR";
    private static final int DISPLAY_BUTTON_TEXT_Y_OFFSET = 0;
    private static final int DISPLAY_BUTTON_WIDTH_TRIM = 0;
    private static final int CONTROL_TEXT_BORDER_PIXELS = 1;
    private static final int CONTROL_TEXT_HORIZONTAL_PADDING_PIXELS = 4;
    private static final int CONTROL_TEXT_VERTICAL_PADDING_PIXELS = 2;
    private static final boolean CONTROL_TEXT_BOUNDS_DEBUG = false;
    private static final int CONTROL_TEXT_BOUNDS_FILL = 0xFFA131A1;
    private static final int CONTROL_TEXT_BOUNDS_TEXT = 0xFFB76DFD;
    private static final int XAERO_LEFT_TOOLBAR_SHIFT = RADAR_TOOLS_BUTTON_SIZE + 6;
    private static final int XAERO_LEFT_TOOLBAR_MAX_X = 32;
    private static final int XAERO_LEFT_TOOLBAR_MAX_WIDTH = 100;
    private static final int XAERO_LEFT_TOOLBAR_MAX_BOTTOM_OFFSET = 300;
    private static final int RADAR_TOOLS_ICON_SIZE = 64;
    private static final int SETTINGS_MODAL_WIDTH = 208;
    private static final int SETTINGS_MODAL_HEIGHT = 150;
    private static final int SETTINGS_MODAL_MIN_WIDTH = SETTINGS_MODAL_WIDTH;
    private static final int SETTINGS_MODAL_MIN_HEIGHT = SETTINGS_MODAL_HEIGHT;
    private static final int SETTINGS_MODAL_RESIZE_HANDLE_SIZE = 14;
    private static final int SETTINGS_MODAL_DRAG_HANDLE_HEIGHT = 28;
    private static final int SETTINGS_MODAL_SCREEN_MARGIN = 4;
    private static final int SETTINGS_MODAL_CONTENT_TOP_OFFSET = 31;
    private static final int SETTINGS_MODAL_CONTENT_BOTTOM_INSET = 8;
    private static final int SETTINGS_MODAL_ROW_TOP_OFFSET = 38;
    private static final int SETTINGS_MODAL_ROW_HORIZONTAL_INSET = 10;
    private static final int SETTINGS_MODAL_ROW_HEIGHT = 22;
    private static final int SETTINGS_MODAL_ROW_STEP = 26;
    private static final int SETTINGS_MODAL_SCROLL_PIXELS = 12;
    private static final int SETTINGS_TOGGLE_BOX_SIZE = 11;
    private static final int SETTINGS_TOGGLE_BOX_RIGHT_INSET = 18;
    private static final int SETTINGS_TOGGLE_BOX_TOP_OFFSET = 6;
    private static final int SETTINGS_MAIN_ROW_COUNT = 5;
    private static final int SETTINGS_ANIMATIONS_ROW_COUNT = 2;
    private static final int DUAL_MAP_SIDE_MARGIN = 0;
    private static final int DUAL_MAP_VERTICAL_MARGIN = 0;
    private static final int DUAL_MAP_PANEL_GAP = 0;
    private static final int DUAL_MAP_PANEL_PADDING = 5;
    private static final int DUAL_MAP_CONTROL_BOTTOM_PADDING = 5;
    private static final int DUAL_MAP_PANEL_HEIGHT_SCREEN_PIXELS = 540;
    private static final int DUAL_MAP_LOWER_PANEL_Y_OFFSET = 0;
    private static final int DUAL_MAP_SEAM_OVERLAP = 0;
    private static final double DUAL_MAP_XAERO_RADAR_ELEMENT_Z_OFFSET = 1670.0D;
    private static final float WORLD_MAP_RADAR_ALPHA = 0.72F;
    private static final float DUAL_MAP_RADAR_ALPHA = WORLD_MAP_RADAR_ALPHA;
    private static final int DUAL_MAP_TEXT_SAFE_TOP = 0;
    private static final double RADAR_TOOLS_OPEN_ANIMATION_SECONDS = 1.05D;
    private static final double RADAR_TOOLS_OPEN_ICON_TRAVEL_END = 0.58D;
    private static final double RADAR_TOOLS_OPEN_ICON_FADE_END = 0.76D;
    private static final double RADAR_TOOLS_OPEN_MENU_FADE_START = 0.78D;
    private static final double RADAR_TOOLS_OPEN_ICON_MAX_SCALE = 2.85D;
    private static final double RADAR_TOOLS_OPEN_ICON_MAX_TILT_DEGREES = 34.0D;
    private static final double RADAR_TOOLS_CLOSE_ANIMATION_SECONDS = 0.75D;
    private static final double RADAR_TOOLS_CLOSE_MIN_SCALE = 0.035D;
    private static final double RADAR_TOOLS_CLOSE_SHRINK_END = 0.86D;
    private static final int RADAR_TOOLS_CLOSE_GENIE_SLICE_HEIGHT = 4;
    private static final int UI_BLUE = 0xFF55AFFF;
    private static final int UI_RED = 0xFFFF5555;
    private static final int UI_BLUE_DARK = 0xFF0F2733;
    private static final int UI_BLACK = 0xFF1E1E1E;
    private static final int UI_BLACK_ALPHA_25 = 0x401E1E1E;
    private static final int UI_BLACK_ALPHA_50 = 0x801E1E1E;
    private static final int UI_BLACK_ALPHA_80 = 0xCC1E1E1E;
    private static final int UI_BLACK_ALPHA_88 = 0xE01E1E1E;
    private static final int UI_BACKGROUND = UI_BLACK;
    private static final int MODE_MENU_BACKGROUND = UI_BLACK;
    private static final int MODE_MENU_BORDER = 0xFFD8D8D8;
    private static final int MODE_MENU_TEXT = 0xFFE0E0E0;
    private static final int MODE_MENU_LIST_TOP_PADDING = 5;
    private static final int MODE_MENU_LIST_BOTTOM_PADDING = 4;
    private static final int MODE_MENU_LIST_ROW_GAP = 4;
    private static final int MODE_MENU_SELECTED_TEXT_Y_OFFSET = 2;
    private static final double MODE_MENU_ANIMATION_SECONDS = 0.16D;
    private static final double MODE_MENU_LIST_REVEAL_START = 0.45D;
    private static final double MODE_MENU_CLICK_CHANGE_ANIMATION_SECONDS = 0.22D;
    private static final double MODE_MENU_SCROLL_CHANGE_ANIMATION_SECONDS = 0.08D;
    private static final ResourceLocation VANILLA_ASCII_FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/font/ascii.png");
    private static final ResourceLocation RADAR_TOOLS_ICON = ResourceLocation.fromNamespaceAndPath("pmradar", "textures/gui/radartools_icon.png");
    private static final ResourceLocation LIGHTNING_ICON = ResourceLocation.fromNamespaceAndPath("pmradar", "textures/gui/thunder.png");
    private static final int LIGHTNING_ICON_TEXTURE_SIZE = 64;
    private static final int LIGHTNING_ICON_DRAW_SIZE = 7;
    private static final Color REFLECTIVITY_BASE = new Color(12, 28, 32);

    private static Field hiddenUiField;
    private static Field cameraXField;
    private static Field cameraZField;
    private static Field scaleField;
    private static Field screenScaleField;
    private static Method getMapProcessorMethod;
    private static Method getMapWorldMethod;
    private static Method getCurrentDimensionMethod;
    private static Method getDimIdMethod;
    private static int buttonLeft;
    private static int buttonTop;
    private static int buttonRight;
    private static int buttonBottom;
    private static int toolsButtonLeft;
    private static int toolsButtonTop;
    private static int toolsButtonRight;
    private static int toolsButtonBottom;
    private static int settingsModalLeft;
    private static int settingsModalTop;
    private static int settingsModalRight;
    private static int settingsModalBottom;
    private static int settingsCloseLeft;
    private static int settingsCloseTop;
    private static int settingsCloseRight;
    private static int settingsCloseBottom;
    private static int settingsBackLeft;
    private static int settingsBackTop;
    private static int settingsBackRight;
    private static int settingsBackBottom;
    private static int settingsResizeLeft;
    private static int settingsResizeTop;
    private static int settingsResizeRight;
    private static int settingsResizeBottom;
    private static int settingsScrollTrackLeft;
    private static int settingsScrollTrackTop;
    private static int settingsScrollTrackRight;
    private static int settingsScrollTrackBottom;
    private static int settingsScrollThumbLeft;
    private static int settingsScrollThumbTop;
    private static int settingsScrollThumbRight;
    private static int settingsScrollThumbBottom;
    private static int displayEnabledLeft;
    private static int displayEnabledTop;
    private static int displayEnabledRight;
    private static int displayEnabledBottom;
    private static int dualModeLeft;
    private static int dualModeTop;
    private static int dualModeRight;
    private static int dualModeBottom;
    private static int radarLocationsLeft;
    private static int radarLocationsTop;
    private static int radarLocationsRight;
    private static int radarLocationsBottom;
    private static int lightningLeft;
    private static int lightningTop;
    private static int lightningRight;
    private static int lightningBottom;
    private static int animationsLeft;
    private static int animationsTop;
    private static int animationsRight;
    private static int animationsBottom;
    private static int menuAnimationsLeft;
    private static int menuAnimationsTop;
    private static int menuAnimationsRight;
    private static int menuAnimationsBottom;
    private static int textSwapAnimationsLeft;
    private static int textSwapAnimationsTop;
    private static int textSwapAnimationsRight;
    private static int textSwapAnimationsBottom;
    private static int modeLeft;
    private static int modeTop;
    private static int modeRight;
    private static int modeBottom;
    private static int modeMenuLeft;
    private static int modeMenuTop;
    private static int modeMenuRight;
    private static int modeMenuBottom;
    private static boolean modeMenuOpen;
    private static double modeMenuAnimationProgress;
    private static long modeMenuAnimationLastNanos;
    private static StormOverlayData.RadarMode modeChangeFrom;
    private static StormOverlayData.RadarMode modeChangeTo;
    private static List<StormOverlayData.RadarMode> modeVisualStack = List.of();
    private static List<StormOverlayData.RadarMode> modeChangeFromStack = List.of();
    private static List<StormOverlayData.RadarMode> modeChangeToStack = List.of();
    private static double modeChangeAnimationProgress = 1.0D;
    private static double modeChangeAnimationSeconds = MODE_MENU_CLICK_CHANGE_ANIMATION_SECONDS;
    private static long modeChangeAnimationLastNanos;
    private static boolean settingsOpen;
    private static boolean radarToolsOpenAnimationActive;
    private static long radarToolsOpenAnimationStartNanos;
    private static double radarToolsOpenStartSide = 1.0D;
    private static double radarToolsOpenStartYRatio = 0.24D;
    private static double radarToolsOpenCurveDirection = 1.0D;
    private static double radarToolsOpenStartRotationDegrees = 26.0D;
    private static boolean radarToolsCloseAnimationActive;
    private static long radarToolsCloseAnimationStartNanos;
    private static double radarToolsCloseStartLeft;
    private static double radarToolsCloseStartTop;
    private static double radarToolsCloseCurveDirection = 1.0D;
    private static double radarToolsCloseRotationPeak = 10.0D;
    private static double radarToolsCloseWobblePhase;
    private static double radarToolsCloseWarpStrength = 0.3D;
    private static boolean settingsModalDragging;
    private static double settingsModalDragOffsetX;
    private static double settingsModalDragOffsetY;
    private static boolean settingsModalResizing;
    private static boolean settingsScrollDragging;
    private static double settingsScrollDragOffsetY;
    private static double settingsModalResizeStartMouseX;
    private static double settingsModalResizeStartMouseY;
    private static double settingsModalResizeStartLeft;
    private static double settingsModalResizeStartTop;
    private static int settingsModalResizeStartWidth;
    private static int settingsModalResizeStartHeight;
    private static int settingsModalWidth = SETTINGS_MODAL_WIDTH;
    private static int settingsModalHeight = SETTINGS_MODAL_HEIGHT;
    private static double settingsModalOffsetX;
    private static double settingsModalOffsetY;
    private static double settingsMenuScrollOffset;
    private static SettingsMenuPage settingsMenuPage = SettingsMenuPage.MAIN;
    private static boolean stationLabelsOpen;
    private static boolean radarLayerRenderedThisFrame;
    private static List<StationLabelBounds> stationLabelBounds = List.of();
    private static List<ModeLabelBounds> modeLabelBounds = List.of();
    private static List<ModeControlBounds> modeControlBounds = List.of();
    private static List<DisplayControlBounds> displayControlBounds = List.of();
    private static ModeControlTarget modeMenuTarget = ModeControlTarget.SINGLE;
    private static DualModeXaeroLabel dualModeCoordinateLabel;
    private static DualModeXaeroLabel dualModeBiomeLabel;
    private static DualModeXaeroLabel dualModeZoomLabel;
    private static int dualModeScreenTextureId = -1;
    private static int dualModeCopyFramebufferId = -1;
    private static int dualModeScreenTextureWidth;
    private static int dualModeScreenTextureHeight;
    private static boolean dualModeScreenTextureReady;
    private static int radarToolsCloseTextureId = -1;
    private static int radarToolsCloseTextureWidth;
    private static int radarToolsCloseTextureHeight;
    private static boolean radarToolsCloseTextureReady;
    private static int dualModeMouseDragPanel;
    private static boolean dualModeMouseDragReleasePending;
    private static double lastMapMouseX;
    private static double lastMapMouseY;
    private static boolean asciiGlyphPixelBoundsLoaded;
    private static Map<Integer, GlyphPixelBounds> asciiGlyphPixelBounds = Map.of();
    private static final Map<RadarTextureKey, SiteTextureCache> radarTextures = new HashMap<>();
    private static final Map<RadarTextureKey, PendingSiteTextureBuild> pendingRadarTextures = new HashMap<>();

    private WorldMapRadarLegendOverlay() {
    }

    public static void renderRadarLayer(Screen screen, GuiGraphics guiGraphics) {
        if (!isWorldMap(screen) || isHiddenUi(screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        moveXaeroLeftToolbarUp(screen, height);
        MapView mapView = mapView(screen, width, height);
        if (mapView == null) {
            return;
        }

        boolean displayEnabled = StormOverlayData.isDisplayEnabled();
        boolean radarLocationsVisible = StormOverlayData.shouldShowRadarLocations();
        if (!displayEnabled && !radarLocationsVisible) {
            clearRadarTextures();
            return;
        }
        if (displayEnabled && StormOverlayData.isDualModeEnabled()) {
            radarLayerRenderedThisFrame = false;
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().last().pose().identity();
            List<StormOverlayData.RadarSiteView> radarSites = StormOverlayData.radarSiteViews(mapView.dimension());
            boolean drawRadarLocationMarkers = shouldDrawRadarLocationMarkers();
            if (displayEnabled) {
                drawDynamicRadar(guiGraphics, mapView, radarSites, drawRadarLocationMarkers);
            }
            guiGraphics.flush();
        } finally {
            guiGraphics.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        radarLayerRenderedThisFrame = true;
    }

    public static void render(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isWorldMap(screen) || isHiddenUi(screen)) {
            clearModeBounds();
            radarLayerRenderedThisFrame = false;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null) {
            clearModeBounds();
            radarLayerRenderedThisFrame = false;
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        stationLabelsOpen = StormOverlayData.isStationLabelsOpen();
        MapView mapView = mapView(screen, width, height);
        boolean displayEnabled = StormOverlayData.isDisplayEnabled();
        boolean radarLocationsVisible = StormOverlayData.shouldShowRadarLocations();
        List<StormOverlayData.RadarSiteView> radarSites = mapView == null || (!displayEnabled && !radarLocationsVisible)
                ? List.of()
                : StormOverlayData.radarSiteViews(mapView.dimension());
        BottomControlsLayout controls = bottomControlsLayout(font, width, height);
        BottomControlsLayout activeControls = controls;
        ToolsButtonLayout tools = toolsButtonLayout(width, height);
        boolean dualModeActive = displayEnabled && StormOverlayData.isDualModeEnabled() && mapView != null;

        if (mapView != null && radarLocationsVisible && !dualModeActive) {
            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().last().pose().identity();
                guiGraphics.pose().translate(0.0F, 0.0F, 420.0F);
                if (displayEnabled) {
                    drawLightningStrikes(guiGraphics, mapView, radarSites, StormOverlayData.lightningStrikeViews(mapView.dimension()));
                }
                drawStationLabels(guiGraphics, font, mapView, radarSites);
                guiGraphics.flush();
            } finally {
                guiGraphics.pose().popPose();
            }
        } else {
            stationLabelBounds = List.of();
        }
        if (dualModeActive) {
            if (!dualModeScreenTextureReady) {
                guiGraphics.flush();
                captureDualModeSourceMap(minecraft);
            }
        } else {
            dualModeScreenTextureReady = false;
        }
        radarLayerRenderedThisFrame = false;

        RenderSystem.disableDepthTest();
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(0.0F, 0.0F, 700.0F);
            toolsButtonLeft = tools.x();
            toolsButtonTop = tools.y();
            toolsButtonRight = tools.x() + tools.size();
            toolsButtonBottom = tools.y() + tools.size();

            if (displayEnabled) {
                if (dualModeActive) {
                    activeControls = drawDualModeMapDisplays(guiGraphics, font, width, height, mapView, radarSites, event.getMouseX(), event.getMouseY());
                    drawDualModeXaeroLabels(guiGraphics, font, width, height);
                } else {
                    drawInteractiveRadarControlRow(guiGraphics, font, width, controls, event.getMouseX(), event.getMouseY());
                }
            } else {
                buttonLeft = 0;
                buttonTop = 0;
                buttonRight = 0;
                buttonBottom = 0;
                modeLeft = 0;
                modeTop = 0;
                modeRight = 0;
                modeBottom = 0;
                modeControlBounds = List.of();
                displayControlBounds = List.of();
                modeMenuOpen = false;
                clearModeMenuBounds();
                resetModeMenuAnimation();
                resetModeChangeAnimation();
            }
            drawRadarToolsButton(guiGraphics, tools.x(), tools.y(), tools.size(), displayEnabled);

            if (settingsOpen || radarToolsCloseAnimationActive) {
                guiGraphics.pose().pushPose();
                try {
                    guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
                    if (settingsOpen) {
                        double animationProgress = updateRadarToolsOpenAnimation();
                        double panelAlpha = radarToolsPanelAlpha(animationProgress);
                        if (panelAlpha > 0.0D) {
                            drawSettingsModal(guiGraphics, font, width, height, panelAlpha, event.getMouseX(), event.getMouseY());
                        } else {
                            clearSettingsBounds();
                        }
                        drawRadarToolsOpenAnimation(guiGraphics, width, height, tools.size(), animationProgress);
                    } else {
                        double animationProgress = updateRadarToolsCloseAnimation();
                        drawRadarToolsCloseAnimation(guiGraphics, font, width, height, tools, animationProgress);
                    }
                } finally {
                    guiGraphics.pose().popPose();
                }
            }

        } finally {
            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }

        if (displayEnabled) {
            buttonLeft = activeControls.buttonX();
            buttonTop = activeControls.buttonY();
            buttonRight = activeControls.buttonX() + activeControls.buttonWidth();
            buttonBottom = activeControls.buttonY() + activeControls.buttonHeight();

            modeLeft = activeControls.modeLeft();
            modeTop = activeControls.modeTop();
            modeRight = activeControls.modeRight();
            modeBottom = activeControls.modeBottom();
        }
    }

    private static BottomControlsLayout bottomControlsLayout(Font font, int width, int height) {
        int controlHeight = bottomControlsStormBarHeight() + 2;
        int buttonY = Math.max(0, height - DUAL_MAP_CONTROL_BOTTOM_PADDING - controlHeight);
        return bottomControlsLayout(font, width, height, StormOverlayData.getRadarMode(), buttonY);
    }

    private static BottomControlsLayout bottomControlsLayout(
            Font font,
            int width,
            int height,
            StormOverlayData.RadarMode mode,
            int buttonY
    ) {
        return bottomControlsLayoutWithin(font, 0, width, mode, buttonY);
    }

    private static BottomControlsLayout bottomControlsLayoutWithin(
            Font font,
            int left,
            int right,
            StormOverlayData.RadarMode mode,
            int buttonY
    ) {
        int availableWidth = Math.max(1, right - left);
        int barWidth = Math.min(240, Math.max(132, availableWidth / 4));
        int controlPadding = bottomControlsPadding();
        String modeText = mode.displayName().toUpperCase(Locale.ROOT);
        int barHeight = bottomControlsStormBarHeight();
        int controlHeight = barHeight + 2;
        float modeScale = controlTextScale(font, controlHeight);
        double textInset = controlTextInset(font, modeScale, controlHeight);
        int modeWidth = (int) Math.ceil(textPixelWidth(font, modeText, modeScale));
        int buttonWidth = displayButtonWidth(font, modeScale, controlHeight);
        double modeListWidth = modeMenuListWidth(font, modeWidth, modeScale, modeStackFor(mode), textInset);
        int modeClosedWidth = (int) Math.ceil(modeWidth + textInset * 2.0D);
        int widestSide = Math.max(buttonWidth, modeClosedWidth);
        int maxBarWidth = Math.max(48, availableWidth - widestSide * 2 - controlPadding * 2);
        barWidth = Math.min(barWidth, maxBarWidth);
        int barX = left + Math.max(0, (availableWidth - barWidth) / 2);
        int x = barX - controlPadding - buttonWidth;
        double modeBoxLeft = barX + barWidth + controlPadding;
        double modeTextX = modeBoxLeft + textInset;
        double modeClosedRight = modeTextX + modeWidth + textInset;
        int barY = buttonY + 1;
        int buttonHeight = controlHeight;
        int modeY = buttonY;
        int modeHeight = controlHeight;

        return new BottomControlsLayout(
                x,
                buttonY,
                buttonWidth,
                buttonHeight,
                barX,
                barY,
                barWidth,
                barHeight,
                modeY,
                modeWidth,
                modeTextX,
                modeBoxLeft,
                modeClosedRight,
                modeHeight,
                modeScale,
                modeText,
                mode
        );
    }

    private static ToolsButtonLayout toolsButtonLayout(int width, int height) {
        int x = 4;
        int y = Math.max(88, height - RADAR_TOOLS_BUTTON_SIZE - RADAR_TOOLS_BOTTOM_MARGIN);
        return new ToolsButtonLayout(x, y, RADAR_TOOLS_BUTTON_SIZE);
    }

    private static void moveXaeroLeftToolbarUp(Screen screen, int height) {
        if (screen == null || height <= 0) {
            return;
        }

        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget) {
                moveXaeroLeftToolbarWidgetUp(widget, height);
            }
        }
    }

    private static void moveXaeroLeftToolbarWidgetUp(AbstractWidget widget, int height) {
        if (widget.getX() > XAERO_LEFT_TOOLBAR_MAX_X || widget.getWidth() > XAERO_LEFT_TOOLBAR_MAX_WIDTH) {
            return;
        }

        int bottomOffset = height - widget.getY();
        if (bottomOffset < 20
                || bottomOffset > XAERO_LEFT_TOOLBAR_MAX_BOTTOM_OFFSET
                || bottomOffset % 20 != 0) {
            return;
        }

        widget.setY(widget.getY() - XAERO_LEFT_TOOLBAR_SHIFT);
    }

    public static void handleScroll(ScreenEvent.MouseScrolled.Pre event) {
        Screen screen = event.getScreen();
        if (!isWorldMap(screen) || isHiddenUi(screen)) {
            return;
        }

        double scrollDelta = event.getScrollDeltaY();
        if (settingsModalCovers(event.getMouseX(), event.getMouseY())) {
            if (scrollDelta != 0.0D) {
                double maxScroll = maxSettingsMenuScroll();
                if (maxScroll > 0.0D) {
                    settingsMenuScrollOffset = clamp(
                            settingsMenuScrollOffset - scrollDelta * SETTINGS_MODAL_SCROLL_PIXELS,
                            0.0D,
                            maxScroll
                    );
                    clearSettingsRowBounds();
                }
            }

            event.setCanceled(true);
            return;
        }

        ModeControlTarget target = modeControlTargetAt(event.getMouseX(), event.getMouseY());
        if (target == null && modeMenuOpen && isInside(event.getMouseX(), event.getMouseY(), modeMenuLeft, modeMenuTop, modeMenuRight, modeMenuBottom)) {
            target = modeMenuTarget;
        }
        if (target == null) {
            return;
        }

        if (scrollDelta == 0.0D) {
            return;
        }

        StormOverlayData.RadarMode previousMode = modeForTarget(target);
        List<StormOverlayData.RadarMode> stack = currentModeStackFor(previousMode);
        boolean rotateUp = scrollDelta > 0.0D;
        StormOverlayData.RadarMode nextMode = modeSelectedByStackRotation(stack, rotateUp, previousMode);
        if (setModeForTarget(target, nextMode)) {
            applyModeChangeAnimation(target, previousMode, modeForTarget(target), MODE_MENU_SCROLL_CHANGE_ANIMATION_SECONDS, rotateUp);
            clearRadarTextures();
            XaeroStormOverlayRegistrar.tick(true, false);
        }

        event.setCanceled(true);
    }

    public static void handleMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!isWorldMap(screen) || isHiddenUi(screen)) {
            return;
        }

        if (event.getButton() != 0) {
            return;
        }

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        if (settingsOpen) {
            if (isInsideMenuBounds(mouseX, mouseY, settingsCloseLeft, settingsCloseTop, settingsCloseRight, settingsCloseBottom)) {
                closeRadarToolsMenu();
                event.setCanceled(true);
                return;
            }

            if (isInsideMenuBounds(mouseX, mouseY, settingsBackLeft, settingsBackTop, settingsBackRight, settingsBackBottom)) {
                settingsMenuPage = SettingsMenuPage.MAIN;
                settingsMenuScrollOffset = 0.0D;
                clearSettingsBounds();
                event.setCanceled(true);
                return;
            }

            if (isInsideMenuBounds(mouseX, mouseY, settingsScrollThumbLeft, settingsScrollThumbTop, settingsScrollThumbRight, settingsScrollThumbBottom)) {
                startSettingsScrollDrag(mouseY, false);
                event.setCanceled(true);
                return;
            }

            if (isInsideMenuBounds(mouseX, mouseY, settingsScrollTrackLeft, settingsScrollTrackTop, settingsScrollTrackRight, settingsScrollTrackBottom)) {
                startSettingsScrollDrag(mouseY, true);
                event.setCanceled(true);
                return;
            }

            if (isInsideMenuBounds(mouseX, mouseY, settingsResizeLeft, settingsResizeTop, settingsResizeRight, settingsResizeBottom)) {
                startSettingsModalResize(mouseX, mouseY);
                event.setCanceled(true);
                return;
            }

            if (settingsMenuPage == SettingsMenuPage.MAIN && isInsideMenuBounds(mouseX, mouseY, displayEnabledLeft, displayEnabledTop, displayEnabledRight, displayEnabledBottom)) {
                setWeatherDisplayEnabled(!StormOverlayData.isDisplayEnabled());
                event.setCanceled(true);
                return;
            }

            if (settingsMenuPage == SettingsMenuPage.MAIN && isInsideMenuBounds(mouseX, mouseY, dualModeLeft, dualModeTop, dualModeRight, dualModeBottom)) {
                StormOverlayData.toggleDualModeEnabled();
                event.setCanceled(true);
                return;
            }

            if (settingsMenuPage == SettingsMenuPage.MAIN && isInsideMenuBounds(mouseX, mouseY, radarLocationsLeft, radarLocationsTop, radarLocationsRight, radarLocationsBottom)) {
                StormOverlayData.toggleRadarLocationsAlwaysVisible();
                clearRadarTextures();
                XaeroStormOverlayRegistrar.tick(true, true);
                event.setCanceled(true);
                return;
            }

            if (settingsMenuPage == SettingsMenuPage.MAIN && isInsideMenuBounds(mouseX, mouseY, lightningLeft, lightningTop, lightningRight, lightningBottom)) {
                StormOverlayData.toggleLightningEnabled();
                event.setCanceled(true);
                return;
            }

            if (settingsMenuPage == SettingsMenuPage.MAIN && isInsideMenuBounds(mouseX, mouseY, animationsLeft, animationsTop, animationsRight, animationsBottom)) {
                settingsMenuPage = SettingsMenuPage.ANIMATIONS;
                settingsMenuScrollOffset = 0.0D;
                clearSettingsBounds();
                event.setCanceled(true);
                return;
            }

            if (settingsMenuPage == SettingsMenuPage.ANIMATIONS && isInsideMenuBounds(mouseX, mouseY, menuAnimationsLeft, menuAnimationsTop, menuAnimationsRight, menuAnimationsBottom)) {
                StormOverlayData.toggleMenuAnimationsEnabled();
                event.setCanceled(true);
                return;
            }

            if (settingsMenuPage == SettingsMenuPage.ANIMATIONS && isInsideMenuBounds(mouseX, mouseY, textSwapAnimationsLeft, textSwapAnimationsTop, textSwapAnimationsRight, textSwapAnimationsBottom)) {
                StormOverlayData.toggleTextSwapAnimationsEnabled();
                if (!StormOverlayData.isTextSwapAnimationsEnabled()) {
                    syncModeVisualStackToCurrentMode();
                }
                event.setCanceled(true);
                return;
            }

            if (isSettingsModalDragArea(mouseX, mouseY)) {
                startSettingsModalDrag(mouseX, mouseY);
                event.setCanceled(true);
                return;
            }

            if (settingsModalCovers(mouseX, mouseY)) {
                event.setCanceled(true);
                return;
            }
        }

        if (isInside(mouseX, mouseY, toolsButtonLeft, toolsButtonTop, toolsButtonRight, toolsButtonBottom)) {
            toggleRadarToolsMenu();
            event.setCanceled(true);
            return;
        }

        if (handleModeMenuClick(mouseX, mouseY)) {
            event.setCanceled(true);
            return;
        }

        if (!StormOverlayData.shouldShowRadarLocations()) {
            return;
        }

        if (displayControlAt(mouseX, mouseY)) {
            stationLabelsOpen = !stationLabelsOpen;
            StormOverlayData.setStationLabelsOpen(stationLabelsOpen);
            event.setCanceled(true);
            return;
        }

        if (!shouldDrawRadarLocationMarkers()) {
            return;
        }

        for (StationLabelBounds bounds : stationLabelBounds) {
            if (!isInside(mouseX, mouseY, bounds.left(), bounds.top(), bounds.right(), bounds.bottom())) {
                continue;
            }

            if (!StormOverlayData.selectedRadarSiteMatches(bounds.pos()) && StormOverlayData.selectRadarSite(bounds.pos())) {
                clearRadarTextures();
                XaeroStormOverlayRegistrar.tick(true, true);
            }

            event.setCanceled(true);
            return;
        }
    }

    public static void handleMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        Screen screen = event.getScreen();
        if (!isWorldMap(screen) || isHiddenUi(screen) || event.getMouseButton() != 0 || (!settingsModalDragging && !settingsModalResizing && !settingsScrollDragging)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (settingsScrollDragging) {
            updateSettingsScrollDrag(event.getMouseY());
        } else if (settingsModalResizing) {
            updateSettingsModalResize(event.getMouseX(), event.getMouseY(), minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        } else {
            updateSettingsModalDrag(event.getMouseX(), event.getMouseY(), minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
        event.setCanceled(true);
    }

    public static void handleMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        Screen screen = event.getScreen();
        if (!isWorldMap(screen) || event.getButton() != 0 || (!settingsModalDragging && !settingsModalResizing && !settingsScrollDragging)) {
            return;
        }

        settingsModalDragging = false;
        settingsModalResizing = false;
        settingsScrollDragging = false;
        settingsScrollDragOffsetY = 0.0D;
        event.setCanceled(true);
    }

    private static void setWeatherDisplayEnabled(boolean enabled) {
        if (!StormOverlayData.setDisplayEnabled(enabled)) {
            return;
        }

        clearRadarTextures();
        modeMenuOpen = false;
        clearModeMenuBounds();
        stationLabelBounds = List.of();
        XaeroStormOverlayRegistrar.tick(true, true);
    }

    private static void toggleRadarToolsMenu() {
        if (settingsOpen) {
            if (radarToolsOpenAnimationActive && updateRadarToolsOpenAnimation() < 1.0D) {
                cancelRadarToolsMenuOpen();
            } else {
                closeRadarToolsMenu();
            }
        } else {
            openRadarToolsMenu();
        }
    }

    private static void openRadarToolsMenu() {
        settingsOpen = true;
        radarToolsCloseAnimationActive = false;
        radarToolsCloseAnimationStartNanos = 0L;
        settingsModalDragging = false;
        settingsModalResizing = false;
        settingsScrollDragging = false;
        settingsModalOffsetX = 0.0D;
        settingsModalOffsetY = 0.0D;
        settingsMenuPage = SettingsMenuPage.MAIN;
        settingsMenuScrollOffset = 0.0D;
        if (StormOverlayData.isMenuAnimationsEnabled()) {
            randomizeRadarToolsOpenMotion();
            radarToolsOpenAnimationActive = true;
            radarToolsOpenAnimationStartNanos = System.nanoTime();
        } else {
            radarToolsOpenAnimationActive = false;
            radarToolsOpenAnimationStartNanos = 0L;
        }
        clearSettingsBounds();
    }

    private static void cancelRadarToolsMenuOpen() {
        settingsOpen = false;
        settingsModalDragging = false;
        settingsModalResizing = false;
        settingsScrollDragging = false;
        settingsModalOffsetX = 0.0D;
        settingsModalOffsetY = 0.0D;
        settingsMenuPage = SettingsMenuPage.MAIN;
        settingsMenuScrollOffset = 0.0D;
        radarToolsOpenAnimationActive = false;
        radarToolsOpenAnimationStartNanos = 0L;
        clearSettingsBounds();
    }

    private static void randomizeRadarToolsOpenMotion() {
        long seed = System.nanoTime();
        radarToolsOpenStartSide = (seed & 1L) == 0L ? -1.0D : 1.0D;
        radarToolsOpenCurveDirection = ((seed >>> 11) & 1L) == 0L ? -1.0D : 1.0D;
        radarToolsOpenStartYRatio = 0.18D + (((seed >>> 22) & 0xFFL) / 255.0D) * 0.18D;
        double tilt = 18.0D + (((seed >>> 34) & 0xFFL) / 255.0D) * (RADAR_TOOLS_OPEN_ICON_MAX_TILT_DEGREES - 18.0D);
        radarToolsOpenStartRotationDegrees = -radarToolsOpenStartSide * tilt;
    }

    private static void closeRadarToolsMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        SettingsModalPlacement placement = settingsModalPlacement(width, height);
        radarToolsCloseStartLeft = settingsModalRight > settingsModalLeft ? settingsModalLeft : placement.left();
        radarToolsCloseStartTop = settingsModalBottom > settingsModalTop ? settingsModalTop : placement.top();
        settingsOpen = false;
        settingsModalDragging = false;
        settingsModalResizing = false;
        settingsScrollDragging = false;
        settingsModalOffsetX = 0.0D;
        settingsModalOffsetY = 0.0D;
        settingsMenuScrollOffset = 0.0D;
        radarToolsOpenAnimationActive = false;
        radarToolsOpenAnimationStartNanos = 0L;
        if (StormOverlayData.isMenuAnimationsEnabled()) {
            randomizeRadarToolsCloseMotion();
            radarToolsCloseAnimationActive = true;
            radarToolsCloseAnimationStartNanos = System.nanoTime();
        } else {
            radarToolsCloseAnimationActive = false;
            radarToolsCloseAnimationStartNanos = 0L;
        }
        clearSettingsBounds();
    }

    private static void randomizeRadarToolsCloseMotion() {
        long seed = System.nanoTime();
        radarToolsCloseCurveDirection = (seed & 1L) == 0L ? 1.0D : -1.0D;
        radarToolsCloseRotationPeak = 5.0D + (((seed >>> 8) & 0xFFL) / 255.0D) * 6.0D;
        radarToolsCloseWobblePhase = (((seed >>> 20) & 0x3FFL) / 1023.0D) * Math.PI * 2.0D;
        radarToolsCloseWarpStrength = 0.36D + (((seed >>> 34) & 0xFFL) / 255.0D) * 0.22D;
    }

    private static boolean isSettingsModalDragArea(double mouseX, double mouseY) {
        int dragBottom = Math.min(settingsModalTop + SETTINGS_MODAL_DRAG_HANDLE_HEIGHT, settingsModalBottom);
        return isInside(mouseX, mouseY, settingsModalLeft, settingsModalTop, settingsModalRight, dragBottom);
    }

    private static void startSettingsModalDrag(double mouseX, double mouseY) {
        settingsModalDragging = true;
        settingsModalResizing = false;
        settingsScrollDragging = false;
        settingsModalDragOffsetX = mouseX - settingsModalLeft;
        settingsModalDragOffsetY = mouseY - settingsModalTop;
    }

    private static void updateSettingsModalDrag(double mouseX, double mouseY, int width, int height) {
        double left = clampSettingsModalLeft(mouseX - settingsModalDragOffsetX, width);
        double top = clampSettingsModalTop(mouseY - settingsModalDragOffsetY, height);
        settingsModalOffsetX = left - defaultSettingsModalLeft(width);
        settingsModalOffsetY = top - defaultSettingsModalTop(height);
        updateSettingsModalBounds((int) Math.round(left), (int) Math.round(top), settingsModalWidth, settingsModalHeight);
        clampSettingsMenuScroll();
    }

    private static void startSettingsScrollDrag(double mouseY, boolean centerThumbOnMouse) {
        double maxScroll = maxSettingsMenuScroll();
        int thumbHeight = settingsScrollThumbBottom - settingsScrollThumbTop + 1;
        if (maxScroll <= 0.0D || thumbHeight <= 0 || settingsScrollTrackBottom <= settingsScrollTrackTop) {
            return;
        }

        settingsScrollDragging = true;
        settingsModalDragging = false;
        settingsModalResizing = false;
        settingsScrollDragOffsetY = centerThumbOnMouse ? thumbHeight / 2.0D : mouseY - settingsScrollThumbTop;
        updateSettingsScrollDrag(mouseY);
    }

    private static void updateSettingsScrollDrag(double mouseY) {
        double maxScroll = maxSettingsMenuScroll();
        int trackHeight = settingsScrollTrackBottom - settingsScrollTrackTop + 1;
        int thumbHeight = settingsScrollThumbBottom - settingsScrollThumbTop + 1;
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        if (maxScroll <= 0.0D || trackHeight <= 0 || thumbHeight <= 0 || thumbTravel <= 0) {
            settingsMenuScrollOffset = 0.0D;
            clearSettingsRowBounds();
            return;
        }

        double thumbTop = clamp(mouseY - settingsScrollDragOffsetY, settingsScrollTrackTop, settingsScrollTrackTop + thumbTravel);
        settingsMenuScrollOffset = clamp((thumbTop - settingsScrollTrackTop) / thumbTravel * maxScroll, 0.0D, maxScroll);
        clearSettingsRowBounds();
    }

    private static void startSettingsModalResize(double mouseX, double mouseY) {
        settingsModalResizing = true;
        settingsModalDragging = false;
        settingsScrollDragging = false;
        settingsModalResizeStartMouseX = mouseX;
        settingsModalResizeStartMouseY = mouseY;
        settingsModalResizeStartLeft = settingsModalLeft;
        settingsModalResizeStartTop = settingsModalTop;
        settingsModalResizeStartWidth = settingsModalWidth;
        settingsModalResizeStartHeight = settingsModalHeight;
    }

    private static void updateSettingsModalResize(double mouseX, double mouseY, int width, int height) {
        int maxWidth = Math.max(SETTINGS_MODAL_MIN_WIDTH, width - SETTINGS_MODAL_SCREEN_MARGIN * 2);
        int maxHeight = Math.max(SETTINGS_MODAL_MIN_HEIGHT, height - SETTINGS_MODAL_SCREEN_MARGIN * 2);
        settingsModalWidth = clampInt(
                (int) Math.round(settingsModalResizeStartWidth + mouseX - settingsModalResizeStartMouseX),
                SETTINGS_MODAL_MIN_WIDTH,
                maxWidth
        );
        settingsModalHeight = clampInt(
                (int) Math.round(settingsModalResizeStartHeight + mouseY - settingsModalResizeStartMouseY),
                SETTINGS_MODAL_MIN_HEIGHT,
                maxHeight
        );

        double left = clampSettingsModalLeft(settingsModalResizeStartLeft, width);
        double top = clampSettingsModalTop(settingsModalResizeStartTop, height);
        settingsModalOffsetX = left - defaultSettingsModalLeft(width);
        settingsModalOffsetY = top - defaultSettingsModalTop(height);
        updateSettingsModalBounds((int) Math.round(left), (int) Math.round(top), settingsModalWidth, settingsModalHeight);
    }

    public static void reset() {
        clearModeBounds();
        stationLabelsOpen = StormOverlayData.isStationLabelsOpen();
        clearRadarTextures();
    }

    public static void beginXaeroWorldMapRender(Screen screen) {
        if (isWorldMap(screen) && StormOverlayData.isDisplayEnabled() && StormOverlayData.isDualModeEnabled()) {
            dualModeScreenTextureReady = false;
            clearDualModeXaeroLabels();
        }
    }

    public static boolean captureDualModePlayerArrow(
            boolean far,
            double x,
            double z,
            float angle,
            double scale,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        return false;
    }

    public static boolean captureDualModeXaeroMapObject(
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
            float red,
            float green,
            float blue,
            float alpha
    ) {
        return false;
    }

    public static boolean captureDualModeXaeroLabel(Screen screen, String text, int x, int y) {
        if (!isWorldMap(screen) || !shouldTransformXaeroDualModeMap() || text == null || text.isEmpty()) {
            return false;
        }

        DualModeXaeroLabel label = new DualModeXaeroLabel(text, x, y);
        if (isCoordinateLabel(text)) {
            dualModeCoordinateLabel = label;
            return true;
        }

        if (isZoomLabel(text)) {
            dualModeZoomLabel = label;
            return true;
        }

        if (isTopInfoLabel(y)) {
            dualModeBiomeLabel = label;
            return true;
        }

        return false;
    }

    public static void captureDualModeSourceMap() {
        captureDualModeSourceMap(Minecraft.getInstance());
    }

    public static boolean isDualModeSourceMapReady() {
        return dualModeScreenTextureReady;
    }

    public static boolean shouldTransformXaeroDualModeMap() {
        return StormOverlayData.isDisplayEnabled() && StormOverlayData.isDualModeEnabled();
    }

    public static void drawDualModeBackground(Screen screen, GuiGraphics guiGraphics) {
        if (!isWorldMap(screen) || !shouldTransformXaeroDualModeMap() || guiGraphics == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().last().pose().identity();
            guiGraphics.pose().translate(0.0F, 0.0F, -1000.0F);
            guiGraphics.fill(0, 0, width, height, 0xFF000000);
        } finally {
            guiGraphics.pose().popPose();
        }
    }

    public static void renderDualModeMapsBeforeXaeroElements(Screen screen, GuiGraphics guiGraphics) {
        if (!isWorldMap(screen) || isHiddenUi(screen) || !shouldTransformXaeroDualModeMap()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }

        if (!dualModeScreenTextureReady) {
            guiGraphics.flush();
            captureDualModeSourceMap(minecraft);
        }
        if (!dualModeScreenTextureReady) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        MapView mapView = mapView(screen, width, height);
        if (mapView == null) {
            return;
        }

        List<StormOverlayData.RadarSiteView> radarSites = StormOverlayData.radarSiteViews(mapView.dimension());
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().last().pose().identity();
            guiGraphics.pose().translate(0.0F, 0.0F, 680.0F);
            drawDualModeMapBaseAndRadarLayer(guiGraphics, width, height, mapView, radarSites);
            guiGraphics.flush();
        } finally {
            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void beginDualModeCurrentXaeroMapElementRender() {
        prepareDualModeCurrentXaeroMapElementRenderPass();
    }

    public static void prepareDualModeCurrentXaeroMapElementRenderPass() {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void endDualModeCurrentXaeroMapElementRender() {
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static double dualModeXaeroRadarElementZOffset() {
        return DUAL_MAP_XAERO_RADAR_ELEMENT_Z_OFFSET;
    }

    public static double dualModeTopMapElementLocalYOffset(double xaeroScale) {
        if (!shouldTransformXaeroDualModeMap() || !Double.isFinite(xaeroScale) || xaeroScale <= 0.0D) {
            return 0.0D;
        }

        return -dualModeRawPanelHeight() / xaeroScale;
    }

    public static void translateXaeroElementsToUpperPanel(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldTransformXaeroDualModeMap() || minecraft == null || minecraft.getWindow() == null) {
            return;
        }
        int rawHeight = minecraft.getWindow().getHeight();
        float projectionY = RenderSystem.getProjectionMatrix().m11();
        if (rawHeight <= 0 || projectionY == 0.0F) {
            return;
        }
        // Translate after the map's zoom transform, matching the framebuffer copy's pixel distance.
        double pixelDistance = rawHeight - dualModeRawPanelHeight();
        float offset = (float) (2.0D * pixelDistance / (rawHeight * projectionY));
        Matrix4f matrix = poseStack.last().pose();
        matrix.m31(matrix.m31() + offset);
    }

    public static int dualModeLowerGuiPanelTop() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 0;
        }

        int height = minecraft.getWindow().getGuiScaledHeight();
        int split = dualModeGuiPanelHeight(height);
        return Math.max(0, split + dualModeCenterBlackLineHeight(height, split) + DUAL_MAP_LOWER_PANEL_Y_OFFSET);
    }

    public static void flushXaeroMinimapRenderBuffers() {
        try {
            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            Object session = sessionClass.getMethod("getCurrentSession").invoke(null);
            if (session == null) {
                return;
            }

            Object modMain = sessionClass.getMethod("getModMain").invoke(session);
            if (modMain == null) {
                return;
            }

            Object interfaceRenderer = modMain.getClass().getMethod("getInterfaceRenderer").invoke(modMain);
            if (interfaceRenderer == null) {
                return;
            }

            Object vertexConsumers = interfaceRenderer.getClass().getMethod("getCustomVertexConsumers").invoke(interfaceRenderer);
            if (vertexConsumers == null) {
                return;
            }

            Object buffers = vertexConsumers.getClass().getMethod("getBetterPVPRenderTypeBuffers").invoke(vertexConsumers);
            if (buffers instanceof BufferSource bufferSource) {
                bufferSource.endBatch();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static void enableDualModeMainMapScissor() {
        if (!shouldTransformXaeroDualModeMap()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }

        int framebufferWidth = minecraft.getWindow().getWidth();
        int framebufferHeight = minecraft.getWindow().getHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        RenderSystem.enableScissor(0, 0, framebufferWidth, Math.min(framebufferHeight, dualModeRawPanelHeight()));
    }

    public static void disableDualModeMainMapScissor() {
        RenderSystem.disableScissor();
    }

    public static void translateDualModeMainMapElements(GuiGraphics guiGraphics) {
        if (!shouldTransformXaeroDualModeMap() || guiGraphics == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }

        guiGraphics.pose().translate(0.0F, (float) dualModeRawQuarterHeight(), 0.0F);
    }

    public static float dualModeMainMapBlitYOffset(double xaeroScale) {
        double fboScale = xaeroScale >= 1.0D ? Math.max(1.0D, Math.floor(xaeroScale)) : xaeroScale;
        double secondaryScale = fboScale > 0.0D && Double.isFinite(fboScale) ? xaeroScale / fboScale : 1.0D;
        if (!Double.isFinite(secondaryScale) || secondaryScale <= 0.0D) {
            secondaryScale = 1.0D;
        }

        return (float) (dualModeRawQuarterHeight() / secondaryScale);
    }

    private static double dualModeRawQuarterHeight() {
        return dualModeRawPanelHeight() * 0.5D;
    }

    public static double dualModeAdjustedRawMouseY(double rawMouseY) {
        if (!shouldTransformXaeroDualModeMap()) {
            clearDualModeMouseDragPanel();
            return rawMouseY;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return rawMouseY;
        }

        int rawHeight = minecraft.getWindow().getHeight();
        if (rawHeight <= 1) {
            return rawMouseY;
        }

        int panelHeight = dualModeRawPanelHeight();
        double offset = panelHeight * 0.5D;
        if (dualModeMouseDragPanel == 1) {
            return rawMouseY + offset;
        }
        if (dualModeMouseDragPanel == 2) {
            return rawMouseY - offset;
        }

        return rawMouseY < dualModeRawLowerPanelTop() ? rawMouseY + offset : rawMouseY - offset;
    }

    public static void beginDualModeMapMousePress(int button, double rawMouseY) {
        if (!shouldTransformXaeroDualModeMap() || (button != 0 && button != 1)) {
            clearDualModeMouseDragPanel();
            return;
        }

        dualModeMouseDragPanel = rawMouseY < dualModeRawLowerPanelTop() ? 1 : 2;
        dualModeMouseDragReleasePending = false;
    }

    public static void endDualModeMapMousePress(int button) {
        if ((button == 0 || button == 1) && dualModeMouseDragPanel != 0) {
            dualModeMouseDragReleasePending = true;
        }
    }

    public static int xaeroRenderMouseY(Screen screen, int mouseX, int mouseY) {
        if (!shouldBlockXaeroMapMouse(screen, mouseX, mouseY)) {
            return mouseY;
        }

        return Integer.MIN_VALUE / 4;
    }

    public static double xaeroRenderRawMouseY(Screen screen, int mouseX, double rawMouseY, boolean mapDragActive) {
        Minecraft minecraft = Minecraft.getInstance();
        int mouseY = rawMouseYToGuiY(minecraft, rawMouseY);
        boolean covered = shouldBlockXaeroMapMouse(screen, mouseX, mouseY);
        double adjustedRawMouseY = covered ? lastMapMouseY : dualModeAdjustedRawMouseY(rawMouseY);
        if (!covered) {
            lastMapMouseY = adjustedRawMouseY;
        }
        if (dualModeMouseDragReleasePending) {
            clearDualModeMouseDragPanel();
        }

        return adjustedRawMouseY;
    }

    public static double xaeroRenderRawMouseX(Screen screen, int mouseX, double rawMouseX, double rawMouseY) {
        int mouseY = rawMouseYToGuiY(Minecraft.getInstance(), rawMouseY);
        if (shouldBlockXaeroMapMouse(screen, mouseX, mouseY)) {
            return lastMapMouseX;
        }
        lastMapMouseX = rawMouseX;
        return rawMouseX;
    }

    private static void clearDualModeMouseDragPanel() {
        dualModeMouseDragPanel = 0;
        dualModeMouseDragReleasePending = false;
    }

    private static boolean shouldBlockXaeroMapMouse(Screen screen, int mouseX, int mouseY) {
        return isWorldMap(screen) && !isHiddenUi(screen) && settingsModalCovers(mouseX, mouseY);
    }

    private static int rawMouseYToGuiY(Minecraft minecraft, double rawMouseY) {
        if (minecraft == null || minecraft.getWindow() == null || minecraft.getWindow().getHeight() <= 0) {
            return (int) Math.round(rawMouseY);
        }

        return (int) Math.round(rawMouseY * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getHeight());
    }

    public static int dualModeGuiPanelHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 0;
        }

        return dualModeGuiPanelHeight(minecraft.getWindow().getGuiScaledHeight());
    }

    private static int dualModeRawPanelHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 0;
        }

        int rawHeight = minecraft.getWindow().getHeight();
        if (rawHeight <= 1) {
            return Math.max(1, rawHeight);
        }

        return DUAL_MAP_PANEL_HEIGHT_SCREEN_PIXELS * 2 <= rawHeight
                ? DUAL_MAP_PANEL_HEIGHT_SCREEN_PIXELS
                : Math.max(1, rawHeight / 2);
    }

    private static void captureDualModeSourceMap(Minecraft minecraft) {
        if (minecraft == null || minecraft.getWindow() == null) {
            dualModeScreenTextureReady = false;
            return;
        }

        int framebufferWidth = minecraft.getWindow().getWidth();
        int framebufferHeight = minecraft.getWindow().getHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            dualModeScreenTextureReady = false;
            return;
        }

        ensureDualModeScreenTexture(framebufferWidth, framebufferHeight);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dualModeScreenTextureId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, framebufferWidth, framebufferHeight);
        dualModeScreenTextureReady = true;
    }

    private static void ensureDualModeScreenTexture(int framebufferWidth, int framebufferHeight) {
        if (dualModeScreenTextureId != -1
                && dualModeScreenTextureWidth == framebufferWidth
                && dualModeScreenTextureHeight == framebufferHeight) {
            return;
        }

        if (dualModeScreenTextureId == -1) {
            dualModeScreenTextureId = GL11.glGenTextures();
        }

        dualModeScreenTextureWidth = framebufferWidth;
        dualModeScreenTextureHeight = framebufferHeight;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dualModeScreenTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                framebufferWidth,
                framebufferHeight,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );
    }

    private static BottomControlsLayout drawDualModeMapDisplays(
            GuiGraphics guiGraphics,
            Font font,
            int width,
            int height,
            MapView sourceView,
            List<StormOverlayData.RadarSiteView> radarSites,
            double mouseX,
            double mouseY
    ) {
        DualMapPanels panels = dualMapPanels(width, height);
        drawDualModeStationLabels(guiGraphics, font, panels, sourceView, radarSites);
        StormOverlayData.RadarMode upperMode = StormOverlayData.getDualUpperMode();
        StormOverlayData.RadarMode lowerMode = StormOverlayData.getDualLowerMode();
        BottomControlsLayout lowerControls = dualModeControls(font, panels.lower(), lowerMode);
        int panelOffset = panels.lower().top() - panels.upper().top();
        BottomControlsLayout upperControls = bottomControlsLayoutWithin(
                font, panels.upper().left(), panels.upper().right(), upperMode,
                lowerControls.buttonY() - panelOffset);
        // Mode text can differ, but both bars and buttons share the lower panel's geometry.
        upperControls = new BottomControlsLayout(
                lowerControls.buttonX(), lowerControls.buttonY() - panelOffset,
                lowerControls.buttonWidth(), lowerControls.buttonHeight(),
                lowerControls.barX(), lowerControls.barY() - panelOffset,
                lowerControls.barWidth(), lowerControls.barHeight(),
                lowerControls.modeY() - panelOffset, upperControls.modeWidth(),
                upperControls.modeTextX(), upperControls.modeBoxLeft(), upperControls.modeClosedRight(),
                lowerControls.modeHeight(), lowerControls.modeScale(), upperControls.modeText(), upperMode);
        modeControlBounds = List.of(
                modeControlBounds(ModeControlTarget.DUAL_UPPER, upperControls),
                modeControlBounds(ModeControlTarget.DUAL_LOWER, lowerControls)
        );
        displayControlBounds = List.of(
                displayControlBounds(upperControls),
                displayControlBounds(lowerControls)
        );

        ModeMenuFrame modeMenuFrame = updateModeMenuFrame(mouseX, mouseY);
        drawDualModeMapChrome(
                guiGraphics,
                font,
                width,
                upperControls,
                ModeControlTarget.DUAL_UPPER,
                modeMenuFrame,
                mouseX,
                mouseY
        );
        drawDualModeMapChrome(
                guiGraphics,
                font,
                width,
                lowerControls,
                ModeControlTarget.DUAL_LOWER,
                modeMenuFrame,
                mouseX,
                mouseY
        );
        return lowerControls;
    }

    private static void drawDualModeMapLayer(
            GuiGraphics guiGraphics,
            int screenWidth,
            int screenHeight,
            MapView sourceView,
            List<StormOverlayData.RadarSiteView> radarSites
    ) {
        drawDualModeMapBaseAndRadarLayer(guiGraphics, screenWidth, screenHeight, sourceView, radarSites);
    }

    private static DualMapPanels drawDualModeMapBaseAndRadarLayer(
            GuiGraphics guiGraphics,
            int screenWidth,
            int screenHeight,
            MapView sourceView,
            List<StormOverlayData.RadarSiteView> radarSites
    ) {
        DualMapPanels panels = dualMapPanels(screenWidth, screenHeight);
        drawCapturedMapSlice(
                guiGraphics,
                panels.upper().left(),
                panels.upper().top(),
                panels.upper().right(),
                Math.min(screenHeight, panels.upper().bottom() + DUAL_MAP_SEAM_OVERLAP),
                screenWidth,
                screenHeight,
                panels.lower().centerY()
        );
        drawDualModeRadarLayer(guiGraphics, panels.upper(), sourceView, radarSites, StormOverlayData.getDualUpperMode(), false);
        drawDualModeRadarLayer(guiGraphics, panels.lower(), sourceView, radarSites, StormOverlayData.getDualLowerMode(), true);
        return panels;
    }

    private static void drawDualModeStationLabels(
            GuiGraphics guiGraphics,
            Font font,
            DualMapPanels panels,
            MapView sourceView,
            List<StormOverlayData.RadarSiteView> radarSites
    ) {
        if (!shouldDrawRadarLocationMarkers() || radarSites.isEmpty()) {
            stationLabelBounds = List.of();
            return;
        }

        List<StationLabelBounds> bounds = new ArrayList<>();
        drawDualModeStationLabelsForPanel(guiGraphics, font, panels.upper(), sourceView, radarSites, bounds);
        drawDualModeStationLabelsForPanel(guiGraphics, font, panels.lower(), sourceView, radarSites, bounds);
        stationLabelBounds = List.copyOf(bounds);
    }

    private static void drawDualModeStationLabelsForPanel(
            GuiGraphics guiGraphics,
            Font font,
            DualMapPanel panel,
            MapView sourceView,
            List<StormOverlayData.RadarSiteView> radarSites,
            List<StationLabelBounds> bounds
    ) {
        MapView panelView = panelMapView(sourceView, panel);
        guiGraphics.enableScissor(panel.left(), panel.top(), panel.right(), panel.bottom());
        try {
            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().translate(panel.left(), panel.top(), 0.0F);
                drawStationLabels(guiGraphics, font, panelView, radarSites, bounds, panel.left(), panel.top());
                guiGraphics.flush();
            } finally {
                guiGraphics.pose().popPose();
            }
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private static void drawDualModeRadarLayer(
            GuiGraphics guiGraphics,
            DualMapPanel panel,
            MapView sourceView,
            List<StormOverlayData.RadarSiteView> radarSites,
            StormOverlayData.RadarMode mode,
            boolean lowerPanel
    ) {
        MapView panelView = panelMapView(sourceView, panel);
        boolean rawLowerRadarClip = lowerPanel && shouldUseDualModeLowerRadarRawClip();
        if (rawLowerRadarClip) {
            enableDualModeLowerRadarRawClip(panel);
        } else {
            guiGraphics.enableScissor(panel.left(), panel.top(), panel.right(), panel.bottom());
        }
        try {
            double lowerRadarTopShift = dualModeLowerRadarTopShift(panel, lowerPanel);
            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().translate(panel.left(), (float) (panel.top() - lowerRadarTopShift), 0.0F);
                drawDynamicRadar(guiGraphics, panelView, radarSites, false, mode, false, DUAL_MAP_RADAR_ALPHA);
            } finally {
                guiGraphics.pose().popPose();
            }

            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().translate(panel.left(), panel.top(), 0.0F);
                if (StormOverlayData.isLightningEnabled()) {
                    drawLightningStrikes(guiGraphics, panelView, radarSites, StormOverlayData.lightningStrikeViews(panelView.dimension()));
                }
                guiGraphics.flush();
            } finally {
                guiGraphics.pose().popPose();
            }
        } finally {
            if (rawLowerRadarClip) {
                RenderSystem.disableScissor();
            } else {
                guiGraphics.disableScissor();
            }
        }
        drawDualModeRangeCircles(guiGraphics, panel, panelView, radarSites, lowerPanel);
    }

    private static void drawDualModeRangeCircles(
            GuiGraphics guiGraphics,
            DualMapPanel panel,
            MapView panelView,
            List<StormOverlayData.RadarSiteView> radarSites,
            boolean lowerPanel
    ) {
        int clipTop = Math.min(panel.bottom(), panel.top() + DUAL_MAP_TEXT_SAFE_TOP);
        if (clipTop >= panel.bottom()) {
            return;
        }

        boolean rawLowerRadarClip = lowerPanel && shouldUseDualModeLowerRadarRawClip();
        if (rawLowerRadarClip) {
            enableDualModeLowerRadarRawClip(panel);
        } else {
            guiGraphics.enableScissor(panel.left(), clipTop, panel.right(), panel.bottom());
        }
        try {
            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().translate(
                        panel.left(),
                        (float) (panel.top() - dualModeLowerRadarTopShift(panel, lowerPanel)),
                        0.0F
                );
                drawRadarRangeCircles(guiGraphics, panelView, radarSites);
            } finally {
                guiGraphics.pose().popPose();
            }
        } finally {
            if (rawLowerRadarClip) {
                RenderSystem.disableScissor();
            } else {
                guiGraphics.disableScissor();
            }
        }
    }

    private static double dualModeLowerRadarTopShift(DualMapPanel panel, boolean lowerPanel) {
        if (!lowerPanel) {
            return 0.0D;
        }

        double guiScale = currentGuiScale();
        if (guiScale < 2.0D || guiScale > 4.0D) {
            return 0.0D;
        }

        return Math.max(0.0D, panel.top() - dualModeRawLowerPanelTop() / guiScale);
    }

    private static boolean shouldUseDualModeLowerRadarRawClip() {
        double guiScale = currentGuiScale();
        return guiScale >= 2.0D && guiScale <= 4.0D;
    }

    private static void enableDualModeLowerRadarRawClip(DualMapPanel panel) {
        Minecraft minecraft = Minecraft.getInstance();
        double guiScale = currentGuiScale();
        if (minecraft == null || minecraft.getWindow() == null || guiScale <= 0.0D) {
            RenderSystem.enableScissor(panel.left(), panel.top(), panel.width(), panel.height());
            return;
        }

        int framebufferWidth = minecraft.getWindow().getWidth();
        int framebufferHeight = minecraft.getWindow().getHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            RenderSystem.enableScissor(panel.left(), panel.top(), panel.width(), panel.height());
            return;
        }

        int rawLeft = clampInt((int) Math.floor(panel.left() * guiScale), 0, framebufferWidth);
        int rawRight = clampInt((int) Math.ceil(panel.right() * guiScale), rawLeft, framebufferWidth);
        int rawTop = clampInt(dualModeRawLowerPanelTop(), 0, framebufferHeight);
        int rawBottomEdge = clampInt(rawTop + dualModeRawPanelHeight(), rawTop, framebufferHeight);
        RenderSystem.enableScissor(rawLeft, framebufferHeight - rawBottomEdge, Math.max(1, rawRight - rawLeft), Math.max(1, rawBottomEdge - rawTop));
    }

    private static int dualModeRawLowerPanelTop() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 0;
        }

        int rawHeight = minecraft.getWindow().getHeight();
        int split = dualModeRawPanelHeight();
        int centerGap = rawHeight - split * 2 == 1 ? 1 : 0;
        return Math.max(0, split + centerGap);
    }

    private static BottomControlsLayout drawDualModeMapChrome(
            GuiGraphics guiGraphics,
            Font font,
            int screenWidth,
            BottomControlsLayout controls,
            ModeControlTarget target,
            ModeMenuFrame modeMenuFrame,
            double mouseX,
            double mouseY
    ) {
        double offset = target == ModeControlTarget.DUAL_LOWER ? dualModeLowerUiOffset() : 0.0D;
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(0.0D, offset, 0.0D);
            drawRadarControlRow(guiGraphics, font, screenWidth, controls, target, modeMenuFrame, mouseX, mouseY - offset);
        } finally {
            guiGraphics.pose().popPose();
        }
        return controls;
    }

    private static double dualModeLowerUiOffset() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null || currentGuiScale() == 1.0D) {
            return 0.0D;
        }
        int rawHeight = minecraft.getWindow().getHeight();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (rawHeight <= 0) {
            return 0.0D;
        }
        // Use the active projection, not ceil-rounded GUI dimensions, to preserve glyph rasterization.
        double pixelsPerGuiY = Math.abs(RenderSystem.getProjectionMatrix().m11()) * rawHeight * 0.5D;
        if (!Double.isFinite(pixelsPerGuiY) || pixelsPerGuiY <= 0.0D) {
            return 0.0D;
        }
        int panelOffset = dualMapPanels(1, guiHeight).lower().top();
        int pixelOffset = rawHeight - dualModeRawPanelHeight();
        return pixelOffset / pixelsPerGuiY - panelOffset;
    }

    private static DualMapPanels dualMapPanels(int width, int height) {
        int split = dualModeGuiPanelHeight(height);
        int centerGap = dualModeCenterBlackLineHeight(height, split);
        int upperTop = 0;
        int upperBottom = upperTop + split;
        int lowerTop = Math.max(0, upperBottom + centerGap + DUAL_MAP_LOWER_PANEL_Y_OFFSET);
        int bottom = Math.min(Math.max(lowerTop + 1, height), lowerTop + split);
        return new DualMapPanels(
                new DualMapPanel(0, upperTop, Math.max(1, width), upperBottom),
                new DualMapPanel(0, lowerTop, Math.max(1, width), bottom)
        );
    }

    private static int dualModeCenterBlackLineHeight(int height, int split) {
        return height - split * 2 == 1 ? 1 : 0;
    }

    private static void drawDualModeXaeroLabels(GuiGraphics guiGraphics, Font font, int width, int height) {
        DualMapPanels panels = dualMapPanels(width, height);
        drawDualModeTopLabels(guiGraphics, font, panels.upper().top());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, dualModeLowerUiOffset(), 0.0D);
        drawDualModeTopLabels(guiGraphics, font, panels.lower().top());
        guiGraphics.pose().popPose();
        if (dualModeZoomLabel != null) {
            drawCenteredLabelWithBackground(
                    guiGraphics,
                    font,
                    dualModeZoomLabel.text(),
                    dualModeZoomLabel.x(),
                    dualModeZoomLabel.y() - (panels.lower().top() - panels.upper().top()) - 27
            );
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0D, dualModeLowerUiOffset(), 0.0D);
            drawCenteredLabelWithBackground(
                    guiGraphics,
                    font,
                    dualModeZoomLabel.text(),
                    dualModeZoomLabel.x(),
                    dualModeZoomLabel.y() - 27
            );
            guiGraphics.pose().popPose();
        }
    }

    private static void drawDualModeTopLabels(GuiGraphics guiGraphics, Font font, int panelTop) {
        if (dualModeCoordinateLabel != null) {
            drawCenteredLabelWithBackground(
                    guiGraphics,
                    font,
                    dualModeCoordinateLabel.text(),
                    dualModeCoordinateLabel.x(),
                    panelTop + dualModeCoordinateLabel.y()
            );
        }

        if (dualModeBiomeLabel != null) {
            drawCenteredLabelWithBackground(
                    guiGraphics,
                    font,
                    dualModeBiomeLabel.text(),
                    dualModeBiomeLabel.x(),
                    panelTop + dualModeBiomeLabel.y()
            );
        }
    }

    private static void drawCenteredLabelWithBackground(GuiGraphics guiGraphics, Font font, String text, int centerX, int y) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int textWidth = font.width(text);
        int textLeft = centerX - textWidth / 2;
        guiGraphics.fill(textLeft - 1, y - 1, textLeft + textWidth + 1, y + font.lineHeight, 0x66000000);
        guiGraphics.drawString(font, text, textLeft, y, 0xFFFFFFFF, false);
    }

    private static boolean isCoordinateLabel(String text) {
        return text != null && text.startsWith("X: ") && text.contains(" Z:");
    }

    private static boolean isZoomLabel(String text) {
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

    private static boolean isTopInfoLabel(int y) {
        return y >= 0 && y <= 16;
    }

    private static void clearDualModeXaeroLabels() {
        dualModeCoordinateLabel = null;
        dualModeBiomeLabel = null;
        dualModeZoomLabel = null;
    }

    private static int dualModeGuiPanelHeight(int height) {
        if (height <= 1) {
            return Math.max(1, height);
        }

        int target = Math.max(1, (int) Math.round(screenPixelsToGuiUnits(DUAL_MAP_PANEL_HEIGHT_SCREEN_PIXELS)));
        return target * 2 <= height ? target : Math.max(1, height / 2);
    }

    private static BottomControlsLayout dualModeControls(Font font, DualMapPanel panel, StormOverlayData.RadarMode mode) {
        int controlHeight = bottomControlsStormBarHeight() + 2;
        int controlY = Math.max(panel.top() + 16, panel.bottom() - DUAL_MAP_CONTROL_BOTTOM_PADDING - controlHeight);
        return bottomControlsLayoutWithin(font, panel.left(), panel.right(), mode, controlY);
    }

    private static MapView panelMapView(MapView sourceView, DualMapPanel panel) {
        return new MapView(
                sourceView.dimension(),
                sourceView.cameraX(),
                sourceView.cameraZ(),
                sourceView.pixelsPerBlock(),
                panel.width(),
                panel.height()
        );
    }

    private static ModeControlBounds modeControlBounds(ModeControlTarget target, BottomControlsLayout controls) {
        return new ModeControlBounds(
                target,
                controls.modeLeft(),
                controls.modeTop(),
                controls.modeRight(),
                controls.modeBottom()
        );
    }

    private static DisplayControlBounds displayControlBounds(BottomControlsLayout controls) {
        return new DisplayControlBounds(
                controls.buttonX(),
                controls.buttonY(),
                controls.buttonX() + controls.buttonWidth(),
                controls.buttonY() + controls.buttonHeight()
        );
    }

    private static void drawCapturedMapSlice(
            GuiGraphics guiGraphics,
            int destLeft,
            int destTop,
            int destRight,
            int destBottom,
            int guiWidth,
            int guiHeight,
            double sourceCenterY
    ) {
        if (!dualModeScreenTextureReady
                || dualModeScreenTextureId == -1
                || destRight <= destLeft
                || destBottom <= destTop
                || guiWidth <= 0
                || guiHeight <= 0
                || dualModeScreenTextureWidth <= 0
                || dualModeScreenTextureHeight <= 0) {
            return;
        }

        // Bypass GUI projection entirely: equal integer rectangles preserve every source pixel.
        guiGraphics.flush();
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (dualModeCopyFramebufferId == -1) {
            dualModeCopyFramebufferId = GL30.glGenFramebuffers();
        }
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, dualModeCopyFramebufferId);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, dualModeScreenTextureId, 0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            int panelPixels = dualModeRawPanelHeight();
            GL30.glBlitFramebuffer(
                    0, 0, dualModeScreenTextureWidth, panelPixels,
                    0, dualModeScreenTextureHeight - panelPixels,
                    dualModeScreenTextureWidth, dualModeScreenTextureHeight,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            if (scissorEnabled) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    private static void drawInteractiveRadarControlRow(
            GuiGraphics guiGraphics,
            Font font,
            int screenWidth,
            BottomControlsLayout controls,
            double mouseX,
            double mouseY
    ) {
        displayControlBounds = List.of(displayControlBounds(controls));
        modeControlBounds = List.of(modeControlBounds(ModeControlTarget.SINGLE, controls));
        drawRadarControlRow(
                guiGraphics,
                font,
                screenWidth,
                controls,
                ModeControlTarget.SINGLE,
                updateModeMenuFrame(mouseX, mouseY),
                mouseX,
                mouseY
        );
    }

    private static void drawRadarControlRow(
            GuiGraphics guiGraphics,
            Font font,
            int screenWidth,
            BottomControlsLayout controls,
            ModeControlTarget target,
            ModeMenuFrame modeMenuFrame,
            double mouseX,
            double mouseY
    ) {
        boolean hoveringDisplayButton = isInside(
                mouseX,
                mouseY,
                controls.buttonX(),
                controls.buttonY(),
                controls.buttonX() + controls.buttonWidth(),
                controls.buttonY() + controls.buttonHeight()
        );
        drawDisplayButton(guiGraphics, font, controls.buttonX(), controls.buttonY(), controls.buttonWidth(), controls.buttonHeight(), hoveringDisplayButton, controls.modeScale(), controls.modeTextX());
        drawRadarBar(guiGraphics, controls.mode(), controls.barX(), controls.barY(), controls.barWidth(), controls.barHeight());

        if (modeMenuFrame.target() == target && modeMenuOpen) {
            drawModeList(
                    guiGraphics,
                    font,
                    screenWidth,
                    controls.modeBoxLeft(),
                    controls.modeY(),
                    controls.modeWidth(),
                    controls.modeHeight(),
                    controls.modeScale(),
                    controls.mode(),
                    target,
                    mouseX,
                    mouseY,
                    modeMenuFrame.menuProgress(),
                    modeMenuFrame.changeProgress()
            );
        } else {
            drawStaticModeBox(guiGraphics, font, controls);
        }
    }

    private static void drawStaticRadarControlRow(GuiGraphics guiGraphics, Font font, BottomControlsLayout controls) {
        drawDisplayButton(guiGraphics, font, controls.buttonX(), controls.buttonY(), controls.buttonWidth(), controls.buttonHeight(), false, controls.modeScale(), controls.modeTextX());
        drawRadarBar(guiGraphics, controls.mode(), controls.barX(), controls.barY(), controls.barWidth(), controls.barHeight());
        drawStaticModeBox(guiGraphics, font, controls);
    }

    private static void drawStaticModeBox(GuiGraphics guiGraphics, Font font, BottomControlsLayout controls) {
        double left = controls.modeBoxLeft();
        double top = controls.modeY();
        double right = controls.modeClosedRight();
        double bottom = controls.modeY() + controls.modeHeight();
        String text = controls.modeText();
        TextPixelBounds bounds = textPixelBounds(font, text);
        float xScale = controlTextXScale(font, text, controls.modeScale());
        double rowTop = visibleTextTopY(font, text, top, bottom - top, controls.modeScale());
        double textX = controls.modeTextX() - bounds.left() * xScale;
        double textY = rowTop - bounds.top() * controls.modeScale();

        fillTranslatedRect(guiGraphics, left, top, right, bottom, MODE_MENU_BACKGROUND);
        drawTranslatedRectBorder(guiGraphics, left, top, right, bottom, UI_BLUE);
        guiGraphics.enableScissor((int) Math.floor(left), (int) Math.floor(top), (int) Math.ceil(right), (int) Math.ceil(bottom));
        try {
            drawModeText(guiGraphics, font, text, textX, textY, UI_BLUE, controls.modeScale());
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private static ModeMenuFrame updateModeMenuFrame(double mouseX, double mouseY) {
        boolean settingsCovered = settingsModalCovers(mouseX, mouseY);
        ModeControlTarget hoveredTarget = settingsCovered ? null : modeControlTargetAt(mouseX, mouseY);
        boolean hoveringCurrentMenu = !settingsCovered
                && modeMenuOpen
                && modeMenuTarget != null
                && isInside(mouseX, mouseY, modeMenuLeft, modeMenuTop, modeMenuRight, modeMenuBottom);
        ModeControlTarget activeTarget = hoveredTarget != null
                ? hoveredTarget
                : hoveringCurrentMenu ? modeMenuTarget : null;

        if (activeTarget != null && activeTarget != modeMenuTarget) {
            modeMenuTarget = activeTarget;
            modeVisualStack = List.copyOf(modeStackFor(modeForTarget(activeTarget)));
            resetModeMenuAnimation();
            resetModeChangeAnimation();
            clearModeMenuBounds();
        }

        double menuProgress = updateModeMenuAnimation(activeTarget != null);
        double changeProgress = updateModeChangeAnimation();
        modeMenuOpen = activeTarget != null || menuProgress > 0.0D || changeProgress < 1.0D;
        if (!modeMenuOpen) {
            clearModeMenuBounds();
        }

        return new ModeMenuFrame(modeMenuOpen ? modeMenuTarget : null, menuProgress, changeProgress);
    }

    private static ModeControlTarget modeControlTargetAt(double mouseX, double mouseY) {
        for (ModeControlBounds bounds : modeControlBounds) {
            if (isInside(mouseX, mouseY, bounds.left(), bounds.top(), bounds.right(), bounds.bottom())) {
                return bounds.target();
            }
        }

        return null;
    }

    private static boolean displayControlAt(double mouseX, double mouseY) {
        for (DisplayControlBounds bounds : displayControlBounds) {
            if (isInside(mouseX, mouseY, bounds.left(), bounds.top(), bounds.right(), bounds.bottom())) {
                return true;
            }
        }

        return false;
    }

    private static boolean handleModeMenuClick(double mouseX, double mouseY) {
        if (!modeMenuOpen) {
            return false;
        }

        for (ModeLabelBounds bounds : modeLabelBounds) {
            if (!isInside(mouseX, mouseY, bounds.left(), bounds.top(), bounds.right(), bounds.bottom())) {
                continue;
            }

            ModeControlTarget target = bounds.target();
            StormOverlayData.RadarMode previousMode = modeForTarget(target);
            int selectedSlot = modeStackIndex(currentModeStackFor(previousMode), bounds.mode());
            boolean rotateUp = selectedSlot <= 0;
            if (setModeForTarget(target, bounds.mode())) {
                applyModeChangeAnimation(target, previousMode, modeForTarget(target), MODE_MENU_CLICK_CHANGE_ANIMATION_SECONDS, rotateUp);
                clearRadarTextures();
                XaeroStormOverlayRegistrar.tick(true, false);
            }

            return true;
        }

        return false;
    }

    private static boolean settingsModalCovers(double mouseX, double mouseY) {
        if (!settingsOpen) {
            return false;
        }

        if (settingsModalRight > settingsModalLeft
                && settingsModalBottom > settingsModalTop
                && isInside(mouseX, mouseY, settingsModalLeft, settingsModalTop, settingsModalRight, settingsModalBottom)) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        SettingsModalPlacement placement = settingsModalPlacement(
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight()
        );
        return isInside(
                mouseX,
                mouseY,
                placement.left(),
                placement.top(),
                placement.left() + placement.width(),
                placement.top() + placement.height()
        );
    }

    private static StormOverlayData.RadarMode modeForTarget(ModeControlTarget target) {
        if (target == ModeControlTarget.DUAL_UPPER) {
            return StormOverlayData.getDualUpperMode();
        }
        if (target == ModeControlTarget.DUAL_LOWER) {
            return StormOverlayData.getDualLowerMode();
        }

        return StormOverlayData.getRadarMode();
    }

    private static boolean setModeForTarget(ModeControlTarget target, StormOverlayData.RadarMode mode) {
        if (mode == null) {
            return false;
        }

        if (target == ModeControlTarget.DUAL_UPPER) {
            return StormOverlayData.setDualUpperMode(mode);
        }
        if (target == ModeControlTarget.DUAL_LOWER) {
            return StormOverlayData.setDualLowerMode(mode);
        }

        return StormOverlayData.setRadarMode(mode);
    }

    private static StormOverlayData.RadarMode secondaryDualMode(StormOverlayData.RadarMode primaryMode) {
        return primaryMode == StormOverlayData.RadarMode.REFLECTIVITY
                ? StormOverlayData.RadarMode.VELOCITY
                : StormOverlayData.RadarMode.REFLECTIVITY;
    }

    private static void drawDisplayButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, boolean hovered, float scale, double modeTextX) {
        double textX = centeredTextX(font, DISPLAY_BUTTON_TEXT, x, width, scale);
        double textY = centeredTextY(font, DISPLAY_BUTTON_TEXT, y, height, scale) + DISPLAY_BUTTON_TEXT_Y_OFFSET;
        boolean active = hovered || stationLabelsOpen;
        int fillColor = active ? 0xFF1594E8 : MODE_MENU_BACKGROUND;
        int textColor = active ? 0xFFFFFFFF : UI_BLUE;

        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        drawTranslatedRectBorder(guiGraphics, x, y, x + width, y + height, UI_BLUE);
        if (CONTROL_TEXT_BOUNDS_DEBUG) {
            drawTextBoundsDebug(guiGraphics, font, DISPLAY_BUTTON_TEXT, textX, textY, scale);
        }
        drawModeText(guiGraphics, font, DISPLAY_BUTTON_TEXT, textX, textY, textColor, scale);
    }

    private static void drawTextBoundsDebug(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            double x,
            double y,
            float scale
    ) {
        double drawX = snappedTextCoordinate(x);
        double drawY = snappedTextCoordinate(y);
        TextPixelBounds bounds = textPixelBounds(font, text);
        float xScale = controlTextXScale(font, text, scale);
        double left = snapToScreenPixel(drawX + bounds.left() * xScale);
        double top = snapToScreenPixel(drawY + bounds.top() * scale);
        double right = snapToScreenPixel(drawX + bounds.right() * xScale);
        double bottom = snapToScreenPixel(drawY + bounds.bottom() * scale);
        fillPreciseRect(guiGraphics, left, top, right, bottom, CONTROL_TEXT_BOUNDS_FILL);
    }

    private static TextPixelBounds textPixelBounds(Font font, String text) {
        Map<Integer, GlyphPixelBounds> glyphBounds = asciiGlyphPixelBounds();
        if (glyphBounds.isEmpty()) {
            return new TextPixelBounds(0, 0, font.width(text), font.lineHeight);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            GlyphPixelBounds glyph = glyphBounds.get(codePoint);
            if (glyph == null) {
                return new TextPixelBounds(0, 0, font.width(text), font.lineHeight);
            }

            if (!glyph.empty()) {
                int glyphX = font.width(text.substring(0, offset));
                minX = Math.min(minX, glyphX + glyph.left());
                minY = Math.min(minY, glyph.top());
                maxX = Math.max(maxX, glyphX + glyph.right());
                maxY = Math.max(maxY, glyph.bottom());
            }
            offset += Character.charCount(codePoint);
        }

        if (maxX <= minX || maxY <= minY) {
            return new TextPixelBounds(0, 0, font.width(text), font.lineHeight);
        }
        return new TextPixelBounds(minX, minY, maxX, maxY);
    }

    private static double textPixelWidth(Font font, String text, float scale) {
        TextPixelBounds bounds = textPixelBounds(font, text);
        return Math.max(1.0D, (bounds.right() - bounds.left()) * controlTextXScale(font, text, scale));
    }

    private static double centeredTextX(Font font, String text, double left, double width, float scale) {
        TextPixelBounds bounds = textPixelBounds(font, text);
        float xScale = controlTextXScale(font, text, scale);
        double textWidth = (bounds.right() - bounds.left()) * xScale;
        return left + (width - textWidth) / 2.0D - bounds.left() * xScale;
    }

    private static double centeredTextY(Font font, String text, double top, double height, float scale) {
        TextPixelBounds bounds = textPixelBounds(font, text);
        double textHeight = (bounds.bottom() - bounds.top()) * scale;
        return top + (height - textHeight) / 2.0D - bounds.top() * scale;
    }

    private static double visibleTextTopY(Font font, String text, double top, double height, float scale) {
        TextPixelBounds bounds = textPixelBounds(font, text);
        double textHeight = (bounds.bottom() - bounds.top()) * scale;
        return top + (height - textHeight) / 2.0D;
    }

    private static Map<Integer, GlyphPixelBounds> asciiGlyphPixelBounds() {
        if (!asciiGlyphPixelBoundsLoaded) {
            asciiGlyphPixelBoundsLoaded = true;
            asciiGlyphPixelBounds = loadAsciiGlyphPixelBounds();
        }

        return asciiGlyphPixelBounds;
    }

    private static Map<Integer, GlyphPixelBounds> loadAsciiGlyphPixelBounds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return Map.of();
        }

        try (
                InputStream input = minecraft.getResourceManager().open(VANILLA_ASCII_FONT_TEXTURE);
                NativeImage image = NativeImage.read(NativeImage.Format.RGBA, input)
        ) {
            int columns = 16;
            int rows = 16;
            int cellWidth = image.getWidth() / columns;
            int cellHeight = image.getHeight() / rows;
            if (cellWidth <= 0 || cellHeight <= 0) {
                return Map.of();
            }

            Map<Integer, GlyphPixelBounds> bounds = new HashMap<>();
            for (int codePoint = 32; codePoint <= 126; codePoint++) {
                bounds.put(codePoint, measureAsciiGlyph(image, codePoint, cellWidth, cellHeight));
            }
            return bounds;
        } catch (IOException | RuntimeException ignored) {
            return Map.of();
        }
    }

    private static GlyphPixelBounds measureAsciiGlyph(NativeImage image, int codePoint, int cellWidth, int cellHeight) {
        int cellX = (codePoint & 15) * cellWidth;
        int cellY = (codePoint >> 4) * cellHeight;
        int minX = cellWidth;
        int minY = cellHeight;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < cellHeight; y++) {
            for (int x = 0; x < cellWidth; x++) {
                if (image.getLuminanceOrAlpha(cellX + x, cellY + y) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x + 1);
                    maxY = Math.max(maxY, y + 1);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new GlyphPixelBounds(0, 0, 0, 0);
        }
        return new GlyphPixelBounds(minX, minY, maxX, maxY);
    }

    private static void drawRadarToolsButton(GuiGraphics guiGraphics, int x, int y, int size, boolean active) {
        drawRoundedRadarToolsIcon(guiGraphics, x, y, size, 1.0D);
    }

    private static void drawRoundedRadarToolsIcon(GuiGraphics guiGraphics, int x, int y, int size, double alpha) {
        guiGraphics.flush();
        boolean culling = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float pixel = (float) (1.0D / currentGuiScale());
        float opacity = (float) clamp(alpha, 0.0D, 1.0D);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, RADAR_TOOLS_ICON);
        BufferBuilder texture = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < 68; i++) {
            float[] a = radarIconCornerPoint(size, pixel, i);
            float[] b = radarIconCornerPoint(size, pixel, i + 1);
            texture.addVertex(matrix, x + size * 0.5F, y + size * 0.5F, 0).setUv(0.5F, 0.5F).setColor(1F, 1F, 1F, opacity);
            texture.addVertex(matrix, x + a[0], y + a[1], 0).setUv(a[0] / size, a[1] / size).setColor(1F, 1F, 1F, opacity);
            texture.addVertex(matrix, x + b[0], y + b[1], 0).setUv(b[0] / size, b[1] / size).setColor(1F, 1F, 1F, opacity);
        }
        BufferUploader.drawWithShader(texture.buildOrThrow());
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder border = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float[] insets = {0, pixel, 2 * pixel, 3 * pixel};
        float[] coverage = {0, opacity, opacity, 0};
        for (int band = 0; band < 3; band++) {
            for (int i = 0; i < 68; i++) {
                float[] a = radarIconCornerPoint(size, insets[band], i);
                float[] b = radarIconCornerPoint(size, insets[band], i + 1);
                float[] c = radarIconCornerPoint(size, insets[band + 1], i + 1);
                float[] d = radarIconCornerPoint(size, insets[band + 1], i);
                border.addVertex(matrix, x + a[0], y + a[1], 0).setColor(0.33F, 0.85F, 0.16F, coverage[band]);
                border.addVertex(matrix, x + b[0], y + b[1], 0).setColor(0.33F, 0.85F, 0.16F, coverage[band]);
                border.addVertex(matrix, x + c[0], y + c[1], 0).setColor(0.33F, 0.85F, 0.16F, coverage[band + 1]);
                border.addVertex(matrix, x + d[0], y + d[1], 0).setColor(0.33F, 0.85F, 0.16F, coverage[band + 1]);
            }
        }
        BufferUploader.drawWithShader(border.buildOrThrow());
        if (culling) {
            RenderSystem.enableCull();
        }
    }

    private static float[] radarIconCornerPoint(int size, float inset, int index) {
        int corner = (index % 68) / 17;
        double angle = (corner * 90.0D + (index % 17) * 90.0D / 16.0D) * Math.PI / 180.0D;
        float radius = size * 0.2F;
        float cx = corner == 0 || corner == 3 ? size - radius : radius;
        float cy = corner < 2 ? size - radius : radius;
        float innerRadius = Math.max(0, radius - inset);
        return new float[]{cx + (float) Math.cos(angle) * innerRadius, cy + (float) Math.sin(angle) * innerRadius};
    }

    private static void drawSettingsModal(
            GuiGraphics guiGraphics,
            Font font,
            int width,
            int height,
            double alpha,
            double mouseX,
            double mouseY
    ) {
        SettingsModalPlacement placement = settingsModalPlacement(width, height);
        drawSettingsModalAt(guiGraphics, font, placement.left(), placement.top(), placement.width(), placement.height(), alpha, mouseX, mouseY, true);
    }

    private static void drawSettingsModalAt(
            GuiGraphics guiGraphics,
            Font font,
            int left,
            int top,
            int modalWidth,
            int modalHeight,
            double alpha,
            double mouseX,
            double mouseY,
            boolean interactive
    ) {
        drawSettingsModalAt(guiGraphics, font, left, top, modalWidth, modalHeight, alpha, mouseX, mouseY, interactive, true);
    }

    private static void drawSettingsModalAt(
            GuiGraphics guiGraphics,
            Font font,
            int left,
            int top,
            int modalWidth,
            int modalHeight,
            double alpha,
            double mouseX,
            double mouseY,
            boolean interactive,
            boolean clipContent
    ) {
        int right = left + modalWidth;
        int bottom = top + modalHeight;

        if (interactive) {
            updateSettingsModalBounds(left, top, modalWidth, modalHeight);
            clampSettingsMenuScroll();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawRoundedPanel(guiGraphics, left, top, right, bottom, withAlpha(UI_BLACK, alpha), withAlpha(UI_BLUE, alpha));

        String title = settingsMenuPage == SettingsMenuPage.ANIMATIONS ? "Animations" : "Radar Tools";
        int titleTop = top + 8;
        int titleCenterY = titleTop + font.lineHeight / 2;
        drawPlainString(guiGraphics, font, title, left + (modalWidth - font.width(title)) / 2, titleTop + 1, withAlpha(0xFFFFFFFF, alpha));
        int lineY = titleTop + font.lineHeight + 7;
        guiGraphics.fill(left, lineY, right, lineY + 1, withAlpha(0xFF55AFFF, alpha));

        int headerButtonSize = 13;
        int headerButtonTop = titleCenterY - headerButtonSize / 2;

        if (interactive && settingsMenuPage == SettingsMenuPage.ANIMATIONS) {
            settingsBackLeft = left + 7;
            settingsBackTop = headerButtonTop;
            settingsBackRight = settingsBackLeft + headerButtonSize;
            settingsBackBottom = settingsBackTop + headerButtonSize;
            drawSettingsBackButton(guiGraphics, settingsBackLeft, settingsBackTop, settingsBackRight, settingsBackBottom, alpha, mouseX, mouseY, interactive);
        } else if (interactive) {
            settingsBackLeft = 0;
            settingsBackTop = 0;
            settingsBackRight = 0;
            settingsBackBottom = 0;
        }

        int closeLeft = right - 20;
        int closeTop = headerButtonTop;
        int closeRight = closeLeft + headerButtonSize;
        int closeBottom = closeTop + headerButtonSize;
        if (interactive) {
            settingsCloseLeft = closeLeft;
            settingsCloseTop = closeTop;
            settingsCloseRight = closeRight;
            settingsCloseBottom = closeBottom;
        }

        boolean closeHovered = interactive && isInside(mouseX, mouseY, closeLeft, closeTop, closeRight, closeBottom);
        int closeFill = closeHovered ? 0xFF3A1010 : UI_BLACK_ALPHA_50;
        int closeBorder = closeHovered ? UI_RED : 0xFF87909A;
        int closeText = closeHovered ? UI_RED : 0xFFFFFFFF;
        drawRoundedPanel(guiGraphics, closeLeft, closeTop, closeRight, closeBottom, withAlpha(closeFill, alpha), withAlpha(closeBorder, alpha));
        drawSettingsCloseX(guiGraphics, closeLeft, closeTop, headerButtonSize, withAlpha(closeText, alpha));

        int contentTop = top + SETTINGS_MODAL_CONTENT_TOP_OFFSET;
        int contentBottom = bottom - SETTINGS_MODAL_CONTENT_BOTTOM_INSET;
        if (contentBottom > contentTop) {
            if (!clipContent) {
                if (settingsMenuPage == SettingsMenuPage.ANIMATIONS) {
                    drawSettingsAnimationsRows(guiGraphics, font, left, top, right, contentTop, contentBottom, alpha, false);
                } else {
                    drawSettingsMainRows(guiGraphics, font, left, top, right, contentTop, contentBottom, alpha, false);
                }
            } else {
                guiGraphics.enableScissor(left + 1, contentTop, right - 1, contentBottom);
                try {
                    if (settingsMenuPage == SettingsMenuPage.ANIMATIONS) {
                        drawSettingsAnimationsRows(guiGraphics, font, left, top, right, contentTop, contentBottom, alpha, interactive);
                    } else {
                        drawSettingsMainRows(guiGraphics, font, left, top, right, contentTop, contentBottom, alpha, interactive);
                    }
                } finally {
                    guiGraphics.disableScissor();
                }
            }
        }

        drawSettingsScrollBar(guiGraphics, right, bottom, contentTop, contentBottom, alpha, interactive);
        drawSettingsResizeHandle(guiGraphics, right, bottom, alpha, mouseX, mouseY, interactive);
    }

    private static void drawSettingsMainRows(GuiGraphics guiGraphics, Font font, int left, int top, int right, int clipTop, int clipBottom, double alpha, boolean interactive) {
        if (interactive) {
            clearSettingsAnimationRowBounds();
        }

        int rowLeft = left + SETTINGS_MODAL_ROW_HORIZONTAL_INSET;
        int rowRight = right - SETTINGS_MODAL_ROW_HORIZONTAL_INSET;
        int rowTop = scrolledSettingsRowTop(top, 0);
        if (interactive) {
            SettingRowBounds bounds = clippedSettingsToggleBounds(rowRight, rowTop, clipTop, clipBottom);
            displayEnabledLeft = bounds.left();
            displayEnabledTop = bounds.top();
            displayEnabledRight = bounds.right();
            displayEnabledBottom = bounds.bottom();
        }
        if (shouldDrawSettingsRow(rowTop, clipTop, clipBottom, interactive)) {
            drawSettingsToggleRow(guiGraphics, font, "Radar Display", rowLeft, rowTop, rowRight, StormOverlayData.isDisplayEnabled(), alpha);
        }

        rowTop = scrolledSettingsRowTop(top, 1);
        if (interactive) {
            SettingRowBounds bounds = clippedSettingsToggleBounds(rowRight, rowTop, clipTop, clipBottom);
            dualModeLeft = bounds.left();
            dualModeTop = bounds.top();
            dualModeRight = bounds.right();
            dualModeBottom = bounds.bottom();
        }
        if (shouldDrawSettingsRow(rowTop, clipTop, clipBottom, interactive)) {
            drawSettingsToggleRow(guiGraphics, font, "Dual Mode", rowLeft, rowTop, rowRight, StormOverlayData.isDualModeEnabled(), alpha);
        }

        rowTop = scrolledSettingsRowTop(top, 2);
        if (interactive) {
            SettingRowBounds bounds = clippedSettingsToggleBounds(rowRight, rowTop, clipTop, clipBottom);
            radarLocationsLeft = bounds.left();
            radarLocationsTop = bounds.top();
            radarLocationsRight = bounds.right();
            radarLocationsBottom = bounds.bottom();
        }
        if (shouldDrawSettingsRow(rowTop, clipTop, clipBottom, interactive)) {
            drawSettingsToggleRow(guiGraphics, font, "Always Show Radar Locations", rowLeft, rowTop, rowRight, StormOverlayData.isRadarLocationsAlwaysVisible(), alpha);
        }

        rowTop = scrolledSettingsRowTop(top, 3);
        if (interactive) {
            SettingRowBounds bounds = clippedSettingsToggleBounds(rowRight, rowTop, clipTop, clipBottom);
            lightningLeft = bounds.left();
            lightningTop = bounds.top();
            lightningRight = bounds.right();
            lightningBottom = bounds.bottom();
        }
        if (shouldDrawSettingsRow(rowTop, clipTop, clipBottom, interactive)) {
            drawSettingsToggleRow(guiGraphics, font, "Lightning", rowLeft, rowTop, rowRight, StormOverlayData.isLightningEnabled(), alpha);
        }

        rowTop = scrolledSettingsRowTop(top, 4);
        if (interactive) {
            SettingRowBounds bounds = clippedSettingsRowBounds(rowLeft, rowTop, rowRight, clipTop, clipBottom);
            animationsLeft = bounds.left();
            animationsTop = bounds.top();
            animationsRight = bounds.right();
            animationsBottom = bounds.bottom();
        }
        if (shouldDrawSettingsRow(rowTop, clipTop, clipBottom, interactive)) {
            drawSettingsNavigationRow(guiGraphics, font, "Animations", rowLeft, rowTop, rowRight, alpha);
        }
    }

    private static void drawSettingsAnimationsRows(GuiGraphics guiGraphics, Font font, int left, int top, int right, int clipTop, int clipBottom, double alpha, boolean interactive) {
        if (interactive) {
            clearSettingsMainRowBounds();
        }

        int rowLeft = left + SETTINGS_MODAL_ROW_HORIZONTAL_INSET;
        int rowRight = right - SETTINGS_MODAL_ROW_HORIZONTAL_INSET;
        int rowTop = scrolledSettingsRowTop(top, 0);
        if (interactive) {
            SettingRowBounds bounds = clippedSettingsToggleBounds(rowRight, rowTop, clipTop, clipBottom);
            menuAnimationsLeft = bounds.left();
            menuAnimationsTop = bounds.top();
            menuAnimationsRight = bounds.right();
            menuAnimationsBottom = bounds.bottom();
        }
        if (shouldDrawSettingsRow(rowTop, clipTop, clipBottom, interactive)) {
            drawSettingsToggleRow(guiGraphics, font, "Menu Animations", rowLeft, rowTop, rowRight, StormOverlayData.isMenuAnimationsEnabled(), alpha);
        }

        rowTop = scrolledSettingsRowTop(top, 1);
        if (interactive) {
            SettingRowBounds bounds = clippedSettingsToggleBounds(rowRight, rowTop, clipTop, clipBottom);
            textSwapAnimationsLeft = bounds.left();
            textSwapAnimationsTop = bounds.top();
            textSwapAnimationsRight = bounds.right();
            textSwapAnimationsBottom = bounds.bottom();
        }
        if (shouldDrawSettingsRow(rowTop, clipTop, clipBottom, interactive)) {
            drawSettingsToggleRow(guiGraphics, font, "Text Swap Animations", rowLeft, rowTop, rowRight, StormOverlayData.isTextSwapAnimationsEnabled(), alpha);
        }
    }

    private static int scrolledSettingsRowTop(int modalTop, int rowIndex) {
        return modalTop + SETTINGS_MODAL_ROW_TOP_OFFSET + rowIndex * SETTINGS_MODAL_ROW_STEP - (int) Math.round(settingsMenuScrollOffset);
    }

    private static boolean shouldDrawSettingsRow(int rowTop, int clipTop, int clipBottom, boolean interactive) {
        return interactive || (rowTop >= clipTop && rowTop + SETTINGS_MODAL_ROW_HEIGHT <= clipBottom);
    }

    private static SettingRowBounds clippedSettingsRowBounds(int left, int top, int right, int clipTop, int clipBottom) {
        int clippedTop = Math.max(top, clipTop);
        int clippedBottom = Math.min(top + SETTINGS_MODAL_ROW_HEIGHT, clipBottom);
        if (clippedBottom <= clippedTop) {
            return new SettingRowBounds(0, 0, 0, 0);
        }

        return new SettingRowBounds(left, clippedTop, right, clippedBottom);
    }

    private static SettingRowBounds clippedSettingsToggleBounds(int rowRight, int rowTop, int clipTop, int clipBottom) {
        int boxLeft = rowRight - SETTINGS_TOGGLE_BOX_RIGHT_INSET;
        int boxTop = rowTop + SETTINGS_TOGGLE_BOX_TOP_OFFSET;
        int boxRight = boxLeft + SETTINGS_TOGGLE_BOX_SIZE - 1;
        int boxBottom = boxTop + SETTINGS_TOGGLE_BOX_SIZE - 1;
        int clippedTop = Math.max(boxTop, clipTop);
        int clippedBottom = Math.min(boxBottom, clipBottom - 1);
        if (boxRight < boxLeft || clippedBottom < clippedTop) {
            return new SettingRowBounds(0, 0, 0, 0);
        }

        return new SettingRowBounds(boxLeft, clippedTop, boxRight, clippedBottom);
    }

    private static void drawSettingsScrollBar(GuiGraphics guiGraphics, int right, int bottom, int contentTop, int contentBottom, double alpha, boolean interactive) {
        double maxScroll = maxSettingsMenuScroll();
        if (maxScroll <= 0.0D || contentBottom <= contentTop) {
            if (interactive) {
                clearSettingsScrollBounds();
            }
            return;
        }

        int trackTop = contentTop + 2;
        int trackBottom = contentBottom - 2;
        int trackHeight = trackBottom - trackTop;
        if (trackHeight <= 8) {
            if (interactive) {
                clearSettingsScrollBounds();
            }
            return;
        }

        int thumbHeight = clampInt((int) Math.round(trackHeight * (trackHeight / (trackHeight + maxScroll))), 8, trackHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbTop = trackTop + (int) Math.round(thumbTravel * (settingsMenuScrollOffset / maxScroll));
        int x = right - 10;
        if (interactive) {
            settingsScrollTrackLeft = x - 2;
            settingsScrollTrackTop = trackTop;
            settingsScrollTrackRight = x + 2;
            settingsScrollTrackBottom = trackBottom - 1;
            settingsScrollThumbLeft = x - 1;
            settingsScrollThumbTop = thumbTop;
            settingsScrollThumbRight = x + 1;
            settingsScrollThumbBottom = thumbTop + thumbHeight - 1;
        }
        guiGraphics.fill(x, trackTop, x + 1, trackBottom, withAlpha(0x8055AFFF, alpha));
        guiGraphics.fill(x - 1, thumbTop, x + 2, thumbTop + thumbHeight, withAlpha(UI_BLUE, alpha));
    }

    private static double maxSettingsMenuScroll() {
        return maxSettingsMenuScroll(settingsModalHeight);
    }

    private static double maxSettingsMenuScroll(int modalHeight) {
        int visibleBottomOffset = modalHeight - SETTINGS_MODAL_CONTENT_BOTTOM_INSET;
        return Math.max(0.0D, settingsMenuContentBottomOffset() - visibleBottomOffset);
    }

    private static int settingsMenuContentBottomOffset() {
        int rowCount = settingsMenuRowCount();
        if (rowCount <= 0) {
            return SETTINGS_MODAL_ROW_TOP_OFFSET;
        }

        return SETTINGS_MODAL_ROW_TOP_OFFSET + (rowCount - 1) * SETTINGS_MODAL_ROW_STEP + SETTINGS_MODAL_ROW_HEIGHT;
    }

    private static int settingsMenuRowCount() {
        return settingsMenuPage == SettingsMenuPage.ANIMATIONS ? SETTINGS_ANIMATIONS_ROW_COUNT : SETTINGS_MAIN_ROW_COUNT;
    }

    private static void clampSettingsMenuScroll() {
        settingsMenuScrollOffset = clamp(settingsMenuScrollOffset, 0.0D, maxSettingsMenuScroll());
    }

    private static void drawSettingsResizeHandle(
            GuiGraphics guiGraphics,
            int right,
            int bottom,
            double alpha,
            double mouseX,
            double mouseY,
            boolean interactive
    ) {
        int handleRight = right - 3;
        int handleBottom = bottom - 3;
        int handleLeft = handleRight - SETTINGS_MODAL_RESIZE_HANDLE_SIZE;
        int handleTop = handleBottom - SETTINGS_MODAL_RESIZE_HANDLE_SIZE;
        if (interactive) {
            settingsResizeLeft = handleLeft;
            settingsResizeTop = handleTop;
            settingsResizeRight = handleRight;
            settingsResizeBottom = handleBottom;
        }

        boolean handleHovered = interactive && isInside(mouseX, mouseY, handleLeft, handleTop, handleRight, handleBottom);
        int handleColor = handleHovered || settingsModalResizing ? 0xFFFFFFFF : UI_BLUE;
        for (int i = 0; i < 3; i++) {
            int offset = i * 4;
            guiGraphics.fill(handleRight - 7 - offset, handleBottom - 2, handleRight - 2, handleBottom - 1, withAlpha(handleColor, alpha));
            guiGraphics.fill(handleRight - 2, handleBottom - 7 - offset, handleRight - 1, handleBottom - 2, withAlpha(handleColor, alpha));
        }
    }

    private static void drawSettingsBackButton(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int right,
            int bottom,
            double alpha,
            double mouseX,
            double mouseY,
            boolean interactive
    ) {
        boolean hovered = interactive && isInside(mouseX, mouseY, left, top, right, bottom);
        int border = hovered ? UI_BLUE : 0xFF87909A;
        int arrow = hovered ? UI_BLUE : 0xFFFFFFFF;
        drawRoundedPanel(guiGraphics, left, top, right, bottom, withAlpha(UI_BLACK_ALPHA_50, alpha), withAlpha(border, alpha));
        drawLeftArrow(guiGraphics, left + (right - left - 4) / 2, top + (bottom - top - 7) / 2, withAlpha(arrow, alpha));
    }

    private static void drawSettingsCloseX(GuiGraphics guiGraphics, int left, int top, int buttonSize, int color) {
        int size = 7;
        int x = left + (buttonSize - size) / 2;
        int y = top + (buttonSize - size) / 2;
        for (int i = 0; i < size; i++) {
            guiGraphics.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            guiGraphics.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, color);
        }
    }

    private static void drawSettingsNavigationRow(GuiGraphics guiGraphics, Font font, String label, int left, int top, int right, double alpha) {
        int bottom = top + 22;
        guiGraphics.fill(left, top, right, bottom, withAlpha(UI_BLACK_ALPHA_25, alpha));
        guiGraphics.drawString(font, label, left + 6, top + 7, withAlpha(0xFFFFFFFF, alpha), false);
        drawRightArrow(guiGraphics, right - 12, top + (bottom - top - 7) / 2, withAlpha(0xFFFFFFFF, alpha));
    }

    private static void drawRightArrow(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.fill(x, y, x + 1, y + 1, color);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + 2, color);
        guiGraphics.fill(x + 2, y + 2, x + 3, y + 3, color);
        guiGraphics.fill(x + 3, y + 3, x + 4, y + 4, color);
        guiGraphics.fill(x + 2, y + 4, x + 3, y + 5, color);
        guiGraphics.fill(x + 1, y + 5, x + 2, y + 6, color);
        guiGraphics.fill(x, y + 6, x + 1, y + 7, color);
    }

    private static void drawLeftArrow(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.fill(x + 3, y, x + 4, y + 1, color);
        guiGraphics.fill(x + 2, y + 1, x + 3, y + 2, color);
        guiGraphics.fill(x + 1, y + 2, x + 2, y + 3, color);
        guiGraphics.fill(x, y + 3, x + 1, y + 4, color);
        guiGraphics.fill(x + 1, y + 4, x + 2, y + 5, color);
        guiGraphics.fill(x + 2, y + 5, x + 3, y + 6, color);
        guiGraphics.fill(x + 3, y + 6, x + 4, y + 7, color);
    }

    private static SettingsModalPlacement settingsModalPlacement(int width, int height) {
        double defaultLeft = defaultSettingsModalLeft(width);
        double defaultTop = defaultSettingsModalTop(height);
        double left = clampSettingsModalLeft(defaultLeft + settingsModalOffsetX, width);
        double top = clampSettingsModalTop(defaultTop + settingsModalOffsetY, height);
        settingsModalOffsetX = left - defaultLeft;
        settingsModalOffsetY = top - defaultTop;
        return new SettingsModalPlacement((int) Math.round(left), (int) Math.round(top), settingsModalWidth, settingsModalHeight);
    }

    private static double defaultSettingsModalLeft(int width) {
        return (width - settingsModalWidth) / 2.0D;
    }

    private static double defaultSettingsModalTop(int height) {
        return (height - settingsModalHeight) / 2.0D;
    }

    private static double clampSettingsModalLeft(double left, int width) {
        double min = SETTINGS_MODAL_SCREEN_MARGIN;
        double max = Math.max(min, width - settingsModalWidth - SETTINGS_MODAL_SCREEN_MARGIN);
        return clamp(left, min, max);
    }

    private static double clampSettingsModalTop(double top, int height) {
        double min = SETTINGS_MODAL_SCREEN_MARGIN;
        double max = Math.max(min, height - settingsModalHeight - SETTINGS_MODAL_SCREEN_MARGIN);
        return clamp(top, min, max);
    }

    private static void updateSettingsModalBounds(int left, int top, int modalWidth, int modalHeight) {
        settingsModalLeft = left;
        settingsModalTop = top;
        settingsModalRight = left + modalWidth;
        settingsModalBottom = top + modalHeight;
    }

    private static void drawSettingsToggleRow(GuiGraphics guiGraphics, Font font, String label, int left, int top, int right, boolean checked, double alpha) {
        int bottom = top + 22;
        guiGraphics.fill(left, top, right, bottom, withAlpha(UI_BLACK_ALPHA_25, alpha));
        guiGraphics.drawString(font, label, left + 6, top + 7, withAlpha(0xFFFFFFFF, alpha), false);
        int boxSize = SETTINGS_TOGGLE_BOX_SIZE;
        int boxLeft = right - SETTINGS_TOGGLE_BOX_RIGHT_INSET;
        int boxTop = top + SETTINGS_TOGGLE_BOX_TOP_OFFSET;
        drawRoundedPanel(guiGraphics, boxLeft, boxTop, boxLeft + boxSize, boxTop + boxSize, withAlpha(UI_BLACK, alpha), withAlpha(0xFFFFFFFF, alpha));
        if (checked) {
            guiGraphics.fill(boxLeft + 3, boxTop + 3, boxLeft + boxSize - 3, boxTop + boxSize - 3, withAlpha(UI_BLUE, alpha));
        }
    }

    private static void drawDynamicRadar(
            GuiGraphics guiGraphics,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites,
            boolean drawRadarLocationMarkers
    ) {
        drawDynamicRadar(guiGraphics, view, radarSites, drawRadarLocationMarkers, StormOverlayData.getRadarMode());
    }

    private static void drawDynamicRadar(
            GuiGraphics guiGraphics,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites,
            boolean drawRadarLocationMarkers,
            StormOverlayData.RadarMode mode
    ) {
        drawDynamicRadar(guiGraphics, view, radarSites, drawRadarLocationMarkers, mode, true);
    }

    private static void drawDynamicRadar(
            GuiGraphics guiGraphics,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites,
            boolean drawRadarLocationMarkers,
            StormOverlayData.RadarMode mode,
            boolean drawRangeCircles
    ) {
        drawDynamicRadar(guiGraphics, view, radarSites, drawRadarLocationMarkers, mode, drawRangeCircles, WORLD_MAP_RADAR_ALPHA);
    }

    private static void drawDynamicRadar(
            GuiGraphics guiGraphics,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites,
            boolean drawRadarLocationMarkers,
            StormOverlayData.RadarMode mode,
            boolean drawRangeCircles,
            float radarAlpha
    ) {
        if (radarSites.isEmpty()) {
            clearRadarTextures();
            return;
        }

        int stateHash = StormOverlayData.stateHash(view.dimension(), mode);
        List<StormOverlayData.RadarSiteView> activeSites = activeRadarSites(radarSites);
        List<StormOverlayData.RadarSiteView> textureSites = new ArrayList<>();
        for (StormOverlayData.RadarSiteView site : activeSites) {
            if (site.operational()) {
                textureSites.add(site);
            }
        }

        pruneRadarTextures(textureSites, activeTextureModes());

        for (StormOverlayData.RadarSiteView site : textureSites) {
            drawRadarTexture(
                    guiGraphics,
                    view.dimension(),
                    site,
                    view.screenX(site.x()),
                    view.screenZ(site.z()),
                    view.pixelsPerBlock(),
                    0,
                    0,
                    view.width(),
                    view.height(),
                    mode,
                    stateHash,
                    radarAlpha
            );
        }

        if (drawRangeCircles) {
            drawRadarRangeCircles(guiGraphics, view, radarSites);
        }
    }

    private static boolean shouldDrawRadarLocationMarkers() {
        return stationLabelsOpen || StormOverlayData.isRadarLocationsAlwaysVisible();
    }

    public static boolean drawRadarTexture(
            GuiGraphics guiGraphics,
            ResourceKey<Level> dimension,
            StormOverlayData.RadarSiteView site,
            double centerX,
            double centerY,
            double pixelsPerBlock,
            int minX,
            int minY,
            int maxX,
            int maxY
    ) {
        return drawRadarTexture(
                guiGraphics,
                dimension,
                site,
                centerX,
                centerY,
                pixelsPerBlock,
                minX,
                minY,
                maxX,
                maxY,
                StormOverlayData.getRadarMode(),
                StormOverlayData.stateHash(dimension),
                1.0F
        );
    }

    public static boolean drawRadarTexture(
            GuiGraphics guiGraphics,
            ResourceKey<Level> dimension,
            StormOverlayData.RadarSiteView site,
            double centerX,
            double centerY,
            double pixelsPerBlock,
            int minX,
            int minY,
            int maxX,
            int maxY,
            StormOverlayData.RadarMode mode,
            int stateHash
    ) {
        return drawRadarTexture(
                guiGraphics,
                dimension,
                site,
                centerX,
                centerY,
                pixelsPerBlock,
                minX,
                minY,
                maxX,
                maxY,
                mode,
                stateHash,
                1.0F
        );
    }

    private static boolean drawRadarTexture(
            GuiGraphics guiGraphics,
            ResourceKey<Level> dimension,
            StormOverlayData.RadarSiteView site,
            double centerX,
            double centerY,
            double pixelsPerBlock,
            int minX,
            int minY,
            int maxX,
            int maxY,
            StormOverlayData.RadarMode mode,
            int stateHash,
            float radarAlpha
    ) {
        if (site == null || !site.operational()) {
            return false;
        }

        double radius = site.radiusBlocks() * pixelsPerBlock;
        if (radius < 1.0D
                || centerX + radius < minX
                || centerX - radius > maxX
                || centerY + radius < minY
                || centerY - radius > maxY) {
            return false;
        }

        SiteTextureCache cache = siteTexture(dimension, site, mode, stateHash);
        if (cache == null || !cache.hasTexture()) {
            return false;
        }

        drawRadarAnnulusTexture(guiGraphics, cache.location(), centerX, centerY, radius, site.radiusBlocks(), cache.size(), radarAlpha);
        return true;
    }

    private static void drawRadarAnnulusTexture(
            GuiGraphics guiGraphics,
            ResourceLocation location,
            double centerX,
            double centerY,
            double outerRadiusPixels,
            double radiusBlocks,
            int textureSize
    ) {
        drawRadarAnnulusTexture(guiGraphics, location, centerX, centerY, outerRadiusPixels, radiusBlocks, textureSize, 1.0F);
    }

    private static void drawRadarAnnulusTexture(
            GuiGraphics guiGraphics,
            ResourceLocation location,
            double centerX,
            double centerY,
            double outerRadiusPixels,
            double radiusBlocks,
            int textureSize,
            float radarAlpha
    ) {
        double innerRatio = clamp(RADAR_BLIND_SPOT_RADIUS_BLOCKS / radiusBlocks, 0.0D, 0.95D);
        double innerRadiusPixels = outerRadiusPixels * innerRatio;
        int segments = clampInt((int) Math.ceil(outerRadiusPixels / 1.35D), 128, 1024);

        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float) clamp(radarAlpha, 0.0D, 1.0D));
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX);

        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            float outerU = (float) (0.5D + cos * 0.5D);
            float outerV = (float) (0.5D + sin * 0.5D);
            float innerU = (float) (0.5D + cos * innerRatio * 0.5D);
            float innerV = (float) (0.5D + sin * innerRatio * 0.5D);

            bufferBuilder.addVertex(matrix, (float) (centerX + cos * outerRadiusPixels), (float) (centerY + sin * outerRadiusPixels), 0.0F)
                    .setUv(outerU, outerV);
            bufferBuilder.addVertex(matrix, (float) (centerX + cos * innerRadiusPixels), (float) (centerY + sin * innerRadiusPixels), 0.0F)
                    .setUv(innerU, innerV);
        }

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static SiteTextureCache siteTexture(
            ResourceKey<Level> dimension,
            StormOverlayData.RadarSiteView site,
            StormOverlayData.RadarMode mode,
            int stateHash
    ) {
        int textureSize = radarTextureSize();
        RadarTextureKey key = new RadarTextureKey(site.pos(), mode);
        SiteTextureCache existing = radarTextures.get(key);
        if (existing != null && existing.matches(site.pos(), stateHash, textureSize)) {
            return existing;
        }

        PendingSiteTextureBuild pending = pendingRadarTextures.get(key);
        if (pending == null || !pending.matches(dimension, site.pos(), mode, stateHash, textureSize)) {
            if (pending != null) {
                pending.close();
            }

            pending = new PendingSiteTextureBuild(dimension, site, mode, stateHash, textureSize);
            pendingRadarTextures.put(key, pending);
        }

        pending.advance(textureSize);
        if (pending.complete()) {
            if (existing != null) {
                existing.close();
            }

            SiteTextureCache cache = pending.toCache();
            pendingRadarTextures.remove(key);
            radarTextures.put(key, cache);
            return cache;
        }

        return existing;
    }

    private static int radarTextureSize() {
        int textureSize = ClientConfig.radarResolution > 0 ? ClientConfig.radarResolution : DEFAULT_RADAR_TEXTURE_SIZE;
        return clampInt(textureSize, MIN_RADAR_TEXTURE_SIZE, MAX_RADAR_TEXTURE_SIZE);
    }

    private static ResourceLocation textureLocation(BlockPos pos, StormOverlayData.RadarMode mode) {
        String path = "radar_site/"
                + Integer.toHexString(pos.getX())
                + "_"
                + Integer.toHexString(pos.getY())
                + "_"
                + Integer.toHexString(pos.getZ())
                + "_"
                + mode.name().toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath("pmradar", path);
    }

    private static Set<StormOverlayData.RadarMode> activeTextureModes() {
        Set<StormOverlayData.RadarMode> modes = new HashSet<>();
        if (StormOverlayData.isDualModeEnabled()) {
            modes.add(StormOverlayData.getDualUpperMode());
            modes.add(StormOverlayData.getDualLowerMode());
            return modes;
        }

        StormOverlayData.RadarMode primary = StormOverlayData.getRadarMode();
        modes.add(primary);
        return modes;
    }

    private static void pruneRadarTextures(List<StormOverlayData.RadarSiteView> activeSites, Set<StormOverlayData.RadarMode> activeModes) {
        Set<BlockPos> activePositions = new HashSet<>();
        for (StormOverlayData.RadarSiteView site : activeSites) {
            activePositions.add(site.pos());
        }

        Iterator<Map.Entry<RadarTextureKey, SiteTextureCache>> iterator = radarTextures.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RadarTextureKey, SiteTextureCache> entry = iterator.next();
            if (activePositions.contains(entry.getKey().pos()) && activeModes.contains(entry.getKey().mode())) {
                continue;
            }

            entry.getValue().close();
            iterator.remove();
        }

        Iterator<Map.Entry<RadarTextureKey, PendingSiteTextureBuild>> pendingIterator = pendingRadarTextures.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<RadarTextureKey, PendingSiteTextureBuild> entry = pendingIterator.next();
            if (activePositions.contains(entry.getKey().pos()) && activeModes.contains(entry.getKey().mode())) {
                continue;
            }

            entry.getValue().close();
            pendingIterator.remove();
        }

    }

    public static void clearRadarTextures() {
        for (SiteTextureCache cache : radarTextures.values()) {
            cache.close();
        }

        radarTextures.clear();
        for (PendingSiteTextureBuild pending : pendingRadarTextures.values()) {
            pending.close();
        }

        pendingRadarTextures.clear();
    }

    public static RadarTextureSample radarTextureSample(ResourceKey<Level> dimension, StormOverlayData.RadarSiteView site) {
        if (site == null || !site.operational()) {
            return null;
        }

        SiteTextureCache cache = siteTexture(dimension, site, StormOverlayData.getRadarMode(), StormOverlayData.stateHash(dimension));
        if (cache == null || !cache.hasTexture() || cache.argbPixels() == null) {
            return null;
        }

        return new RadarTextureSample(site.x(), site.z(), site.radiusBlocks(), cache.size(), cache.argbPixels());
    }

    private static int argbToAbgr(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    private static int radarBlockTextureArgb(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha == 0) {
            return 0;
        }

        float brightness = alpha / 255.0F * 0.75F + 0.25F;
        int red = clampInt(Math.round(((argb >>> 16) & 0xFF) * brightness), 0, 255);
        int green = clampInt(Math.round(((argb >>> 8) & 0xFF) * brightness), 0, 255);
        int blue = clampInt(Math.round((argb & 0xFF) * brightness), 0, 255);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static void drawLightningStrikes(
            GuiGraphics guiGraphics,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites,
            List<StormOverlayData.LightningStrikeView> strikes
    ) {
        if (strikes.isEmpty() || radarSites.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (StormOverlayData.LightningStrikeView strike : strikes) {
            if (!isInsideActiveRadarCoverage(strike.x(), strike.z(), radarSites)) {
                continue;
            }

            float alpha = (float) clamp(strike.alpha(), 0.0F, 1.0F);
            if (alpha <= 0.02F) {
                continue;
            }

            double centerX = view.screenX(strike.x());
            double centerY = view.screenZ(strike.z());
            int size = LIGHTNING_ICON_DRAW_SIZE;
            int left = (int) Math.round(centerX - size * 0.5D);
            int top = (int) Math.round(centerY - size * 0.5D);
            if (left > view.width() || left + size < 0 || top > view.height() || top + size < 0) {
                continue;
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blit(
                    LIGHTNING_ICON,
                    left,
                    top,
                    size,
                    size,
                    0.0F,
                    0.0F,
                    LIGHTNING_ICON_TEXTURE_SIZE,
                    LIGHTNING_ICON_TEXTURE_SIZE,
                    LIGHTNING_ICON_TEXTURE_SIZE,
                    LIGHTNING_ICON_TEXTURE_SIZE
            );
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static boolean isInsideActiveRadarCoverage(
            double blockX,
            double blockZ,
            List<StormOverlayData.RadarSiteView> radarSites
    ) {
        double blindSpotSqr = RADAR_BLIND_SPOT_RADIUS_BLOCKS * RADAR_BLIND_SPOT_RADIUS_BLOCKS;
        for (StormOverlayData.RadarSiteView site : activeRadarSites(radarSites)) {
            if (!site.operational()) {
                continue;
            }

            double dx = blockX - site.x();
            double dz = blockZ - site.z();
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr <= site.radiusBlocks() * site.radiusBlocks() && distanceSqr >= blindSpotSqr) {
                return true;
            }
        }

        return false;
    }

    private static void drawRadarRangeCircles(
            GuiGraphics guiGraphics,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites
    ) {
        for (StormOverlayData.RadarSiteView site : activeRadarSites(radarSites)) {
            double centerX = view.screenX(site.x());
            double centerY = view.screenZ(site.z());
            int color = site.operational() ? 0x55DDE6EF : 0x88FF3030;
            drawCircleOutline(guiGraphics, centerX, centerY, site.radiusBlocks() * view.pixelsPerBlock(), color);
        }
    }

    private static void drawCircleOutline(GuiGraphics guiGraphics, double centerX, double centerY, double radius, int color) {
        if (radius < 2.0D) {
            return;
        }

        double thickness = Math.max(1.0D, Math.min(2.5D, radius / 220.0D));
        double outerRadius = radius + thickness * 0.5D;
        double innerRadius = Math.max(0.0D, radius - thickness * 0.5D);
        int segments = clampInt((int) Math.ceil(radius / 1.35D), 128, 1024);
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            bufferBuilder.addVertex(matrix, (float) (centerX + cos * outerRadius), (float) (centerY + sin * outerRadius), 0.0F)
                    .setColor(red, green, blue, alpha);
            bufferBuilder.addVertex(matrix, (float) (centerX + cos * innerRadius), (float) (centerY + sin * innerRadius), 0.0F)
                    .setColor(red, green, blue, alpha);
        }

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void drawStationLabels(
            GuiGraphics guiGraphics,
            Font font,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites
    ) {
        if (!shouldDrawRadarLocationMarkers() || radarSites.isEmpty()) {
            stationLabelBounds = List.of();
            return;
        }

        List<StationLabelBounds> bounds = new ArrayList<>();
        drawStationLabels(guiGraphics, font, view, radarSites, bounds, 0, 0);
        stationLabelBounds = List.copyOf(bounds);
    }

    private static void drawStationLabels(
            GuiGraphics guiGraphics,
            Font font,
            MapView view,
            List<StormOverlayData.RadarSiteView> radarSites,
            List<StationLabelBounds> bounds,
            int boundsOffsetX,
            int boundsOffsetY
    ) {
        for (StormOverlayData.RadarSiteView site : radarSites) {
            String label = site.stationCode();
            int labelWidth = font.width(label) + 10;
            int labelHeight = font.lineHeight + 7;
            double labelScale = 0.75D;
            double left = snapToScreenPixel(view.screenX(site.x()) - labelWidth * labelScale * 0.5D);
            double top = snapToScreenPixel(view.screenZ(site.z()) - labelHeight * labelScale * 0.5D);
            double right = left + labelWidth * labelScale;
            double bottom = top + labelHeight * labelScale;
            if (right < 0 || left > view.width() || bottom < 0 || top > view.height()) {
                continue;
            }

            int fill = site.operational()
                    ? site.selected() ? 0xFF1594E8 : UI_BLACK_ALPHA_88
                    : site.selected() ? 0xFFB01818 : 0xE03A1515;
            int border = site.operational()
                    ? site.selected() ? UI_BLUE : 0xFFFFFFFF
                    : 0xFFFF5050;

            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().translate(left, top, 0.0D);
                guiGraphics.pose().scale((float) labelScale, (float) labelScale, 1.0F);
                guiGraphics.fill(0, 0, labelWidth, labelHeight, fill);
                guiGraphics.fill(0, 0, labelWidth, 1, border);
                guiGraphics.fill(0, labelHeight - 1, labelWidth, labelHeight, border);
                guiGraphics.fill(0, 0, 1, labelHeight, border);
                guiGraphics.fill(labelWidth - 1, 0, labelWidth, labelHeight, border);
                guiGraphics.drawString(font, label, 5, 4, 0xFFFFFFFF, false);
            } finally {
                guiGraphics.pose().popPose();
            }
            bounds.add(new StationLabelBounds(
                    site.pos(),
                    (int) Math.floor(left + boundsOffsetX),
                    (int) Math.floor(top + boundsOffsetY),
                    (int) Math.ceil(right + boundsOffsetX),
                    (int) Math.ceil(bottom + boundsOffsetY)
            ));
        }
    }

    private static List<StormOverlayData.RadarSiteView> activeRadarSites(List<StormOverlayData.RadarSiteView> radarSites) {
        for (StormOverlayData.RadarSiteView site : radarSites) {
            if (site.selected()) {
                return List.of(site);
            }
        }

        return radarSites;
    }

    private static void drawModeList(
            GuiGraphics guiGraphics,
            Font font,
            int screenWidth,
            double selectedBoxLeft,
            int labelTop,
            int labelWidth,
            int labelHeight,
            float scale,
            StormOverlayData.RadarMode activeMode,
            ModeControlTarget target,
            double mouseX,
            double mouseY,
            double animationProgress,
            double changeAnimationProgress
    ) {
        List<StormOverlayData.RadarMode> currentStack = currentModeStackFor(activeMode);
        List<StormOverlayData.RadarMode> modes = currentStack.subList(0, currentStack.size() - 1);
        if (modes.isEmpty()) {
            clearModeMenuBounds();
            return;
        }

        int textHeight = modeMenuTextHeight(font, scale);
        double textInset = controlTextInset(font, scale, labelHeight);
        double listWidth = modeMenuListWidth(font, labelWidth, scale, currentStack, textInset);
        double selectedLineTop = labelTop;
        double selectedBottom = selectedLineTop + labelHeight;
        double listHeight = modeMenuListHeight(modes.size(), textHeight);
        double panelTop = Math.max(4.0D, selectedLineTop - listHeight);
        double widthProgress = modeMenuWidthProgress(animationProgress);
        double listProgress = modeMenuListProgress(animationProgress);
        double slidingPanelTop = selectedLineTop - (selectedLineTop - panelTop) * listProgress;
        double panelBottom = selectedBottom;
        double x = selectedBoxLeft;
        double textVisibleLeft = x + textInset;
        double closedBoxRight = textVisibleLeft + labelWidth + textInset;
        double fullBoxRight = x + listWidth;
        if (screenWidth > 0 && fullBoxRight > screenWidth - 4.0D) {
            x = Math.max(4.0D, x - (fullBoxRight - (screenWidth - 4.0D)));
            textVisibleLeft = x + textInset;
            closedBoxRight = textVisibleLeft + labelWidth + textInset;
            fullBoxRight = x + listWidth;
        }
        double selectedBoxRight = lerp(closedBoxRight, fullBoxRight, widthProgress);
        int left = (int) Math.floor(x);
        int right = (int) Math.ceil(selectedBoxRight);
        int fullRight = (int) Math.ceil(fullBoxRight);
        double inactiveRowStep = textHeight + MODE_MENU_LIST_ROW_GAP;
        double[] slotTops = new double[currentStack.size()];
        for (int i = 0; i < modes.size(); i++) {
            double openRowTop = panelTop + MODE_MENU_LIST_TOP_PADDING + i * inactiveRowStep;
            slotTops[i] = openRowTop + (selectedLineTop - openRowTop) * (1.0D - listProgress);
        }
        int selectedSlot = currentStack.size() - 1;
        String selectedName = activeMode.displayName().toUpperCase(Locale.ROOT);
        slotTops[selectedSlot] = visibleTextTopY(
                font,
                selectedName,
                selectedLineTop,
                selectedBottom - selectedLineTop,
                scale
        );

        List<ModeLabelBounds> bounds = modeHitBounds(
                currentStack,
                selectedSlot,
                slotTops,
                left,
                fullRight,
                textHeight,
                (int) Math.floor(slidingPanelTop),
                (int) Math.floor(selectedLineTop),
                target
        );
        StormOverlayData.RadarMode hoveredMode = hoveredMode(mouseX, mouseY, bounds);
        fillTranslatedRect(guiGraphics, x, selectedLineTop, selectedBoxRight, panelBottom, MODE_MENU_BACKGROUND);
        if (listProgress > 0.0D && slidingPanelTop < selectedLineTop) {
            fillTranslatedRect(guiGraphics, x, slidingPanelTop, selectedBoxRight, selectedLineTop, MODE_MENU_BACKGROUND);
            drawOpenBottomBorder(guiGraphics, left, slidingPanelTop, right, selectedLineTop, MODE_MENU_BORDER);

            int scissorTop = (int) Math.floor(slidingPanelTop);
            int selectedClipTop = (int) Math.floor(selectedLineTop);
            if (scissorTop < selectedClipTop) {
                guiGraphics.enableScissor(left, scissorTop, right, selectedClipTop);
                try {
                    drawModeStackLabels(
                            guiGraphics,
                            font,
                            activeMode,
                            currentStack,
                            slotTops,
                            selectedSlot,
                            left,
                            right,
                            textVisibleLeft,
                            textHeight,
                            scale,
                            hoveredMode,
                            changeAnimationProgress,
                            scissorTop,
                            selectedClipTop,
                            false
                    );
                } finally {
                    guiGraphics.disableScissor();
                }
            }
        }

        int selectedClipTop = (int) Math.floor(selectedLineTop);
        int selectedClipBottom = (int) Math.ceil(selectedBottom);
        drawTranslatedRectBorder(guiGraphics, left, selectedLineTop, right, selectedBottom, UI_BLUE);
        guiGraphics.enableScissor(left, selectedClipTop, right, selectedClipBottom);
        try {
            drawModeStackLabels(
                    guiGraphics,
                    font,
                    activeMode,
                    currentStack,
                    slotTops,
                    selectedSlot,
                    left,
                    right,
                    textVisibleLeft,
                    textHeight,
                    scale,
                    hoveredMode,
                    changeAnimationProgress,
                    selectedClipTop,
                    selectedClipBottom,
                    true
            );
        } finally {
            guiGraphics.disableScissor();
        }

        modeMenuLeft = left - 4;
        modeMenuTop = (int) Math.floor(slidingPanelTop) - 4;
        modeMenuRight = right + 4;
        modeMenuBottom = (int) Math.ceil(panelBottom) + 4;
        modeLabelBounds = List.copyOf(bounds);
    }

    private static void drawModeStackLabels(
            GuiGraphics guiGraphics,
            Font font,
            StormOverlayData.RadarMode activeMode,
            List<StormOverlayData.RadarMode> currentStack,
            double[] slotTops,
            int selectedSlot,
            int left,
            int right,
            double textVisibleLeft,
            int textHeight,
            float scale,
            StormOverlayData.RadarMode hoveredMode,
            double changeAnimationProgress,
            int clipTop,
            int clipBottom,
            boolean selectedClip
    ) {
        boolean changingMode = !modeChangeFromStack.isEmpty()
                && !modeChangeToStack.isEmpty()
                && selectedModeInStack(modeChangeToStack) == activeMode
                && changeAnimationProgress < 1.0D;
        List<StormOverlayData.RadarMode> fromStack = changingMode ? modeChangeFromStack : currentStack;
        List<StormOverlayData.RadarMode> toStack = changingMode ? modeChangeToStack : currentStack;
        double easedChange = changingMode ? easeModeMenuAnimation(changeAnimationProgress) : 1.0D;

        for (StormOverlayData.RadarMode mode : StormOverlayData.RadarMode.values()) {
            int fromSlot = modeStackIndex(fromStack, mode);
            int toSlot = modeStackIndex(toStack, mode);
            if (fromSlot < 0 || toSlot < 0 || fromSlot >= slotTops.length || toSlot >= slotTops.length) {
                continue;
            }

            if (selectedClip) {
                boolean selectedLabel = changingMode
                        ? fromSlot == selectedSlot || toSlot == selectedSlot
                        : mode == activeMode;
                if (!selectedLabel) {
                    continue;
                }
            }

            String name = mode.displayName().toUpperCase(Locale.ROOT);
            double rowTop = lerp(slotTops[fromSlot], slotTops[toSlot], easedChange);
            TextPixelBounds textBounds = textPixelBounds(font, name);
            float xScale = controlTextXScale(font, name, scale);
            double textDrawY = rowTop - textBounds.top() * scale;
            double rowBottom = rowTop + Math.max(1.0D, (textBounds.bottom() - textBounds.top()) * scale);
            int rowBoundsTop = Math.max((int) Math.floor(rowTop), clipTop);
            int rowBoundsBottom = Math.min((int) Math.ceil(rowBottom), clipBottom);
            if (rowBoundsBottom <= rowBoundsTop) {
                continue;
            }

            boolean clickable = !selectedClip && mode != activeMode;
            boolean hovered = clickable && mode == hoveredMode;
            int textColor = selectedClip || hovered ? UI_BLUE : MODE_MENU_TEXT;
            double textX = textVisibleLeft - textBounds.left() * xScale;
            if (CONTROL_TEXT_BOUNDS_DEBUG) {
                drawTextBoundsDebug(guiGraphics, font, name, textX, textDrawY, scale);
            }
            drawModeText(guiGraphics, font, name, textX, textDrawY, textColor, scale);
            if (hovered) {
                int underlineLeft = (int) Math.floor(textX + textBounds.left() * xScale);
                int underlineRight = (int) Math.ceil(textX + textBounds.right() * xScale);
                int underlineY = Math.min((int) Math.ceil(rowBottom + 1.0D), clipBottom - 1);
                if (underlineRight > underlineLeft && underlineY >= clipTop) {
                    guiGraphics.fill(underlineLeft, underlineY, underlineRight, underlineY + 1, UI_BLUE);
                }
            }
        }
    }

    private static List<ModeLabelBounds> modeHitBounds(
            List<StormOverlayData.RadarMode> currentStack,
            int selectedSlot,
            double[] slotTops,
            int left,
            int right,
            int textHeight,
            int clipTop,
            int clipBottom,
            ModeControlTarget target
    ) {
        List<ModeLabelBounds> bounds = new ArrayList<>(selectedSlot);
        for (int i = 0; i < selectedSlot; i++) {
            double rowTop = slotTops[i];
            double rowBottom = rowTop + textHeight;
            int rowBoundsTop = Math.max((int) Math.floor(rowTop) - 2, clipTop);
            int rowBoundsBottom = Math.min((int) Math.ceil(rowBottom) + 3, clipBottom);
            if (rowBoundsBottom > rowBoundsTop) {
                bounds.add(new ModeLabelBounds(target, currentStack.get(i), left, rowBoundsTop, right, rowBoundsBottom));
            }
        }

        return bounds;
    }

    private static StormOverlayData.RadarMode hoveredMode(double mouseX, double mouseY, List<ModeLabelBounds> bounds) {
        for (ModeLabelBounds bound : bounds) {
            if (isInside(mouseX, mouseY, bound.left(), bound.top(), bound.right(), bound.bottom())) {
                return bound.mode();
            }
        }

        return null;
    }

    private static List<StormOverlayData.RadarMode> modeStackFor(StormOverlayData.RadarMode active) {
        List<StormOverlayData.RadarMode> modes = new ArrayList<>();
        for (StormOverlayData.RadarMode mode : StormOverlayData.RadarMode.values()) {
            if (mode != active) {
                modes.add(mode);
            }
        }

        modes.add(active);
        return modes;
    }

    private static List<StormOverlayData.RadarMode> currentModeStackFor(StormOverlayData.RadarMode active) {
        if (isCompleteModeStack(modeVisualStack) && selectedModeInStack(modeVisualStack) == active) {
            return modeVisualStack;
        }

        List<StormOverlayData.RadarMode> stack = modeStackFor(active);
        modeVisualStack = List.copyOf(stack);
        return modeVisualStack;
    }

    private static StormOverlayData.RadarMode selectedModeInStack(List<StormOverlayData.RadarMode> stack) {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    private static boolean isCompleteModeStack(List<StormOverlayData.RadarMode> stack) {
        if (stack.size() != StormOverlayData.RadarMode.values().length) {
            return false;
        }

        Set<StormOverlayData.RadarMode> modes = new HashSet<>(stack);
        return modes.size() == StormOverlayData.RadarMode.values().length;
    }

    private static StormOverlayData.RadarMode modeSelectedByStackRotation(
            List<StormOverlayData.RadarMode> stack,
            boolean rotateUp,
            StormOverlayData.RadarMode fallback
    ) {
        if (!isCompleteModeStack(stack)) {
            return fallback;
        }

        return rotateUp ? stack.get(0) : stack.get(stack.size() - 2);
    }

    private static List<StormOverlayData.RadarMode> rotatedModeStackToSelected(
            List<StormOverlayData.RadarMode> sourceStack,
            StormOverlayData.RadarMode selectedMode,
            boolean rotateUp
    ) {
        if (!isCompleteModeStack(sourceStack)) {
            return modeStackFor(selectedMode);
        }

        List<StormOverlayData.RadarMode> rotated = new ArrayList<>(sourceStack);
        for (int i = 0; i < rotated.size(); i++) {
            if (selectedModeInStack(rotated) == selectedMode) {
                return List.copyOf(rotated);
            }

            if (rotateUp) {
                StormOverlayData.RadarMode top = rotated.remove(0);
                rotated.add(top);
            } else {
                StormOverlayData.RadarMode bottom = rotated.remove(rotated.size() - 1);
                rotated.add(0, bottom);
            }
        }

        return modeStackFor(selectedMode);
    }

    private static int modeStackIndex(List<StormOverlayData.RadarMode> stack, StormOverlayData.RadarMode mode) {
        for (int i = 0; i < stack.size(); i++) {
            if (stack.get(i) == mode) {
                return i;
            }
        }

        return -1;
    }

    private static List<StormOverlayData.RadarMode> inactiveModes() {
        StormOverlayData.RadarMode active = StormOverlayData.getRadarMode();
        List<StormOverlayData.RadarMode> modes = new ArrayList<>();

        for (StormOverlayData.RadarMode mode : StormOverlayData.RadarMode.values()) {
            if (mode != active) {
                modes.add(mode);
            }
        }

        return modes;
    }

    private static void drawRadarBar(
            GuiGraphics guiGraphics,
            StormOverlayData.RadarMode mode,
            int x,
            int y,
            int width,
            double height
    ) {
        for (int offset = 0; offset < width; offset++) {
            float amount = width <= 1 ? 0.0F : offset / (float) (width - 1);
            fillTranslatedRect(guiGraphics, x + offset, y, x + offset + 1, y + height, legendColorFor(mode, amount));
        }

        drawTranslatedRectBorder(
                guiGraphics,
                x - 1.0D,
                y - 1.0D,
                x + width + 1.0D,
                y + height + 1.0D,
                MODE_MENU_BACKGROUND
        );
    }

    private static int legendColorFor(StormOverlayData.RadarMode mode, float amount) {
        float normalized = (float) clamp(amount, 0.0F, 1.0F);
        Color color = switch (mode) {
            case REFLECTIVITY -> brighten(
                    ColorTables.getReflectivity(
                            REFLECTIVITY_LEGEND_MIN_DBZ + normalized * (REFLECTIVITY_LEGEND_MAX_DBZ - REFLECTIVITY_LEGEND_MIN_DBZ),
                            REFLECTIVITY_BASE
                    ),
                    1.2F
            );
            case VELOCITY -> {
                float velocity = -VELOCITY_LEGEND_MAX_MPH + normalized * VELOCITY_LEGEND_MAX_MPH * 2.0F;
                yield StormOverlayData.colorForVelocity(velocity / 1.75F);
            }
            case CORRELATION_COEFFICIENT -> correlationCoefficientLegendColor(normalized);
        };

        return argb(color);
    }

    private static Color correlationCoefficientLegendColor(float coefficient) {
        float cc = CORRELATION_MIN + (float) clamp(coefficient, 0.0F, 1.0F) * (CORRELATION_MAX - CORRELATION_MIN);
        Color color = new Color(0x1E1E1E);
        color = ColorTables.lerp(correlationStep(cc, 0.208F, 0.330F), color, new Color(0x2C2B30));
        color = ColorTables.lerp(correlationStep(cc, 0.330F, 0.455F), color, new Color(0x94939B));
        color = ColorTables.lerp(correlationStep(cc, 0.455F, 0.505F), color, new Color(0xE8E8F0));
        color = ColorTables.lerp(correlationStep(cc, 0.505F, 0.560F), color, new Color(0x80809A));
        color = ColorTables.lerp(correlationStep(cc, 0.560F, 0.650F), color, new Color(0x353491));
        color = ColorTables.lerp(correlationStep(cc, 0.650F, 0.720F), color, new Color(0x0E0AB9));
        color = ColorTables.lerp(correlationStep(cc, 0.720F, 0.770F), color, new Color(0x322FD6));
        color = ColorTables.lerp(correlationStep(cc, 0.770F, 0.805F), color, new Color(0x817ED9));
        color = ColorTables.lerp(correlationStep(cc, 0.805F, 0.835F), color, new Color(0x73C19A));
        color = ColorTables.lerp(correlationStep(cc, 0.835F, 0.865F), color, new Color(0x5EFC50));
        color = ColorTables.lerp(correlationStep(cc, 0.865F, 0.895F), color, new Color(0xA0CD01));
        color = ColorTables.lerp(correlationStep(cc, 0.895F, 0.925F), color, new Color(0xE2C500));
        color = ColorTables.lerp(correlationStep(cc, 0.925F, 0.950F), color, new Color(0xFF4A00));
        color = ColorTables.lerp(correlationStep(cc, 0.950F, 0.970F), color, new Color(0xCF0805));
        color = ColorTables.lerp(correlationStep(cc, 0.970F, 0.990F), color, new Color(0x970546));
        color = ColorTables.lerp(correlationStep(cc, 0.990F, 1.010F), color, new Color(0xAE2E78));
        color = ColorTables.lerp(correlationStep(cc, 1.010F, 1.030F), color, new Color(0xE998C1));
        return ColorTables.lerp(correlationStep(cc, 1.030F, CORRELATION_MAX), color, new Color(0xFFFFFF));
    }

    private static float correlationStep(float value, float low, float high) {
        return (float) clamp((value - low) / (high - low), 0.0F, 1.0F);
    }

    private static Color brighten(Color color, float factor) {
        return new Color(
                Math.min(255, Math.round(color.getRed() * factor)),
                Math.min(255, Math.round(color.getGreen() * factor)),
                Math.min(255, Math.round(color.getBlue() * factor))
        );
    }

    private static void drawPlainString(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color, false);
    }

    private static void drawScaledPlainString(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            int x,
            int y,
            int color,
            float scale
    ) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        drawPlainString(guiGraphics, font, text, 0, 0, color);
        guiGraphics.pose().popPose();
    }

    private static void drawModeText(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            double x,
            double y,
            int color,
            float scale
    ) {
        if (CONTROL_TEXT_BOUNDS_DEBUG) {
            color = CONTROL_TEXT_BOUNDS_TEXT;
        }

        double drawX = snappedTextCoordinate(x);
        double drawY = snappedTextCoordinate(y);
        float xScale = controlTextXScale(font, text, scale);
        if (xScale == 1.0F && scale == 1.0F) {
            guiGraphics.drawString(font, text, (int) Math.round(drawX), (int) Math.round(drawY), color, false);
            return;
        }

        drawScaledString(guiGraphics, font, text, drawX, drawY, color, xScale, scale);
    }

    private static void drawScaledString(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            double x,
            double y,
            int color,
            float xScale,
            float yScale
    ) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) x, (float) y, 0.0F);
        guiGraphics.pose().scale(xScale, yScale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private static double screenPixelsToGuiUnits(int pixels) {
        double guiScale = currentGuiScale();
        return guiScale <= 0.0D ? pixels : pixels / guiScale;
    }

    private static double currentGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1.0D;
        }

        return minecraft.getWindow().getGuiScale();
    }

    private static double snappedTextCoordinate(double guiValue) {
        return snapToScreenPixel(guiValue);
    }

    private static double selectedModeTextY(int labelTop) {
        return labelTop + MODE_MENU_SELECTED_TEXT_Y_OFFSET;
    }

    private static double snapToScreenPixel(double guiValue) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return Math.round(guiValue);
        }

        double guiScale = minecraft.getWindow().getGuiScale();
        return guiScale <= 0.0D ? Math.round(guiValue) : Math.round(guiValue * guiScale) / guiScale;
    }

    private static double alignToScreenPixelPhase(double guiValue, double referenceGuiValue) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return guiValue;
        }

        double guiScale = minecraft.getWindow().getGuiScale();
        if (guiScale <= 0.0D) {
            return guiValue;
        }

        double deltaPixels = referenceGuiValue * guiScale - guiValue * guiScale;
        deltaPixels -= Math.floor(deltaPixels);
        if (deltaPixels > 0.5D) {
            deltaPixels -= 1.0D;
        }
        return guiValue + deltaPixels / guiScale;
    }

    private static double updateModeMenuAnimation(boolean opening) {
        long now = System.nanoTime();
        if (modeMenuAnimationLastNanos == 0L) {
            modeMenuAnimationLastNanos = now;
        }

        double deltaSeconds = Math.min(0.05D, (now - modeMenuAnimationLastNanos) / 1_000_000_000.0D);
        modeMenuAnimationLastNanos = now;
        double step = deltaSeconds / MODE_MENU_ANIMATION_SECONDS;
        if (opening) {
            modeMenuAnimationProgress = Math.min(1.0D, modeMenuAnimationProgress + step);
        } else {
            modeMenuAnimationProgress = Math.max(0.0D, modeMenuAnimationProgress - step);
        }

        return modeMenuAnimationProgress;
    }

    private static double modeMenuWidthProgress(double progress) {
        return easeModeMenuAnimation(clamp(progress / MODE_MENU_LIST_REVEAL_START, 0.0D, 1.0D));
    }

    private static double modeMenuListProgress(double progress) {
        return easeModeMenuAnimation(clamp(
                (progress - MODE_MENU_LIST_REVEAL_START) / (1.0D - MODE_MENU_LIST_REVEAL_START),
                0.0D,
                1.0D
        ));
    }

    private static double updateRadarToolsOpenAnimation() {
        if (!radarToolsOpenAnimationActive) {
            return 1.0D;
        }

        long now = System.nanoTime();
        if (radarToolsOpenAnimationStartNanos == 0L) {
            radarToolsOpenAnimationStartNanos = now;
        }

        double elapsedSeconds = (now - radarToolsOpenAnimationStartNanos) / 1_000_000_000.0D;
        double progress = clamp(elapsedSeconds / RADAR_TOOLS_OPEN_ANIMATION_SECONDS, 0.0D, 1.0D);
        if (progress >= 1.0D) {
            radarToolsOpenAnimationActive = false;
            radarToolsOpenAnimationStartNanos = 0L;
        }

        return progress;
    }

    private static double radarToolsPanelAlpha(double progress) {
        return easeRadarToolsPanelFade(clamp(
                (progress - RADAR_TOOLS_OPEN_MENU_FADE_START) / (1.0D - RADAR_TOOLS_OPEN_MENU_FADE_START),
                0.0D,
                1.0D
        ));
    }

    private static double updateRadarToolsCloseAnimation() {
        if (!radarToolsCloseAnimationActive) {
            return 1.0D;
        }

        long now = System.nanoTime();
        if (radarToolsCloseAnimationStartNanos == 0L) {
            radarToolsCloseAnimationStartNanos = now;
        }

        double elapsedSeconds = (now - radarToolsCloseAnimationStartNanos) / 1_000_000_000.0D;
        double progress = clamp(elapsedSeconds / RADAR_TOOLS_CLOSE_ANIMATION_SECONDS, 0.0D, 1.0D);
        if (progress >= 1.0D) {
            radarToolsCloseAnimationActive = false;
            radarToolsCloseAnimationStartNanos = 0L;
            clearSettingsBounds();
        }

        return progress;
    }

    private static void drawRadarToolsCloseAnimation(
            GuiGraphics guiGraphics,
            Font font,
            int width,
            int height,
            ToolsButtonLayout tools,
            double progress
    ) {
        double travelProgress = easeRadarToolsCloseTravel(progress);
        double fadeProgress = easeRadarToolsCloseFade(clamp((progress - 0.76D) / 0.24D, 0.0D, 1.0D));
        double alpha = 1.0D - fadeProgress;
        if (alpha <= 0.0D) {
            return;
        }

        double startCenterX = radarToolsCloseStartLeft + settingsModalWidth / 2.0D;
        double startCenterY = radarToolsCloseStartTop + settingsModalHeight / 2.0D;
        double targetCenterX = tools.x() + tools.size() / 2.0D;
        double targetCenterY = tools.y() + tools.size() / 2.0D;
        double deltaX = targetCenterX - startCenterX;
        double deltaY = targetCenterY - startCenterY;
        double distance = Math.max(1.0D, Math.hypot(deltaX, deltaY));
        double normalX = -deltaY / distance;
        double normalY = deltaX / distance;
        double curve = Math.min(150.0D, Math.max(24.0D, distance * 0.24D)) * radarToolsCloseCurveDirection;
        double lift = Math.max(24.0D, Math.min(118.0D, Math.abs(deltaY) * 0.15D + Math.abs(deltaX) * 0.06D));
        double controlOneX = lerp(startCenterX, targetCenterX, 0.10D) + normalX * curve * 0.65D;
        double controlOneY = startCenterY - lift + normalY * curve * 0.28D;
        double controlTwoX = lerp(startCenterX, targetCenterX, 0.96D) - normalX * curve * 0.22D;
        double controlTwoY = targetCenterY - lift * 0.10D - normalY * curve * 0.10D;

        double centerX = cubicBezier(startCenterX, controlOneX, controlTwoX, targetCenterX, travelProgress);
        double centerY = cubicBezier(startCenterY, controlOneY, controlTwoY, targetCenterY, travelProgress);

        double shrinkProgress = easeRadarToolsPanelFade(clamp(progress / RADAR_TOOLS_CLOSE_SHRINK_END, 0.0D, 1.0D));
        double targetScale = Math.max(
                RADAR_TOOLS_CLOSE_MIN_SCALE,
                tools.size() * 0.42D / Math.max(settingsModalWidth, settingsModalHeight)
        );
        double scale = lerp(1.0D, targetScale, shrinkProgress);
        double rotation = radarToolsCloseCurveDirection * Math.sin(progress * Math.PI) * radarToolsCloseRotationPeak * 0.35D;
        drawClosingModalCopy(guiGraphics, font, centerX, centerY, scale, scale, rotation, 0.0D, alpha, settingsModalWidth, settingsModalHeight);
    }

    private static double easeRadarToolsSuctionTravel(double progress) {
        double clamped = clamp(progress, 0.0D, 1.0D);
        return Math.pow(clamped, 2.15D);
    }

    private static void drawClosingModalTrail(
            GuiGraphics guiGraphics,
            Font font,
            double startCenterX,
            double startCenterY,
            double controlOneX,
            double controlOneY,
            double controlTwoX,
            double controlTwoY,
            double targetCenterX,
            double targetCenterY,
            double progress,
            double angleToTarget,
            int modalWidth,
            int modalHeight
    ) {
        double pullProgress = easeRadarToolsPanelFade(clamp((progress - 0.32D) / 0.68D, 0.0D, 1.0D));
        if (pullProgress <= 0.0D) {
            return;
        }

        for (int i = 1; i <= 2; i++) {
            double trailProgress = clamp(easeRadarToolsSuctionTravel(Math.max(0.0D, progress - i * 0.055D)), 0.0D, 1.0D);
            double trailAlpha = (0.16D - i * 0.045D) * pullProgress * (1.0D - progress);
            if (trailAlpha <= 0.0D) {
                continue;
            }

            double x = cubicBezier(startCenterX, controlOneX, controlTwoX, targetCenterX, trailProgress);
            double y = cubicBezier(startCenterY, controlOneY, controlTwoY, targetCenterY, trailProgress);
            double scale = lerp(0.86D, RADAR_TOOLS_CLOSE_MIN_SCALE * 2.2D, trailProgress);
            drawClosingModalCopy(
                    guiGraphics,
                    font,
                    x,
                    y,
                    scale * (1.0D + pullProgress * 0.7D),
                    scale * (1.0D - pullProgress * 0.42D),
                    radarToolsCloseCurveDirection * 4.0D * (1.0D - trailProgress),
                    angleToTarget,
                    trailAlpha,
                    modalWidth,
                    modalHeight
            );
        }
    }

    private static void drawClosingModalGenie(
            GuiGraphics guiGraphics,
            Font font,
            double startLeft,
            double startTop,
            double controlOneX,
            double controlOneY,
            double controlTwoX,
            double controlTwoY,
            double targetCenterX,
            double targetCenterY,
            int iconSize,
            double progress,
            double alpha,
            int modalWidth,
            int modalHeight,
            int screenWidth,
            int screenHeight
    ) {
        boolean textured = captureRadarToolsCloseTexture(guiGraphics, font, startLeft, startTop, modalWidth, modalHeight, screenWidth, screenHeight);
        if (progress <= 0.08D) {
            drawSettingsModalAt(
                    guiGraphics,
                    font,
                    (int) Math.round(startLeft),
                    (int) Math.round(startTop),
                    modalWidth,
                    modalHeight,
                    alpha,
                    -1.0D,
                    -1.0D,
                    false
            );
            return;
        }

        int segments = clampInt((int) Math.ceil(modalHeight / (double) RADAR_TOOLS_CLOSE_GENIE_SLICE_HEIGHT), 26, 70);
        List<GenieBoundary> boundaries = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double sourceY = modalHeight * (i / (double) segments);
            boundaries.add(closingGenieBoundary(
                    startLeft,
                    startTop,
                    controlOneX,
                    controlOneY,
                    controlTwoX,
                    controlTwoY,
                    targetCenterX,
                    targetCenterY,
                    iconSize,
                    progress,
                    modalWidth,
                    modalHeight,
                    sourceY
            ));
        }

        drawSolidClosingGenieSurface(guiGraphics, boundaries, withAlpha(UI_BLACK, alpha * 0.72D));
        if (textured) {
            drawTexturedClosingGenieSurface(guiGraphics, boundaries, modalHeight, alpha);
        }

        double borderAlpha = alpha * 0.38D * (1.0D - easeRadarToolsPanelFade(clamp((progress - 0.34D) / 0.42D, 0.0D, 1.0D)));
        if (borderAlpha > 0.02D) {
            drawClosingGenieBorder(guiGraphics, boundaries, withAlpha(UI_BLUE, borderAlpha));
        }
    }

    private static boolean captureRadarToolsCloseTexture(
            GuiGraphics guiGraphics,
            Font font,
            double left,
            double top,
            int modalWidth,
            int modalHeight,
            int screenWidth,
            int screenHeight
    ) {
        if (radarToolsCloseTextureReady) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null || modalWidth <= 0 || modalHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) {
            return false;
        }

        int framebufferWidth = minecraft.getWindow().getWidth();
        int framebufferHeight = minecraft.getWindow().getHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return false;
        }

        double scaleX = framebufferWidth / (double) screenWidth;
        double scaleY = framebufferHeight / (double) screenHeight;
        int textureWidth = clampInt((int) Math.round(modalWidth * scaleX), 1, framebufferWidth);
        int textureHeight = clampInt((int) Math.round(modalHeight * scaleY), 1, framebufferHeight);
        if (radarToolsCloseTextureId == -1) {
            radarToolsCloseTextureId = GL11.glGenTextures();
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, radarToolsCloseTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        if (radarToolsCloseTextureWidth != textureWidth || radarToolsCloseTextureHeight != textureHeight) {
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    textureWidth,
                    textureHeight,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (ByteBuffer) null
            );
            radarToolsCloseTextureWidth = textureWidth;
            radarToolsCloseTextureHeight = textureHeight;
        }

        drawSettingsModalAt(
                guiGraphics,
                font,
                (int) Math.round(left),
                (int) Math.round(top),
                modalWidth,
                modalHeight,
                1.0D,
                -1.0D,
                -1.0D,
                false
        );
        guiGraphics.flush();

        int copyLeft = clampInt((int) Math.round(left * scaleX), 0, Math.max(0, framebufferWidth - textureWidth));
        int copyBottom = clampInt(framebufferHeight - (int) Math.round((top + modalHeight) * scaleY), 0, Math.max(0, framebufferHeight - textureHeight));
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, radarToolsCloseTextureId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, copyLeft, copyBottom, textureWidth, textureHeight);
        radarToolsCloseTextureReady = true;
        return true;
    }

    private static GenieBoundary closingGenieBoundary(
            double startLeft,
            double startTop,
            double controlOneX,
            double controlOneY,
            double controlTwoX,
            double controlTwoY,
            double targetCenterX,
            double targetCenterY,
            int iconSize,
            double progress,
            int modalWidth,
            int modalHeight,
            double sourceY
    ) {
        double t = clamp(sourceY / Math.max(1.0D, modalHeight), 0.0D, 1.0D);
        double startCenterX = startLeft + modalWidth * 0.5D;
        double startCenterY = startTop + modalHeight * 0.5D;
        double startY = startTop + sourceY;
        double targetEdge = targetCenterY >= startCenterY ? t : 1.0D - t;
        double shapeProgress = easeRadarToolsPanelFade(clamp((progress - 0.04D) / 0.96D, 0.0D, 1.0D));
        double suction = easeRadarToolsPanelFade(shapeProgress);
        double stagedPull = clamp((suction - (1.0D - targetEdge) * 0.44D) / 0.62D, 0.0D, 1.0D);
        double pull = Math.max(easeRadarToolsPanelFade(stagedPull), Math.pow(suction, 2.65D) * 0.22D);
        double sourceOffsetY = sourceY - modalHeight * 0.5D;
        double wave = Math.sin(t * Math.PI * 2.0D + radarToolsCloseWobblePhase + progress * Math.PI * 1.6D)
                * radarToolsCloseWarpStrength
                * iconSize
                * 0.10D
                * Math.sin(suction * Math.PI);
        double centerX = cubicBezier(
                startCenterX,
                controlOneX + wave * radarToolsCloseCurveDirection,
                controlTwoX - wave * 0.35D * radarToolsCloseCurveDirection,
                targetCenterX,
                pull
        );
        double targetSpreadY = (t - 0.5D) * iconSize * 0.28D * (1.0D - pull);
        double centerY = cubicBezier(
                startY,
                controlOneY + sourceOffsetY * 0.22D,
                controlTwoY + sourceOffsetY * 0.05D,
                targetCenterY + targetSpreadY,
                pull
        );

        double widthPull = easeRadarToolsPanelFade(clamp((suction + targetEdge * 0.34D - 0.22D) / 0.98D, 0.0D, 1.0D));
        double throatWidth = Math.max(2.0D, iconSize * 0.22D);
        double edgeWidth = throatWidth + Math.abs(t - 0.5D) * iconSize * 0.10D;
        double sideRipple = 1.0D + Math.sin(t * Math.PI * 3.0D + radarToolsCloseWobblePhase)
                * 0.045D
                * radarToolsCloseWarpStrength
                * suction
                * (1.0D - widthPull);
        double width = lerp(modalWidth, edgeWidth, Math.pow(widthPull, 1.18D)) * sideRipple;
        double finalPinch = easeRadarToolsPanelFade(clamp((progress - 0.82D) / 0.18D, 0.0D, 1.0D));
        width = lerp(width, throatWidth * 0.34D, finalPinch);
        centerX += radarToolsCloseCurveDirection * Math.sin(progress * Math.PI) * radarToolsCloseRotationPeak * (t - 0.5D) * 0.18D;
        return new GenieBoundary(centerX, centerY, width);
    }

    private static void drawClosingModalSlice(
            GuiGraphics guiGraphics,
            Font font,
            int sourceTop,
            int sourceHeight,
            double destCenterX,
            double destTop,
            double destWidth,
            double destHeight,
            double alpha,
            int modalWidth,
            int modalHeight,
            int screenWidth,
            int screenHeight
    ) {
        double destLeft = destCenterX - destWidth * 0.5D;
        double destRight = destCenterX + destWidth * 0.5D;
        double destBottom = destTop + destHeight;
        int scissorLeft = clampInt((int) Math.floor(destLeft) - 1, 0, screenWidth);
        int scissorTop = clampInt((int) Math.floor(destTop) - 1, 0, screenHeight);
        int scissorRight = clampInt((int) Math.ceil(destRight) + 1, 0, screenWidth);
        int scissorBottom = clampInt((int) Math.ceil(destBottom) + 1, 0, screenHeight);
        if (sourceHeight <= 0 || scissorRight <= scissorLeft || scissorBottom <= scissorTop) {
            return;
        }

        guiGraphics.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate((float) destLeft, (float) destTop, 0.0F);
            guiGraphics.pose().scale(
                    (float) (destWidth / Math.max(1.0D, modalWidth)),
                    (float) (destHeight / Math.max(1.0D, sourceHeight)),
                    1.0F
            );
            guiGraphics.pose().translate(0.0F, (float) -sourceTop, 0.0F);
            drawSettingsModalAt(
                    guiGraphics,
                    font,
                    0,
                    0,
                    modalWidth,
                    modalHeight,
                    alpha,
                    -1.0D,
                    -1.0D,
                    false,
                    false
            );
        } finally {
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
        }
    }

    private static void drawTexturedClosingGenieSurface(
            GuiGraphics guiGraphics,
            List<GenieBoundary> boundaries,
            int modalHeight,
            double alpha
    ) {
        if (radarToolsCloseTextureId == -1 || boundaries.size() < 2 || modalHeight <= 0) {
            return;
        }

        float fadedAlpha = (float) clamp(alpha, 0.0D, 1.0D);
        if (fadedAlpha <= 0.0F) {
            return;
        }

        RenderSystem.setShaderTexture(0, radarToolsCloseTextureId);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
        int lastIndex = boundaries.size() - 1;
        for (int i = 0; i < boundaries.size(); i++) {
            GenieBoundary boundary = boundaries.get(i);
            float v = (float) (1.0D - (i / (double) lastIndex));
            float left = (float) (boundary.centerX() - boundary.width() * 0.5D);
            float right = (float) (boundary.centerX() + boundary.width() * 0.5D);
            float y = (float) boundary.y();
            bufferBuilder.addVertex(matrix, left, y, 0.0F)
                    .setUv(0.0F, v)
                    .setColor(1.0F, 1.0F, 1.0F, fadedAlpha);
            bufferBuilder.addVertex(matrix, right, y, 0.0F)
                    .setUv(1.0F, v)
                    .setColor(1.0F, 1.0F, 1.0F, fadedAlpha);
        }
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void drawSolidClosingGenieSurface(GuiGraphics guiGraphics, List<GenieBoundary> boundaries, int color) {
        if (boundaries.size() < 2) {
            return;
        }

        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (GenieBoundary boundary : boundaries) {
            float left = (float) (boundary.centerX() - boundary.width() * 0.5D);
            float right = (float) (boundary.centerX() + boundary.width() * 0.5D);
            float y = (float) boundary.y();
            bufferBuilder.addVertex(matrix, left, y, 0.0F).setColor(red, green, blue, alpha);
            bufferBuilder.addVertex(matrix, right, y, 0.0F).setColor(red, green, blue, alpha);
        }
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void drawClosingGenieBorder(GuiGraphics guiGraphics, List<GenieBoundary> boundaries, int color) {
        if (boundaries.size() < 2 || ((color >>> 24) & 0xFF) == 0) {
            return;
        }

        GenieBoundary first = boundaries.get(0);
        GenieBoundary last = boundaries.get(boundaries.size() - 1);
        drawWarpedLine(
                guiGraphics,
                first.centerX() - first.width() * 0.5D,
                first.y(),
                first.centerX() + first.width() * 0.5D,
                first.y(),
                1.0D,
                color
        );
        drawWarpedLine(
                guiGraphics,
                last.centerX() - last.width() * 0.5D,
                last.y(),
                last.centerX() + last.width() * 0.5D,
                last.y(),
                1.0D,
                color
        );

        for (int i = 1; i < boundaries.size(); i++) {
            GenieBoundary previous = boundaries.get(i - 1);
            GenieBoundary current = boundaries.get(i);
            drawWarpedLine(
                    guiGraphics,
                    previous.centerX() - previous.width() * 0.5D,
                    previous.y(),
                    current.centerX() - current.width() * 0.5D,
                    current.y(),
                    1.0D,
                    color
            );
            drawWarpedLine(
                    guiGraphics,
                    previous.centerX() + previous.width() * 0.5D,
                    previous.y(),
                    current.centerX() + current.width() * 0.5D,
                    current.y(),
                    1.0D,
                    color
            );
        }
    }

    private static void drawClosingModalCopy(
            GuiGraphics guiGraphics,
            Font font,
            double centerX,
            double centerY,
            double xScale,
            double yScale,
            double rotation,
            double distortionAngle,
            double alpha,
            int modalWidth,
            int modalHeight
    ) {
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate((float) centerX, (float) centerY, 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((float) rotation));
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((float) distortionAngle));
            guiGraphics.pose().scale((float) xScale, (float) yScale, 1.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((float) -distortionAngle));
            drawSettingsModalAt(
                    guiGraphics,
                    font,
                    -modalWidth / 2,
                    -modalHeight / 2,
                    modalWidth,
                    modalHeight,
                    alpha,
                    -1.0D,
                    -1.0D,
                    false,
                    false
            );
        } finally {
            guiGraphics.pose().popPose();
        }
    }

    private static void drawRadarToolsClosePullSpot(
            GuiGraphics guiGraphics,
            double centerX,
            double centerY,
            int iconSize,
            double progress,
            double sourceAlpha
    ) {
        double pullProgress = easeRadarToolsPanelFade(clamp((progress - 0.18D) / 0.82D, 0.0D, 1.0D));
        if (pullProgress <= 0.0D) {
            return;
        }

        double pulse = 0.45D + Math.sin(pullProgress * Math.PI) * 0.55D;
        int coreSize = Math.max(4, (int) Math.round(iconSize * (0.42D + pullProgress * 0.18D)));
        drawRoundedPanel(
                guiGraphics,
                (int) Math.round(centerX - coreSize * 0.5D),
                (int) Math.round(centerY - coreSize * 0.5D),
                (int) Math.round(centerX + coreSize * 0.5D),
                (int) Math.round(centerY + coreSize * 0.5D),
                withAlpha(0xFF000000, sourceAlpha * pullProgress * 0.55D),
                withAlpha(UI_BLUE, sourceAlpha * pullProgress * 0.38D)
        );

        for (int i = 0; i < 4; i++) {
            double phase = clamp(pullProgress - i * 0.09D, 0.0D, 1.0D);
            double ringScale = 2.25D - phase * (1.05D + i * 0.10D);
            double ringAlpha = sourceAlpha * pulse * (0.24D - i * 0.035D);
            if (ringAlpha <= 0.0D) {
                continue;
            }

            int ringSize = Math.max(4, (int) Math.round(iconSize * ringScale));
            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().translate((float) centerX, (float) centerY, 0.0F);
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((float) (radarToolsCloseCurveDirection * (phase * 26.0D + i * 17.0D))));
                guiGraphics.pose().scale((float) (1.0D + phase * 0.22D), (float) (1.0D - phase * 0.24D), 1.0F);
                drawRoundedPanel(
                        guiGraphics,
                        -ringSize / 2,
                        -ringSize / 2,
                        ringSize / 2,
                        ringSize / 2,
                        withAlpha(UI_BLACK_ALPHA_25, ringAlpha * 0.32D),
                        withAlpha(UI_BLUE, ringAlpha)
                );
            } finally {
                guiGraphics.pose().popPose();
            }
        }
    }

    private static void drawRadarToolsOpenAnimation(GuiGraphics guiGraphics, int width, int height, int iconSize, double progress) {
        double travelProgress = easeModeMenuAnimation(clamp(progress / RADAR_TOOLS_OPEN_ICON_TRAVEL_END, 0.0D, 1.0D));
        double burstProgress = easeModeMenuAnimation(clamp(
                (progress - RADAR_TOOLS_OPEN_ICON_TRAVEL_END) / (RADAR_TOOLS_OPEN_ICON_FADE_END - RADAR_TOOLS_OPEN_ICON_TRAVEL_END),
                0.0D,
                1.0D
        ));
        double alpha = 1.0D - burstProgress;
        if (alpha <= 0.0D) {
            return;
        }

        double targetX = width * 0.5D;
        double targetY = height * 0.5D;
        double startX = radarToolsOpenStartSide < 0.0D ? -iconSize : width + iconSize;
        double startY = height * radarToolsOpenStartYRatio;
        double distance = Math.max(1.0D, Math.hypot(targetX - startX, targetY - startY));
        double curve = Math.min(120.0D, Math.max(28.0D, distance * 0.15D)) * radarToolsOpenCurveDirection;
        double sideInset = Math.min(width * 0.16D, 88.0D);
        double controlOneX = radarToolsOpenStartSide < 0.0D ? sideInset : width - sideInset;
        double controlOneY = startY + curve;
        double controlTwoX = lerp(startX, targetX, 0.76D);
        double controlTwoY = targetY - curve * 0.46D;
        double centerX = cubicBezier(startX, controlOneX, controlTwoX, targetX, travelProgress);
        double centerY = cubicBezier(startY, controlOneY, controlTwoY, targetY, travelProgress);
        double rotation = radarToolsOpenStartRotationDegrees * (1.0D - travelProgress);
        double scaleProgress = burstProgress * burstProgress;
        double scale = 1.0D + (RADAR_TOOLS_OPEN_ICON_MAX_SCALE - 1.0D) * scaleProgress;
        drawAnimatedRadarToolsIcon(guiGraphics, centerX, centerY, iconSize, scale, rotation, alpha);
    }

    private static void drawAnimatedRadarToolsIcon(
            GuiGraphics guiGraphics,
            double centerX,
            double centerY,
            int iconSize,
            double scale,
            double rotationDegrees,
            double alpha
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate((float) centerX, (float) centerY, 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((float) rotationDegrees));
            guiGraphics.pose().scale((float) scale, (float) scale, 1.0F);
            int left = -iconSize / 2;
            int top = -iconSize / 2;
            drawRoundedRadarToolsIcon(guiGraphics, left, top, iconSize, alpha);
        } finally {
            guiGraphics.pose().popPose();
        }
    }

    private static void drawTexturedQuadWithAlpha(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int left,
            int top,
            int width,
            int height,
            double alpha
    ) {
        float fadedAlpha = (float) clamp(alpha, 0.0D, 1.0D);
        if (fadedAlpha <= 0.0F) {
            return;
        }

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(matrix, left, top + height, 0.0F)
                .setUv(0.0F, 1.0F)
                .setColor(1.0F, 1.0F, 1.0F, fadedAlpha);
        bufferBuilder.addVertex(matrix, left + width, top + height, 0.0F)
                .setUv(1.0F, 1.0F)
                .setColor(1.0F, 1.0F, 1.0F, fadedAlpha);
        bufferBuilder.addVertex(matrix, left + width, top, 0.0F)
                .setUv(1.0F, 0.0F)
                .setColor(1.0F, 1.0F, 1.0F, fadedAlpha);
        bufferBuilder.addVertex(matrix, left, top, 0.0F)
                .setUv(0.0F, 0.0F)
                .setColor(1.0F, 1.0F, 1.0F, fadedAlpha);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void drawTexturedRegionWithColor(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            float left,
            float top,
            int width,
            int height,
            int textureX,
            int textureY,
            float textureWidth,
            float textureHeight,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        drawTexturedRegionWithColor(
                guiGraphics,
                texture,
                left,
                top,
                width,
                height,
                textureX,
                textureY,
                textureWidth,
                textureHeight,
                red,
                green,
                blue,
                alpha,
                GL11.GL_LINEAR
        );
    }

    private static void drawTexturedRegionWithColor(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            float left,
            float top,
            int width,
            int height,
            int textureX,
            int textureY,
            float textureWidth,
            float textureHeight,
            float red,
            float green,
            float blue,
            float alpha,
            int filter
    ) {
        if (alpha <= 0.0F) {
            return;
        }

        float u1 = textureX / textureWidth;
        float v1 = textureY / textureHeight;
        float u2 = (textureX + width) / textureWidth;
        float v2 = (textureY + height) / textureHeight;
        RenderSystem.setShaderTexture(0, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(matrix, left, top + height, 0.0F)
                .setUv(u1, v2)
                .setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, left + width, top + height, 0.0F)
                .setUv(u2, v2)
                .setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, left + width, top, 0.0F)
                .setUv(u2, v1)
                .setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, left, top, 0.0F)
                .setUv(u1, v1)
                .setColor(red, green, blue, alpha);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static double easeRadarToolsPanelFade(double progress) {
        double clamped = clamp(progress, 0.0D, 1.0D);
        double inverse = 1.0D - clamped;
        return 1.0D - inverse * inverse * inverse;
    }

    private static double easeRadarToolsCloseTravel(double progress) {
        double clamped = clamp(progress, 0.0D, 1.0D);
        double inverse = 1.0D - clamped;
        return 1.0D - inverse * inverse;
    }

    private static double easeRadarToolsCloseFade(double progress) {
        double clamped = clamp(progress, 0.0D, 1.0D);
        return clamped * clamped;
    }

    private static double cubicBezier(double p0, double p1, double p2, double p3, double progress) {
        double t = clamp(progress, 0.0D, 1.0D);
        double inverse = 1.0D - t;
        return inverse * inverse * inverse * p0
                + 3.0D * inverse * inverse * t * p1
                + 3.0D * inverse * t * t * p2
                + t * t * t * p3;
    }

    private static void applyModeChangeAnimation(
            ModeControlTarget target,
            StormOverlayData.RadarMode previousMode,
            StormOverlayData.RadarMode nextMode,
            double seconds,
            boolean rotateUp
    ) {
        if (StormOverlayData.isTextSwapAnimationsEnabled()) {
            startModeChangeAnimation(previousMode, nextMode, seconds, rotateUp);
        } else {
            syncModeVisualStackToTarget(target);
        }
    }

    private static void syncModeVisualStackToCurrentMode() {
        syncModeVisualStackToTarget(ModeControlTarget.SINGLE);
    }

    private static void syncModeVisualStackToTarget(ModeControlTarget target) {
        modeVisualStack = List.copyOf(modeStackFor(modeForTarget(target)));
        resetModeChangeAnimation();
    }

    private static void startModeChangeAnimation(
            StormOverlayData.RadarMode previousMode,
            StormOverlayData.RadarMode nextMode,
            double seconds,
            boolean rotateUp
    ) {
        if (previousMode == null || nextMode == null || previousMode == nextMode) {
            return;
        }

        List<StormOverlayData.RadarMode> fromStack = currentModeStackFor(previousMode);
        List<StormOverlayData.RadarMode> toStack = rotatedModeStackToSelected(fromStack, nextMode, rotateUp);
        modeChangeFrom = previousMode;
        modeChangeTo = nextMode;
        modeChangeFromStack = List.copyOf(fromStack);
        modeChangeToStack = List.copyOf(toStack);
        modeVisualStack = modeChangeToStack;
        modeChangeAnimationProgress = 0.0D;
        modeChangeAnimationSeconds = Math.max(0.01D, seconds);
        modeChangeAnimationLastNanos = 0L;
    }

    private static double updateModeChangeAnimation() {
        if (modeChangeFrom == null || modeChangeTo == null) {
            return 1.0D;
        }

        long now = System.nanoTime();
        if (modeChangeAnimationLastNanos == 0L) {
            modeChangeAnimationLastNanos = now;
        }

        double deltaSeconds = Math.min(0.05D, (now - modeChangeAnimationLastNanos) / 1_000_000_000.0D);
        modeChangeAnimationLastNanos = now;
        modeChangeAnimationProgress = Math.min(1.0D, modeChangeAnimationProgress + deltaSeconds / modeChangeAnimationSeconds);
        if (modeChangeAnimationProgress >= 1.0D) {
            resetModeChangeAnimation();
            return 1.0D;
        }

        return modeChangeAnimationProgress;
    }

    private static double easeModeMenuAnimation(double progress) {
        double clamped = clamp(progress, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static void resetModeMenuAnimation() {
        modeMenuAnimationProgress = 0.0D;
        modeMenuAnimationLastNanos = 0L;
    }

    private static void resetModeChangeAnimation() {
        modeChangeFrom = null;
        modeChangeTo = null;
        modeChangeFromStack = List.of();
        modeChangeToStack = List.of();
        modeChangeAnimationProgress = 1.0D;
        modeChangeAnimationLastNanos = 0L;
    }

    private static double modeMenuListHeight(int rowCount, int textHeight) {
        int rowGaps = Math.max(0, rowCount - 1);
        return MODE_MENU_LIST_TOP_PADDING
                + MODE_MENU_LIST_BOTTOM_PADDING
                + rowCount * (double) textHeight
                + rowGaps * (double) MODE_MENU_LIST_ROW_GAP;
    }

    private static int modeMenuTextHeight(Font font, float scale) {
        return Math.max(1, (int) Math.ceil(controlTextPixelHeight(font) * scale));
    }

    private static double modeMenuListWidth(
            Font font,
            int labelWidth,
            float scale,
            List<StormOverlayData.RadarMode> modes,
            double textInset
    ) {
        double widest = labelWidth;
        for (StormOverlayData.RadarMode mode : modes) {
            widest = Math.max(widest, textPixelWidth(font, mode.displayName().toUpperCase(Locale.ROOT), scale));
        }

        return Math.ceil(widest + textInset * 2.0D);
    }

    private static int displayButtonWidth(Font font, float scale, int controlHeight) {
        double textInset = controlTextInset(font, scale, controlHeight);
        double width = textPixelWidth(font, DISPLAY_BUTTON_TEXT, scale)
                + textInset * 2.0D
                - DISPLAY_BUTTON_WIDTH_TRIM;
        return Math.max(1, (int) Math.ceil(width));
    }

    private static int bottomControlsPadding() {
        return BOTTOM_CONTROLS_PADDING;
    }

    private static int bottomControlsStormBarHeight() {
        return BOTTOM_CONTROLS_STORM_BAR_HEIGHT;
    }

    private static float controlTextScale(Font font, int controlHeight) {
        return 1.0F;
    }

    private static float controlTextXScale(Font font, String text, float yScale) {
        return yScale;
    }

    private static int controlTextPixelHeight(Font font) {
        int fallbackHeight = Math.max(1, font.lineHeight - 1);
        int tallest = Math.max(1, textPixelBounds(font, DISPLAY_BUTTON_TEXT).bottom() - textPixelBounds(font, DISPLAY_BUTTON_TEXT).top());
        for (StormOverlayData.RadarMode mode : StormOverlayData.RadarMode.values()) {
            TextPixelBounds bounds = textPixelBounds(font, mode.displayName().toUpperCase(Locale.ROOT));
            tallest = Math.max(tallest, bounds.bottom() - bounds.top());
        }

        return Math.max(1, Math.min(tallest, fallbackHeight));
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * clamp(progress, 0.0D, 1.0D);
    }

    private static double controlTextInset(Font font, float scale, int controlHeight) {
        return CONTROL_TEXT_BORDER_PIXELS + CONTROL_TEXT_HORIZONTAL_PADDING_PIXELS;
    }

    private static void fillTranslatedRect(GuiGraphics guiGraphics, double left, double top, double right, double bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) left, (float) top, 0.0F);
        guiGraphics.fill(
                0,
                0,
                Math.max(1, (int) Math.round(right - left)),
                Math.max(1, (int) Math.ceil(bottom - top)),
                color
        );
        guiGraphics.pose().popPose();
    }

    private static void fillPreciseRect(GuiGraphics guiGraphics, double left, double top, double right, double bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) left, (float) top, 0.0F);
        guiGraphics.pose().scale((float) (right - left), (float) (bottom - top), 1.0F);
        guiGraphics.fill(0, 0, 1, 1, color);
        guiGraphics.pose().popPose();
    }

    private static void drawWarpedLine(
            GuiGraphics guiGraphics,
            double x1,
            double y1,
            double x2,
            double y2,
            double thickness,
            int color
    ) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length <= 0.01D || thickness <= 0.0D) {
            return;
        }

        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate((float) x1, (float) y1, 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1))));
            guiGraphics.pose().scale((float) length, (float) thickness, 1.0F);
            guiGraphics.fill(0, -1, 1, 1, color);
        } finally {
            guiGraphics.pose().popPose();
        }
    }

    private static void drawRectBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }

        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }

    private static void drawTranslatedRectBorder(GuiGraphics guiGraphics, double left, double top, double right, double bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }

        double borderSize = 1.0D;
        fillTranslatedRect(guiGraphics, left, top, right, top + borderSize, color);
        fillTranslatedRect(guiGraphics, left, bottom - borderSize, right, bottom, color);
        fillTranslatedRect(guiGraphics, left, top, left + borderSize, bottom, color);
        fillTranslatedRect(guiGraphics, right - borderSize, top, right, bottom, color);
    }

    private static void drawOpenBottomBorder(GuiGraphics guiGraphics, int left, double top, int right, double bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }

        double borderSize = 1.0D;
        fillTranslatedRect(guiGraphics, left, top, right, top + borderSize, color);
        fillTranslatedRect(guiGraphics, left, top, left + borderSize, bottom, color);
        fillTranslatedRect(guiGraphics, right - borderSize, top, right, bottom, color);
    }

    private static void drawRoundedPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int fill, int border) {
        if (right <= left || bottom <= top) {
            return;
        }

        guiGraphics.fill(left + 2, top, right - 2, bottom, fill);
        guiGraphics.fill(left, top + 2, right, bottom - 2, fill);
        guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1, fill);
        guiGraphics.fill(left + 2, top, right - 2, top + 1, border);
        guiGraphics.fill(left + 2, bottom - 1, right - 2, bottom, border);
        guiGraphics.fill(left, top + 2, left + 1, bottom - 2, border);
        guiGraphics.fill(right - 1, top + 2, right, bottom - 2, border);
        guiGraphics.fill(left + 1, top + 1, left + 2, top + 2, border);
        guiGraphics.fill(right - 2, top + 1, right - 1, top + 2, border);
        guiGraphics.fill(left + 1, bottom - 2, left + 2, bottom - 1, border);
        guiGraphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, border);
    }

    private static void drawSmallGear(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        guiGraphics.fill(centerX - 1, y, centerX + 2, y + size, color);
        guiGraphics.fill(x, centerY - 1, x + size, centerY + 2, color);
        guiGraphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, color);
        guiGraphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, UI_BLACK);
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
    }

    private static void drawFilledCircle(GuiGraphics guiGraphics, double centerX, double centerY, double radius, int color) {
        if (radius <= 0.75D) {
            int x = (int) Math.round(centerX);
            int y = (int) Math.round(centerY);
            guiGraphics.fill(x, y, x + 1, y + 1, color);
            return;
        }

        int roundedRadius = Math.max(1, (int) Math.ceil(radius));
        int x = (int) Math.round(centerX);
        int y = (int) Math.round(centerY);
        for (int offsetY = -roundedRadius; offsetY <= roundedRadius; offsetY++) {
            int offsetX = (int) Math.floor(Math.sqrt(Math.max(0.0D, radius * radius - offsetY * offsetY)));
            guiGraphics.fill(x - offsetX, y + offsetY, x + offsetX + 1, y + offsetY + 1, color);
        }
    }

    private static int argb(Color color) {
        return 0xFF000000 | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    private static int withAlpha(int color, double alpha) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int fadedAlpha = clampInt((int) Math.round(sourceAlpha * clamp(alpha, 0.0D, 1.0D)), 0, 255);
        return (color & 0x00FFFFFF) | (fadedAlpha << 24);
    }

    private static boolean isWorldMap(Screen screen) {
        return screen != null && XAERO_WORLD_MAP_SCREEN.equals(screen.getClass().getName());
    }

    private static MapView mapView(Screen screen, int width, int height) {
        try {
            ResourceKey<Level> dimension = currentMapDimension(screen);
            double scale = scaleField(screen).getDouble(screen);
            double screenScale = screenScaleField(screen).getDouble(screen);
            if (screenScale <= 0.0D) {
                screenScale = Minecraft.getInstance().getWindow().getGuiScale();
            }

            double pixelsPerBlock = scale / screenScale;
            if (!Double.isFinite(pixelsPerBlock) || pixelsPerBlock <= 0.0D || dimension == null) {
                return null;
            }

            return new MapView(
                    dimension,
                    cameraXField(screen).getDouble(screen),
                    cameraZField(screen).getDouble(screen),
                    pixelsPerBlock,
                    width,
                    height
            );
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<Level> currentMapDimension(Screen screen) throws ReflectiveOperationException {
        Object processor = getMapProcessorMethod(screen).invoke(screen);
        if (processor == null) {
            return null;
        }

        Object mapWorld = getMapWorldMethod(processor).invoke(processor);
        if (mapWorld == null) {
            return null;
        }

        Object mapDimension = getCurrentDimensionMethod(mapWorld).invoke(mapWorld);
        if (mapDimension == null) {
            return null;
        }

        Object dimension = getDimIdMethod(mapDimension).invoke(mapDimension);
        return dimension instanceof ResourceKey<?> key ? (ResourceKey<Level>) key : null;
    }

    private static boolean isHiddenUi(Screen screen) {
        try {
            if (hiddenUiField == null) {
                hiddenUiField = screen.getClass().getField("hiddenUI");
            }

            return hiddenUiField.getBoolean(null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static Field cameraXField(Screen screen) throws NoSuchFieldException {
        if (cameraXField == null) {
            cameraXField = doubleField(screen, "cameraX");
        }

        return cameraXField;
    }

    private static Field cameraZField(Screen screen) throws NoSuchFieldException {
        if (cameraZField == null) {
            cameraZField = doubleField(screen, "cameraZ");
        }

        return cameraZField;
    }

    private static Field scaleField(Screen screen) throws NoSuchFieldException {
        if (scaleField == null) {
            scaleField = doubleField(screen, "scale");
        }

        return scaleField;
    }

    private static Field screenScaleField(Screen screen) throws NoSuchFieldException {
        if (screenScaleField == null) {
            screenScaleField = doubleField(screen, "screenScale");
        }

        return screenScaleField;
    }

    private static Field doubleField(Screen screen, String name) throws NoSuchFieldException {
        Field field = screen.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method getMapProcessorMethod(Screen screen) throws NoSuchMethodException {
        if (getMapProcessorMethod == null) {
            getMapProcessorMethod = screen.getClass().getMethod("getMapProcessor");
        }

        return getMapProcessorMethod;
    }

    private static Method getMapWorldMethod(Object processor) throws NoSuchMethodException {
        if (getMapWorldMethod == null) {
            getMapWorldMethod = processor.getClass().getMethod("getMapWorld");
        }

        return getMapWorldMethod;
    }

    private static Method getCurrentDimensionMethod(Object mapWorld) throws NoSuchMethodException {
        if (getCurrentDimensionMethod == null) {
            getCurrentDimensionMethod = mapWorld.getClass().getMethod("getCurrentDimension");
        }

        return getCurrentDimensionMethod;
    }

    private static Method getDimIdMethod(Object mapDimension) throws NoSuchMethodException {
        if (getDimIdMethod == null) {
            getDimIdMethod = mapDimension.getClass().getMethod("getDimId");
        }

        return getDimIdMethod;
    }

    private static boolean isInside(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private static boolean isInsideMenuBounds(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            return false;
        }

        return isInside(mouseX, mouseY, left, top, right, bottom);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void clearModeBounds() {
        buttonLeft = 0;
        buttonTop = 0;
        buttonRight = 0;
        buttonBottom = 0;
        toolsButtonLeft = 0;
        toolsButtonTop = 0;
        toolsButtonRight = 0;
        toolsButtonBottom = 0;
        modeLeft = 0;
        modeTop = 0;
        modeRight = 0;
        modeBottom = 0;
        settingsOpen = false;
        radarToolsOpenAnimationActive = false;
        radarToolsOpenAnimationStartNanos = 0L;
        radarToolsOpenStartSide = 1.0D;
        radarToolsOpenStartYRatio = 0.24D;
        radarToolsOpenCurveDirection = 1.0D;
        radarToolsOpenStartRotationDegrees = 26.0D;
        radarToolsCloseAnimationActive = false;
        radarToolsCloseAnimationStartNanos = 0L;
        settingsModalDragging = false;
        settingsModalDragOffsetX = 0.0D;
        settingsModalDragOffsetY = 0.0D;
        settingsModalResizing = false;
        settingsScrollDragging = false;
        settingsScrollDragOffsetY = 0.0D;
        settingsModalResizeStartMouseX = 0.0D;
        settingsModalResizeStartMouseY = 0.0D;
        settingsModalResizeStartLeft = 0.0D;
        settingsModalResizeStartTop = 0.0D;
        settingsModalResizeStartWidth = SETTINGS_MODAL_WIDTH;
        settingsModalResizeStartHeight = SETTINGS_MODAL_HEIGHT;
        settingsModalWidth = SETTINGS_MODAL_WIDTH;
        settingsModalHeight = SETTINGS_MODAL_HEIGHT;
        settingsModalOffsetX = 0.0D;
        settingsModalOffsetY = 0.0D;
        settingsMenuScrollOffset = 0.0D;
        settingsMenuPage = SettingsMenuPage.MAIN;
        stationLabelBounds = List.of();
        modeControlBounds = List.of();
        displayControlBounds = List.of();
        modeMenuTarget = ModeControlTarget.SINGLE;
        clearDualModeXaeroLabels();
        clearModeMenuBounds();
        resetModeMenuAnimation();
        modeVisualStack = List.of();
        clearSettingsBounds();
    }

    private static void clearModeMenuBounds() {
        modeMenuLeft = 0;
        modeMenuTop = 0;
        modeMenuRight = 0;
        modeMenuBottom = 0;
        modeLabelBounds = List.of();
    }

    private static void clearSettingsRowBounds() {
        clearSettingsMainRowBounds();
        clearSettingsAnimationRowBounds();
    }

    private static void clearSettingsMainRowBounds() {
        displayEnabledLeft = 0;
        displayEnabledTop = 0;
        displayEnabledRight = 0;
        displayEnabledBottom = 0;
        dualModeLeft = 0;
        dualModeTop = 0;
        dualModeRight = 0;
        dualModeBottom = 0;
        radarLocationsLeft = 0;
        radarLocationsTop = 0;
        radarLocationsRight = 0;
        radarLocationsBottom = 0;
        lightningLeft = 0;
        lightningTop = 0;
        lightningRight = 0;
        lightningBottom = 0;
        animationsLeft = 0;
        animationsTop = 0;
        animationsRight = 0;
        animationsBottom = 0;
    }

    private static void clearSettingsAnimationRowBounds() {
        menuAnimationsLeft = 0;
        menuAnimationsTop = 0;
        menuAnimationsRight = 0;
        menuAnimationsBottom = 0;
        textSwapAnimationsLeft = 0;
        textSwapAnimationsTop = 0;
        textSwapAnimationsRight = 0;
        textSwapAnimationsBottom = 0;
    }

    private static void clearSettingsBounds() {
        settingsModalLeft = 0;
        settingsModalTop = 0;
        settingsModalRight = 0;
        settingsModalBottom = 0;
        settingsCloseLeft = 0;
        settingsCloseTop = 0;
        settingsCloseRight = 0;
        settingsCloseBottom = 0;
        settingsBackLeft = 0;
        settingsBackTop = 0;
        settingsBackRight = 0;
        settingsBackBottom = 0;
        settingsResizeLeft = 0;
        settingsResizeTop = 0;
        settingsResizeRight = 0;
        settingsResizeBottom = 0;
        clearSettingsScrollBounds();
        clearSettingsRowBounds();
    }

    private static void clearSettingsScrollBounds() {
        settingsScrollTrackLeft = 0;
        settingsScrollTrackTop = 0;
        settingsScrollTrackRight = 0;
        settingsScrollTrackBottom = 0;
        settingsScrollThumbLeft = 0;
        settingsScrollThumbTop = 0;
        settingsScrollThumbRight = 0;
        settingsScrollThumbBottom = 0;
    }

    private record MapView(
            ResourceKey<Level> dimension,
            double cameraX,
            double cameraZ,
            double pixelsPerBlock,
            int width,
            int height
    ) {
        private double screenX(double blockX) {
            return width * 0.5D + (blockX - cameraX) * pixelsPerBlock;
        }

        private double screenZ(double blockZ) {
            return height * 0.5D + (blockZ - cameraZ) * pixelsPerBlock;
        }

        private double worldX(double screenX) {
            return cameraX + (screenX - width * 0.5D) / pixelsPerBlock;
        }

        private double worldZ(double screenY) {
            return cameraZ + (screenY - height * 0.5D) / pixelsPerBlock;
        }
    }

    private record StationLabelBounds(BlockPos pos, int left, int top, int right, int bottom) {
    }

    private record ModeLabelBounds(ModeControlTarget target, StormOverlayData.RadarMode mode, int left, int top, int right, int bottom) {
    }

    private record ModeControlBounds(ModeControlTarget target, int left, int top, int right, int bottom) {
    }

    private record DisplayControlBounds(int left, int top, int right, int bottom) {
    }

    private record DualModeXaeroLabel(String text, int x, int y) {
    }

    private record SettingRowBounds(int left, int top, int right, int bottom) {
    }

    private record GenieBoundary(double centerX, double y, double width) {
    }

    private record RadarTextureKey(BlockPos pos, StormOverlayData.RadarMode mode) {
    }

    private record GlyphPixelBounds(int left, int top, int right, int bottom) {
        private boolean empty() {
            return right <= left || bottom <= top;
        }
    }

    private record TextPixelBounds(int left, int top, int right, int bottom) {
    }

    private record ToolsButtonLayout(int x, int y, int size) {
    }

    private record SettingsModalPlacement(int left, int top, int width, int height) {
    }

    private record DualMapPanels(DualMapPanel upper, DualMapPanel lower) {
    }

    private record DualMapPanel(int left, int top, int right, int bottom) {
        private int width() {
            return Math.max(1, right - left);
        }

        private int height() {
            return Math.max(1, bottom - top);
        }

        private double centerX() {
            return (left + right) * 0.5D;
        }

        private double centerY() {
            return (top + bottom) * 0.5D;
        }
    }

    private record ModeMenuFrame(ModeControlTarget target, double menuProgress, double changeProgress) {
    }

    private enum SettingsMenuPage {
        MAIN,
        ANIMATIONS
    }

    private enum ModeControlTarget {
        SINGLE,
        DUAL_UPPER,
        DUAL_LOWER
    }

    private record BottomControlsLayout(
            int buttonX,
            int buttonY,
            int buttonWidth,
            int buttonHeight,
            int barX,
            int barY,
            int barWidth,
            double barHeight,
            int modeY,
            int modeWidth,
            double modeTextX,
            double modeBoxLeft,
            double modeClosedRight,
            int modeHeight,
            float modeScale,
            String modeText,
        StormOverlayData.RadarMode mode
    ) {
        private int modeLeft() {
            return (int) Math.floor(modeBoxLeft) - 3;
        }

        private int modeTop() {
            return modeY - 3;
        }

        private int modeRight() {
            return (int) Math.ceil(modeClosedRight) + 5;
        }

        private int modeBottom() {
            return modeY + modeHeight + 3;
        }

    }

    private static final class PendingSiteTextureBuild {
        private final ResourceKey<Level> dimension;
        private final StormOverlayData.RadarSiteView site;
        private final StormOverlayData.RadarMode mode;
        private final int stateHash;
        private final int size;
        private final NativeImage image;
        private final int[] argbPixels;
        private int nextY;
        private boolean hasPixels;
        private boolean imageTransferred;

        private PendingSiteTextureBuild(ResourceKey<Level> dimension, StormOverlayData.RadarSiteView site, StormOverlayData.RadarMode mode, int stateHash, int size) {
            this.dimension = dimension;
            this.site = site;
            this.mode = mode;
            this.stateHash = stateHash;
            this.size = size;
            this.image = new NativeImage(size, size, true);
            this.argbPixels = new int[size * size];
        }

        private boolean matches(ResourceKey<Level> dimension, BlockPos pos, StormOverlayData.RadarMode mode, int stateHash, int size) {
            return this.dimension.equals(dimension)
                    && this.site.pos().equals(pos)
                    && this.mode == mode
                    && this.stateHash == stateHash
                    && this.size == size;
        }

        private void advance(int rows) {
            int endY = Math.min(size, nextY + Math.max(1, rows));
            for (int y = nextY; y < endY; y++) {
                double normalZ = (y / (double) size) * 2.0D - 1.0D;
                for (int x = 0; x < size; x++) {
                    double normalX = (x / (double) size) * 2.0D - 1.0D;
                    double blockX = site.x() + normalX * site.radiusBlocks();
                    double blockZ = site.z() + normalZ * site.radiusBlocks();
                    int color = radarBlockTextureArgb(StormOverlayData.argbForRadarSiteTexture(dimension, site.pos(), blockX, blockZ, mode));
                    if ((color >>> 24) == 0) {
                        continue;
                    }

                    image.setPixelRGBA(x, y, argbToAbgr(color));
                    argbPixels[y * size + x] = color;
                    hasPixels = true;
                }
            }

            nextY = endY;
        }

        private boolean complete() {
            return nextY >= size;
        }

        private SiteTextureCache toCache() {
            ResourceLocation location = textureLocation(site.pos(), mode);
            DynamicTexture texture = null;
            if (hasPixels) {
                texture = new DynamicTexture(image);
                texture.setFilter(false, false);
                Minecraft.getInstance().getTextureManager().register(location, texture);
                imageTransferred = true;
            } else {
                image.close();
                imageTransferred = true;
            }

            return new SiteTextureCache(site.pos(), stateHash, size, location, texture, hasPixels, argbPixels);
        }

        private void close() {
            if (!imageTransferred) {
                image.close();
                imageTransferred = true;
            }
        }
    }

    private record SiteTextureCache(
            BlockPos pos,
            int stateHash,
            int size,
            ResourceLocation location,
            DynamicTexture texture,
            boolean hasTexture,
            int[] argbPixels
    ) {
        private boolean matches(BlockPos pos, int stateHash, int size) {
            return this.pos.equals(pos) && this.stateHash == stateHash && this.size == size;
        }

        private void close() {
            if (texture != null) {
                texture.close();
            }
        }
    }

    public record RadarTextureSample(double centerX, double centerZ, double radiusBlocks, int size, int[] argbPixels) {
        public int colorAt(double blockX, double blockZ) {
            double normalX = (blockX - centerX) / radiusBlocks;
            double normalZ = (blockZ - centerZ) / radiusBlocks;
            double distanceSquared = normalX * normalX + normalZ * normalZ;
            double blindSpot = RADAR_BLIND_SPOT_RADIUS_BLOCKS / radiusBlocks;
            if (distanceSquared > 1.0D || distanceSquared < blindSpot * blindSpot) {
                return 0;
            }

            int x = clampInt((int) Math.floor((normalX * 0.5D + 0.5D) * size), 0, size - 1);
            int y = clampInt((int) Math.floor((normalZ * 0.5D + 0.5D) * size), 0, size - 1);
            return argbPixels[y * size + x];
        }
    }
}
