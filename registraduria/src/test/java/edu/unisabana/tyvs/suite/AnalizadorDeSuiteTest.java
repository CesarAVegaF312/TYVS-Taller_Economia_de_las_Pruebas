package edu.unisabana.tyvs.suite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El analizador es codigo de produccion del taller, asi que se prueba como
 * tal.
 *
 * Detalle de diseno que merece la pena notar: NO se prueba contra los
 * informes reales de target/, sino contra un XML fijo escrito aqui. Si se
 * probara contra los informes reales, esta prueba dependeria de cuanto tardo
 * la suite en esta maquina concreta, y seria inestable — justo la patologia
 * que el modulo 3 estudia. Habria sido facil caer en ella.
 */
@DisplayName("El analizador de coste de la suite")
class AnalizadorDeSuiteTest {

    /** Fragmento real de un informe de Surefire, recortado. */
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="edu.unisabana.tyvs.EjemploTest" tests="3" time="1.807">
              <testcase name="rapida" classname="edu.unisabana.tyvs.EjemploTest" time="0.004"/>
              <testcase name="lenta" classname="edu.unisabana.tyvs.patologias.LentaTest" time="1.502"/>
              <testcase name="media" classname="edu.unisabana.tyvs.EjemploTest" time="0.301"/>
            </testsuite>
            """;

    @Test
    @DisplayName("01 - Extrae nombre, clase y tiempo de cada caso")
    void extraeLosCasos() {
        List<AnalizadorDeSuite.Prueba> pruebas = AnalizadorDeSuite.analizar(XML);

        assertEquals(3, pruebas.size());
        assertEquals("rapida", pruebas.get(0).nombre);
        assertEquals("edu.unisabana.tyvs.EjemploTest", pruebas.get(0).clase);
        assertEquals(0.004, pruebas.get(0).segundos, 1e-9);
        assertEquals("LentaTest#lenta", pruebas.get(1).nombreCorto());
    }

    @Test
    @DisplayName("02 - Suma el tiempo total")
    void sumaElTiempoTotal() {
        assertEquals(1.807, AnalizadorDeSuite.total(AnalizadorDeSuite.analizar(XML)), 1e-9);
    }

    @Test
    @DisplayName("03 - El informe ordena de mayor a menor coste")
    void elInformeOrdenaPorCoste() {
        String informe = AnalizadorDeSuite.informe(AnalizadorDeSuite.analizar(XML));

        int posicionLenta = informe.indexOf("LentaTest#lenta");
        int posicionMedia = informe.indexOf("EjemploTest#media");
        int posicionRapida = informe.indexOf("EjemploTest#rapida");

        assertTrue(posicionLenta < posicionMedia,
                "La prueba mas lenta deberia aparecer primero");
        assertTrue(posicionMedia < posicionRapida,
                "El orden deberia ser descendente por tiempo");
    }

    @Test
    @DisplayName("04 - Separa el coste del paquete de patologias")
    void separaElCosteDeLasPatologias() {
        String informe = AnalizadorDeSuite.informe(AnalizadorDeSuite.analizar(XML));

        // 1.502 de 1.807 es el 83.1%.
        //
        // ESTA PRUEBA ENCONTRO UN DEFECTO REAL, y merece la pena contarlo
        // porque es del tipo que estudia este taller.
        //
        // La primera version del analizador usaba String.format sin indicar
        // Locale. En una maquina configurada en espanol imprimia "83,1%" con
        // coma, y en una configurada en ingles "83.1%" con punto. El informe
        // cambiaba segun la maquina.
        //
        // Es exactamente una prueba dependiente del entorno: habria pasado en
        // el portatil de quien la escribio y fallado en el runner de CI, o al
        // reves. Se arreglo forzando Locale.ROOT en el analizador, que es la
        // correccion correcta: el problema estaba en el codigo, no en la
        // prueba. La tentacion de "arreglarlo" aceptando las dos formas en la
        // asercion habria dejado el defecto intacto.
        assertTrue(informe.contains("83.1%"),
                "El informe deberia atribuir el 83.1% del tiempo a las patologias:\n" + informe);
    }

    @Test
    @DisplayName("05 - Sin informes, lo dice en vez de reventar")
    void sinInformesLoDice() {
        String informe = AnalizadorDeSuite.informe(List.of());

        assertTrue(informe.contains("No hay informes"),
                "Deberia explicar que falta ejecutar mvn test");
    }
}
