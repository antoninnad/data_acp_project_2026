 package graph;

import abstraction.PCA;
import math.Vector;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;


public class VarianceGraph {
	private PCA pca;
	private int maxNumberAxes;
	private String[] namesOfAxes;
	private double eigensumThreshold;

	public VarianceGraph(PCA pca) {
		this.pca = pca;
		this.maxNumberAxes = pca.getMaxNumberOfKeptAxes();
		this.namesOfAxes = new String[maxNumberAxes];
		this.eigensumThreshold = PCA.getKeptinertiathreshold() * 100;
	}

	
	/**
	 * Generate a graph with the cumulative variance depending on the number of axes 
	 * 
	 * @return graph with cumulative variance and the inertia threshold
	 * 
	 * */
	
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
