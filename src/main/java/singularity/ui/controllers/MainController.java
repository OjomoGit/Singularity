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

    @FXML
    public void initialize() {
        UpdateChecker.checkForUpdatesAsync();
        setupWindowDragging();
        setupMaximizedListener();
        setupSidebarHover();
        setupDesktopTiles();
        setupAnimations();

        showDashboard();
        showSettingsGeneral();

        applyLogoCutout();

        // Initialize Widget System in Right Sidebar
        setupWidgetSystem();

        accentColorPicker.setValue(Color.web("#d946ef"));
        accentColorPicker.valueProperty().addListener((obs, oldC, newC) -> updateDynamicStyles());

        updateDynamicStyles();

        // Cursor Follower Light Overlay
        setupCursorGlow();

        // Logo Gradient Animator
        setupAnimatedLogoGradient();

        // Fluid Startup Entrance
        playStartupAnimations();

        if (mainLogo != null && mainLogo.getParent() instanceof HBox) {
            for (Node node : ((HBox) mainLogo.getParent()).getChildren()) {
                if (node instanceof Label) {
                    ((Label) node).setFont(Font.font("Miracle History", 36));
                }
            }
        }

        // Letter-Spacing (ausgenommen Logo)
        Platform.runLater(() -> applyLetterSpacing(rootPane));
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
        AnimationUtils.applySmoothHoverAndClick(navDashboardBtn);
        AnimationUtils.applySmoothHoverAndClick(navSettingsBtn);
        AnimationUtils.applySmoothHoverAndClick(tabGeneralBtn);
        AnimationUtils.applySmoothHoverAndClick(tabGraphicsBtn);
        AnimationUtils.applySmoothHoverAndClick(tabKeybindsBtn);
        AnimationUtils.applySmoothHoverAndClick(tabSoundsBtn);
        AnimationUtils.applySmoothHoverAndClick(sampleDesktopTile);
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
        makeDraggable(sampleDesktopTile);
    }

    private void makeDraggable(Node node) {
        class Delta { double x, y; }
        final Delta dragDelta = new Delta();

        node.setOnMousePressed(me -> {
            dragDelta.x = node.getLayoutX() - me.getSceneX();
            dragDelta.y = node.getLayoutY() - me.getSceneY();
            node.toFront();
        });
        node.setOnMouseDragged(me -> {
            node.setLayoutX(me.getSceneX() + dragDelta.x);
            node.setLayoutY(me.getSceneY() + dragDelta.y);
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
