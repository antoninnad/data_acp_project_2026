package graph;

import abstraction.PCA;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import math.Matrix;
import math.Vector;	

public class QuadraticErrorGraph {
	private PCA pca;
	private int numberOfKeptAxes = pca.getNumberOfKeptAxes();        
	private Matrix eigenFacesMatrix= pca.getEigenfaces();
	private String[] namesOfAxes = new String[numberOfKeptAxes]; 
	private Matrix projectedFacesOnKeptAxes = pca.getProjectedFacesOnKeptAxes() ;
	Matrix projected = pca.getProjectedFacesOnKeptAxes();
	Matrix original = pca.getFacesCoordinates();
	Matrix reconstructed;
	
	/**
	 * 
	 * */
	public QuadraticErrorGraph(PCA pca) {
		this.pca = pca;
	}
	
	
	/**
	 * Generate a graph with the quadratic error 
	 * 
	 * @return graph with the quadratic error 
	 * 
	 * */
	

	public LineChart<String, Number> generateQuadraticErrorGraph() {
		int nbImages = pca.getFacesCoordinates().getNbColumns();

		//definitions of the x-axis 
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Axes");
		
		//definitions of the y-axis 
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Quadratic Error E(J)");
        
        //creation of the graph
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Evolution of the quadratic error depending on axes");
		
        //creation of the data series
        XYChart.Series<String, Number> ErrorSeries = new XYChart.Series<>();
        ErrorSeries.setName("Error");
		
        for (int i = 1; i <= numberOfKeptAxes; i++) {
            String nomAxe = "axe" + (i);
            namesOfAxes[i] = nomAxe;
            
            Matrix eigen_i = eigenFacesMatrix.subMatrixFirstColumns(i);  // [nbPixels × k]
            Matrix proj_i  = projectedFacesOnKeptAxes.getSubRows(0, i - 1);       // [k × nbImages]
            Matrix reconstructed = eigen_i.multiply(proj_i);  
            

        	double total_error =0.0;

        	for (int j = 0; j < nbImages; j++) {
        	    Vector diff = original.getColumn(j).difference(reconstructed.getColumn(j));
        	    total_error += diff.norm();  // ||J - Jp||₂
        	}


        	double meanError = total_error/nbImages;


            //add values to the data series to stock them
            ErrorSeries.getData().add(new XYChart.Data<>(namesOfAxes[i], meanError));
        }

        //add name of axes in order
        xAxis.getCategories().addAll(namesOfAxes);
        
        
        lineChart.getData().add(ErrorSeries);
        
        return lineChart;     
		
	}

	
	
}
