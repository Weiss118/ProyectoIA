package uabc.fernandez.proyectoia;

import javafx.beans.property.SimpleStringProperty;

/**
 * Modelo para las filas de la TableView en la pantalla de juego.
 */
public class MetricaAgente {
    private final SimpleStringProperty agente;
    private final SimpleStringProperty iteraciones;
    private final SimpleStringProperty posicion;

    public MetricaAgente(String agente, String iteraciones, String posicion) {
        this.agente = new SimpleStringProperty(agente);
        this.iteraciones = new SimpleStringProperty(iteraciones);
        this.posicion = new SimpleStringProperty(posicion);
    }

    public String getAgente() { return agente.get(); }
    public String getIteraciones() { return iteraciones.get(); }
    public String getPosicion() { return posicion.get(); }
}
