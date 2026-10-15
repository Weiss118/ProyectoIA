package uabc.fernandez.proyectoia;

import jade.core.Agent;

/**
 * Clase padre abstracta que extiende de JADE Agent
 * y establece las tres fases del ciclo de vida del agente.
 */
public abstract class Agente extends Agent {

    @Override
    protected void setup() {
        System.out.println("Inicializando Agente: " + getLocalName());
    }

    // Área de percepción: Recepción de mensajes ACL o lectura de datos
    public abstract void percepcion();

    // Área de razonamiento: Lógica interna, actualización de estado y decisiones
    public abstract void razonamiento();

    // Área de actuar: Actualización visual en JavaFX o envío de mensajes
    public abstract void actuar();
}
