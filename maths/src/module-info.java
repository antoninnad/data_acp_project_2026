module maths {
    // Modules requis
    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    
    // Configuration pour le package 'app'
    exports app;
    opens app to javafx.graphics, javafx.fxml;
    
    // Configuration pour le package 'graph' (Ajoute ces deux lignes !)
    exports graph;
    opens graph to javafx.graphics, javafx.fxml;
    
    // Ouvre les autres packages au cas où
    opens abstraction to javafx.fxml;
}