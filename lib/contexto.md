# Contexto: 8-Puzzle SMA en JADE — Jugadores 1 y 2 (Integrante 2)

Documento para darle contexto a un asistente de IA. Objetivo inmediato: **tener los agentes Jugador 1 (Hamming) y Jugador 2 (Manhattan) funcionando en JADE**, integrados con el diseño que ya implementó el Integrante 3 (Jugador 3). Al final está el código fuente completo.

## 1. Proyecto

Proyecto de equipo (4 integrantes) para Inteligencia Artificial. Sistema multiagente en **JADE 4.x** (Java, `lib/jade.jar`, proyecto de IntelliJ) que resuelve el **8-Puzzle** con **Greedy Best-First Search** (f(n) = h(n)) usando tres heurísticas, y compara su desempeño.

| Integrante | Responsabilidad |
|---|---|
| 1 | Agente Generador (entrega los hijos de cada estado) |
| **2 (yo)** | Heurísticas Hamming y Manhattan; Agentes Jugador 1 y 2 |
| 3 | Heurística conflicto lineal, Agente Jugador 3, clase base `Agentejugador`, motor `BusquedaConGenerador`, mocks y lanzador `PruebaJade` |
| 4 | Agente de Despliegue (GUI) |

Requisito del avance: demo de los agentes jugadores aplicando el algoritmo de forma eficiente, simulando el intercambio de mensajes con los demás agentes (se permiten mocks).

## 2. Convenciones (NO cambiar sin acuerdo del equipo)

- Estado: `String` de 9 caracteres por filas, `'0'` = hueco. Meta: `"123456780"`.
- Movimientos `U D L R` = hacia dónde se mueve el **hueco**. Orden de hijos: U, D, L, R.
- Resoluble ⇔ número de inversiones (sin contar el 0) par.
- Greedy: `PriorityQueue` por h; empate → el que entró primero (contador `orden`). `HashSet` de visitados marcado al generar.
- Límite: 200,000 nodos expandidos.
- Paquetes: `puzzle.busqueda` (motor y heurísticas, sin JADE), `puzzle.agentes` (agentes), `puzzle.pruebas` (mocks, lanzadores, pruebas sin JADE). La interfaz `Heuristica` está en `puzzle.busqueda` (no en un subpaquete).

## 3. Protocolo de mensajes (FIPA-ACL)

```
Despliegue ──REQUEST content="182043765"──────────────▶ JugadorN
  repetir:   JugadorN ──REQUEST content=<estado elegido>, conversation-id=jugadorN-k──▶ generador
             JugadorN ◀──INFORM content="U:812403765,D:182743065,R:182403765"────────── generador
             (también acepta solo estados: "812403765,182743065"; el movimiento se deduce)
JugadorN ──INFORM content="secuencia;longitud;nodos;tiempoMs"──▶ Despliegue   p. ej. "RURDDLURD;9;11;2"
JugadorN ──FAILURE content=<motivo>──▶ Despliegue
   motivos: estado-invalido | no-resoluble | limite-excedido | generador-sin-respuesta | generador-error
```

- El jugador, cuando está libre, toma **cualquier** REQUEST como reto nuevo (no filtra ontología ni conversation-id).
- Mientras busca, solo acepta respuestas con su conversation-id o que vengan del agente `generador` y no sean REQUEST.
- Timeout de respuesta del generador: 10 s (`-DtimeoutGeneradorMs=...`). Nombre del generador: `-Dgenerador=...` (default `generador`).
- Pendiente con el equipo: si la GUI necesita también la lista de estados del camino (formato de 5 campos `secuencia;longitud;nodos;tiempoMs;estados`). Hoy el INFORM tiene 4 campos.

## 4. Arquitectura del código

- **`Agentejugador`** (abstracta, `puzzle.agentes`): un solo `CyclicBehaviour` con ciclo PERCIBIR → RAZONAR → ACTUAR. Maneja el reto, pide hijos al generador uno por uno, aplica timeout con `block(ms)`, responde a quien mandó el reto con `createReply()`, y limpia mensajes tardíos. Cada jugador concreto solo implementa `protected Heuristica crearHeuristica()` (patrón Template Method).
- **`BusquedaConGenerador`** (`puzzle.busqueda`, sin JADE): Greedy incremental. `iniciar(estado)` → estado cuyos hijos pedir (o null si terminó); `recibirHijos(lista)` → siguiente estado a pedir (o null); `getResultado()` → `ResultadoBusqueda` con `aInform()`. Incluye `Nodo`, `Hijo`, `ResultadoBusqueda`, validación, resolubilidad y `parsearHijos/formatearHijos`.
- **Heurísticas** (`implements Heuristica { int calcular(String); String nombre(); }`): `Hamming`, `Manhattan`, `ConflictoLineal`.
- **Jugadores**: `AgenteJugador1` (Hamming), `AgenteJugador2` (Manhattan), `AgenteJugador3` (ConflictoLineal), todos `extends Agentejugador`.
- **`VentanaJugador`** (`puzzle.gui`): visor Swing paso a paso de UN jugador. No manda ni recibe mensajes; es independiente de la GUI del equipo (Integrante 4). Hoy solo lo usa `AgenteJugador1`, vía el gancho `alTerminar()`. Se apaga con `-DsinVentana=true`.
- **Mocks** (`puzzle.pruebas`): `AgenteGeneradorMock` (responde hijos usando `TableroPrueba`; opciones `-DretardoMs`, `-DsinMovimiento=true`, `-DsinConversacion=true`) y `AgenteDespliegueMock` (manda el reto al agente indicado por `-Djugador=...`, default `jugador3`, y verifica que la secuencia llegue a la meta; tablero por argumento o `-Destado=...`).
- **`PruebaJade`**: lanzador. Argumentos `<modo> [agentes...]`; modo `local` (todo en una PC), `principal`, o la IP del contenedor principal. Opciones `-Destado`, `-DmiIp`, `-DsinGui=true`, `-DpuertoLocal`.
- **Pruebas sin JADE**: `PruebaJugador3` y `PruebaJugadores12` (valores conocidos + 200 tableros, mismo motor que el agente).

## 5. Estado actual y lo que falta para correr con JADE

Hecho y verificado (sin JADE): Hamming(182043765)=6, Manhattan(182043765)=9, ambas 0 en la meta. Con el motor del equipo, 200 tableros aleatorios (semilla 42), todos llegan a la meta:

| Heurística | longitud prom | nodos prom |
|---|---|---|
| Hamming | 48.3 | 725.9 |
| Manhattan | 42.3 | 282.0 |
| Conflicto lineal | 31.5 | 66.4 |

Tablero `182043765`: las tres dan `RURDDLURD` (9 movimientos; 11, 10 y 10 nodos). Tablero `867254301`: Hamming 63 mov / 337 nodos; Manhattan 47 / 100.

**Hecho (21-oct-2026), ya corriendo sobre JADE 4.6.0 + JDK 17:**

1. El código del Integrante 3 no estaba en el repo (ni en `main` ni en `origin/Integrante2`): solo vivía en este documento. Se materializó tal cual desde la sección 9: `BusquedaConGenerador`, `ConflictoLineal`, el `Agentejugador` completo (en el repo había un stub de 4 líneas), `AgenteJugador3`, `TableroPrueba`, `AgenteGeneradorMock`, `AgenteDespliegueMock`, `PruebaJade`. `ConflictoLineal` quedó en la versión original del Integrante 3 (sin aplicar la corrección de la sección 9).
2. `PruebaJugadores12.java` estaba en `src/puzzle/` declarando `package puzzle.pruebas`. Se movió a `src/puzzle/pruebas/`.
3. `PruebaJade` ahora crea cualquier combinación de `jugador1 jugador2 jugador3 generador despliegue`, más el atajo `jugadores` (los tres). El orden de arranque lo fija la tabla `CATALOGO`: jugadores → generador → despliegue.
4. `AgenteDespliegueMock` acepta `-Djugador=jugador1,jugador2,jugador3`: manda un solo REQUEST con los tres receptores y al final imprime una tabla comparativa (longitud, nodos, ms, secuencia). El default sigue siendo `jugador3`.
5. Se agregaron tres run configurations de IntelliJ: *Local 3 jugadores*, *Principal (mis jugadores)*, *Unirme al principal*.

**Verificado en ejecución:**

