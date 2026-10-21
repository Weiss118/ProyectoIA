module uabc.fernandez.proyectoia {
    requires javafx.controls;
    requires javafx.fxml;
    requires jade;
    requires java.desktop;


    opens uabc.fernandez.proyectoia to javafx.fxml;
    exports uabc.fernandez.proyectoia;
}