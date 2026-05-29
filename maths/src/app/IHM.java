import java.util.ArrayList;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane; 
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Slider;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class IHM extends Application{

    TabPane fenetre = new TabPane();

    @Override
    public void start(Stage stage) throws Exception{

        Scene s = new Scene(this.fenetre, 800, 600);
        ImageView viewer1 = new ImageView();
        stage.setScene(s);
        stage.setTitle("Reconnaissance faciale - ACP");

        //first onglet
        Tab tab1 = new Tab("Rechercher");
        tab1.setContent(new Label("Faire une recherche"));
        tab1.setClosable(false);

        Tab tab2 = new Tab("Graphs");
        tab2.setContent(new Label("Graphes de l'ACP"));

        Tab tab3 = new Tab("Resultats");
        tab3.setContent(new Label("Images de la Database"));

        Tab tab4 = new Tab("Parametres");
        tab4.setContent(new Label("Plot axes"));
        
        
        this.fenetre.getTabs().addAll(tab1,tab2,tab3,tab4);
        stage.show();
    }
    
    public static void main(String[] args) {
		System.out.println("Le dossier racine est : " + System.getProperty("user.dir"));
        launch(args);
    }


}

    