| Prueba | Resultado |
|---|---|
| `PruebaJugadores12` (sin JADE) | reproduce exactamente la tabla de arriba |
| `local jugadores generador despliegue` | los 3 responden `RURDDLURD`; 11 / 10 / 10 nodos |
| 2 contenedores en distinto puerto, `867254301` | jugador1 63 mov/337 nodos, jugador2 47/100 (coincide con lo esperado) |
| tablero no resoluble `213456780` | `FAILURE no-resoluble` |
| generador con `-DretardoMs=6000` y `-DtimeoutGeneradorMs=2000` | `FAILURE generador-sin-respuesta` |
| sin agente generador en la plataforma | `FAILURE generador-error` (el AMS avisa de la entrega fallida con el mismo conversation-id, así que no espera el timeout) |
| generador con `-DsinMovimiento=true -DsinConversacion=true` | INFORM correcto: el jugador deduce el movimiento y reconoce la respuesta por el remitente |

**Falta:**
1. **Bloqueante — el Generador real responde INFORM con content vacío.** Diagnosticado el 21-oct-2026 con `SondaGenerador` contra la plataforma del Integrante 1 (`10.21.59.160`). Detalle y qué pedirle en la sección 8.
2. Verificar con el Sniffer de JADE (RMA → Tools → Start Sniffer) los REQUEST/INFORM entre despliegue, jugador y generador.
3. Cerrar con el equipo si el INFORM lleva 4 o 5 campos (ver sección 3).
4. El agente de la GUI del equipo se llama `agenteGUI`, no `despliegue`. No afecta al jugador (contesta con `createReply()` a quien lo retó), pero conviene anotarlo.

**Agregado el 21-oct-2026 — ventana paso a paso del Jugador 1:**

- `BusquedaConGenerador` ahora guarda los **estados** del camino, no solo los movimientos: `Nodo.caminoEstados()` y el campo `ResultadoBusqueda.estados` (longitud+1 elementos; lista vacía, nunca null, si falló). Verificado en 60 tableros: cada estado se deriva del anterior aplicando el movimiento correspondiente, empieza en el inicial y acaba en la meta.
- `aInform()` **no cambió**: sigue siendo de 4 campos. Se añadió `aInformConEstados()` (5 campos) listo para cuando el equipo decida, pero nadie lo llama todavía.
- `Agentejugador` tiene un gancho nuevo `protected void alTerminar(ResultadoBusqueda)`, vacío por defecto, que se llama **después** de enviar la respuesta y de quedar libre, envuelto en try/catch. Jugador 2 y Jugador 3 no cambian de comportamiento.
- `AgenteJugador1` lo sobreescribe para abrir `VentanaJugador`. Si la búsqueda falla no abre nada.
- Dos bugs de layout corregidos en la ventana al probarla: el marco salía de 506×241 en vez de 440×395 porque el gestor de ventanas imponía su geometría, y el tablero se quedaba con casillas de 12 px. Se arregló con `setMinimumSize` en el marco tras `pack()` y cambiando el panel inferior de `GridLayout` (filas iguales, acaparaba 130 px) a `BoxLayout`.

## 6. Problemas conocidos

- **`ConflictoLineal` del Integrante 3 no es admisible**: suma +2 por cada PAR invertido en una línea; sobreestima en 7 de 181,440 estados (p. ej. `087654321`: h=32, óptimo real 28). Lo correcto es +2 por cada ficha que hay que sacar de la línea = 2·(n − subsecuencia creciente más larga). Con eso sobreestima en 0. La fila `3 1 2` suma +2, no +4. Hay una versión corregida propuesta (sección 9), pero el archivo es del Integrante 3: no cambiarlo sin avisar.
- Eficiencia: cada nodo expandido es un viaje de mensajes al generador (~726 por reto con Hamming).
- Código paralelo que no usan los agentes (implementación propia anterior del Integrante 2, compila pero nadie la llama): `busqueda/BusquedaGreedy.java`, `busqueda/Nodo.java`, `comun/Tablero.java`, `comun/Resultado.java`, `pruebas/mayra.java`. `gui/Prueba.java` sí está vacío. Decidir con el equipo si se borran o se quedan como respaldo.
- `Agentejugador` no limpia los INFORM que llegan tarde después de un timeout: el `while (receive(MatchConversationId(...)))` corre antes de que lleguen. Quedan en la cola del agente sin estorbar (estando libre solo lee REQUEST), pero se acumulan.

## 7. Notas de JADE para no inventar API

- Agentes: `extends jade.core.Agent`, lógica en `setup()`, limpieza en `takeDown()`. Nombres locales con `new AID(nombre, AID.ISLOCALNAME)`.
- Comportamientos: `CyclicBehaviour` (se repite), `OneShotBehaviour` (una vez), `WakerBehaviour`, `TickerBehaviour`. Dentro de `action()`: `ACLMessage m = myAgent.receive(plantilla); if (m == null) { block(); return; }`. `block(ms)` despierta al llegar un mensaje o al vencer el tiempo.
- Plantillas: `MessageTemplate.MatchPerformative`, `MatchConversationId`, `MatchSender`, `and/or/not`, o `new MessageTemplate(m -> ...)` (MatchExpression).
- Respuestas: `msg.createReply()` copia receptor (el remitente), conversation-id, ontología y pone in-reply-to.
- Arranque programático: `Runtime.instance()`, `new ProfileImpl()`, `Profile.GUI="true"` (abre RMA), `createMainContainer` / `createAgentContainer(new ProfileImpl(ip, 1099, null))`, `contenedor.createNewAgent(nombre, "paquete.Clase", args).start()`.
- Error típico: puerto 1099 ocupado si quedó otra plataforma abierta.
- No llamar a Swing desde el hilo del agente sin `SwingUtilities.invokeLater`.

## 8. Correr la plataforma entre varias computadoras

### Cómo funciona JADE en red

Una sola **plataforma** repartida en varios **contenedores**. UNA computadora corre el *contenedor principal* (tiene el AMS y el DF, puerto 1099); las demás corren contenedores que se unen a esa IP. Con eso `new AID("generador", AID.ISLOCALNAME)` sigue funcionando aunque el generador viva en otra PC: lo resuelve el AMS. **No hay que cambiar nada del código de los agentes para pasar de local a distribuido.**

Si el contenedor principal se cae, se cae la plataforma: conviene que lo corra quien tenga la conexión más estable (o quien tenga la GUI).

### El problema real: JADE anuncia la IP equivocada

Al correr en modo `local` en esta máquina, JADE eligió sola la interfaz de Docker:

```
INFO: Listening for intra-platform commands on address:
- jicp://172.17.0.1:1099          <-- docker0, inalcanzable desde otra PC
```

Esta máquina tiene cuatro interfaces (`lo`, `wlp0s20f3 10.21.22.52`, `tailscale0`, `docker0 172.17.0.1`) y JADE no adivina cuál sirve: hay que decírselo con **`-DmiIp=<la IP buena>`**, que `PruebaJade` pasa a `Profile.LOCAL_HOST`. La IP buena es la del WiFi. Con el `-DmiIp` correcto el log pasa a `jicp://10.21.22.52:1099` y ahí sí se conectan los demás.

Esto hay que hacerlo en **todas** las PCs, no solo en la del principal: si un contenedor se anuncia como `172.17.0.1`, se une bien pero nadie puede contestarle y los mensajes se pierden sin error claro.

### Decisión del equipo: IP de LAN directa, sin VPN

**Se conecta por IP de la red local (WiFi de la escuela). Nada de VPN.** Las 4 PCs tienen que estar en la misma WiFi y se usan sus IPs `10.21.x.x` tal cual.

Tailscale se descartó: está instalado en esta máquina (`100.91.238.65`) pero en la red de la escuela aparece `offline`, y `tailscale status` reporta que **un equipo Fortinet bloquea el tráfico de Tailscale**. Encima, en el tailnet no hay compañeros, solo dispositivos propios. Si algún día tienen que trabajar desde casas distintas, habría que volver a este punto: por IP de LAN eso no funciona (harían falta IPs públicas y port forwarding).

Consecuencias de usar la IP del WiFi:

- **Las IPs son de DHCP y pueden cambiar.** Cada vez que se reconecten, verificar con `ip -4 -o addr show` y volver a pasar el `-DmiIp` correcto. Por eso las run configurations traen `-DmiIp=CAMBIA_POR_MI_IP` como recordatorio en lugar de una IP fija.
- Si la red tiene *client isolation*, los equipos no se ven entre sí y JADE no puede hacer nada. Comprobarlo ANTES de culpar al código (ver checklist).
- Esta red es `10.21.0.0/18`, o sea `10.21.0.1`–`10.21.63.254`. Si la IP de un compañero no está en ese rango, están en redes distintas y no se van a ver.

