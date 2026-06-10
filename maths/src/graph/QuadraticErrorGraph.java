package graph;

import abstraction.PCA;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import math.Matrix;
import math.Vector;
import java.util.Arrays;

public class QuadraticErrorGraph {
	private PCA pca;
	private int numberOfKeptAxes;
	private Matrix eigenFacesMatrix;
	private String[] namesOfAxes;
	private Matrix projectedFacesOnKeptAxes;
	private Matrix original;

	/**
	 * Generate a graph with the quadratic error
	 */
	public QuadraticErrorGraph(PCA pca) {
		this.pca = pca;
		this.numberOfKeptAxes        = pca.getNumberOfKeptAxes();
		this.eigenFacesMatrix        = pca.getKeptEigenfaces();
		this.namesOfAxes             = new String[numberOfKeptAxes];
		this.projectedFacesOnKeptAxes = pca.getProjectedFacesOnKeptAxes();
		this.original                = pca.getFacesCoordinates();
	}


	/**
	 * Generate a graph with the quadratic error
	 *
	 * @return graph with the quadratic error
	 */
	public LineChart<String, Number> generateQuadraticErrorGraph() {
		int nbImages = original.getNbColumns();

		//definitions of the x-axis
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Axes");

		//definitions of the y-axis
		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Quadratic Error E(J)");

		//creation of the graph
		LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
		lineChart.setTitle("Evolution of the quadratic error depending on axes");
		lineChart.setCreateSymbols(false);

		//creation of the data series
		XYChart.Series<String, Number> ErrorSeries = new XYChart.Series<>();
		ErrorSeries.setName("Error");

		for (int i = 1; i <= numberOfKeptAxes; i++) {
			String nomAxe = "axe" + i;
			namesOfAxes[i - 1] = nomAxe;

			Matrix eigen_i        = eigenFacesMatrix.subMatrixFirstColumns(i - 1); // [nbPixels × i]
			Matrix proj_i         = projectedFacesOnKeptAxes.getSubRows(0, i - 1); // [i × nbImages]
			Matrix reconstructed  = eigen_i.multiply(proj_i);

			double total_error = 0.0;
			for (int j = 0; j < nbImages; j++) {
				Vector diff = original.getColumn(j).difference(reconstructed.getColumn(j));
				total_error += diff.norm();  // ||original - reconstructed||₂
			}

			double meanError = total_error / nbImages;

			//add values to the data series to stock them
			ErrorSeries.getData().add(new XYChart.Data<>(namesOfAxes[i - 1], meanError));
		}

		//add name of axes in order
		xAxis.getCategories().addAll(Arrays.asList(namesOfAxes));

		lineChart.getData().add(ErrorSeries);

		return lineChart;
	}
}
