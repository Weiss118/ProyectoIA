package puzzle.busqueda;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Greedy Best-First Search cuando los hijos de cada nodo los entrega el AGENTE GENERADOR (por mensajes).
 * No usa JADE solo necesita una Heuristica.
 * Aquí viven también el nodo, el resultado y las reglas mínimas del tablero que necesita el jugador.
 *
 * Uso (así lo hace el agente):
 *   String pedir = busqueda.iniciar(estadoInicial);           // nodo cuyos hijos hay que pedir
 *   while (pedir != null) pedir = busqueda.recibirHijos(...); // hijos que respondió el generador
 *   ResultadoBusqueda r = busqueda.getResultado();            // al terminar (pedir == null)
 */
public class BusquedaConGenerador {

    public static final String META = "123456780";
    public static final int LIMITE_NODOS_DEFAULT = 200_000;

    // ------------------------------------------------------------------ tipos auxiliares

    /** Un hijo que entrega el generador. movimiento = '?' si el generador no lo envió. */
    public static class Hijo {
        public final char movimiento;
        public final String estado;

        public Hijo(char movimiento, String estado) {
            this.movimiento = movimiento;
            this.estado = estado;
        }
    }

    /** Resultado de la búsqueda: es lo que el jugador manda en el INFORM (o en el FAILURE). */
    public static class ResultadoBusqueda {
        public static final String FALLA_FORMATO = "estado-invalido";
        public static final String FALLA_NO_RESOLUBLE = "no-resoluble";
        public static final String FALLA_LIMITE = "limite-excedido";
        public static final String FALLA_GENERADOR_SIN_RESPUESTA = "generador-sin-respuesta";
        public static final String FALLA_GENERADOR_ERROR = "generador-error";

        public final boolean exito;
        public final String secuencia;      // movimientos del hueco, ej. "RURDDLURD"
        public final int longitud;
        public final int nodosExpandidos;
        public final long tiempoMs;
        public final String motivoFalla;    // null si hubo éxito

        private ResultadoBusqueda(boolean exito, String secuencia, int nodos, long ms, String motivo) {
            this.exito = exito;
            this.secuencia = secuencia;
            this.longitud = secuencia.length();
            this.nodosExpandidos = nodos;
            this.tiempoMs = ms;
            this.motivoFalla = motivo;
        }

        static ResultadoBusqueda exito(String secuencia, int nodos, long ms) {
            return new ResultadoBusqueda(true, secuencia, nodos, ms, null);
        }

        static ResultadoBusqueda falla(String motivo, int nodos, long ms) {
            return new ResultadoBusqueda(false, "", nodos, ms, motivo);
        }

        /** Formato acordado del INFORM a la GUI: secuencia;longitud;nodos;tiempoMs */
        public String aInform() {
            return secuencia + ";" + longitud + ";" + nodosExpandidos + ";" + tiempoMs;
        }

        @Override
        public String toString() {
            return exito ? "secuencia=" + secuencia + " longitud=" + longitud + " nodos=" + nodosExpandidos + " tiempo=" + tiempoMs + "ms"
                         : "FALLA(" + motivoFalla + ") nodos=" + nodosExpandidos;
        }
    }

    /** Nodo de la búsqueda. Sale primero el de menor h; si empatan, el que entró antes. */
    private static class Nodo implements Comparable<Nodo> {
        final String estado;
        final Nodo padre;
        final char movimiento;
        final int h;
        final long orden;

        Nodo(String estado, Nodo padre, char movimiento, int h, long orden) {
            this.estado = estado;
            this.padre = padre;
            this.movimiento = movimiento;
            this.h = h;
            this.orden = orden;
        }

        @Override
        public int compareTo(Nodo otro) {
            int c = Integer.compare(h, otro.h);
            return c != 0 ? c : Long.compare(orden, otro.orden);
        }

        // Se recorre hacia atrás sacando los movimientos y luego se invierte
        String camino() {
            StringBuilder sb = new StringBuilder();
            for (Nodo n = this; n.padre != null; n = n.padre) sb.append(n.movimiento);
            return sb.reverse().toString();
        }
    }

    // ------------------------------------------------------------------ estado de la búsqueda

    private final Heuristica heuristica;
    private final int limiteNodos;

    private final PriorityQueue<Nodo> frontera = new PriorityQueue<>();  // sale el de menor h
    private final Set<String> visitados = new HashSet<>();               // evita bucles
    private long contador = 0;                                           // desempate (FIFO)
    private int expandidos = 0;
    private long t0;
    private Nodo actual;                                                 // nodo del que se esperan hijos
    private ResultadoBusqueda resultado;

    public BusquedaConGenerador(Heuristica heuristica) {
        this(heuristica, LIMITE_NODOS_DEFAULT);
    }

    public BusquedaConGenerador(Heuristica heuristica, int limiteNodos) {
        this.heuristica = heuristica;
        this.limiteNodos = limiteNodos;
    }

