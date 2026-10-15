package uabc.fernandez.proyectoia;

import jade.core.behaviours.CyclicBehaviour;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgenteGUI extends Agente {

    private GUIApp guiApp;
    private String estadoTableroInicial;
    private String[] estadosJugadores;
    private int jugadorActual = 0;

    // Constructor: Inicializa las cadenas antes de que JADE ejecute setup()
    public AgenteGUI() {
        this.estadoTableroInicial = generarEstadoAleatorio();
        this.estadosJugadores = new String[]{
                generarEstadoAleatorio(),
                generarEstadoAleatorio(),
                generarEstadoAleatorio()
        };
    }

    @Override
    protected void setup() {
        super.setup();

        // Comportamiento cíclico de JADE
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                percepcion();
                razonamiento();
                actuar();
                block(1000);
            }
        });
    }

    public void setGuiApp(GUIApp app) {
        this.guiApp = app;
    }

    @Override
    public void percepcion() { }

    @Override
    public void razonamiento() { }

    @Override
    public void actuar() {
        if (guiApp != null) {
            Platform.runLater(() -> {
                // Actualizaciones de UI si son necesarias
            });
        }
    }

    // Generador de cadenas de 9 dígitos (del '0' al '8')
    public String generarEstadoAleatorio() {
        List<Character> fichas = new ArrayList<>();
        for (char c = '0'; c <= '8'; c++) {
            fichas.add(c);
        }
        Collections.shuffle(fichas);
        StringBuilder sb = new StringBuilder();
        for (char c : fichas) {
            sb.append(c);
        }
        return sb.toString();
    }

    // Getters y Setters
    public String getEstadoTableroInicial() {
        if (estadoTableroInicial == null) {
            estadoTableroInicial = generarEstadoAleatorio();
        }
        return estadoTableroInicial;
    }

    public void setEstadoTableroInicial(String nuevo) {
        this.estadoTableroInicial = nuevo;
    }

    public String[] getEstadosJugadores() {
        return estadosJugadores;
    }

    public int getJugadorActual() {
        return jugadorActual;
    }

    public void setJugadorActual(int idx) {
        this.jugadorActual = idx;
    }
}