### Firewall (Fedora, firewalld activo en esta máquina)

El contenedor principal necesita el **1099** abierto, y cada contenedor su puerto JICP. En esta máquina **ya está cubierto**: la zona `FedoraWorkstation` (la de `wlp0s20f3`) tiene abierto `1025-65535/tcp`, así que el 1099 pasa sin tocar nada.

Si en otra PC del equipo el firewall sí estorba:

```bash
sudo firewall-cmd --permanent --add-port=1099/tcp    # contenedor principal y contenedores que se unen
sudo firewall-cmd --permanent --add-port=1100/tcp    # solo si corren 2 contenedores en la MISMA PC
sudo firewall-cmd --permanent --add-port=7778/tcp    # MTP HTTP (solo si se habla con OTRA plataforma)
sudo firewall-cmd --reload
```

En Windows, la primera vez salta el diálogo de Windows Defender al abrir el puerto: hay que permitirlo en **redes privadas**. Si lo rechazaron, se arregla en Firewall → Permitir una aplicación.

### Comandos

Primero, cada quien averigua su IP de la WiFi:

```bash
ip -4 -o addr show            # Linux: la de wlp0s20f3 / wlan0, NO la de docker0 ni lo
ipconfig                      # Windows: "Dirección IPv4" del adaptador Wi-Fi
```

Quien corre el principal (digamos que su IP es `10.21.22.40`):

```bash
# IntelliJ: run configuration "Principal (mis jugadores)", cambiando -DmiIp
java -DmiIp=10.21.22.40 -cp out/production/8puzzle-jade:lib/jade.jar \
     puzzle.pruebas.PruebaJade principal generador despliegue
```

Cada compañero, uniéndose (ejemplo con la IP de esta máquina):

```bash
# run configuration "Unirme al principal"
java -DmiIp=10.21.22.52 -cp out/production/8puzzle-jade:lib/jade.jar \
     puzzle.pruebas.PruebaJade 10.21.22.40 jugador1 jugador2
```

El primer argumento es la IP del principal; el `-DmiIp` es la IP propia. Son distintas y las dos son obligatorias.

Dos contenedores en la MISMA PC: al segundo hay que darle `-DpuertoLocal=1100` (si no, choca en 1099 con el error `Cannot bind server socket to localhost port 1099`).

Mientras el generador real no exista, cualquiera puede levantar el mock: añadir `generador` a la lista de agentes. Si ya existe el real, **no** levantarlo: dos agentes con el mismo nombre local no pueden coexistir en la plataforma.

### Diagnóstico de interoperabilidad: `SondaGenerador`

`puzzle.pruebas.SondaGenerador` es una herramienta de diagnóstico (no parte del sistema). Se lanza con `PruebaJade ... sonda` y hace tres cosas:

1. Pregunta al AMS la lista completa de agentes con sus nombres locales exactos. Ojo: hay que poner `constraints.setMaxResults(-1L)`; con los `SearchConstraints` por defecto **el AMS devuelve un solo agente** y parece que la plataforma está casi vacía.
2. Manda un REQUEST al generador por cada formato de contenido plausible (el acordado de 9 chars, dígitos con coma, con espacio, filas, JSON...) y vuelca TODO lo que contesta: performativa, remitente, conversation-id, in-reply-to, ontología, protocolo, lenguaje y content en crudo.
3. Pasa cada respuesta por `BusquedaConGenerador.parsearHijos()` y dice cuántos hijos entendió y cuántos son válidos.

```bash
java -DmiIp=<mi ip> -DpuertoLocal=1100 -DesperaMs=6000 \
     -cp out/production/8puzzle-jade:lib/jade.jar \
     puzzle.pruebas.PruebaJade <ip del principal> sonda
```

(`-DpuertoLocal` solo si ya hay otro contenedor tuyo en el 1099. Opciones: `-Dgenerador=`, `-Destado=`, `-DunSoloFormato=true`.)

### Resultado del sondeo del 21-oct-2026 (Generador del Integrante 1)

Agentes en la plataforma: `agenteGUI`, `generador`, `rma`, `da0` (Dummy Agent), `ams`, `df`.

Lo que **sí** funciona — el transporte está perfecto, no hay nada que arreglar del lado de la red ni de los jugadores:

| Comprobación | Resultado |
|---|---|
| El generador se llama exactamente `generador` | sí, coincide con el default del jugador |
| Es alcanzable desde otra PC | sí, contesta en milisegundos |
| Responde con performativa INFORM | sí |
| Copia el `conversation-id` del REQUEST | sí (usa `createReply()`) |
| Acepta el formato de estado acordado (`182043765`) | sí: es el único al que contesta; los demás los ignora en silencio |

Lo que **no**:

> **El INFORM llega con `content` vacío (`[]`), siempre.** Probado con `182043765`, `867254301` y `123456780` (la meta). Nunca manda hijos.

Efecto en el jugador: `parsearHijos("")` devuelve lista vacía → no entra nada a la frontera → la frontera se vacía → el jugador contesta `FAILURE no-resoluble` aunque el tablero sí tenga solución. Es un falso negativo, no un error de comunicación.

**Qué pedirle al Integrante 1** (es un cambio de una línea en su agente, el `setContent`):

> Al recibir `REQUEST` con `content="182043765"`, responder con
> `INFORM` y `content="U:812403765,D:182743065,R:182403765"`
>
> - `movimiento:estado`, varios separados por coma, sin espacios.
> - El movimiento es hacia dónde se mueve el **hueco** (`U D L R`), no la ficha.
> - Solo los hijos legales: 2, 3 o 4 según dónde esté el hueco.
> - Si le resulta más fácil, también se acepta mandar **solo los estados**: `"812403765,182743065,182403765"`; el jugador deduce el movimiento comparando dónde quedó el `0`. Esto ya está probado y funciona.
> - Si el estado llega mal formado, responder `FAILURE` con `content="estado-invalido"` en lugar de un INFORM vacío, para distinguir el error del "no hay hijos".

Mientras eso no esté, el mock `AgenteGeneradorMock` hace exactamente lo correcto y sirve de referencia: ver `formatearHijos()` en `BusquedaConGenerador`.

### Checklist cuando algo no conecta

1. ¿Se ven en la red? `ping -c2 <ip del compañero>` y luego `nc -zv <ip del principal> 1099`. Si el ping falla, es *client isolation* o están en redes distintas: el problema es la red, no JADE. Si el ping pasa pero el `nc` no, el principal no está levantado o se anunció con otra IP.
2. En el log del principal, confirmar `jicp://<IP esperada>:1099`. Si dice `172.17.0.1` o `127.0.0.1`, falta `-DmiIp`.
3. En el log del que se une, confirmar `Agent container Container-N@<IP> is ready`.
4. Abrir el RMA del principal: deben aparecer los agentes de todas las PCs en el mismo árbol. Si no aparecen, no están en la misma plataforma.
5. Si se ven en el RMA pero los mensajes no llegan: casi siempre es un contenedor anunciando la IP equivocada (punto 2) o el firewall del que recibe.
6. Que todos tengan `jade.jar` 4.6.0 (el de `lib/`) y las mismas clases compiladas; un `.class` viejo en una PC da errores raros de deserialización.
7. El reloj: JADE no lo exige, pero los tiempos reportados en el INFORM los mide cada jugador en su propia PC, así que no son comparables entre máquinas distintas. Para comparar heurísticas en serio, correr los tres jugadores en la misma PC.

## 9. Código fuente

### `src/puzzle/agentes/Agentejugador.java`

