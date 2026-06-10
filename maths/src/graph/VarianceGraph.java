package graph;

import abstraction.PCA;
import math.Vector;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;


/**
 * Generates a JavaFX line chart showing the cumulative variance explained by
 * the PCA eigenfaces as a function of the number of retained axes.
 *
 * <p>The chart plots two series: the cumulative variance curve (in %) and a
 * horizontal threshold line at the inertia target defined in {@link PCA}
 * (default 85 %). The x-axis lists each axis from "axe1" to "axeN", and the
 * y-axis shows the cumulative percentage of explained variance.</p>
 *
 * @see PCA
 */
public class VarianceGraph {
	private PCA pca;
	private int maxNumberAxes;
	private String[] namesOfAxes;
	private double eigensumThreshold;

	/**
	 * Constructs a VarianceGraph bound to an already-computed PCA model.
	 *
	 * @param pca trained PCA instance used to read eigenvalues and axis count
	 */
	public VarianceGraph(PCA pca) {
		this.pca = pca;
		this.maxNumberAxes = pca.getMaxNumberOfKeptAxes();
		this.namesOfAxes = new String[maxNumberAxes];
		this.eigensumThreshold = PCA.getKeptinertiathreshold() * 100;
	}

	
	/**
	 * Builds and returns a JavaFX line chart with two series: the cumulative variance
	 * curve and the acceptance threshold line.
	 *
	 * @return a {@code LineChart} ready to be embedded in a JavaFX scene
	 */
	public LineChart<String, Number> generateVarianceGraph() {
		Vector eigenvaluesVector = pca.getEigenValues();
		double total = 0;
		for (int i = 0; i < eigenvaluesVector.getDimension(); i++) {
			total += Math.max(0, eigenvaluesVector.get(i));
		}
		double sum = 0.0;
		
		//definitions of the x-axis 
		CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Number of axes");
        
        //definitions of the y-axis 
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Cumulative variance (%)");
        
        //creation of the graph
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Graph of the cumultative variance");
        
        //creation of the data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("cumulative variance");
		
        //calculate the sum and add to the serie
        for (int i = 0; i < maxNumberAxes; i++) {
            String axeName = "axe" + (i + 1);
            namesOfAxes[i] = axeName;
            sum += total > 0 ? Math.max(0, eigenvaluesVector.get(i)) / total * 100 : 0;
            //add values to the data series to stock them
            series.getData().add(new XYChart.Data<>(axeName, sum));
        }
        //add name of axes in order
        xAxis.getCategories().addAll(namesOfAxes);
        
        //creation of the line for the threshold(0.8)
        XYChart.Series<String, Number> thresholdSeries = new XYChart.Series<>();
        thresholdSeries.setName("Acceptance threshold");
        
        // add the threshold to the data series
        for (int i = 0; i < maxNumberAxes; i++) {
            thresholdSeries.getData().add(new XYChart.Data<>(namesOfAxes[i], eigensumThreshold));
        }

        //add the data series to the chart
        lineChart.getData().add(thresholdSeries);     
        lineChart.getData().add(series);
		
        return lineChart;
       
	}	

}
