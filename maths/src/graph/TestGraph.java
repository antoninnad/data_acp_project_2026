package graph;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;	

public class TestGraph extends Application {
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Mon Graphique JavaFX");

		// VarianceGraph nécessite une instance PCA — utiliser new VarianceGraph(pca)
		// VarianceGraph varianceGraph = new VarianceGraph();
		// LineChart<String, Number> monGraphique = varianceGraph.generateVarianceGraph();
	
		
		/*FacesGraph facesGraph = new FacesGraph();
		ScatterChart<Number, Number> monGraphique2 = facesGraph.generateFacesGraph();
		*/

		
		
		
		
		
        // 5. Configuration de la Scène et affichage dans le Stage
        Scene scene = new Scene(new javafx.scene.layout.StackPane(), 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
		
		
	}
	
	
	public static void main(String[] args) {
		launch();		
	}

}
