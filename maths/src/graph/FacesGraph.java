package graph;

import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

public class FacesGraph {

	/**
	 * Generate a scatter graph
	 * 
	 * @return a scatter graph with faces(center and reduced) as point and the mean face is in the center(0,0)
	 * */
	
	
	public ScatterChart<Number, Number> generateFacesGraph() {

		double[] eigenvalues = {0.70,0.22,0.04,0.01,0.002};
			
		//initialization of axes
		NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel("Dim 1/Axe 1 (" + eigenvalues[0]*100  +")" );
		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Dim 2/Axe 2 (" + eigenvalues[1]*100  +")" );

		//creation of the faces graph
		ScatterChart<Number, Number> facesGraph = new ScatterChart<>(xAxis, yAxis);
		facesGraph.setTitle("Representation of faces");
		
		//creation of the data series
		XYChart.Series<Number, Number> facesSeries = new XYChart.Series<>();
		facesSeries.setName("Faces");

		
		/* mettre les coordonnées des images centrées et réduites*/
		//TEST DE COORDONNÉES
		facesSeries.getData().add(new XYChart.Data<>(1.5, 2.3));
		facesSeries.getData().add(new XYChart.Data<>(-2.1, 1.0));
		facesSeries.getData().add(new XYChart.Data<>(3.0, -1.5));
		facesSeries.getData().add(new XYChart.Data<>(-1.0, -3.2));
		facesSeries.getData().add(new XYChart.Data<>(0.5, 0.8));
	
		
		facesGraph.getData().add(facesSeries);

		
		return facesGraph;
		
		
		
	}
	
	
	
	
}