```java
package puzzle.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import puzzle.busqueda.BusquedaConGenerador;
import puzzle.busqueda.BusquedaConGenerador.ResultadoBusqueda;
import puzzle.busqueda.Heuristica;

/**
 * Clase padre de los 3 Agentes Jugadores. Ciclo de vida: PERCIBIR -> RAZONAR -> ACTUAR.
 *
 * Flujo de un reto:
 *   GUI --REQUEST(estado inicial)--> Jugador
 *   repetir: Jugador --REQUEST(estado del nodo elegido)--> Generador
 *            Jugador <--INFORM("U:estado,D:estado,...")--- Generador   (los hijos)
 *            (el jugador descarta los ya visitados, calcula h y elige el siguiente nodo)
 *   al llegar a la meta: Jugador --INFORM(secuencia;longitud;nodos;tiempoMs)--> GUI
 *   si falla:            Jugador --FAILURE(motivo)--> GUI
 *
 * Cada jugador hijo solo define su heurística.
 */
public abstract class Agentejugador extends Agent {

    // ---- Contrato con los otros agentes ----
    public static final String GENERADOR = System.getProperty("generador", "generador"); // nombre del agente generador
    public static final long TIMEOUT_GENERADOR_MS = Long.getLong("timeoutGeneradorMs", 10_000L);

    /** true = imprime en consola cada vez que pide hijos al generador. */
    protected boolean traza = true;

    /** Cada hijo devuelve su propia heurística. */
    protected abstract Heuristica crearHeuristica();

    /**
     * Gancho opcional: se llama DESPUÉS de haberle contestado a quien mandó el reto.
     * Por defecto no hace nada, así que los jugadores que no lo sobreescriban se comportan igual que antes.
     * Lo usa AgenteJugador1 para abrir su ventana paso a paso sin retrasar el INFORM.
     * Ojo: corre en el hilo del agente, así que nada de Swing directo (usar SwingUtilities.invokeLater).
     */
    protected void alTerminar(ResultadoBusqueda resultado) { }

    private Heuristica heuristica;
    private BusquedaConGenerador busqueda;  // la búsqueda en curso
    private ACLMessage solicitud;           // el reto que atiendo (null = estoy libre)
    private String conversacion;            // id para reconocer las respuestas del generador
    private long vencimiento;               // hora límite para que responda el generador
    private int numeroBusqueda = 0;

    @Override
    protected void setup() {
        heuristica = crearHeuristica();
        System.out.println(getLocalName() + " listo (" + heuristica.nombre() + "), generador esperado: '" + GENERADOR + "'");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage percepcion = percibir();                       // 1. PERCIBIR
                if (percepcion == null && !tiempoAgotado()) {
                    if (solicitud == null) block();                       // libre: espero un reto
                    else block(Math.max(1, vencimiento - System.currentTimeMillis())); // espero al generador
                    return;
                }
                String siguiente = razonar(percepcion);                   // 2. RAZONAR
                actuar(siguiente);                                        // 3. ACTUAR
            }
        });
    }

    // PERCIBIR: libre -> busca un reto (REQUEST de la GUI); ocupado -> busca la respuesta del generador.
    protected ACLMessage percibir() {
        if (solicitud == null) {
            return receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        }
        // La respuesta se reconoce por el conversation-id, o por venir del generador
        // (por si el generador no copia el conversation-id al responder).
        return receive(new MessageTemplate(m ->
                conversacion.equals(m.getConversationId())
                || (m.getSender() != null && GENERADOR.equals(m.getSender().getLocalName())
                    && m.getPerformative() != ACLMessage.REQUEST)));
    }

    // RAZONAR: actualiza la búsqueda con lo percibido.
    // Devuelve el estado cuyos hijos hay que pedir, o null si la búsqueda terminó (éxito o falla).
    protected String razonar(ACLMessage percepcion) {
        if (solicitud == null) {                                  // llegó un reto nuevo
            solicitud = percepcion;
            conversacion = getLocalName() + "-" + (++numeroBusqueda);
            busqueda = new BusquedaConGenerador(heuristica);
            String estado = percepcion.getContent() == null ? "" : percepcion.getContent().trim();
            System.out.println(getLocalName() + " percibió reto: " + estado);
            return busqueda.iniciar(estado);
        }
        if (percepcion == null) {                                 // el generador no respondió a tiempo
            busqueda.abortar(ResultadoBusqueda.FALLA_GENERADOR_SIN_RESPUESTA);
            return null;
        }
        if (percepcion.getPerformative() == ACLMessage.INFORM) {  // llegaron los hijos
            return busqueda.recibirHijos(BusquedaConGenerador.parsearHijos(percepcion.getContent()));
        }
        busqueda.abortar(ResultadoBusqueda.FALLA_GENERADOR_ERROR); // FAILURE u otra cosa
        return null;
    }

    // ACTUAR: pide hijos al generador, o responde a la GUI si ya terminó.
    protected void actuar(String siguiente) {
        if (siguiente != null) {
            ACLMessage pedido = new ACLMessage(ACLMessage.REQUEST);
            pedido.addReceiver(new AID(GENERADOR, AID.ISLOCALNAME));
            pedido.setConversationId(conversacion);
            pedido.setContent(siguiente);
            send(pedido);
            vencimiento = System.currentTimeMillis() + TIMEOUT_GENERADOR_MS;
            if (traza) {
                System.out.println(getLocalName() + " -> pide hijos de " + siguiente
                        + " (h=" + busqueda.getHActual() + ", expandidos=" + busqueda.getExpandidos()
                        + ", frontera=" + busqueda.getTamFrontera() + ")");
            }
            return;
        }

        ResultadoBusqueda r = busqueda.getResultado();
        ACLMessage respuesta = solicitud.createReply();
        if (r.exito) {
            respuesta.setPerformative(ACLMessage.INFORM);
            respuesta.setContent(r.aInform());                    // secuencia;longitud;nodos;tiempoMs
        } else {
            respuesta.setPerformative(ACLMessage.FAILURE);
            respuesta.setContent(r.motivoFalla);
        }
        send(respuesta);
        System.out.println(getLocalName() + " responde " + ACLMessage.getPerformative(respuesta.getPerformative())
                + ": " + respuesta.getContent());

        while (receive(MessageTemplate.MatchConversationId(conversacion)) != null) { /* limpia respuestas tardías */ }
        solicitud = null;                                         // libre para otro reto
        busqueda = null;

        // Ya contestamos y estamos libres: ahora lo opcional. Si el gancho falla, el agente sigue vivo.
        try {
            alTerminar(r);
        } catch (RuntimeException e) {
            System.out.println(getLocalName() + " aviso: alTerminar() falló (" + e + "), la búsqueda sí terminó bien");
        }
    }

    private boolean tiempoAgotado() {
        return solicitud != null && System.currentTimeMillis() >= vencimiento;
    }
}
```

### `src/puzzle/agentes/AgenteJugador1.java`

```java
package puzzle.agentes;

import puzzle.busqueda.BusquedaConGenerador.ResultadoBusqueda;
import puzzle.busqueda.Hamming;
import puzzle.busqueda.Heuristica;
import puzzle.gui.VentanaJugador;

/**
 * Agente Jugador 1: Greedy con la Heurística 1 (Hamming, fichas fuera de lugar).
 *
 * Además de responder el INFORM a la GUI (eso lo hace Agentejugador), abre una ventana
 * local con el recorrido paso a paso. La ventana es solo un visor: no manda mensajes.
 */
public class AgenteJugador1 extends Agentejugador {

    private static final String TITULO = "Agente 1 (Hamming)";

    @Override
    protected Heuristica crearHeuristica() {
        return new Hamming();
    }

    /** Se llama después de haber contestado, así que la ventana no retrasa el INFORM. */
    @Override
    protected void alTerminar(ResultadoBusqueda resultado) {
        if (!resultado.exito) {
            System.out.println(getLocalName() + ": no abro ventana, la búsqueda falló ("
                    + resultado.motivoFalla + ")");
            return;
        }
        // Historial de estados del camino: "182043765", "182403765", ... hasta "123456780"
        VentanaJugador.mostrar(TITULO, resultado.estados,
                "Hamming  |  " + resultado.longitud + " movimientos  |  "
                        + resultado.nodosExpandidos + " nodos expandidos  |  "
                        + resultado.tiempoMs + " ms");
    }
}
```

### `src/puzzle/agentes/AgenteJugador2.java`

```java
package puzzle.agentes;

import puzzle.busqueda.Heuristica;
import puzzle.busqueda.Manhattan;

/** Agente Jugador 2: Greedy con la Heurística 2 (distancia de Manhattan). */
public class AgenteJugador2 extends Agentejugador {

    @Override
    protected Heuristica crearHeuristica() {
        return new Manhattan();
    }
}
```

### `src/puzzle/agentes/AgenteJugador3.java`

```java
package puzzle.agentes;

import puzzle.busqueda.ConflictoLineal;
import puzzle.busqueda.Heuristica;

/** Agente Jugador 3: Greedy con la Heurística 3 (Manhattan + conflicto lineal). */
public class AgenteJugador3 extends Agentejugador {

    @Override
    protected Heuristica crearHeuristica() {
        return new ConflictoLineal();
    }
}
```

### `src/puzzle/busqueda/Heuristica.java`

```java
package puzzle.busqueda;

public interface Heuristica {
int calcular(String estado);
String nombre();
}
```

