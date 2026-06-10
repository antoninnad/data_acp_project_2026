module maths {
    // Required modules
    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    
    // Configuration for the package 'app'
    exports app;
    opens app to javafx.graphics, javafx.fxml;
    
    // Configuration for the package 'graph'
    exports graph;
    opens graph to javafx.graphics, javafx.fxml;
    
    // Configuration for the package 'abstraction'
    exports abstraction;
    opens abstraction to javafx.graphics, javafx.fxml;
    
}