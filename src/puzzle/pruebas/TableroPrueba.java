package puzzle.pruebas;

import puzzle.busqueda.BusquedaConGenerador.Hijo;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas del tablero SOLO PARA PRUEBAS: simulan lo que hará el Generador real (mover el hueco y listar hijos).
 * El jugador (Agentejugador, BusquedaConGenerador) no depende de esta clase.
 */
public class TableroPrueba {

    /** Mueve el hueco (0) hacia U, D, L o R. Devuelve null si el movimiento no es posible. */
    public static String mover(String estado, char mov) {
        int z = estado.indexOf('0');
        int destino;
        if (mov == 'U' && z >= 3)         destino = z - 3;
        else if (mov == 'D' && z < 6)     destino = z + 3;
        else if (mov == 'L' && z % 3 > 0) destino = z - 1;
        else if (mov == 'R' && z % 3 < 2) destino = z + 1;
        else return null;
        char[] c = estado.toCharArray();
        c[z] = c[destino];
        c[destino] = '0';
        return new String(c);
    }

    /** Hijos de un estado, como los entregaría el Generador (orden U, D, L, R). */
    public static List<Hijo> hijosDe(String estado) {
        List<Hijo> lista = new ArrayList<>();
        for (char m : new char[]{'U', 'D', 'L', 'R'}) {
            String siguiente = mover(estado, m);
            if (siguiente != null) lista.add(new Hijo(m, siguiente));
        }
        return lista;
    }

    /** Dibuja el tablero en 3 filas. */
    public static void imprimir(String e) {
        for (int fila = 0; fila < 3; fila++) {
            System.out.println("  " + e.charAt(fila * 3) + " " + e.charAt(fila * 3 + 1) + " " + e.charAt(fila * 3 + 2));
        }
    }
}