    /** Arranca la búsqueda. Devuelve el estado cuyos hijos hay que pedir, o null si ya terminó. */
    public String iniciar(String inicial) {
        t0 = System.nanoTime();
        if (!esValido(inicial)) {
            return terminar(ResultadoBusqueda.falla(ResultadoBusqueda.FALLA_FORMATO, 0, 0));
        }
        if (!esResoluble(inicial)) {
            return terminar(ResultadoBusqueda.falla(ResultadoBusqueda.FALLA_NO_RESOLUBLE, 0, ms()));
        }
        frontera.add(new Nodo(inicial, null, '-', heuristica.calcular(inicial), contador++));
        visitados.add(inicial);
        return elegirSiguiente();
    }

    /** Recibe los hijos del nodo pedido. Devuelve el siguiente estado a pedir, o null si ya terminó. */
    public String recibirHijos(List<Hijo> hijos) {
        for (Hijo hijo : hijos) {
            if (!esValido(hijo.estado)) continue;                  // ignora basura
            if (!visitados.add(hijo.estado)) continue;             // ya visto -> evita ciclos
            char mov = (hijo.movimiento == '?') ? deducirMovimiento(actual.estado, hijo.estado) : hijo.movimiento;
            frontera.add(new Nodo(hijo.estado, actual, mov, heuristica.calcular(hijo.estado), contador++));
        }
        return elegirSiguiente();
    }

    /** Corta la búsqueda con un motivo (por ejemplo, el generador no respondió). */
    public void abortar(String motivo) {
        terminar(ResultadoBusqueda.falla(motivo, expandidos, ms()));
    }

    // Saca el nodo de menor h de la frontera
    private String elegirSiguiente() {
        if (frontera.isEmpty()) {
            return terminar(ResultadoBusqueda.falla(ResultadoBusqueda.FALLA_NO_RESOLUBLE, expandidos, ms()));
        }
        actual = frontera.poll();
        expandidos++;
        if (actual.estado.equals(META)) {
            return terminar(ResultadoBusqueda.exito(actual.camino(), expandidos, ms()));
        }
        if (expandidos >= limiteNodos) {
            return terminar(ResultadoBusqueda.falla(ResultadoBusqueda.FALLA_LIMITE, expandidos, ms()));
        }
        return actual.estado;   // hay que pedirle al generador los hijos de este nodo
    }

    private String terminar(ResultadoBusqueda r) {
        resultado = r;
        return null;
    }

    private long ms() {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    public ResultadoBusqueda getResultado() { return resultado; }
    public int getHActual()                 { return actual.h; }
    public int getExpandidos()              { return expandidos; }
    public int getTamFrontera()             { return frontera.size(); }

    // ------------------------------------------------------------------ reglas mínimas del tablero

    /** 9 caracteres, permutación de '0'..'8'. */
    public static boolean esValido(String e) {
        if (e == null || e.length() != 9) return false;
        boolean[] usado = new boolean[9];
        for (char c : e.toCharArray()) {
            if (c < '0' || c > '8' || usado[c - '0']) return false;
            usado[c - '0'] = true;
        }
        return true;
    }

    /** Tiene solución si las inversiones (sin contar el 0) son pares. */
    public static boolean esResoluble(String e) {
        int inversiones = 0;
        for (int i = 0; i < 9; i++)
            for (int j = i + 1; j < 9; j++)
                if (e.charAt(i) != '0' && e.charAt(j) != '0' && e.charAt(i) > e.charAt(j)) inversiones++;
        return inversiones % 2 == 0;
    }

    // Si el generador solo manda estados, el movimiento se deduce de cuánto se movió el hueco
    private static char deducirMovimiento(String desde, String hacia) {
        int dif = hacia.indexOf('0') - desde.indexOf('0');
        if (dif == -3) return 'U';
        if (dif == 3)  return 'D';
        if (dif == -1) return 'L';
        return 'R';
    }

    // ------------------------------------------------------------------ formato del mensaje de hijos
    //  Generador -> Jugador:  "U:812403765,D:182743065,R:182403765"   (movimiento:estado, separados por coma)
    //  También se acepta solo estados: "812403765,182743065"

    public static String formatearHijos(List<Hijo> hijos, boolean conMovimiento) {
        StringBuilder sb = new StringBuilder();
        for (Hijo h : hijos) {
            if (sb.length() > 0) sb.append(',');
            sb.append(conMovimiento ? h.movimiento + ":" + h.estado : h.estado);
        }
        return sb.toString();
    }

    public static List<Hijo> parsearHijos(String contenido) {
        List<Hijo> lista = new ArrayList<>();
        if (contenido == null || contenido.trim().isEmpty()) return lista;
        for (String parte : contenido.trim().split(",")) {
            parte = parte.trim();
            if (parte.isEmpty()) continue;
            if (parte.contains(":")) {
                String[] p = parte.split(":", 2);
                if (p[0].trim().length() == 1) lista.add(new Hijo(p[0].trim().charAt(0), p[1].trim()));
            } else {
                lista.add(new Hijo('?', parte));
            }
        }
        return lista;
    }
}