### `src/puzzle/busqueda/Hamming.java`

```java
package puzzle.busqueda;

/**
 * Heurística 1: fichas fuera de lugar (distancia de Hamming).
 * Cuenta cuántas de las 8 fichas no están en su posición de la meta "123456780".
 * El hueco no se cuenta; si se contara, dejaría de ser admisible.
 *
 * Ejemplo: "182043765" -> fuera de lugar 8, 2, 4, 3, 6, 5 -> h = 6
 */
public class Hamming implements Heuristica {

    @Override
    public int calcular(String estado) {
        int fueraDeLugar = 0;
        for (int i = 0; i < 9; i++) {
            char ficha = estado.charAt(i);
            if (ficha != '0' && ficha != BusquedaConGenerador.META.charAt(i)) {
                fueraDeLugar++;
            }
        }
        return fueraDeLugar;
    }

    @Override
    public String nombre() {
        return "Hamming";
    }
}
```

### `src/puzzle/busqueda/Manhattan.java`

```java
package puzzle.busqueda;

/**
 * Heurística 2: distancia de Manhattan.
 * Suma, para cada ficha, cuántas filas y columnas le faltan para llegar a su lugar en la meta.
 * La ficha 'k' va en la posición k-1 (la '1' en la 0, ..., la '8' en la 7). El hueco no cuenta.
 *
 * Ejemplo: "182043765" -> 8:2, 2:1, 4:1, 3:1, 6:2, 5:2 -> h = 9
 */
public class Manhattan implements Heuristica {

    @Override
    public int calcular(String estado) {
        int h = 0;
        for (int i = 0; i < 9; i++) {
            char ficha = estado.charAt(i);
            if (ficha == '0') continue;
            int meta = ficha - '1';                         // posición correcta de la ficha
            h += Math.abs(i / 3 - meta / 3) + Math.abs(i % 3 - meta % 3);
        }
        return h;
    }

    @Override
    public String nombre() {
        return "Manhattan";
    }
}
```

### `src/puzzle/busqueda/ConflictoLineal.java`

```java
package puzzle.busqueda;

/**
 * Heurística 3: distancia de Manhattan + 2 por cada conflicto lineal.
 * Conflicto lineal: dos fichas que están en su fila (o columna) correcta, pero en orden invertido.
 * La meta es  1 2 3 / 4 5 6 / 7 8 0 : la ficha '1' va en la posición 0, la '2' en la 1, etc.
 */
public class ConflictoLineal implements Heuristica {

    @Override
    public int calcular(String estado) {
        int h = 0;

        // 1) Manhattan: cuánto le falta a cada ficha para llegar a su lugar
        for (int i = 0; i < 9; i++) {
            if (estado.charAt(i) == '0') continue;
            int meta = estado.charAt(i) - '1';              // posición correcta de la ficha
            h += Math.abs(i / 3 - meta / 3) + Math.abs(i % 3 - meta % 3);
        }

        // 2) Conflictos en filas
        for (int fila = 0; fila < 3; fila++) {
            for (int a = 0; a < 3; a++) {
                for (int b = a + 1; b < 3; b++) {
                    char x = estado.charAt(fila * 3 + a);
                    char y = estado.charAt(fila * 3 + b);
                    if (x == '0' || y == '0') continue;
                    int metaX = x - '1', metaY = y - '1';
                    // las dos pertenecen a esta fila, pero x debería ir después de y
                    if (metaX / 3 == fila && metaY / 3 == fila && metaX % 3 > metaY % 3) h += 2;
                }
            }
        }

        // 3) Conflictos en columnas (igual que las filas)
        for (int col = 0; col < 3; col++) {
            for (int a = 0; a < 3; a++) {
                for (int b = a + 1; b < 3; b++) {
                    char x = estado.charAt(a * 3 + col);
                    char y = estado.charAt(b * 3 + col);
                    if (x == '0' || y == '0') continue;
                    int metaX = x - '1', metaY = y - '1';
                    if (metaX % 3 == col && metaY % 3 == col && metaX / 3 > metaY / 3) h += 2;
                }
            }
        }
        return h;
    }

    @Override
    public String nombre() {
        return "Conflicto lineal";
    }
}
```

### `src/puzzle/busqueda/BusquedaConGenerador.java`

```java
package puzzle.busqueda;

import java.util.ArrayList;
import java.util.Collections;
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
        /** Estados del camino, del inicial a la meta. Tiene longitud+1 elementos; vacía si falló. */
        public final List<String> estados;

        private ResultadoBusqueda(boolean exito, String secuencia, int nodos, long ms, String motivo,
                                  List<String> estados) {
            this.exito = exito;
            this.secuencia = secuencia;
            this.longitud = secuencia.length();
            this.nodosExpandidos = nodos;
            this.tiempoMs = ms;
            this.motivoFalla = motivo;
            this.estados = estados;
        }

        static ResultadoBusqueda exito(String secuencia, int nodos, long ms, List<String> estados) {
            return new ResultadoBusqueda(true, secuencia, nodos, ms, null, estados);
        }

        static ResultadoBusqueda falla(String motivo, int nodos, long ms) {
            return new ResultadoBusqueda(false, "", nodos, ms, motivo, new ArrayList<>());
        }

        /** Formato acordado del INFORM a la GUI: secuencia;longitud;nodos;tiempoMs */
        public String aInform() {
            return secuencia + ";" + longitud + ";" + nodosExpandidos + ";" + tiempoMs;
        }

        /**
         * Variante de 5 campos con los estados del camino: secuencia;longitud;nodos;tiempoMs;estados
         * (estados separados por coma). NO se usa todavía: el contrato con la GUI sigue siendo
         * el de 4 campos hasta que el equipo lo acuerde (ver seccion 3 del documento).
         */
        public String aInformConEstados() {
            return aInform() + ";" + String.join(",", estados);
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

        /** Los estados del camino, del inicial (raíz) a este nodo. Uno más que movimientos. */
        List<String> caminoEstados() {
            List<String> lista = new ArrayList<>();
            for (Nodo n = this; n != null; n = n.padre) lista.add(n.estado);
            Collections.reverse(lista);
            return lista;
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
            return terminar(ResultadoBusqueda.exito(actual.camino(), expandidos, ms(), actual.caminoEstados()));
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
```

### `src/puzzle/pruebas/PruebaJade.java`

```java
package puzzle.pruebas;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PruebaJade {

    /** nombre del agente -> clase que lo implementa. El orden importa: jugadores y generador antes del despliegue. */
    private static final String[][] CATALOGO = {
            {"jugador1",   "puzzle.agentes.AgenteJugador1"},
            {"jugador2",   "puzzle.agentes.AgenteJugador2"},
            {"jugador3",   "puzzle.agentes.AgenteJugador3"},
            {"generador",  "puzzle.pruebas.AgenteGeneradorMock"},
            {"despliegue", "puzzle.pruebas.AgenteDespliegueMock"},
            {"sonda",      "puzzle.pruebas.SondaGenerador"},        // diagnóstico, va al final
    };

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "local";
        List<String> pedidos = args.length > 1
                ? expandir(Arrays.asList(args).subList(1, args.length))
                : modo.equals("local") ? Arrays.asList("jugador1", "jugador2", "jugador3", "generador", "despliegue")
                : Arrays.asList("jugador1", "jugador2", "jugador3");

        Runtime rt = Runtime.instance();
        Profile p;
        ContainerController contenedor;
        if (modo.equals("local") || modo.equals("principal")) {
            p = new ProfileImpl();
            p.setParameter(Profile.GUI, System.getProperty("sinGui") == null ? "true" : "false");
            if (System.getProperty("miIp") != null) p.setParameter(Profile.LOCAL_HOST, System.getProperty("miIp"));
            contenedor = rt.createMainContainer(p);
        } else {
            p = new ProfileImpl(modo, 1099, null);          // modo = IP de la PC con el contenedor principal
            if (System.getProperty("miIp") != null) p.setParameter(Profile.LOCAL_HOST, System.getProperty("miIp"));
            if (System.getProperty("puertoLocal") != null) p.setParameter(Profile.LOCAL_PORT, System.getProperty("puertoLocal"));
            contenedor = rt.createAgentContainer(p);
        }

        // Orden fijo del CATALOGO: primero jugadores y generador, al final la GUI (que es la que envía el reto)
        int creados = 0;
        for (String[] entrada : CATALOGO) {
            if (pedidos.contains(entrada[0])) {
                contenedor.createNewAgent(entrada[0], entrada[1], null).start();
                System.out.println("PruebaJade: inicia " + entrada[0] + " (" + entrada[1] + ")");
                creados++;
            }
        }

        // Un nombre mal escrito dejaba la plataforma vacía sin decir nada: ahora avisa.
        for (String nombre : pedidos) {
            if (!conocido(nombre)) {
                System.out.println("PruebaJade: AVISO, no conozco el agente '" + nombre + "'. Opciones: "
                        + "jugador1 jugador2 jugador3 jugadores generador despliegue sonda");
            }
        }
        if (creados == 0) {
            System.out.println("PruebaJade: AVISO, no se inició ningún agente. La plataforma está vacía.");
        }
    }

    private static boolean conocido(String nombre) {
        for (String[] entrada : CATALOGO) if (entrada[0].equals(nombre)) return true;
        return false;
    }

    /** "jugadores" es atajo de jugador1 + jugador2 + jugador3. */
    private static List<String> expandir(List<String> pedidos) {
        List<String> lista = new ArrayList<>();
        for (String nombre : pedidos) {
            if (nombre.equals("jugadores")) lista.addAll(Arrays.asList("jugador1", "jugador2", "jugador3"));
            else lista.add(nombre);
        }
        return lista;
    }
}
```

