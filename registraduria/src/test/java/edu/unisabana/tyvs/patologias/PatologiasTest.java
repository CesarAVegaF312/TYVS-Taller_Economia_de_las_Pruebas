package edu.unisabana.tyvs.patologias;

import edu.unisabana.tyvs.domain.model.Gender;
import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;
import edu.unisabana.tyvs.domain.service.Registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ============================================================================
 * SEIS PATOLOGIAS. TODAS ESTAS PRUEBAS PASAN. NINGUNA APORTA NADA.
 * ============================================================================
 *
 * Este archivo es el material del taller. Cada prueba de aqui esta en verde,
 * suma cobertura, aparece en el informe como una linea mas de "tests: OK" y
 * cuesta tiempo, mantenimiento y confianza mal puesta.
 *
 * Ninguna detectaria un solo defecto si el codigo de produccion se rompiera.
 * El modulo 2 lo demuestra con numeros: se ejecuta PIT con y sin este paquete
 * y el mutation score no se mueve ni un punto.
 *
 * El objetivo del taller NO es "escriba mas pruebas". Es aprender a mirar una
 * suite existente y decidir cuales sobran, que es una decision que casi nadie
 * se atreve a tomar porque borrar una prueba se siente mal.
 *
 * Se conservan en el repositorio a proposito, igual que defectuosa.html en el
 * taller de UX y BuscadorVulnerable en el de seguridad. El paquete se llama
 * `patologias` para que nadie las copie por error.
 */
@DisplayName("Patologias de suite (todas pasan, ninguna sirve)")
class PatologiasTest {

    private static Person persona(int id, int edad, boolean viva) {
        return new Person("Nombre" + id, id, edad, Gender.FEMALE, viva);
    }

    // ========================================================================
    // PATOLOGIA 1 — La prueba sin aserciones
    // ========================================================================

    /**
     * Ejecuta codigo y no comprueba nada. Solo falla si el metodo lanza una
     * excepcion.
     *
     * Es la patologia mas comun de todas, y suele nacer de buena fe: alguien
     * necesitaba subir la cobertura y esto la sube igual que una prueba de
     * verdad, porque JaCoCo cuenta lineas EJECUTADAS y no le importa si
     * alguien miro el resultado.
     *
     * Si manana registerVoter devolviera siempre INVALID, esta prueba seguiria
     * verde.
     */
    @Test
    @DisplayName("P1 - Ejecuta el codigo y no comprueba nada")
    void pruebaSinAserciones() {
        Registry registry = new Registry();

        registry.registerVoter(persona(1, 30, true));
        registry.registerVoter(persona(2, 17, true));
        registry.registerVoter(persona(3, 30, false));
        registry.registerVoter(persona(-1, 30, true));

        // Y aqui no hay nada. Cobertura alta, valor cero.
    }

    /**
     * Variante mas dificil de detectar: SI tiene aserciones, pero no
     * discriminan. assertNotNull sobre un metodo que nunca devuelve null es
     * decorativo, y a simple vista parece una prueba en regla.
     */
    @Test
    @DisplayName("P1b - Tiene aserciones, pero no discriminan")
    void pruebaConAsercionesDecorativas() {
        Registry registry = new Registry();

        assertNotNull(registry.registerVoter(persona(1, 30, true)));
        assertNotNull(registry.registerVoter(persona(2, 17, true)));
        assertTrue(registry.totalInscritos() >= 0);   // siempre cierto

        // registerVoter nunca devuelve null y un tamano nunca es negativo.
        // Estas tres lineas no pueden fallar jamas.
    }

    // ========================================================================
    // PATOLOGIA 2 — Probar la libreria, no el codigo propio
    // ========================================================================

    /**
     * Comprueba que JUnit sabe sumar y que los getters de un POJO devuelven lo
     * que se les puso. Nada de eso es codigo suyo, y si fallara tendria
     * problemas mucho mas graves que este.
     *
     * Aparece sola en proyectos con un objetivo de cobertura obligatorio: los
     * POJO son la forma mas barata de subir el porcentaje.
     */
    @Test
    @DisplayName("P2 - Prueba la libreria y el lenguaje, no el sistema")
    void pruebaDeLaLibreria() {
        assertEquals(4, 2 + 2);
        assertEquals("ab", "a" + "b");
        Assertions.assertNotEquals(null, new ArrayList<>());

        Person p = persona(1, 30, true);
        assertEquals(1, p.getId());
        assertEquals(30, p.getAge());
        assertEquals(Gender.FEMALE, p.getGender());
        assertTrue(p.isAlive());

        // Los cuatro getters de abajo suben la cobertura de Person a casi el
        // 100%. La logica de negocio sigue exactamente igual de probada que
        // antes: nada.
    }

