package puzzle.comun;

/**
 * Utilidades sobre el estado del 8-Puzzle.
 * El estado es un String de 9 caracteres leído por filas; '0' es el hueco.
 * Los movimientos U, D, L, R indican hacia dónde se mueve el HUECO.
 */
public final class Tablero {

    public static final String META = "123456780";

    /** Orden fijo de generación: up, down, left, right */
    private static final char[] MOVIMIENTOS = {'U', 'D', 'L', 'R'};

    private Tablero() {}

    public static boolean esValido(String estado) {
        if (estado == null || estado.length() != 9) return false;
        boolean[] visto = new boolean[9];
        for (char c : estado.toCharArray()) {
            int d = c - '0';
            if (d < 0 || d > 8 || visto[d]) return false;
            visto[d] = true;
        }
        return true;
    }

    public static int inversiones(String estado) {
        int inv = 0;
        for (int i = 0; i < 9; i++) {
            char a = estado.charAt(i);
            if (a == '0') continue;
            for (int j = i + 1; j < 9; j++) {
                char b = estado.charAt(j);
                if (b != '0' && a > b) inv++;
            }
        }
        return inv;
    }

    public static boolean esResoluble(String estado) {
        return inversiones(estado) % 2 == 0;
    }

    public static String mover(String estado, char mov) {
        int hueco = estado.indexOf('0');
        int fila = hueco / 3, col = hueco % 3;
        int destino;
        switch (mov) {
            case 'U': if (fila == 0) return null; destino = hueco - 3; break;
            case 'D': if (fila == 2) return null; destino = hueco + 3; break;
            case 'L': if (col == 0)  return null; destino = hueco - 1; break;
            case 'R': if (col == 2)  return null; destino = hueco + 1; break;
            default: throw new IllegalArgumentException("Movimiento inválido: " + mov);
        }
        char[] c = estado.toCharArray();
        c[hueco] = c[destino];
        c[destino] = '0';
        return new String(c);
    }

    /** U, D, L, R. Según dónde esté el hueco, 2, 3 o 4 de ellos son válidos. */
    public static char[] movimientos() {
        return MOVIMIENTOS.clone();
    }


}