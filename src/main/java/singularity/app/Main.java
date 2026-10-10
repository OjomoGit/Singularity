package singularity.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import singularity.ui.utils.Windows11Blur;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Automatisch alle Fonts aus dem Ordner in den JavaFX Speicher laden, BEVOR die FXML geladen wird
        loadAllFonts();

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/singularity/fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 900);

        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("/singularity/css/styles.css")).toExternalForm());
        scene.setFill(Color.TRANSPARENT);

        stage.initStyle(StageStyle.UNDECORATED);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(true);

        String windowTitle = "Hub";
        stage.setTitle(windowTitle);
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        stage.show();

        Windows11Blur.applyAcrylic(windowTitle);
    }

    // Lädt automatisch alle Fonts (.ttf und .otf) aus dem Ordner in den Speicher
    private void loadAllFonts() {
        try {
            URL fontDirUrl = Main.class.getResource("/singularity/fonts/");
            if (fontDirUrl != null) {
                Path fontDirPath = Paths.get(fontDirUrl.toURI());
                if (Files.exists(fontDirPath) && Files.isDirectory(fontDirPath)) {
                    try (Stream<Path> stream = Files.list(fontDirPath)) {
                        stream.filter(path -> {
                            String name = path.getFileName().toString().toLowerCase();
                            return name.endsWith(".ttf") || name.endsWith(".otf");
                        }).forEach(path -> {
                            try (InputStream is = Files.newInputStream(path)) {
                                Font loadedFont = Font.loadFont(is, 14);
                                if (loadedFont != null) {
                                    System.out.println("Font geladen: " + path.getFileName() + " (" + loadedFont.getFamily() + ")");
                                }
                            } catch (Exception ex) {
                                System.err.println("Fehler beim Laden von Font " + path.getFileName() + ": " + ex.getMessage());
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim automatischen Laden der Fonts: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