    // ========================================================================
    // PATOLOGIA 3 — La prueba lenta sin motivo
    // ========================================================================

    /**
     * Cinco esperas de 300 ms sobre un metodo que responde en microsegundos.
     *
     * El sleep casi nunca se pone por gusto: se pone para "estabilizar" algo
     * que fallaba de forma intermitente, y se queda ahi para siempre porque
     * nadie se atreve a quitarlo.
     *
     * Aisladamente son 1.5 segundos y no parece grave. El problema es la
     * escala: mil pruebas asi son media hora de espera en cada commit, y a los
     * veinte minutos de feedback la gente deja de correr la suite antes de
     * subir el codigo. Ahi es donde la suite deja de servir para nada.
     */
    @Test
    @DisplayName("P3 - Lenta sin razon: 1.5 segundos de espera pura")
    void pruebaLenta() throws InterruptedException {
        Registry registry = new Registry();

        for (int i = 1; i <= 5; i++) {
            Thread.sleep(300);   // no espera nada: el metodo es sincrono
            assertEquals(RegisterResult.VALID, registry.registerVoter(persona(i, 30, true)));
        }
    }

    // ========================================================================
    // PATOLOGIA 4 — Redundancia
    // ========================================================================

    /**
     * Ocho aserciones sobre EL MISMO camino de codigo: persona adulta y viva.
     * Cambiar la edad de 30 a 31 no ejerce nada nuevo.
     *
     * El coste no es tanto el tiempo como el MANTENIMIENTO: el dia que cambie
     * la regla, hay ocho sitios que actualizar en vez de uno, y la tentacion
     * de actualizarlos en bloque sin mirar es enorme.
     *
     * Regla practica: si al borrar una prueba el mutation score no baja, esa
     * prueba era redundante. El modulo 2 lo mide.
     */
    @Test
    @DisplayName("P4 - Ocho aserciones sobre el mismo camino de codigo")
    void pruebaRedundante() {
        Registry registry = new Registry();
        int[] edades = { 25, 30, 31, 40, 45, 50, 60, 70 };

        for (int i = 0; i < edades.length; i++) {
            assertEquals(RegisterResult.VALID,
                    registry.registerVoter(persona(100 + i, edades[i], true)));
        }
        // Las ocho recorren exactamente las mismas lineas.
    }

    // ========================================================================
    // PATOLOGIA 5 — La prueba que se adapta al codigo
    // ========================================================================

    /**
     * En vez de afirmar cual DEBE ser el resultado, pregunta al sistema y
     * afirma lo que le contesta.
     *
     * Es la peor de las seis, porque es imposible que falle: si manana la
     * regla se invierte, la prueba se invierte con ella y sigue verde. Y en el
     * informe se ve igual que cualquier otra.
     *
     * Suele aparecer cuando alguien "arregla" una prueba que fallaba
     * copiandole el valor que devolvia el codigo, sin preguntarse si ese valor
     * era el correcto.
     */
    @Test
    @DisplayName("P5 - Se adapta al codigo: no puede fallar nunca")
    void pruebaQueSeAdaptaAlCodigo() {
        Registry registry = new Registry();
        Person menor = persona(200, 17, true);

        RegisterResult loQueDevuelve = registry.registerVoter(menor);

        // "Comprobamos" que devuelve lo que devuelve.
        assertEquals(loQueDevuelve, new Registry().registerVoter(menor));
        assertNotNull(loQueDevuelve);
    }

    // ========================================================================
    // PATOLOGIA 6 — Acoplamiento a la implementacion
    // ========================================================================

    /**
     * Afirma sobre detalles internos en vez de sobre comportamiento
     * observable: aqui, el numero exacto de constantes del enum.
     *
     * No es que este mal, es que se rompe cuando alguien ANADE un resultado
     * nuevo, que es un cambio perfectamente valido y no rompe a ningun
     * usuario del sistema.
     *
     * Una suite llena de estas es la razon por la que un equipo empieza a
     * temerle a refactorizar: cada cambio interno produce una tarde de arreglar
     * pruebas que no encontraron ningun defecto.
     */
    @Test
    @DisplayName("P6 - Acoplada a la implementacion, no al comportamiento")
    void pruebaAcopladaALaImplementacion() {
        assertEquals(6, RegisterResult.values().length,
                "Alguien anadio un resultado nuevo y esta prueba se rompio sin motivo");

        List<String> nombres = new ArrayList<>();
        for (RegisterResult r : RegisterResult.values()) {
            nombres.add(r.name());
        }
        assertEquals("[VALID, DUPLICATED, INVALID, DEAD, UNDERAGE, INVALID_AGE]",
                nombres.toString(),
                "Y esta se rompe si alguien REORDENA el enum, que no cambia nada");
    }
}
