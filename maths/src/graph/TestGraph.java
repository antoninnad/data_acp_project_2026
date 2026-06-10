package graph;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;

/**
 * Standalone JavaFX application used to test and visualise graph classes in isolation.
 *
 * <p>Graph classes ({@link VarianceGraph}, {@link QuadraticErrorGraph}) require a live
 * {@link abstraction.PCA} instance and cannot be instantiated without one. The actual
 * test calls are therefore commented out and replaced with an empty scene.</p>
 *
 * @see VarianceGraph
 * @see QuadraticErrorGraph
 */
public class TestGraph extends Application {

	/**
	 * JavaFX entry point. Displays an empty window; graph test code can be
	 * uncommented and a PCA instance supplied to test chart rendering.
	 *
	 * @param primaryStage the primary window provided by the JavaFX runtime
	 * @throws Exception if the scene cannot be built
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Mon Graphique JavaFX");

		// VarianceGraph nécessite une instance PCA — utiliser new VarianceGraph(pca)
		// VarianceGraph varianceGraph = new VarianceGraph();
		// LineChart<String, Number> monGraphique = varianceGraph.generateVarianceGraph();
	
		

		
		// QuadraticErrorGraph nécessite une instance PCA — utiliser new QuadraticErrorGraph(pca)
		// QuadraticErrorGraph errorGraph = new QuadraticErrorGraph();
		// LineChart<String, Number> monGraphique2 = errorGraph.generateQuadraticErrorGraph();
		
		
		
        // 5. Configuration de la Scène et affichage dans le Stage
        Scene scene = new Scene(new javafx.scene.layout.StackPane(), 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
		
		
	}
	
	
	public static void main(String[] args) {
		launch();		
	}

}
