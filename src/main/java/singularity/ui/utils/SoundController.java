package singularity.ui.utils;

import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundController {

    private static void playSound(String fileName, double volume) {
        try {
            URL resource = SoundController.class.getResource("/singularity/sounds/" + fileName);
            if (resource != null) {
                new AudioClip(resource.toExternalForm()).play(volume);
            }
        } catch (Exception e) {
            System.out.println("Sound nicht gefunden oder Fehler: " + fileName);
        }
    }

    public static void playHover() { playSound("hover.wav", 0.025); }
    public static void playClick() { playSound("click.wav", 0.5); }
    public static void playStartup() { playSound("startup.wav", 0.05); }
    public static void playShutdown() { playSound("shutdown.wav", 0.05); }
}
