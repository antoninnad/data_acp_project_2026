 package graph;

import abstraction.PCA;
import math.Vector;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;


public class VarianceGraph {
	private PCA pca;
	private double sum=0.0;
	private int maxNumberAxes = pca.getMaxNumberOfKeptAxes() ;
	private String[] namesOfAxes = new String[maxNumberAxes]; 
	private double eigensumThreshold = PCA.getKeptinertiathreshold()*100;

	
	public LineChart<String, Number> generateVarianceGraph() {
		Vector eigenvaluesVector = pca.getEigenValues();
		
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
		

        for (int i = 0; i < maxNumberAxes; i++) {
            String axeName = "axe" + (i + 1);
            namesOfAxes[i] = axeName;
            sum+= eigenvaluesVector.get(i)*100;
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