### `src/puzzle/pruebas/AgenteGeneradorMock.java`

```java
package puzzle.pruebas;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import puzzle.busqueda.BusquedaConGenerador;

/**
 * SIMULA al Agente Generador (solo para probar al jugador).
 * Recibe REQUEST con un estado y responde INFORM con sus hijos: "U:estado,D:estado,..."
 * Opciones (-D en la JVM):  -DretardoMs=200 simula latencia de red;  -DsinMovimiento=true envía solo estados;
 * -DsinConversacion=true responde con un mensaje nuevo (sin conversation-id), como lo haría un generador descuidado.
 */
public class AgenteGeneradorMock extends Agent {

    private static final long RETARDO_MS = Long.getLong("retardoMs", 0L);
    private static final boolean CON_MOVIMIENTO = !Boolean.getBoolean("sinMovimiento");
    private static final boolean SIN_CONVERSACION = Boolean.getBoolean("sinConversacion");

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " (MOCK del generador) listo");
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg == null) { block(); return; }

                String estado = msg.getContent() == null ? "" : msg.getContent().trim();
                if (RETARDO_MS > 0) {
                    try { Thread.sleep(RETARDO_MS); } catch (InterruptedException e) { /* nada */ }
                }

                ACLMessage respuesta = msg.createReply();
                if (SIN_CONVERSACION) {                       // mensaje nuevo: sin conversation-id ni in-reply-to
                    respuesta = new ACLMessage(ACLMessage.INFORM);
                    respuesta.addReceiver(msg.getSender());
                }
                if (!BusquedaConGenerador.esValido(estado)) {
                    respuesta.setPerformative(ACLMessage.FAILURE);
                    respuesta.setContent("estado-invalido");
                } else {
                    respuesta.setPerformative(ACLMessage.INFORM);
                    respuesta.setContent(BusquedaConGenerador.formatearHijos(TableroPrueba.hijosDe(estado), CON_MOVIMIENTO));
                }
                myAgent.send(respuesta);
                System.out.println(getLocalName() + " <- hijos de " + estado + " -> " + respuesta.getContent());
            }
        });
    }
}
```

### `src/puzzle/pruebas/AgenteDespliegueMock.java`

```java
package puzzle.pruebas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import puzzle.busqueda.BusquedaConGenerador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SIMULA al Agente de Despliegue (GUI) (solo para probar a los jugadores).
 * Envía el reto a uno o varios jugadores y muestra la respuesta, comprobando que la secuencia llegue a la meta.
 * Cuando todos contestan, imprime una tabla comparativa (longitud, nodos, tiempo).
 *
 * El tablero se puede pasar como argumento del agente o con -Destado=182043765
 * A quién retar: -Djugador=jugador1  o varios separados por coma: -Djugador=jugador1,jugador2,jugador3
 */
public class AgenteDespliegueMock extends Agent {

    private String estado;
    private final List<String> jugadores = new ArrayList<>();
    /** respuestas recibidas: jugador -> contenido del INFORM/FAILURE (para la tabla final) */
    private final Map<String, String[]> respuestas = new LinkedHashMap<>();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        estado = (args != null && args.length > 0) ? args[0].toString() : System.getProperty("estado", "182043765");
        for (String nombre : System.getProperty("jugador", "jugador3").split(",")) {
            nombre = nombre.trim();
            if (!nombre.isEmpty()) jugadores.add(nombre);
        }

        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                for (String jugador : jugadores) req.addReceiver(new AID(jugador, AID.ISLOCALNAME));
                req.setContent(estado);
                System.out.println(getLocalName() + " (MOCK GUI) -> REQUEST a " + jugadores + ": " + estado);
                myAgent.send(req);
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage m = myAgent.receive();
                if (m == null) { block(); return; }
                String de = m.getSender().getLocalName();
                String tipo = ACLMessage.getPerformative(m.getPerformative());
                if (m.getPerformative() != ACLMessage.INFORM) {
                    System.out.println(getLocalName() + " <- " + tipo + " de " + de + ": " + m.getContent());
                    anotar(de, null);
                    return;
                }
                String[] p = m.getContent().split(";", -1);
                System.out.println(getLocalName() + " <- " + tipo + " de " + de + ": movimientos=" + p[0]
                        + " | longitud=" + p[1] + " | nodos=" + p[2] + " | tiempo=" + p[3] + " ms");

                // Comprobación: repetir los movimientos sobre el tablero inicial
                String actual = estado;
                for (char mov : p[0].toCharArray()) actual = (actual == null) ? null : TableroPrueba.mover(actual, mov);
                boolean llega = BusquedaConGenerador.META.equals(actual);
                System.out.println(getLocalName() + "    verificación: "
                        + (llega ? "la secuencia llega a la meta" : "ERROR, no llega a la meta"));
                anotar(de, llega ? p : null);
            }
        });
    }

    /** Guarda la respuesta y, si ya contestaron todos los retados, imprime la comparación. */
    private void anotar(String jugador, String[] campos) {
        respuestas.put(jugador, campos);
        if (!respuestas.keySet().containsAll(jugadores)) return;

        System.out.println("\n=== Comparación (tablero " + estado + ") ===");
        System.out.printf("%-12s %-10s %-9s %-7s %s%n", "jugador", "longitud", "nodos", "ms", "secuencia");
        for (String nombre : jugadores) {
            String[] c = respuestas.get(nombre);
            if (c == null) System.out.printf("%-12s %s%n", nombre, "(sin solución válida)");
            else System.out.printf("%-12s %-10s %-9s %-7s %s%n", nombre, c[1], c[2], c[3], c[0]);
        }
        System.out.println();
    }
}
```

### `src/puzzle/pruebas/TableroPrueba.java`

```java
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
```

### `src/puzzle/pruebas/PruebaJugadores12.java`

```java
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
```

### `src/puzzle/pruebas/SondaGenerador.java`

