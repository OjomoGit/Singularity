package singularity.ui.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import singularity.core.ModuleManager;
import singularity.ui.animations.AnimationUtils;
import singularity.ui.utils.SoundController;
import singularity.ui.utils.UpdateChecker;
import singularity.ui.widgets.WidgetManager;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private HBox titleBar;

    @FXML private VBox sidebar;
    @FXML private Label navDashboardTxt, navSettingsTxt;

    @FXML private Pane dashboardView, settingsView;
    @FXML private ImageView mainLogo;
    @FXML private Label logoLabel;
    @FXML private HBox logoContainer;
    @FXML private StackPane centerContent;
    @FXML private VBox rightSidebar;

    @FXML private Button navDashboardBtn, navSettingsBtn;
    @FXML private Button tabGeneralBtn, tabGraphicsBtn, tabKeybindsBtn, tabSoundsBtn;
    @FXML private Pane generalPane, graphicsPane, keybindsPane, soundsPane;

    @FXML private ColorPicker accentColorPicker;
    @FXML private VBox sampleDesktopTile;

    private WidgetManager widgetManager;

    private double xOffset = 0;
    private double yOffset = 0;

    // Cursor Glow Tracking
    private Circle cursorGlow;
    private double targetMouseX = 500, targetMouseY = 400;
    private double curMouseX = 500, curMouseY = 400;

    // Grid constants for desktop tile snapping
    private static final double TILE_WIDTH  = 90.0;
    private static final double TILE_HEIGHT = 90.0;  // taller to fit label below icon
    private static final double GRID_GAP    = 20.0;
    private static final double GRID_CELL_W = TILE_WIDTH + GRID_GAP;   // 110
    private static final double GRID_CELL_H = TILE_HEIGHT + GRID_GAP;  // 130
    private static final double PADDING     = 20.0;

    @FXML
    public void initialize() {
        UpdateChecker.checkForUpdatesAsync();
        setupWindowDragging();
        setupMaximizedListener();
        setupSidebarHover();
        setupAnimations();

        showDashboard();
        showSettingsGeneral();

        applyLogoCutout();
        setupWidgetSystem();

        accentColorPicker.setValue(Color.web("#d946ef"));
        accentColorPicker.valueProperty().addListener((obs, oldC, newC) -> updateDynamicStyles());
        updateDynamicStyles();

        setupCursorGlow();
        setupAnimatedLogoGradient();

        // 1. Alle Module automatisch suchen und laden
        ModuleManager.getInstance().autoDiscoverAndLoadModules();

        // 2. NEU: Den Desktop anhand der gefundenen Module generieren
        generateDesktopTiles();

        playStartupAnimations();

        if (mainLogo != null && mainLogo.getParent() instanceof HBox) {
            for (Node node : ((HBox) mainLogo.getParent()).getChildren()) {
                if (node instanceof Label) {
                    ((Label) node).setFont(Font.font("Miracle History", 36));
                }
            }
        }

        Platform.runLater(() -> applyLetterSpacing(rootPane));
    }

    /**
     * Generiert für jedes geladene Modul ein visuelles Tile auf dem Dashboard.
     */
    private void generateDesktopTiles() {
        dashboardView.getChildren().clear(); // Desktop leeren

        // Start-Koordinaten für das erste Grid-Feld
        double startX = PADDING;
        double startY = PADDING;

        int count = 0;

        for (singularity.core.Module module : ModuleManager.getInstance().getModules()) {

            // Neues VBox-Tile erstellen
            VBox tile = new VBox();
            tile.getStyleClass().addAll("glass-pane", "desktop-tile");
            tile.setAlignment(javafx.geometry.Pos.CENTER);
            tile.setSpacing(8);

            // Initiale Grid-Position zuweisen
            // (Wir legen die Kacheln beim Start einfach von links nach rechts nebeneinander)
            tile.setLayoutX(startX + (count * GRID_CELL_W));
            tile.setLayoutY(startY);

            // Icon hinzufügen
            org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon();
            icon.setIconLiteral(module.getIconLiteral());
            icon.setIconSize(34);
            icon.getStyleClass().add("accent-icon");

            // Text Label hinzufügen
            Label label = new Label(module.getModuleName());
            label.getStyleClass().add("desktop-tile-label");

            tile.getChildren().addAll(icon, label);

            // Hover-Animation (identisch zu den Navigations-Buttons)
            AnimationUtils.applySmoothHoverAndClick(tile);

            // Drag & Drop Grid-Snapping Logik anhängen
            makeDraggableAndGridSnapped(tile);

            // NEU: Doppelklick öffnet das Modul
            tile.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    SoundController.playClick();
                    openModule(module);
                }
            });

            // Zum Dashboard hinzufügen
            dashboardView.getChildren().add(tile);
            count++;
        }
    }

    private void openModule(singularity.core.Module module) {
        if (module == null || module.getView() == null || centerContent == null) {
            return;
        }

        Node moduleView = module.getView();

        // 1. Schließen-Aktion verknüpfen
        module.setCloseHandler(() -> closeCurrentModule(module));

        // 2. Dashboard ausblenden
        dashboardView.setVisible(false);
        if (settingsView != null) {
            settingsView.setVisible(false);
        }

        // 3. Modul-View einfügen
        if (!centerContent.getChildren().contains(moduleView)) {
            centerContent.getChildren().add(moduleView);
        }

        module.onEnable();

        // 4. Einblend-Animation
        moduleView.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), moduleView);
        ft.setToValue(1.0);
        ft.play();
    }

    private void closeCurrentModule(singularity.core.Module module) {
        if (module == null || module.getView() == null) {
            return;
        }

        Node moduleView = module.getView();

        FadeTransition ft = new FadeTransition(Duration.millis(200), moduleView);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            centerContent.getChildren().remove(moduleView);
            module.onDisable();
            showDashboard(); // Zeigt dashboardView wieder an
        });
        ft.play();
    }

    private void setupWidgetSystem() {
        if (rightSidebar != null) {
            rightSidebar.getChildren().clear();
            widgetManager = new WidgetManager();
            rightSidebar.getChildren().add(widgetManager);
        }
    }

    private void setupCursorGlow() {
        cursorGlow = new Circle(220);
        cursorGlow.setMouseTransparent(true);
        cursorGlow.setManaged(false);
        cursorGlow.setBlendMode(BlendMode.ADD);
        cursorGlow.setOpacity(0.35);

        updateGlowGradient();

        Platform.runLater(() -> {
            if (rootPane.getParent() instanceof Pane p) {
                p.getChildren().add(0, cursorGlow);
            } else {
                rootPane.getChildren().add(0, cursorGlow);
            }
        });

        rootPane.setOnMouseMoved(e -> {
            targetMouseX = e.getSceneX();
            targetMouseY = e.getSceneY();
        });
        rootPane.setOnMouseDragged(e -> {
            targetMouseX = e.getSceneX();
            targetMouseY = e.getSceneY();
        });

        AnimationTimer glowTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                curMouseX += (targetMouseX - curMouseX) * 0.12;
                curMouseY += (targetMouseY - curMouseY) * 0.12;
                cursorGlow.setCenterX(curMouseX);
                cursorGlow.setCenterY(curMouseY);
            }
        };
        glowTimer.start();
    }

    private void updateGlowGradient() {
        if (cursorGlow == null) return;
        Color accent = accentColorPicker.getValue() != null ? accentColorPicker.getValue() : Color.web("#d946ef");
        RadialGradient glowGradient = new RadialGradient(
                0, 0, 0.5, 0.5, 0.25, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.35)),
                new Stop(0.5, Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.10)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        cursorGlow.setFill(glowGradient);
    }

    private void setupAnimatedLogoGradient() {
        AnimationTimer timer = new AnimationTimer() {
            private double angle = 0;

            @Override
            public void handle(long now) {
                angle = (angle + 1.2) % 360;
                Color accent = accentColorPicker.getValue() != null ? accentColorPicker.getValue() : Color.web("#d946ef");

                double rad = Math.toRadians(angle);
                double startX = 0.5 + 0.5 * Math.cos(rad);
                double startY = 0.5 + 0.5 * Math.sin(rad);
                double endX = 1.0 - startX;
                double endY = 1.0 - startY;

                Label targetLabel = logoLabel;
                if (targetLabel == null && mainLogo != null && mainLogo.getParent() instanceof HBox) {
                    for (Node node : ((HBox) mainLogo.getParent()).getChildren()) {
                        if (node instanceof Label l) {
                            targetLabel = l;
                            break;
                        }
                    }
                }

                if (targetLabel != null) {
                    LinearGradient gradient = new LinearGradient(
                            startX, startY, endX, endY, true, CycleMethod.NO_CYCLE,
                            new Stop(0, accent),
                            new Stop(0.5, Color.WHITE),
                            new Stop(1.0, accent)
                    );
                    targetLabel.setTextFill(gradient);
                }
            }
        };
        timer.start();
    }

    private void playStartupAnimations() {
        rootPane.setOpacity(0);
        rootPane.setScaleX(0.94); rootPane.setScaleY(0.94);

        FadeTransition rootFade = new FadeTransition(Duration.millis(2600), rootPane);
        rootFade.setToValue(1.0);
        rootFade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition rootScale = new ScaleTransition(Duration.millis(2600), rootPane);
        rootScale.setToX(1.0); rootScale.setToY(1.0);
        rootScale.setInterpolator(Interpolator.EASE_OUT);

        if (sidebar != null) {
            sidebar.setTranslateX(-60);
            sidebar.setOpacity(0);
            TranslateTransition sbTrans = new TranslateTransition(Duration.millis(400), sidebar);
            sbTrans.setToX(0);
            sbTrans.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition sbFade = new FadeTransition(Duration.millis(900), sidebar);
            sbFade.setToValue(1.0);

            new ParallelTransition(sbTrans, sbFade).play();
        }

        if (logoContainer != null) {
            logoContainer.setTranslateY(-40);
            logoContainer.setOpacity(0);
            TranslateTransition logoTrans = new TranslateTransition(Duration.millis(800), logoContainer);
            logoTrans.setToY(0);
            logoTrans.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition logoFade = new FadeTransition(Duration.millis(800), logoContainer);
            logoFade.setToValue(1.0);

            new ParallelTransition(logoTrans, logoFade).play();
        }

        if (centerContent != null) {
            centerContent.setTranslateY(30);
            centerContent.setOpacity(0);
            TranslateTransition cntTrans = new TranslateTransition(Duration.millis(1000), centerContent);
            cntTrans.setToY(0);
            cntTrans.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition cntFade = new FadeTransition(Duration.millis(1000), centerContent);
            cntFade.setToValue(1.0);

            new ParallelTransition(cntTrans, cntFade).play();
        }

        if (rightSidebar != null) {
            rightSidebar.setTranslateX(60);
            rightSidebar.setOpacity(0);
            TranslateTransition rsTrans = new TranslateTransition(Duration.millis(900), rightSidebar);
            rsTrans.setToX(0);
            rsTrans.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition rsFade = new FadeTransition(Duration.millis(900), rightSidebar);
            rsFade.setToValue(1.0);

            new ParallelTransition(rsTrans, rsFade).play();
        }

        new ParallelTransition(rootFade, rootScale).play();
        SoundController.playStartup();
    }

    private void applyLetterSpacing(Node node) {
        if (node == null) return;

        if (node.getStyleClass().contains("logo-text-large")) {
            return;
        }

        // Skip desktop tile labels — letter spacing breaks the short label text
        if (node.getStyleClass().contains("desktop-tile-label")) {
            return;
        }

        if (node instanceof Labeled labeled) {
            String text = labeled.getText();
            if (text != null && !text.isEmpty() && !text.contains("\u2006")) {
                labeled.setText(String.join("\u2006", text.split("")));
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyLetterSpacing(child);
            }
        }
    }

    private void applyLogoCutout() {
        Image original = mainLogo.getImage();
        if (original != null) {
            int w = (int) original.getWidth();
            int h = (int) original.getHeight();
            WritableImage cutout = new WritableImage(w, h);
            PixelReader reader = original.getPixelReader();
            PixelWriter writer = cutout.getPixelWriter();

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color c = reader.getColor(x, y);
                    if (c.getBrightness() < 0.2) {
                        writer.setColor(x, y, Color.TRANSPARENT);
                    } else {
                        writer.setColor(x, y, Color.WHITE);
                    }
                }
            }
            mainLogo.setImage(cutout);
        }
    }

    private void setupAnimations() {
        // Only apply hover/click animations to nav and tab buttons — NOT desktop tiles!
        AnimationUtils.applySmoothHoverAndClick(navDashboardBtn);
        AnimationUtils.applySmoothHoverAndClick(navSettingsBtn);
        AnimationUtils.applySmoothHoverAndClick(tabGeneralBtn);
        AnimationUtils.applySmoothHoverAndClick(tabGraphicsBtn);
        AnimationUtils.applySmoothHoverAndClick(tabKeybindsBtn);
        AnimationUtils.applySmoothHoverAndClick(tabSoundsBtn);
        // DO NOT apply to sampleDesktopTile — it conflicts with drag handlers
    }

    public void updateDynamicStyles() {
        Color color = accentColorPicker.getValue();
        String hex = String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255), (int)(color.getGreen() * 255), (int)(color.getBlue() * 255));

        rootPane.setStyle("-fx-accent: " + hex + ";");
        updateGlowGradient();

        if (widgetManager != null) {
            widgetManager.setAccentColor(color);
        }
    }

    private void setupSidebarHover() {
        sidebar.setOnMouseEntered(e -> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(150),
                    new KeyValue(sidebar.prefWidthProperty(), 100)));
            timeline.play();
            navDashboardTxt.setVisible(true); navDashboardTxt.setManaged(true);
            navSettingsTxt.setVisible(true); navSettingsTxt.setManaged(true);
        });
        sidebar.setOnMouseExited(e -> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(150),
                    new KeyValue(sidebar.prefWidthProperty(), 60)));
            timeline.play();
            navDashboardTxt.setVisible(false); navDashboardTxt.setManaged(false);
            navSettingsTxt.setVisible(false); navSettingsTxt.setManaged(false);
        });
    }

    private void setupDesktopTiles() {
        if (sampleDesktopTile != null) {
            makeDraggableAndGridSnapped(sampleDesktopTile);
        }
    }

    /**
     * Makes a desktop tile freely draggable and snaps to a grid on mouse release.
     * Uses addEventFilter so handlers cannot be overridden by later calls.
     */
    private void makeDraggableAndGridSnapped(Node node) {
        class DragContext {
            double dragOffsetX;
            double dragOffsetY;
            boolean isDragging;
            Timeline snapAnimation;
        }

        final DragContext ctx = new DragContext();

        // Use EVENT FILTERS (not setOn*) so they fire first and can't be replaced
        node.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, me -> {
            if (ctx.snapAnimation != null) ctx.snapAnimation.stop();

            if (!(node.getParent() instanceof Pane parent)) return;

            javafx.geometry.Point2D mouseInParent = parent.sceneToLocal(me.getSceneX(), me.getSceneY());
            ctx.dragOffsetX = mouseInParent.getX() - node.getLayoutX();
            ctx.dragOffsetY = mouseInParent.getY() - node.getLayoutY();
            ctx.isDragging = false;

            node.toFront();
        });

        node.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, me -> {
            if (!(node.getParent() instanceof Pane parent)) return;

            ctx.isDragging = true;
            node.setOpacity(0.75);

            javafx.geometry.Point2D mouseInParent = parent.sceneToLocal(me.getSceneX(), me.getSceneY());

            double newX = mouseInParent.getX() - ctx.dragOffsetX;
            double newY = mouseInParent.getY() - ctx.dragOffsetY;

            double maxX = Math.max(PADDING, parent.getWidth()  - TILE_WIDTH  - PADDING);
            double maxY = Math.max(PADDING, parent.getHeight() - TILE_HEIGHT - PADDING);

            node.setLayoutX(Math.max(PADDING, Math.min(newX, maxX)));
            node.setLayoutY(Math.max(PADDING, Math.min(newY, maxY)));

            me.consume(); // prevent any other handler from interfering during drag
        });

        node.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, me -> {
            node.setOpacity(1.0);

            if (!ctx.isDragging) return; // was just a click, not a drag
            ctx.isDragging = false;

            if (!(node.getParent() instanceof Pane parent)) return;

            // Snap to nearest grid cell
            double targetX = Math.round((node.getLayoutX() - PADDING) / GRID_CELL_W) * GRID_CELL_W + PADDING;
            double targetY = Math.round((node.getLayoutY() - PADDING) / GRID_CELL_H) * GRID_CELL_H + PADDING;

            // Clamp to desktop bounds
            double maxX = Math.max(PADDING, parent.getWidth()  - TILE_WIDTH  - PADDING);
            double maxY = Math.max(PADDING, parent.getHeight() - TILE_HEIGHT - PADDING);
            targetX = Math.max(PADDING, Math.min(targetX, maxX));
            targetY = Math.max(PADDING, Math.min(targetY, maxY));

            ctx.snapAnimation = new Timeline(
                new KeyFrame(Duration.millis(180),
                    new KeyValue(node.layoutXProperty(), targetX, Interpolator.EASE_OUT),
                    new KeyValue(node.layoutYProperty(), targetY, Interpolator.EASE_OUT)
                )
            );
            ctx.snapAnimation.play();
            SoundController.playClick();

            me.consume();
        });
    }

    private void setupMaximizedListener() {
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.maximizedProperty().addListener((obs, wasMaximized, isNowMaximized) -> {
                if (isNowMaximized) rootPane.getStyleClass().add("maximized");
                else rootPane.getStyleClass().remove("maximized");
            });
        });
    }

    private void setupWindowDragging() {
        titleBar.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    @FXML private void showDashboard() {
        dashboardView.setVisible(true); settingsView.setVisible(false);
        navDashboardBtn.getStyleClass().add("active-nav"); navSettingsBtn.getStyleClass().remove("active-nav");
    }

    @FXML private void showSettings() {
        dashboardView.setVisible(false); settingsView.setVisible(true);
        navSettingsBtn.getStyleClass().add("active-nav"); navDashboardBtn.getStyleClass().remove("active-nav");
    }

    private void resetSettingsTabs() {
        generalPane.setVisible(false); graphicsPane.setVisible(false);
        keybindsPane.setVisible(false); soundsPane.setVisible(false);
        tabGeneralBtn.getStyleClass().remove("active-sub-tab"); tabGraphicsBtn.getStyleClass().remove("active-sub-tab");
        tabKeybindsBtn.getStyleClass().remove("active-sub-tab"); tabSoundsBtn.getStyleClass().remove("active-sub-tab");
    }

    @FXML private void showSettingsGeneral() { resetSettingsTabs(); generalPane.setVisible(true); tabGeneralBtn.getStyleClass().add("active-sub-tab"); }
    @FXML private void showSettingsGraphics() { resetSettingsTabs(); graphicsPane.setVisible(true); tabGraphicsBtn.getStyleClass().add("active-sub-tab"); }
    @FXML private void showSettingsKeybinds() { resetSettingsTabs(); keybindsPane.setVisible(true); tabKeybindsBtn.getStyleClass().add("active-sub-tab"); }
    @FXML private void showSettingsSounds() { resetSettingsTabs(); soundsPane.setVisible(true); tabSoundsBtn.getStyleClass().add("active-sub-tab"); }

    @FXML private void minimizeWindow() { ((Stage) rootPane.getScene().getWindow()).setIconified(true); }
    @FXML private void maximizeWindow() { Stage s = (Stage) rootPane.getScene().getWindow(); s.setMaximized(!s.isMaximized()); }

    @FXML private void closeWindow() {
        SoundController.playShutdown();

        FadeTransition ft = new FadeTransition(Duration.millis(300), rootPane);
        ft.setToValue(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), rootPane);
        st.setToX(0.4); st.setToY(0.4);

        ft.setOnFinished(e -> { Platform.exit(); System.exit(0); });
        ft.play(); st.play();
    }
}
