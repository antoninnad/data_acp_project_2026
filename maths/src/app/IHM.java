package Ihm;
package maths;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.JFileChooser;

import java.util.List;
import java.io.IOException;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane; 
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Insets;
import javafx.scene.control.Slider;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.nio.file.*;
import java.io.File;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;



public class InterfaceGraphique extends Application{
	TabPane fenetre = new TabPane();
    /* 
    private PCA pca;  // Instance PCA pour la reconnaissance faciale
    private Query query;  // Instance Query pour la recherche
    private TextArea resultArea;  // Zone pour afficher les résultats
    private Label statusLabel;  // Label pour afficher l'état
*/
	
	public void creerBandeauHaut(HBox bandeauHaut) {
		
		MenuButton menu = new MenuButton("Menu");
		MenuItem opFile = new MenuItem("Open file");
		MenuItem reloadDB = new MenuItem("Reload DB");
		MenuItem addPicDB = new MenuItem("Add pictore to DB");
		
		menu.getItems().addAll(opFile,reloadDB,addPicDB);
		
		
		bandeauHaut.getChildren().add(menu);
		
		Button help = new Button("Help");
		bandeauHaut.getChildren().add(help);
		
	}
	
	public void creerInputImage(VBox inputImage) {
		Label in = new Label("Input Image");
		ImageView viewer1 = new ImageView();
		Button search = new Button ("Search for face");
		inputImage.getChildren().add(in);
		inputImage.getChildren().add(viewer1);
		inputImage.getChildren().add(search);
		Label saisie = new Label ("Veuillez saisir une image");
		Button parcourir = new Button("Parcourir...");
		
		
		inputImage.getChildren().add(saisie);
		inputImage.getChildren().add(parcourir);
		
		viewer1.setFitWidth(450);
		viewer1.setFitHeight(250);
		
		
		parcourir.setOnAction(e->{
			FileChooser chooser = new FileChooser();
			chooser.setInitialDirectory(new File(System.getProperty("user.home")));
			chooser.setTitle("Saisir une image");
			chooser.getExtensionFilters().add(
					new FileChooser.ExtensionFilter("Images", "*.jpg"));
			
			
			File fichier= chooser.showOpenDialog(null);
			
			if(fichier!=null) {
				Image image = new Image(fichier.toURI().toString());
				viewer1.setImage(image);
			}
			
		});
	
	}
	
	
	public void creerDBimage(VBox DBimage) {
		Label db= new Label("DB image");
		Label resultat= new Label("Nom : ");
		ImageView viewer1 = new ImageView();
		DBimage.getChildren().add(db);
		DBimage.getChildren().add(viewer1);
		DBimage.getChildren().add(resultat);
	}
	
	public void creerGraphe(TabPane graph) {
		Tab tab1 = new Tab("Projections");
		tab1.setClosable(false);
		
		Tab tab2 = new Tab("Cumulated Eigenvalues");
		tab2.setClosable(false);
		
		Tab tab3 = new Tab("Average faces");
		tab3.setClosable(false);
		
		Tab tab4 = new Tab("Eigenfaces");
		tab4.setClosable(false);
		
		graph.getTabs().addAll(tab1,tab2,tab3,tab4);
		
	}
	
	public void creerParametre(VBox param) {
		HBox plotmain = new HBox();
		Label plotaxes = new Label("Plot axes : ");
		Button b1 = new Button("1");
		Button b2 = new Button("2");
		plotmain.getChildren().add(plotaxes);
		plotmain.getChildren().add(b1);
		plotmain.getChildren().add(b2);
		Label axesconsidered = new Label("Number of axes considered : "+numero());
		Label cumulvar = new Label("Cumulative variance : "+numero());
		Label errRate = new Label("Error rate : "+numero()+"%");
		
		param.getChildren().add(plotmain);
		param.getChildren().add(axesconsidered);
		param.getChildren().add(cumulvar);
		param.getChildren().add(errRate);
	}
	
	public double numero() {
		double x = Math.random();
		return x*100;		
	}
	
	public void creerCentre(GridPane centre) {
		VBox inputImage = new VBox();
		VBox DBimage = new VBox();
		TabPane graph = new TabPane();
		VBox param = new VBox();
		
		centre.add(inputImage, 0, 0);
		centre.add(DBimage, 0, 1);
		centre.add(graph, 1, 0);
		centre.add(param, 1, 1);
		creerInputImage(inputImage);
		creerDBimage(DBimage);
		creerGraphe(graph);
		creerParametre(param);
		// Contraintes colonnes
		ColumnConstraints col = new ColumnConstraints();
		col.setPercentWidth(50);
		centre.getColumnConstraints().addAll(col, col); // ⚠️ crée 2 objets distincts

		// Contraintes lignes
		RowConstraints row = new RowConstraints();
		row.setPercentHeight(50);
		centre.getRowConstraints().addAll(row, row); // ⚠️ crée 2 objets distincts
	
		for (Region element : new Region[]{inputImage, DBimage, graph, param}) {
		    GridPane.setHgrow(element, Priority.ALWAYS);
		    GridPane.setVgrow(element, Priority.ALWAYS);
		    element.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		}

	}
	
	
    @Override
    public void start(Stage stage) throws Exception{

        Scene s = new Scene(this.fenetre, 1000, 700);
        stage.setScene(s);
        
        stage.setTitle("Reconnaissance faciale - ACP");
       
        BorderPane tabPrincipal = new BorderPane();
        tabPrincipal.setPadding(new Insets(10));
        HBox bandeauHaut = new HBox();
        GridPane centre = new GridPane();
        creerCentre(centre);
        creerBandeauHaut(bandeauHaut);
        tabPrincipal.setCenter(centre);
        tabPrincipal.setTop(bandeauHaut);
        
        Tab tab = new Tab("Principal", tabPrincipal);
        tab.setClosable(false);
        this.fenetre.getTabs().add(tab);
        stage.show();
    }

    public static void main(String[] args) {
        System.out.println("Le dossier racine est : " + System.getProperty("user.dir"));
        launch(args);
    }
}