```java
package puzzle.pruebas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import puzzle.busqueda.BusquedaConGenerador;
import puzzle.busqueda.BusquedaConGenerador.Hijo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HERRAMIENTA DE DIAGNÓSTICO (no es parte del sistema).
 *
 * Sirve para averiguar por qué un jugador no logra hablar con el Agente Generador real.
 *   1. Pregunta al AMS qué agentes hay en la plataforma, con sus nombres locales exactos.
 *   2. Le manda al generador un REQUEST por cada formato de contenido plausible y vuelca TODO
 *      lo que contesta: performativa, remitente, conversation-id, ontología, protocolo, lenguaje
 *      y contenido en crudo. Luego lo interpreta con el mismo parser del jugador.
 *   3. Al final dice qué formato (si alguno) produjo hijos utilizables.
 *
 * Opciones (-D):  -Dgenerador=nombre   (default "generador")
 *                 -Destado=182043765
 *                 -DesperaMs=8000      cuánto esperar cada respuesta
 *                 -DunSoloFormato=true solo prueba el formato acordado por el equipo
 */
public class SondaGenerador extends Agent {

    private final String generador = System.getProperty("generador", "generador");
    private final String estado = System.getProperty("estado", "182043765");
    private final long esperaMs = Long.getLong("esperaMs", 8_000L);
    private final boolean unSoloFormato = Boolean.getBoolean("unSoloFormato");

    /** etiqueta -> contenido a enviar. El primero es el formato acordado en el documento. */
    private final Map<String, String> variantes = new LinkedHashMap<>();
    private final List<String> etiquetas = new ArrayList<>();
    private final Map<String, String> veredicto = new LinkedHashMap<>();

    private int indice = 0;
    private long vencimiento;
    private boolean respondio;

    @Override
    protected void setup() {
        armarVariantes();
        listarAgentes();

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                if (vencimiento == 0) { enviar(); return; }

                ACLMessage m = myAgent.receive();          // SIN plantilla: cae todo
                if (m == null) {
                    long falta = vencimiento - System.currentTimeMillis();
                    if (falta > 0) { block(falta); return; }
                    if (!respondio) {
                        System.out.println("  >> NADIE CONTESTÓ en " + esperaMs + " ms.");
                        veredicto.put(etiquetas.get(indice - 1), "sin respuesta");
                    }
                    siguiente();
                    return;
                }
                respondio = true;
                volcar(m);
                vencimiento = System.currentTimeMillis() + 1500;   // margen por si manda varios
            }
        });
    }

    private void armarVariantes() {
        variantes.put("acordado (9 chars)", estado);
        if (!unSoloFormato) {
            StringBuilder comas = new StringBuilder(), espacios = new StringBuilder();
            for (int i = 0; i < estado.length(); i++) {
                if (i > 0) { comas.append(','); espacios.append(' '); }
                comas.append(estado.charAt(i));
                espacios.append(estado.charAt(i));
            }
            variantes.put("digitos con coma", comas.toString());
            variantes.put("digitos con espacio", espacios.toString());
            variantes.put("filas con coma", estado.substring(0, 3) + "," + estado.substring(3, 6) + "," + estado.substring(6));
            variantes.put("filas con salto", estado.substring(0, 3) + "\n" + estado.substring(3, 6) + "\n" + estado.substring(6));
            variantes.put("arreglo JSON", "[" + comas + "]");
            variantes.put("objeto JSON", "{\"estado\":\"" + estado + "\"}");
        }
        etiquetas.addAll(variantes.keySet());
    }

    private void enviar() {
        if (indice >= etiquetas.size()) { resumen(); doDelete(); return; }
        String etiqueta = etiquetas.get(indice);
        String contenido = variantes.get(etiqueta);

        ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
        req.addReceiver(new AID(generador, AID.ISLOCALNAME));
        req.setConversationId("sonda-" + (indice + 1));
        req.setContent(contenido);
        send(req);

        respondio = false;
        vencimiento = System.currentTimeMillis() + esperaMs;
        indice++;
        System.out.println("\n--- SONDA " + indice + "/" + etiquetas.size() + ": formato \"" + etiqueta + "\" ---");
        System.out.println("  REQUEST a '" + generador + "', conv-id sonda-" + indice);
        System.out.println("  content enviado: [" + contenido.replace("\n", "\\n") + "]");
    }

    private void siguiente() {
        vencimiento = 0;
        enviar();
    }

    /** Pregunta al AMS la lista completa de agentes. Sin setMaxResults(-1) el AMS devuelve solo 1. */
    private void listarAgentes() {
        System.out.println("\n=== SONDA: agentes en la plataforma segun el AMS ===");
        try {
            SearchConstraints todos = new SearchConstraints();
            todos.setMaxResults(-1L);
            AMSAgentDescription[] agentes = AMSService.search(this, new AMSAgentDescription(), todos);
            for (AMSAgentDescription d : agentes) {
                AID aid = d.getName();
                System.out.printf("  %-18s  %s%n", aid.getLocalName(), aid.getName());
            }
            System.out.println("  (" + agentes.length + " agentes; el nombre local de la izquierda va en AID.ISLOCALNAME)");
        } catch (Exception e) {
            System.out.println("  no se pudo consultar el AMS: " + e);
        }
    }

    /** Imprime todos los campos del mensaje y lo que el parser del jugador entendería. */
    private void volcar(ACLMessage m) {
        String etiqueta = etiquetas.get(indice - 1);
        System.out.println("  RESPUESTA:");
        System.out.println("    performativa  : " + ACLMessage.getPerformative(m.getPerformative())
                + " (" + m.getPerformative() + ")");
        System.out.println("    remitente     : " + (m.getSender() == null ? "null" : m.getSender().getLocalName()));
        System.out.println("    conversation  : " + m.getConversationId()
                + (("sonda-" + indice).equals(m.getConversationId()) ? "   <- lo copió bien" : "   <- NO copió el conversation-id"));
        System.out.println("    in-reply-to   : " + m.getInReplyTo());
        System.out.println("    ontologia     : " + m.getOntology());
        System.out.println("    protocolo     : " + m.getProtocol());
        System.out.println("    lenguaje      : " + m.getLanguage());
        String c = m.getContent();
        System.out.println("    content crudo : [" + (c == null ? "null" : c.replace("\n", "\\n")) + "]"
                + (c == null || c.isEmpty() ? "   <- VACÍO" : "   (" + c.length() + " chars)"));

        if (m.getPerformative() != ACLMessage.INFORM) {
            System.out.println("    >> El jugador trataría esto como FALLA 'generador-error':"
                    + " Agentejugador solo acepta INFORM como entrega de hijos.");
            veredicto.put(etiqueta, "respondió " + ACLMessage.getPerformative(m.getPerformative()) + ", no INFORM");
            return;
        }

        List<Hijo> hijos = BusquedaConGenerador.parsearHijos(c);
        int validos = 0;
        for (Hijo h : hijos) if (BusquedaConGenerador.esValido(h.estado)) validos++;
        System.out.println("    >> parsearHijos() entendió " + hijos.size() + " hijo(s), " + validos + " válido(s)");
        for (Hijo h : hijos) {
            System.out.printf("       mov='%c'  estado='%s'  %s%n", h.movimiento, h.estado,
                    BusquedaConGenerador.esValido(h.estado) ? "valido" : "INVALIDO (se descarta)");
        }
        veredicto.put(etiqueta, validos > 0 ? "OK: " + validos + " hijos utilizables"
                : "INFORM pero 0 hijos utilizables");
    }

    private void resumen() {
        System.out.println("\n=== SONDA: resumen ===");
        for (Map.Entry<String, String> e : veredicto.entrySet()) {
            System.out.printf("  %-22s %s%n", e.getKey(), e.getValue());
        }
        boolean alguno = veredicto.values().stream().anyMatch(v -> v.startsWith("OK"));
        System.out.println();
        if (alguno) {
            System.out.println("  Hay al menos un formato que funciona: usar ese con el Integrante 1.");
        } else {
            System.out.println("  Ningún formato produjo hijos. Qué pedirle al Integrante 1:");
            System.out.println("   - que al recibir REQUEST con content=\"182043765\" responda");
            System.out.println("     INFORM con content=\"U:812403765,D:182743065,R:182403765\"");
            System.out.println("     (movimiento:estado separados por coma; el movimiento es del HUECO)");
            System.out.println("   - o, si prefiere, solo los estados: \"812403765,182743065,182403765\"");
            System.out.println("   - que use msg.createReply() para que el conversation-id vuelva igual");
        }
    }
}
```

### `src/puzzle/gui/VentanaJugador.java`

