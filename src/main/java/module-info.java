module uabc.fernandez.proyectoia {
    requires javafx.controls;
    requires javafx.fxml;
    requires jade;


    opens uabc.fernandez.proyectoia to javafx.fxml;
    exports uabc.fernandez.proyectoia;
}