package graph;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;	

public class QuadraticErrorGraph {

	public LineChart<String, Number> generateQuadraticErrorGraph() {

		//definitions of the x-axis 
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Axes");
		
		//definitions of the y-axis 
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Quadratic Error");
        
        //creation of the graph
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Graph of the quadratic error");
		
        //creation of the data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Error");
		
        
        for (int i = 0; i < pca.numberOfKeptAxes; i++) {
            String nomAxe = "axe" + (i + 1);
            namesOfAxes[i] = nomAxe;
            
            
  
            //matrix
            projectedFaces = eigenfaces.transpose().multiply(facesCoordinates); 

            
            
            
            
            //add values to the data series to stock them
            series.getData().add(new XYChart.Data<>(nomAxe, error));
        }
        
        //add name of axes in order
        xAxis.getCategories().addAll(namesOfAxes);
        
        lineChart.getData().add(series);
        
        return lineChart;     
		
	}
	
	public void CalculateImages() {
		centredImage = .centredVector();
		
		
		
		
	}
	
	
}
