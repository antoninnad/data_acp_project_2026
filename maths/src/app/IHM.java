package app;

import abstraction.Image;
import abstraction.PCA;
import abstraction.Query;
import graph.VarianceGraph;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import math.Matrix;
import math.Vector;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

/**
 * Main JavaFX application window for the PCA-based face recognition system.
 *
 * <p>The interface is split into four zones in a 2×2 grid:</p>
 * <ul>
 *   <li><b>Top-left</b>  – query image selection and search button.</li>
 *   <li><b>Top-right</b> – visualisation tabs (average face, eigenfaces,
 *       cumulative eigenvalue chart, quadratic error chart).</li>
 *   <li><b>Bottom-left</b> – recognition result (matched person and their image).</li>
 *   <li><b>Bottom-right</b> – PCA parameters (number of kept axes, cumulative variance).</li>
 * </ul>
 *
 * <p>The {@link PCA} model is built asynchronously on startup. The heavy quadratic error
 * computation also runs on a background thread and updates the UI via
 * {@link javafx.application.Platform#runLater}.</p>
 *
 * @see PCA
 * @see Query
 */
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
    private Tab cumulativeEigenvaluesTab;
    private Tab quadraticErrorTab;

    private File selectedFile;

    /**
     * JavaFX entry point. Builds the scene graph, shows the window, and triggers
     * asynchronous PCA initialisation.
     *
     * @param stage the primary window provided by the JavaFX runtime
     */
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

    /**
     * Builds the top bar containing the menu and the status label.
     *
     * @return an {@code HBox} ready to be placed at the top of the root {@code BorderPane}
     */
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

    /**
     * Builds the central 2×2 grid that contains all four functional zones of the UI.
     *
     * @return a {@code GridPane} with search, visuals, result, and parameter zones
     */
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

    /**
     * Builds the top-left zone with the query image display, browse button, and search button.
     *
     * @return a {@code VBox} containing the image container and action buttons
     */
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

    /**
     * Builds the bottom-left zone that displays the matched person's name and a sample image.
     *
     * @return a {@code VBox} containing the result label and image view
     */
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

    /**
     * Builds the top-right tab pane with four tabs: average face, eigenfaces,
     * cumulative eigenvalue chart, and quadratic error chart.
     * Chart tabs start with a placeholder until the PCA model is ready.
     *
     * @return a {@code TabPane} with all four visualisation tabs
     */
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

        cumulativeEigenvaluesTab = new Tab("Cumulative Eigenvalues");
        cumulativeEigenvaluesTab.setClosable(false);
        Label placeholder = new Label("Chargement de la base...");
        placeholder.setStyle("-fx-text-fill: #aaaaaa; -fx-font-style: italic;");
        StackPane phPane = new StackPane(placeholder);
        phPane.setStyle("-fx-background-color: #f7f7f7;");
        cumulativeEigenvaluesTab.setContent(phPane);

        quadraticErrorTab = new Tab("Quadratic Error");
        quadraticErrorTab.setClosable(false);
        Label qePlaceholder = new Label("Chargement de la base...");
        qePlaceholder.setStyle("-fx-text-fill: #aaaaaa; -fx-font-style: italic;");
        StackPane qePane = new StackPane(qePlaceholder);
        qePane.setStyle("-fx-background-color: #f7f7f7;");
        quadraticErrorTab.setContent(qePane);

        tabs.getTabs().addAll(avgTab, eigenTab, cumulativeEigenvaluesTab, quadraticErrorTab);
        return tabs;
    }

    /**
     * Builds the bottom-right zone with the axes input field, apply button,
     * and read-only statistics labels.
     *
     * @return a {@code VBox} containing the parameter controls
     */
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

    /**
     * Launches PCA initialisation on a daemon background thread.
     * Disables the search and axes buttons during loading, then calls
     * {@link #onPcaReady()} on the JavaFX thread when done.
     */
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

    /**
     * Called on the JavaFX thread once the PCA model has been fully initialised.
     * Enables controls, updates statistics, loads visual elements, and triggers
     * asynchronous chart generation.
     */
    private void onPcaReady() {
        applyAxesButton.setDisable(false);
        if (selectedFile != null) searchButton.setDisable(false);
        axesField.setText(String.valueOf(pca.getNumberOfKeptAxes()));
        updateStats();
        loadVisuals();
        loadVarianceGraph();
        loadQuadraticErrorGraph();
        setStatus("Base chargée — " + pca.getNumberOfKeptAxes() + " axes retenus", false);
    }

    /**
     * Computes the quadratic reconstruction error on a background thread and
     * builds the corresponding line chart on the JavaFX thread.
     * Updates the content of {@link #quadraticErrorTab} when done, or shows
     * an error label if the computation fails.
     */
    private void loadQuadraticErrorGraph() {
        Thread t = new Thread(() -> {
            try {
                int nAxes = pca.getNumberOfKeptAxes();
                Matrix eigenFacesMatrix = pca.getKeptEigenfaces();
                Matrix projected = pca.getProjectedFacesOnKeptAxes();
                Matrix original = pca.getFacesCoordinates();
                int nbImages = original.getNbColumns();

                String[] names = new String[nAxes];
                double[] errors = new double[nAxes];
                for (int i = 1; i <= nAxes; i++) {
                    names[i - 1] = "axe" + i;
                    Matrix eigen_i = eigenFacesMatrix.subMatrixFirstColumns(i - 1);
                    Matrix proj_i  = projected.getSubRows(0, i - 1);
                    Matrix recon   = eigen_i.multiply(proj_i);
                    double total   = 0.0;
                    for (int j = 0; j < nbImages; j++) {
                        Vector diff = original.getColumn(j).difference(recon.getColumn(j));
                        total += diff.norm();
                    }
                    errors[i - 1] = total / nbImages;
                }

                Platform.runLater(() -> {
                    CategoryAxis xAxis = new CategoryAxis();
                    xAxis.setLabel("Axes");
                    NumberAxis yAxis = new NumberAxis();
                    yAxis.setLabel("Quadratic Error E(J)");
                    LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
                    chart.setTitle("Evolution of the quadratic error depending on axes");
                    chart.setCreateSymbols(false);
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Error");
                    for (int i = 0; i < names.length; i++) {
                        series.getData().add(new XYChart.Data<>(names[i], errors[i]));
                    }
                    xAxis.getCategories().addAll(Arrays.asList(names));
                    chart.getData().add(series);
                    ScrollPane scroll = new ScrollPane(chart);
                    scroll.setFitToWidth(true);
                    scroll.setFitToHeight(true);
                    quadraticErrorTab.setContent(scroll);
                });
            } catch (Exception ex) {
                System.err.println("[IHM] quadratic error graph : " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Label err = new Label("Erreur : " + ex.getMessage());
                    err.setStyle("-fx-text-fill: #cc3333;");
                    quadraticErrorTab.setContent(new StackPane(err));
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Builds the cumulative eigenvalue chart synchronously (lightweight) and sets it
     * as the content of {@link #cumulativeEigenvaluesTab}.
     */
    private void loadVarianceGraph() {
        try {
            VarianceGraph vg = new VarianceGraph(pca);
            ScrollPane scroll = new ScrollPane(vg.generateVarianceGraph());
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            cumulativeEigenvaluesTab.setContent(scroll);
        } catch (Exception ex) {
            System.err.println("[IHM] variance graph : " + ex.getMessage());
        }
    }

    // ── Statistiques ──────────────────────────────────────────────────────────

    /**
     * Refreshes the statistics labels in the parameter zone with the current
     * number of kept axes and their cumulative explained variance.
     */
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

    /**
     * Loads and displays the average face and the first 20 eigenfaces in their
     * respective tabs. Each image is written to a temporary JPEG and loaded into
     * an {@code ImageView} rotated 90° to match the dataset orientation.
     */
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
                averageFaceView.setRotate(90);
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
                        iv.setRotate(90);

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

    /**
     * Opens a file chooser dialog, loads the selected image into the query image view,
     * and enables the search button. Resets any previous search result.
     */
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
            queryImageView.setRotate(90);
            resultNameLabel.setText("—");
            resultNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
            resultImageView.setImage(null);
            if (pca != null) searchButton.setDisable(false);
        }
    }

    /**
     * Projects the selected query image into the PCA eigenspace on a background thread
     * and displays the matched person's label and image in the result zone.
     * The search button is re-enabled once the result is available.
     */
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
                            resultImageView.setRotate(90);
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

    /**
     * Reads a query image file and returns its pixel vector in the format expected by
     * {@link PCA#projectVector}. If the image is already at the right size it is used
     * directly; otherwise it is resized to the expected square side length.
     *
     * @param imageFile image file to read (JPG or PNG)
     * @return flat greyscale pixel vector of the expected dimension
     * @throws IOException if the file cannot be read or decoded
     */
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

    /**
     * Reads the axes text field, validates the value, updates the PCA model, and
     * refreshes the statistics labels. Shows an error dialog on invalid input.
     */
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

    /**
     * Displays a modal error dialog with the given message.
     *
     * @param msg error message to show to the user
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * Updates the status label text and style in the top bar.
     *
     * @param msg     message to display
     * @param loading {@code true} to use the grey italic "loading" style,
     *                {@code false} to use the normal dark style
     */
    private void setStatus(String msg, boolean loading) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(loading
                ? "-fx-text-fill: #888888; -fx-font-style: italic;"
                : "-fx-text-fill: #333333;");
    }

    /**
     * Application entry point. Prints the working directory and launches the JavaFX runtime.
     *
     * @param args command-line arguments (passed to the JavaFX launcher)
     */
    public static void main(String[] args) {
        System.out.println("Le dossier racine est : " + System.getProperty("user.dir"));
        launch(args);
    }
}
