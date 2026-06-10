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
    
    // Configuration pour le package 'graph'
    exports graph;
    opens graph to javafx.graphics, javafx.fxml;
    
    // Configuration pour le package 'abstraction'
    exports abstraction;
    opens abstraction to javafx.graphics, javafx.fxml;
    
}