```java
package puzzle.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana paso a paso de UN jugador: dibuja el tablero en cada estado del camino encontrado.
 *
 * Es un visor LOCAL para la demo, independiente de la GUI del equipo (Agente de Despliegue,
 * Integrante 4): no manda ni recibe mensajes, solo muestra lo que el jugador ya resolvió.
 *
 * Uso desde el agente:
 *     VentanaJugador.mostrar("Agente 1 (Hamming)", historialPasoAPaso);
 *
 * Se puede apagar con -DsinVentana=true (útil al correr sin pantalla o en pruebas por consola).
 */
public class VentanaJugador {

    private static final boolean DESACTIVADA = Boolean.getBoolean("sinVentana");
    /** ms entre pasos en modo automático */
    private static final int RETARDO_AUTO_MS = Integer.getInteger("retardoVentanaMs", 500);

    /** Una ventana por título, para que un segundo reto reutilice la misma en vez de abrir otra. */
    private static final java.util.Map<String, VentanaJugador> ABIERTAS = new java.util.HashMap<>();

    private final JFrame marco;
    private final JLabel[] casillas = new JLabel[9];
    private final JLabel etiquetaPaso = new JLabel();
    private final JLabel etiquetaResumen = new JLabel();
    private final JSlider barra = new JSlider();
    private final JButton botonAuto = new JButton("Reproducir");
    private final Timer temporizador;

    private List<String> estados = new ArrayList<>();
    private int paso = 0;

    /**
     * Abre (o actualiza) la ventana del jugador y muestra el recorrido.
     * Seguro de llamar desde el hilo del agente: salta a Swing por su cuenta.
     *
     * @param titulo  nombre a mostrar, p. ej. "Agente 1 (Hamming)"
     * @param estados estados del camino, del inicial a la meta
     */
    public static void mostrar(String titulo, List<String> estados) {
        mostrar(titulo, estados, null);
    }

    /** Igual, con una línea extra de métricas debajo del tablero. */
    public static void mostrar(String titulo, List<String> estados, String resumen) {
        if (DESACTIVADA) return;
        if (estados == null || estados.isEmpty()) {
            System.out.println("VentanaJugador: nada que mostrar para " + titulo + " (lista de estados vacía)");
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("VentanaJugador: sin pantalla disponible, no abro ventana para " + titulo);
            return;
        }
        // copia defensiva: la lista viene del agente, que sigue trabajando
        final List<String> copia = new ArrayList<>(estados);
        SwingUtilities.invokeLater(() -> {
            VentanaJugador v = ABIERTAS.get(titulo);
            if (v == null) {
                v = new VentanaJugador(titulo);
                ABIERTAS.put(titulo, v);
            }
            v.cargar(copia, resumen);
        });
    }

    private VentanaJugador(String titulo) {
        marco = new JFrame(titulo);
        marco.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);   // cerrar la ventana NO mata al agente
        marco.setLayout(new BorderLayout(8, 8));

        JLabel cabecera = new JLabel(titulo, SwingConstants.CENTER);
        cabecera.setFont(cabecera.getFont().deriveFont(Font.BOLD, 16f));
        cabecera.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        marco.add(cabecera, BorderLayout.NORTH);

        JPanel tablero = new JPanel(new GridLayout(3, 3, 4, 4));
        tablero.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));
        // sin esto, si el gestor de ventanas achica el marco, BorderLayout.CENTER cede
        // todo el recorte y las casillas quedan de 12 px de alto
        tablero.setMinimumSize(new Dimension(228, 216));
        for (int i = 0; i < 9; i++) {
            casillas[i] = new JLabel("", SwingConstants.CENTER);
            casillas[i].setPreferredSize(new Dimension(64, 64));
            casillas[i].setFont(casillas[i].getFont().deriveFont(Font.BOLD, 28f));
            casillas[i].setOpaque(true);
            casillas[i].setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120)));
            tablero.add(casillas[i]);
        }
        marco.add(tablero, BorderLayout.CENTER);

        // BoxLayout y no GridLayout: GridLayout reparte filas IGUALES y se queda con mucho
        // mas alto del necesario, que BorderLayout le resta al tablero.
        JPanel abajo = new JPanel();
        abajo.setLayout(new BoxLayout(abajo, BoxLayout.Y_AXIS));
        abajo.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));

        etiquetaPaso.setAlignmentX(Component.CENTER_ALIGNMENT);
        etiquetaResumen.setAlignmentX(Component.CENTER_ALIGNMENT);
        etiquetaResumen.setFont(etiquetaResumen.getFont().deriveFont(Font.PLAIN, 11f));
        abajo.add(etiquetaPaso);
        abajo.add(Box.createVerticalStrut(2));
        abajo.add(etiquetaResumen);
        abajo.add(Box.createVerticalStrut(6));

        barra.setMinimum(0);
        barra.setAlignmentX(Component.CENTER_ALIGNMENT);
        barra.addChangeListener(e -> {
            if (barra.getValue() != paso) { paso = barra.getValue(); pintar(); }
        });
        abajo.add(barra);

        JButton anterior = new JButton("< Anterior");
        JButton siguiente = new JButton("Siguiente >");
        JButton reinicio = new JButton("Reiniciar");
        anterior.addActionListener(e -> irA(paso - 1));
        siguiente.addActionListener(e -> irA(paso + 1));
        reinicio.addActionListener(e -> { detener(); irA(0); });
        botonAuto.addActionListener(e -> alternarAuto());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        botones.setAlignmentX(Component.CENTER_ALIGNMENT);
        botones.add(anterior);
        botones.add(botonAuto);
        botones.add(siguiente);
        botones.add(reinicio);
        abajo.add(Box.createVerticalStrut(6));
        abajo.add(botones);

        marco.add(abajo, BorderLayout.SOUTH);

        temporizador = new Timer(RETARDO_AUTO_MS, e -> {
            if (paso + 1 < estados.size()) irA(paso + 1);
            else detener();
        });
    }

    private void cargar(List<String> nuevos, String resumen) {
        detener();
        estados = nuevos;
        paso = 0;
        barra.setMaximum(estados.size() - 1);
        barra.setValue(0);
        etiquetaResumen.setText(resumen == null ? " " : resumen);
        pintar();
        if (!marco.isVisible()) {
            marco.pack();
            // el gestor de ventanas puede imponer su propia geometria y dejar el marco mas
            // bajo de lo que pide pack(); con el minimo fijado ya no puede aplastar el tablero
            marco.setMinimumSize(marco.getSize());
            marco.setLocationByPlatform(true);
            marco.setVisible(true);
        }
        marco.toFront();
    }

    private void irA(int destino) {
        if (destino < 0 || destino >= estados.size()) return;
        paso = destino;
        barra.setValue(destino);
        pintar();
    }

    private void alternarAuto() {
        if (temporizador.isRunning()) { detener(); return; }
        if (paso == estados.size() - 1) irA(0);
        temporizador.start();
        botonAuto.setText("Pausa");
    }

    private void detener() {
        temporizador.stop();
        botonAuto.setText("Reproducir");
    }

    /** Dibuja el estado actual; resalta en verde las fichas ya colocadas en su lugar de la meta. */
    private void pintar() {
        String estado = estados.get(paso);
        for (int i = 0; i < 9; i++) {
            char ficha = estado.charAt(i);
            boolean hueco = ficha == '0';
            boolean enSuLugar = !hueco && ficha == "123456780".charAt(i);
            casillas[i].setText(hueco ? "" : String.valueOf(ficha));
            casillas[i].setBackground(hueco ? new Color(238, 238, 238)
                    : enSuLugar ? new Color(168, 214, 168) : new Color(252, 226, 170));
            casillas[i].setForeground(new Color(30, 30, 30));
        }
        etiquetaPaso.setText("Paso " + paso + " de " + (estados.size() - 1)
                + (paso == 0 ? "  (inicial)" : paso == estados.size() - 1 ? "  (meta)" : ""));
    }
}
```

### Propuesta (NO aplicada): `ConflictoLineal` corregida, admisible

```java
package puzzle.busqueda;

public class ConflictoLineal implements Heuristica {
    @Override
    public int calcular(String estado) {
        int h = 0;
        for (int i = 0; i < 9; i++) {
            if (estado.charAt(i) == '0') continue;
            int meta = estado.charAt(i) - '1';
            h += Math.abs(i / 3 - meta / 3) + Math.abs(i % 3 - meta % 3);
        }
        int[] metas = new int[3];
        for (int fila = 0; fila < 3; fila++) {          // filas
            int n = 0;
            for (int col = 0; col < 3; col++) {
                char x = estado.charAt(fila * 3 + col);
                if (x != '0' && (x - '1') / 3 == fila) metas[n++] = (x - '1') % 3;
            }
            h += penalizacion(metas, n);
        }
        for (int col = 0; col < 3; col++) {             // columnas
            int n = 0;
            for (int fila = 0; fila < 3; fila++) {
                char x = estado.charAt(fila * 3 + col);
                if (x != '0' && (x - '1') % 3 == col) metas[n++] = (x - '1') / 3;
            }
            h += penalizacion(metas, n);
        }
        return h;
    }

    /** 2 x (fichas que hay que sacar de la línea para que las demás queden en orden). */
    private static int penalizacion(int[] metas, int n) {
        int masLarga = 0;                                // subsecuencia creciente más larga
        int[] largo = new int[n];
        for (int i = 0; i < n; i++) {
            largo[i] = 1;
            for (int j = 0; j < i; j++) {
                if (metas[j] < metas[i]) largo[i] = Math.max(largo[i], largo[j] + 1);
            }
            masLarga = Math.max(masLarga, largo[i]);
        }
        return 2 * (n - masLarga);
    }

    @Override
    public String nombre() { return "Conflicto lineal"; }
}
```