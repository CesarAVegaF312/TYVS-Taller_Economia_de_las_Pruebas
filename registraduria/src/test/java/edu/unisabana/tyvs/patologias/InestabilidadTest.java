package edu.unisabana.tyvs.patologias;

import edu.unisabana.tyvs.domain.model.Gender;
import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;
import edu.unisabana.tyvs.domain.service.Registry;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MODULO 3 — PRUEBAS INESTABLES (FLAKY)
 *
 * Una prueba inestable es la que a veces pasa y a veces falla sin que el
 * codigo haya cambiado. Es el defecto mas caro que puede tener una suite, y
 * por una razon que no es el tiempo perdido:
 *
 *   DESTRUYE LA UNICA COSA QUE HACE UTIL A UNA SUITE, QUE ES QUE SE LE CREA.
 *
 * La secuencia es siempre la misma y ocurre en semanas:
 *
 *   1. Una prueba falla de vez en cuando.
 *   2. Alguien la vuelve a lanzar y pasa. "Nada, cosas del CI."
 *   3. Se convierte en costumbre relanzar ante cualquier rojo.
 *   4. Un dia una prueba detecta un defecto REAL...
 *   5. ...y alguien relanza, pasa por casualidad, y el defecto llega a
 *      produccion.
 *
 * En el paso 5 la suite ya no vale nada, y llevaba semanas sin valer nada.
 *
 * SOBRE LOS REINTENTOS AUTOMATICOS. Casi todo el mundo llega a la misma
 * solucion: configurar el CI para reintentar los fallos. Alivia el sintoma y
 * empeora la enfermedad, porque convierte el paso 3 en politica de la empresa.
 * Si acaso, sirven para MEDIR (cuantas pruebas necesitan reintento) y nunca
 * para tapar.
 *
 * Este archivo no tiene ninguna prueba inestable de verdad, y es a proposito:
 * ponerla haria inestable el build del taller. Lo que hay es un DETECTOR, que
 * es lo que hay que saber construir.
 */
@DisplayName("Modulo 3 - Deteccion de pruebas inestables")
class InestabilidadTest {

    /**
     * Una operacion que falla aproximadamente el 15% de las veces.
     *
     * Simula las tres causas reales mas frecuentes de inestabilidad:
     * dependencia del reloj, dependencia del azar y dependencia del orden de
     * ejecucion. Ninguna tiene que ver con el codigo que se quiere probar.
     */
    private static boolean operacionInestable() {
        return ThreadLocalRandom.current().nextInt(100) >= 15;
    }

    /**
     * ASI SE VERIA LA PRUEBA INESTABLE ESCRITA DE VERDAD.
     *
     * Esta deshabilitada porque, si no, este taller tendria una probabilidad
     * del 15% de fallar en cada ejecucion. Quitele el @Disabled y ejecute la
     * suite cinco o seis veces seguidas para verlo: esa experiencia (verde,
     * verde, rojo, verde) es exactamente la que erosiona la confianza de un
     * equipo.
     */
    @Test
    @Disabled("Inestable a proposito. Quite esta linea y ejecute la suite varias veces.")
    @DisplayName("EJEMPLO - Prueba inestable (deshabilitada a proposito)")
    void ejemploDePruebaInestable() {
        assertTrue(operacionInestable(), "Fallo esta vez. Vuelva a ejecutar y pasara.");
    }

    @Test
    @DisplayName("01 - El detector mide la tasa real de fallo en vez de suponerla")
    void elDetectorMideLaTasaDeFallo() {
        // La forma correcta de tratar una prueba sospechosa no es relanzarla
        // hasta que pase: es ejecutarla MUCHAS veces y medir.
        final int ejecuciones = 2000;
        int fallos = 0;

        for (int i = 0; i < ejecuciones; i++) {
            if (!operacionInestable()) {
                fallos++;
            }
        }

        double tasa = (double) fallos / ejecuciones;
        System.out.printf("  Tasa de fallo medida: %.1f%% (%d de %d)%n",
                tasa * 100, fallos, ejecuciones);

        // Con 2000 ejecuciones la estimacion es estable. La banda es ancha a
        // proposito: esta prueba mide una cosa aleatoria y no debe convertirse
        // ella misma en inestable, que seria irónico y bastante comun.
        assertTrue(tasa > 0.10 && tasa < 0.20,
                "La tasa medida fue " + tasa + ", fuera de la banda esperada");

        // Una tasa del 15% en una suite de 500 pruebas significa que el build
        // completo falla practicamente siempre por motivos que no existen.
    }

    @Test
    @DisplayName("02 - Por que reintentar esconde el problema en vez de resolverlo")
    void porQueReintentarEsUnaTrampa() {
        // Con una tasa de fallo p, la probabilidad de que una prueba pase
        // despues de n reintentos es 1 - p^n.
        double p = 0.15;

        double conUnIntento = 1 - p;
        double conDosIntentos = 1 - Math.pow(p, 2);
        double conTresIntentos = 1 - Math.pow(p, 3);

        System.out.printf("  1 intento : pasa el %.1f%% de las veces%n", conUnIntento * 100);
        System.out.printf("  2 intentos: pasa el %.2f%% de las veces%n", conDosIntentos * 100);
        System.out.printf("  3 intentos: pasa el %.2f%% de las veces%n", conTresIntentos * 100);

        assertTrue(conTresIntentos > 0.996,
                "Con tres reintentos el problema se vuelve practicamente invisible");

        // Y AHI ESTA LA TRAMPA: con tres reintentos, el 99.6% de las veces el
        // semaforo esta verde y nadie recuerda que la prueba era inestable.
        // Pero el defecto de la prueba sigue exactamente igual, y la misma
        // configuracion tapa tambien los fallos REALES intermitentes: una
        // condicion de carrera de verdad en el codigo de produccion pasaria
        // los reintentos igual de bien.
    }

    @Test
    @DisplayName("03 - Una prueba estable da el mismo resultado siempre")
    void unaPruebaEstableDaSiempreLoMismo() {
        // El contraste. La suite buena no depende del reloj, ni del azar, ni
        // del orden: mil ejecuciones, mil veces el mismo resultado.
        Person menor = new Person("Sara", 500, 17, Gender.FEMALE, true);

        for (int i = 0; i < 1000; i++) {
            assertEquals(RegisterResult.UNDERAGE, new Registry().registerVoter(menor));
        }

        // Tres preguntas para auditar cualquier prueba sospechosa:
        //   1. Depende de la hora, la fecha o una zona horaria?
        //   2. Depende de un numero aleatorio, un hash o el orden de un HashMap?
        //   3. Depende de que otra prueba se haya ejecutado antes?
        // Si alguna respuesta es "si", la prueba es inestable aunque hoy pase.
        assertTrue(LocalTime.now().getHour() >= 0);   // trivialmente cierto: ver abajo
    }

    /**
     * NOTA sobre la ultima linea de la prueba 03: es un ejemplo de dependencia
     * del reloj que NO produce inestabilidad, porque la condicion se cumple a
     * cualquier hora. La dependencia del reloj solo es un problema cuando el
     * resultado cambia con ella: una prueba que compare con "hoy" fallara a
     * medianoche, y una que use una zona horaria implicita fallara en marzo,
     * cuando cambie el horario de verano en otro pais.
     */
}
