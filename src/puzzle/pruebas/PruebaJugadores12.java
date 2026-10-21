package puzzle.pruebas;

import puzzle.busqueda.BusquedaConGenerador;
import puzzle.busqueda.BusquedaConGenerador.ResultadoBusqueda;
import puzzle.busqueda.ConflictoLineal;
import puzzle.busqueda.Hamming;
import puzzle.busqueda.Heuristica;
import puzzle.busqueda.Manhattan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Prueba por consola (SIN JADE) de los Jugadores 1 y 2, con el mismo motor que usa el agente.
 * Los hijos que en la realidad entrega el Generador aquí los simula TableroPrueba.
 *  1) Valores conocidos de Hamming y Manhattan.
 *  2) Un tablero resuelto con cada heurística (incluye la 3 para comparar).
 *  3) 200 tableros aleatorios: verifica que todas las soluciones lleguen a la meta y compara promedios.
 */
public class PruebaJugadores12 {

    public static void main(String[] args) {
        String estado = args.length > 0 ? args[0] : "182043765";
        Heuristica[] heuristicas = {new Hamming(), new Manhattan(), new ConflictoLineal()};

        // 1) Valores conocidos
        System.out.println("=== Valores conocidos ===");
        verificar("Hamming(182043765)", new Hamming().calcular("182043765"), 6);
        verificar("Manhattan(182043765)", new Manhattan().calcular("182043765"), 9);
        verificar("Hamming(meta)", new Hamming().calcular(BusquedaConGenerador.META), 0);
        verificar("Manhattan(meta)", new Manhattan().calcular(BusquedaConGenerador.META), 0);
        verificar("Manhattan(123456708)", new Manhattan().calcular("123456708"), 1);

        // 2) Un tablero con cada heurística
        System.out.println("\n=== Tablero " + estado + " ===");
        TableroPrueba.imprimir(estado);
        for (Heuristica h : heuristicas) {
            ResultadoBusqueda r = resolver(h, estado);
            System.out.printf("%-17s h0=%-3d %s%n", h.nombre(), h.calcular(estado), r);
            if (r.exito) System.out.println("                  INFORM -> " + r.aInform());
        }

        // 3) Comparación con tableros aleatorios
        int n = 200;
        Random rnd = new Random(42);
        List<String> tableros = new ArrayList<>();
        while (tableros.size() < n) {
            String s = aleatorio(rnd);
            if (BusquedaConGenerador.esResoluble(s)) tableros.add(s);
        }
        System.out.println("\n=== " + n + " tableros aleatorios resolubles ===");
        for (Heuristica h : heuristicas) {
            long sumaLong = 0, sumaNodos = 0;
            int errores = 0;
            for (String s : tableros) {
                ResultadoBusqueda r = resolver(h, s);
                if (!r.exito || !BusquedaConGenerador.META.equals(aplicar(s, r.secuencia))) errores++;
                sumaLong += r.longitud;
                sumaNodos += r.nodosExpandidos;
            }
            System.out.printf("%-17s longitud prom = %5.1f   nodos prom = %7.1f   %s%n",
                    h.nombre(), (double) sumaLong / n, (double) sumaNodos / n,
                    errores == 0 ? "OK" : "ERRORES: " + errores);
        }
    }

    /** Igual que el agente, pero los hijos los da TableroPrueba en lugar del Generador. */
    private static ResultadoBusqueda resolver(Heuristica h, String estado) {
        BusquedaConGenerador motor = new BusquedaConGenerador(h);
        String pedir = motor.iniciar(estado);
        while (pedir != null) {
            pedir = motor.recibirHijos(TableroPrueba.hijosDe(pedir));
        }
        return motor.getResultado();
    }

    private static String aplicar(String estado, String secuencia) {
        String actual = estado;
        for (char m : secuencia.toCharArray()) {
            if (actual == null) return null;
            actual = TableroPrueba.mover(actual, m);
        }
        return actual;
    }

    private static void verificar(String nombre, int obtenido, int esperado) {
        System.out.println((obtenido == esperado ? "OK    " : "ERROR ") + nombre + " = " + obtenido
                + (obtenido == esperado ? "" : " (esperado " + esperado + ")"));
    }

    private static String aleatorio(Random rnd) {
        List<Character> l = new ArrayList<>();
        for (char c : "012345678".toCharArray()) l.add(c);
        Collections.shuffle(l, rnd);
        StringBuilder sb = new StringBuilder();
        for (char c : l) sb.append(c);
        return sb.toString();
    }
}
