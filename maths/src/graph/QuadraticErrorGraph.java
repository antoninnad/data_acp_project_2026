package graph;

import abstraction.PCA;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import math.Matrix;
import math.Vector;
import java.util.Arrays;

/**
 * Generates JavaFX line charts related to PCA reconstruction quality.
 *
 * <p>Two charts are available:</p>
 * <ul>
 *   <li>{@link #generateQuadraticErrorGraph()} — mean squared reconstruction error
 *       E(J) = ‖J − J_p‖² as a function of the number of kept axes.</li>
 *   <li>{@link #generateHotellingT2Graph()} — mean Hotelling T² statistic
 *       T²(J) = Σ β_i²/λ_i as a function of the number of kept axes.</li>
 * </ul>
 *
 * @see PCA
 */
public class QuadraticErrorGraph {
	private PCA pca;
	private int numberOfKeptAxes;
	private Matrix eigenFacesMatrix;
	private String[] namesOfAxes;
	private Matrix projectedFacesOnKeptAxes;
	private Matrix original;

	/**
	 * Constructs a QuadraticErrorGraph bound to an already-computed PCA model.
	 * All required matrices are fetched from the model at construction time.
	 *
	 * @param pca trained PCA instance providing eigenfaces and face projections
	 */
	public QuadraticErrorGraph(PCA pca) {
		this.pca = pca;
		this.numberOfKeptAxes         = pca.getNumberOfKeptAxes();
		this.eigenFacesMatrix         = pca.getKeptEigenfaces();
		this.namesOfAxes              = new String[numberOfKeptAxes];
		this.projectedFacesOnKeptAxes = pca.getProjectedFacesOnKeptAxes();
		this.original                 = pca.getFacesCoordinates();
	}

	/**
	 * Builds and returns a JavaFX line chart showing the reconstruction error as a
	 * percentage of the baseline error E(0) (reconstruction with 0 axes = zero vector).
	 * Each point is E(i)/E(0) × 100, monotonically decreasing from ~100% toward 0%.
	 * Must be called on the JavaFX Application Thread.
	 *
	 * @return a {@code LineChart} ready to be embedded in a JavaFX scene
	 */
	public LineChart<String, Number> generateQuadraticErrorGraph() {
		int nbImages = original.getNbColumns();

		// E(0) : erreur avec 0 axe conservé (reconstruction = vecteur nul)
		double baselineError = 0.0;
		for (int j = 0; j < nbImages; j++) {
			double n = original.getColumn(j).norm();
			baselineError += n * n;
		}
		baselineError /= nbImages;

		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Axes");

		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Erreur résiduelle (%)");

		LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
		lineChart.setTitle("Evolution of the quadratic error depending on axes");
		lineChart.setCreateSymbols(false);

		XYChart.Series<String, Number> series = new XYChart.Series<>();
		series.setName("E(J) / E(0) [%]");

		for (int i = 1; i <= numberOfKeptAxes; i++) {
			namesOfAxes[i - 1] = "axe" + i;

			Matrix eigen_i       = eigenFacesMatrix.subMatrixFirstColumns(i - 1);
			Matrix proj_i        = projectedFacesOnKeptAxes.getSubRows(0, i - 1);
			Matrix reconstructed = eigen_i.multiply(proj_i);

			double total_error = 0.0;
			for (int j = 0; j < nbImages; j++) {
				Vector diff = original.getColumn(j).difference(reconstructed.getColumn(j));
				double n = diff.norm();
				total_error += n * n;  // ||J - Jp||²
			}

			double pct = baselineError > 0 ? (total_error / nbImages) / baselineError * 100.0 : 0.0;
			series.getData().add(new XYChart.Data<>(namesOfAxes[i - 1], pct));
		}

		xAxis.getCategories().addAll(Arrays.asList(namesOfAxes));
		lineChart.getData().add(series);
		return lineChart;
	}

	/**
	 * Builds and returns a JavaFX line chart showing the mean Hotelling T² statistic
	 * as a function of the number of kept axes.
	 *
	 * <p>For each image J and each number of axes i, the T² is computed as:</p>
	 * <pre>T²(J) = Σ_{k=1}^{i} β_k² / λ_k</pre>
	 * <p>where β_k is the projection of J onto axis k and λ_k is the variance of all
	 * training images along axis k (estimated as the mean squared projection).
	 * The chart plots the mean T² over all training images for each axis count.</p>
	 * Must be called on the JavaFX Application Thread.
	 *
	 * @return a {@code LineChart} ready to be embedded in a JavaFX scene
	 */
	public LineChart<String, Number> generateHotellingT2Graph() {
		int nbImages = projectedFacesOnKeptAxes.getNbColumns();

		// Compute λ_k = mean squared projection along each kept axis
		double[] eigenvalues = new double[numberOfKeptAxes];
		for (int k = 0; k < numberOfKeptAxes; k++) {
			double sumSq = 0.0;
			for (int j = 0; j < nbImages; j++) {
				double v = projectedFacesOnKeptAxes.get(k, j);
				sumSq += v * v;
			}
			eigenvalues[k] = nbImages > 0 ? sumSq / nbImages : 1.0;
		}

		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Axes");

		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("T² = Σ β²/λ");

		LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
		lineChart.setTitle("Hotelling T² statistic depending on axes");
		lineChart.setCreateSymbols(false);

		XYChart.Series<String, Number> series = new XYChart.Series<>();
		series.setName("T²");

		String[] names = new String[numberOfKeptAxes];
		for (int i = 1; i <= numberOfKeptAxes; i++) {
			names[i - 1] = "axe" + i;

			double totalT2 = 0.0;
			for (int j = 0; j < nbImages; j++) {
				double t2 = 0.0;
				for (int k = 0; k < i; k++) {
					double beta = projectedFacesOnKeptAxes.get(k, j);
					double lambda = eigenvalues[k] > 1e-12 ? eigenvalues[k] : 1e-12;
					t2 += (beta * beta) / lambda;
				}
				totalT2 += t2;
			}

			series.getData().add(new XYChart.Data<>(names[i - 1], totalT2 / nbImages));
		}

		xAxis.getCategories().addAll(Arrays.asList(names));
		lineChart.getData().add(series);
		return lineChart;
	}
}
