package singularity.ui.animations;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;
import singularity.ui.utils.SoundController;

public class AnimationUtils {

    public static void applySmoothHoverAndClick(Node node) {
        if (node == null) return;

        // Hover In: Scale + leichtes Anheben (-2px)
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(180), node);
        scaleIn.setToX(1.04);
        scaleIn.setToY(1.04);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition translateIn = new TranslateTransition(Duration.millis(180), node);
        translateIn.setToY(-2);
        translateIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition hoverIn = new ParallelTransition(scaleIn, translateIn);

        // Hover Out: Zurück zur Ausgangsposition
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), node);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);
        scaleOut.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition translateOut = new TranslateTransition(Duration.millis(180), node);
        translateOut.setToY(0);
        translateOut.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition hoverOut = new ParallelTransition(scaleOut, translateOut);

        // Click Press: Leichtes Eindrucken (Scale 0.96)
        ScaleTransition scalePress = new ScaleTransition(Duration.millis(70), node);
        scalePress.setToX(0.96);
        scalePress.setToY(0.96);

        ScaleTransition scaleRelease = new ScaleTransition(Duration.millis(90), node);
        scaleRelease.setToX(1.04);
        scaleRelease.setToY(1.04);

        node.setOnMouseEntered(e -> {
            SoundController.playHover();
            hoverOut.stop();
            hoverIn.playFromStart();
        });

        node.setOnMouseExited(e -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        });

        node.setOnMousePressed(e -> {
            SoundController.playClick();
            scaleRelease.stop();
            scalePress.playFromStart();
        });

        node.setOnMouseReleased(e -> {
            scalePress.stop();
            scaleRelease.playFromStart();
        });
    }
}
