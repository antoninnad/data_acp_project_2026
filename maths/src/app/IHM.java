package app;

import abstraction.Image;
import abstraction.PCA;
import abstraction.Query;
import math.Matrix;
import math.Vector;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

public class IHM extends Application {

    private PCA pca;
    private Query query;
    private Map<String, List<Vector>> dataBase;
    private Map<String, String> labelToImagePath;

    private ImageView queryImageView;
    private ImageView resultImageView;
    private Label resultNameLabel;
    private Label statusLabel;
    private Label axesLabel;
    private Label varianceLabel;
    private TextField axesField;
    private Button searchButton;
    private Button applyAxesButton;
    private ImageView averageFaceView;
    private FlowPane eigenfacesPane;

    private File selectedFile;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox topBar = buildTopBar();
        root.setTop(topBar);
        BorderPane.setMargin(topBar, new Insets(0, 0, 10, 0));

        GridPane centre = buildCentre();
        root.setCenter(centre);

        Scene scene = new Scene(root, 1100, 750);
        stage.setScene(scene);
        stage.setTitle("Reconnaissance faciale - ACP");
        stage.show();

        initPcaAsync();
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 8, 6, 8));
        bar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #dcdcdc; -fx-border-width: 0 0 1 0;");

        MenuButton menu = new MenuButton("Menu");
        MenuItem reloadItem = new MenuItem("Recharger la base");
        reloadItem.setOnAction(e -> initPcaAsync());
        menu.getItems().add(reloadItem);

        statusLabel = new Label("Initialisation...");
        statusLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(menu, spacer, statusLabel);
        return bar;
    }

    private GridPane buildCentre() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(62);
        row1.setVgrow(Priority.ALWAYS);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(38);
        row2.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(row1, row2);

        VBox searchZone = buildSearchZone();
        TabPane visualsTab = buildVisualsTab();
        VBox resultZone = buildResultZone();
        VBox paramZone = buildParamZone();

        grid.add(searchZone,  0, 0);
        grid.add(visualsTab,  1, 0);
        grid.add(resultZone,  0, 1);
        grid.add(paramZone,   1, 1);

        Region[] all = { searchZone, visualsTab, resultZone, paramZone };
        for (Region r : all) {
            GridPane.setHgrow(r, Priority.ALWAYS);
            GridPane.setVgrow(r, Priority.ALWAYS);
            r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        return grid;
    }

    // ── Zone saisie image ──────────────────────────────────────────────────────

    private VBox buildSearchZone() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #d0d0d0; -fx-border-radius: 4; -fx-background-color: white;");

        Label title = new Label("Image à analyser");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        queryImageView = new ImageView();
        queryImageView.setPreserveRatio(true);
        queryImageView.setFitWidth(380);
        queryImageView.setFitHeight(220);

        Label placeholder = new Label("Aucune image sélectionnée");
        placeholder.setStyle("-fx-text-fill: #aaaaaa;");

        StackPane imgContainer = new StackPane(placeholder, queryImageView);
        imgContainer.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #e0e0e0;");
        imgContainer.setMinHeight(220);
        VBox.setVgrow(imgContainer, Priority.ALWAYS);

        queryImageView.imageProperty().addListener((obs, old, nv) -> placeholder.setVisible(nv == null));

        Button browseBtn = new Button("Parcourir...");
        browseBtn.setOnAction(e -> browseImage());

        searchButton = new Button("Rechercher");
        searchButton.setDisable(true);
        searchButton.setStyle("-fx-background-color: #3d8fd4; -fx-text-fill: white; -fx-font-weight: bold;");
        searchButton.setOnAction(e -> lancerRecherche());

        HBox buttons = new HBox(10, browseBtn, searchButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(title, imgContainer, buttons);
        return box;
    }

    // ── Zone résultat ──────────────────────────────────────────────────────────

    private VBox buildResultZone() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #d0d0d0; -fx-border-radius: 4; -fx-background-color: white;");

        Label title = new Label("Résultat de la recherche");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        resultNameLabel = new Label("—");
        resultNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        resultImageView = new ImageView();
        resultImageView.setPreserveRatio(true);
        resultImageView.setFitWidth(190);
        resultImageView.setFitHeight(140);

        Label resultPlaceholder = new Label("Lancez une recherche pour voir le résultat");
        resultPlaceholder.setStyle("-fx-text-fill: #aaaaaa;");

        StackPane resultImgContainer = new StackPane(resultPlaceholder, resultImageView);
        resultImgContainer.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #e0e0e0;");
        resultImgContainer.setMinHeight(140);
        VBox.setVgrow(resultImgContainer, Priority.ALWAYS);

        resultImageView.imageProperty().addListener((obs, old, nv) -> resultPlaceholder.setVisible(nv == null));

        box.getChildren().addAll(title, resultNameLabel, resultImgContainer);
        return box;
    }

    // ── Onglets visualisations ─────────────────────────────────────────────────

    private TabPane buildVisualsTab() {
        TabPane tabs = new TabPane();

        // Onglet visage moyen
        Tab avgTab = new Tab("Visage moyen");
        avgTab.setClosable(false);
        averageFaceView = new ImageView();
        averageFaceView.setPreserveRatio(true);
        averageFaceView.setFitWidth(220);
        averageFaceView.setFitHeight(220);
        StackPane avgPane = new StackPane(averageFaceView);
        avgPane.setStyle("-fx-background-color: #f7f7f7;");
        ScrollPane avgScroll = new ScrollPane(avgPane);
        avgScroll.setFitToWidth(true);
        avgScroll.setFitToHeight(true);
        avgTab.setContent(avgScroll);

        // Onglet eigenfaces
        Tab eigenTab = new Tab("Eigenfaces");
        eigenTab.setClosable(false);
        eigenfacesPane = new FlowPane(8, 8);
        eigenfacesPane.setPadding(new Insets(10));
        ScrollPane eigenScroll = new ScrollPane(eigenfacesPane);
        eigenScroll.setFitToWidth(true);
        eigenTab.setContent(eigenScroll);

        tabs.getTabs().addAll(avgTab, eigenTab);
        return tabs;
    }

    // ── Zone paramètres ────────────────────────────────────────────────────────

    private VBox buildParamZone() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #d0d0d0; -fx-border-radius: 4; -fx-background-color: white;");

        Label title = new Label("Paramètres");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label axesInputLabel = new Label("Nombre d'axes :");
        axesField = new TextField();
        axesField.setPromptText("entier > 0");
        axesField.setPrefWidth(90);

        applyAxesButton = new Button("Appliquer");
        applyAxesButton.setDisable(true);
        applyAxesButton.setOnAction(e -> applyAxes());

        HBox axesRow = new HBox(8, axesInputLabel, axesField, applyAxesButton);
        axesRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();

        axesLabel    = new Label("Axes retenus : —");
        varianceLabel = new Label("Variance cumulée : —");

        box.getChildren().addAll(title, axesRow, sep, axesLabel, varianceLabel);
        return box;
    }

    // ── Initialisation PCA ─────────────────────────────────────────────────────

    private void initPcaAsync() {
        if (searchButton     != null) searchButton.setDisable(true);
        if (applyAxesButton  != null) applyAxesButton.setDisable(true);
        setStatus("Chargement de la base...", true);

        Thread t = new Thread(() -> {
            try {
                PCA newPca                          = new PCA();
                Query newQuery                      = new Query();
                Map<String, List<Vector>> newDb     = newPca.getMapSign();
                Map<String, String> newPaths        = newPca.getLabelToImagePath();

                Platform.runLater(() -> {
                    pca              = newPca;
                    query            = newQuery;
                    dataBase         = newDb;
                    labelToImagePath = newPaths;
                    onPcaReady();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Erreur : " + ex.getMessage(), false));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void onPcaReady() {
        applyAxesButton.setDisable(false);
        if (selectedFile != null) searchButton.setDisable(false);
        axesField.setText(String.valueOf(pca.getNumberOfKeptAxes()));
        updateStats();
        loadVisuals();
        setStatus("Base chargée — " + pca.getNumberOfKeptAxes() + " axes retenus", false);
    }

    // ── Statistiques ──────────────────────────────────────────────────────────

    private void updateStats() {
        int n = pca.getNumberOfKeptAxes();
        int max = pca.getMaxNumberOfKeptAxes();
        axesLabel.setText("Axes retenus : " + n + " / " + max);

        try {
            Vector eigenvals = pca.getEigenValues();
            double total = 0;
            double[] vals = new double[eigenvals.getDimension()];
            for (int i = 0; i < eigenvals.getDimension(); i++) {
                vals[i] = Math.max(0, eigenvals.get(i));
                total  += vals[i];
            }
            java.util.Arrays.sort(vals); // tri croissant
            // on prend les n plus grandes valeurs (fin du tableau trié)
            double cumul = 0;
            for (int i = vals.length - 1; i >= vals.length - n && i >= 0; i--) {
                cumul += vals[i];
            }
            double pct = total > 0 ? 100.0 * cumul / total : 0;
            varianceLabel.setText(String.format("Variance cumulée : %.1f%%", pct));
        } catch (Exception ex) {
            varianceLabel.setText("Variance cumulée : N/A");
        }
    }

    // ── Visualisations ────────────────────────────────────────────────────────

    private void loadVisuals() {
        // Visage moyen
        try {
            Vector face = pca.getPixelMeans();
            if (face == null) face = pca.getMeanFace();
            if (face != null) {
                File tmp = File.createTempFile("ihm_mean_", ".jpg");
                tmp.deleteOnExit();
                // pixelMeans : valeurs brutes [0-255], toImage clip sans déformation
                // getMeanFace : valeurs centrées, centeredVectorToImage normalise
                if (pca.getPixelMeans() != null) {
                    Image.toImage(face, tmp.getAbsolutePath());
                } else {
                    Image.centeredVectorToImage(face, tmp.getAbsolutePath());
                }
                averageFaceView.setImage(new javafx.scene.image.Image(tmp.toURI().toString()));
            }
        } catch (Exception ex) {
            System.err.println("[IHM] visage moyen : " + ex.getMessage());
        }

        // Eigenfaces
        eigenfacesPane.getChildren().clear();
        try {
            Matrix eigenfaces = pca.getEigenfaces();
            if (eigenfaces != null) {
                int count = Math.min(20, eigenfaces.getNbColumns());
                for (int i = 0; i < count; i++) {
                    try {
                        File tmp = File.createTempFile("ihm_ef" + i + "_", ".jpg");
                        tmp.deleteOnExit();
                        Image.centeredVectorToImage(eigenfaces.getColumn(i), tmp.getAbsolutePath());

                        ImageView iv = new ImageView(new javafx.scene.image.Image(tmp.toURI().toString()));
                        iv.setFitWidth(64);
                        iv.setFitHeight(64);
                        iv.setPreserveRatio(true);

                        Label lbl = new Label("EF " + (i + 1));
                        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #555555;");

                        VBox item = new VBox(3, iv, lbl);
                        item.setAlignment(Pos.CENTER);
                        eigenfacesPane.getChildren().add(item);
                    } catch (Exception ex) {
                        System.err.println("[IHM] eigenface " + i + " : " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[IHM] eigenfaces : " + ex.getMessage());
        }
    }

    // ── Recherche ─────────────────────────────────────────────────────────────

    private void browseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner une image");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png")
        );
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file;
            queryImageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            resultNameLabel.setText("—");
            resultNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
            resultImageView.setImage(null);
            if (pca != null) searchButton.setDisable(false);
        }
    }

    private void lancerRecherche() {
        if (selectedFile == null || pca == null) return;
        searchButton.setDisable(true);
        setStatus("Recherche en cours...", true);

        Thread t = new Thread(() -> {
            try {
                Vector pixels    = getQueryPixels(selectedFile);
                Vector projected = pca.projectVector(pixels);
                String label     = query.findBestMatch(projected, dataBase);
                String imgPath   = label.isEmpty() ? null : labelToImagePath.get(label);

                Platform.runLater(() -> {
                    if (label.isEmpty()) {
                        resultNameLabel.setText("Personne inconnue");
                        resultNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #cc3333;");
                        resultImageView.setImage(null);
                    } else {
                        resultNameLabel.setText("Personne n° " + label);
                        resultNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2a7a2a;");
                        if (imgPath != null) {
                            resultImageView.setImage(
                                    new javafx.scene.image.Image(new File(imgPath).toURI().toString())
                            );
                        }
                    }
                    searchButton.setDisable(false);
                    setStatus("Recherche terminée", false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    resultNameLabel.setText("Erreur : " + ex.getMessage());
                    searchButton.setDisable(false);
                    setStatus("Erreur lors de la recherche", false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private Vector getQueryPixels(File imageFile) throws IOException {
        // Image.getPixels() gère déjà le recadrage CelebA (178x218 → 64x64)
        Image img    = new Image(imageFile.getAbsolutePath(), "query");
        Vector pixels = img.getPixels();

        int expectedDim = pca.getFacesCoordinates().getNbRows();
        if (pixels.getDimension() == expectedDim) {
            return pixels;
        }

        // Redimensionnement si l'image n'est pas au bon format
        int side = (int) Math.round(Math.sqrt(expectedDim));
        BufferedImage raw = ImageIO.read(imageFile);
        if (raw == null) throw new IOException("Impossible de lire : " + imageFile.getName());

        BufferedImage resized = new BufferedImage(side, side, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(raw, 0, 0, side, side, null);
        g2.dispose();

        Vector result = new Vector(side * side);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int rgb = resized.getRGB(x, y);
                double r  = (rgb >> 16) & 0xFF;
                double gv = (rgb >>  8) & 0xFF;
                double b  =  rgb        & 0xFF;
                result.set(y * side + x, (r + gv + b) / 3.0);
            }
        }
        return result;
    }

    // ── Paramètres ────────────────────────────────────────────────────────────

    private void applyAxes() {
        if (pca == null) return;
        try {
            int n = Integer.parseInt(axesField.getText().trim());
            if (n <= 0) {
                showError("Le nombre d'axes doit être un entier strictement positif.");
                return;
            }
            pca.setNumberOfKeptAxes(n);
            dataBase = pca.getMapSign();
            int actual = pca.getNumberOfKeptAxes();
            axesField.setText(String.valueOf(actual));
            updateStats();
            setStatus("Axes mis à jour : " + actual, false);
        } catch (NumberFormatException ex) {
            showError("Valeur invalide — entrez un entier positif.");
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private void setStatus(String msg, boolean loading) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(loading
                ? "-fx-text-fill: #888888; -fx-font-style: italic;"
                : "-fx-text-fill: #333333;");
    }

    public static void main(String[] args) {
        System.out.println("Le dossier racine est : " + System.getProperty("user.dir"));
        launch(args);
    }
}
