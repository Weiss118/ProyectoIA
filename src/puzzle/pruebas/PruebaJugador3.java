package puzzle.pruebas;

import puzzle.busqueda.BusquedaConGenerador;
import puzzle.busqueda.BusquedaConGenerador.ResultadoBusqueda;
import puzzle.busqueda.ConflictoLineal;
import puzzle.busqueda.Heuristica;

/**
 * Prueba por consola (SIN JADE): resuelve UN tablero con la Heurística 3 y muestra el recorrido.
 * Los hijos que en la realidad entrega el Generador aquí los simula TableroPrueba.
 */
public class PruebaJugador3 {

    public static void main(String[] args) {
        //  Tablero a resolver
        //  Opción A: String de 9 caracteres, fila por fila. 0 = espacio vacío.
        //            "182043765" equivale a:  1 8 2 / 0 4 3 / 7 6 5
        String estado = "182043765";

        //  Opción B: vector de 9 enteros (descomenta esta línea; la A se ignora)
        // estado = desdeVector(new int[]{1, 8, 2, 0, 4, 3, 7, 6, 5});

        if (!BusquedaConGenerador.esValido(estado)) {
            System.out.println("Tablero inválido: deben ser 9 dígitos distintos del 0 al 8 -> " + estado);
            return;
        }
        System.out.println("Tablero inicial:");
        TableroPrueba.imprimir(estado);

        if (!BusquedaConGenerador.esResoluble(estado)) {
            System.out.println("\nEste tablero NO tiene solución.");
            return;
        }

        Heuristica h3 = new ConflictoLineal();
        System.out.println("\nh3 inicial = " + h3.calcular(estado));

        // Misma lógica que usa el agente, pero los hijos los da TableroPrueba en vez del Generador
        BusquedaConGenerador motor = new BusquedaConGenerador(h3);
        String pedir = motor.iniciar(estado);
        while (pedir != null) {
            pedir = motor.recibirHijos(TableroPrueba.hijosDe(pedir));
        }
        ResultadoBusqueda r = motor.getResultado();
        System.out.println("\n" + r);
        System.out.println("INFORM -> " + r.aInform() + "   (secuencia;longitud;nodos;tiempoMs)");

        if (!r.exito || r.longitud == 0) return;

        // Mostrar cada paso
        System.out.println("\nRecorrido:");
        String actual = estado;
        for (int i = 0; i < r.secuencia.length(); i++) {
            char mov = r.secuencia.charAt(i);
            actual = TableroPrueba.mover(actual, mov);
            System.out.println("Paso " + (i + 1) + " (" + mov + "):");
            TableroPrueba.imprimir(actual);
        }
        System.out.println(actual.equals(BusquedaConGenerador.META) ? "\nLlegó a la meta." : "\nERROR: no llegó a la meta.");
    }

    // Convierte {1,8,2,0,4,3,7,6,5} en "182043765"
    private static String desdeVector(int[] v) {
        StringBuilder sb = new StringBuilder();
        for (int x : v) sb.append(x);
        return sb.toString();
    }
}
