package edu.unisabana.tyvs.suite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MODULO 2 — VALOR CONTRA COSTE
 *
 * El experimento central del taller. Se ejecuta PIT dos veces sobre el MISMO
 * codigo de produccion, cambiando solo que pruebas participan:
 *
 *   mvn test-compile org.pitest:pitest-maven:mutationCoverage
 *   mvn -Psolo-buenas test-compile org.pitest:pitest-maven:mutationCoverage
 *
 * RESULTADO MEDIDO (2026-08-29, 25 mutantes generados):
 *
 *   Suite completa (13 pruebas)   -> 23 mutantes muertos   92%
 *   Solo RegistryTest (7 pruebas) ->  22 mutantes muertos   88%
 *
 * Merece la pena detenerse en esto, porque el resultado NO es el que se
 * esperaba al escribir el taller.
 *
 * La hipotesis era que las patologias no aportarian NADA. Es falso: aportan
 * un mutante. Y ahi esta lo interesante, porque la pregunta economica real
 * casi nunca es "sirve o no sirve", sino "cuanto aporta y a que precio".
 *
 * CUAL ES ESE MUTANTE. Se identifico comparando los supervivientes de las dos
 * ejecuciones:
 *
 *   Solo RegistryTest sobreviven 3:
 *     - Registry::registerVoter    changed conditional boundary  (linea 43)
 *     - Person::getGender          replaced return value with null   NO_COVERAGE
 *     - Person::getName            replaced return value with ""     NO_COVERAGE
 *
 *   Con todas sobreviven 2: los mismos, menos Person::getGender.
 *
 * Es decir: la aportacion completa de las seis patologias es matar UN mutante
 * en el getter de un objeto de datos. No en una regla de negocio: en un
 * getter. Y lo mata `pruebaDeLaLibreria`, que es precisamente la patologia
 * "probar la libreria y no el codigo propio".
 *
 * LA CUENTA, que es de lo que trata el taller:
 *
 *   Coste  : el paquete `patologias` se lleva alrededor del 60% del tiempo
 *            total de la suite (mirelo en el informe del modulo 1).
 *   Valor  : +1 mutante de 25, en un getter.
 *
 * Sesenta por ciento del presupuesto por el cuatro por ciento del resultado,
 * y encima en la parte del codigo donde menos importa. Un equipo que borre
 * ese paquete entero pierde muy poco y gana mucho. Pero notese que la
 * decision correcta NO es "las patologias son inutiles": es "cuestan
 * demasiado para lo que dan". Es una decision de inversion, no de moral.
 */
@DisplayName("Modulo 2 - Valor contra coste de la suite")
class ValorVsCosteTest {

    // ---- Numeros medidos, no inventados -----------------------------------
    private static final int MUTANTES_GENERADOS = 25;
    private static final int MUERTOS_SUITE_COMPLETA = 23;
    private static final int MUERTOS_SOLO_BUENAS = 22;

    /**
     * Proporcion del tiempo total que consume el paquete `patologias`.
     *
     * Medida entre el 60% y el 93% segun la ejecucion. La variacion no es
     * ruido de medicion: las pruebas rapidas tardan mas la primera vez por el
     * calentamiento de la JVM, asi que su peso relativo cambia. Se toma el
     * extremo CONSERVADOR (60%) para que la conclusion no dependa de haber
     * elegido la ejecucion que mas favorecia al argumento.
     *
     * Ejecute `mvn verify` y lea su propio numero al final de la salida.
     */
    private static final double FRACCION_DE_COSTE_PATOLOGIAS = 0.60;

    @Test
    @DisplayName("01 - La aportacion marginal de las patologias es de un mutante")
    void laAportacionMarginalEsDeUnMutante() {
        int aportacion = MUERTOS_SUITE_COMPLETA - MUERTOS_SOLO_BUENAS;

        assertEquals(1, aportacion,
                "Las seis patologias juntas matan " + aportacion + " mutante(s) mas");

        double scoreCompleto = (double) MUERTOS_SUITE_COMPLETA / MUTANTES_GENERADOS;
        double scoreSoloBuenas = (double) MUERTOS_SOLO_BUENAS / MUTANTES_GENERADOS;

        System.out.printf("  Suite completa   : %d/%d = %.0f%%%n",
                MUERTOS_SUITE_COMPLETA, MUTANTES_GENERADOS, scoreCompleto * 100);
        System.out.printf("  Solo RegistryTest: %d/%d = %.0f%%%n",
                MUERTOS_SOLO_BUENAS, MUTANTES_GENERADOS, scoreSoloBuenas * 100);
        System.out.printf("  Aportacion de las patologias: +%.0f puntos%n",
                (scoreCompleto - scoreSoloBuenas) * 100);

        assertTrue(scoreCompleto - scoreSoloBuenas < 0.05,
                "La aportacion es menor de cinco puntos porcentuales");
    }

    @Test
    @DisplayName("02 - Coste por mutante: las patologias son 20 veces mas caras")
    void elCostePorMutanteEsVeinteVecesMayor() {
        // Se normaliza el tiempo total a 1.0 para que la cuenta no dependa de
        // lo rapida que sea esta maquina.
        double costePatologias = FRACCION_DE_COSTE_PATOLOGIAS;
        double costeSuiteBuena = 1.0 - FRACCION_DE_COSTE_PATOLOGIAS;

        int mutantesDeLasPatologias = MUERTOS_SUITE_COMPLETA - MUERTOS_SOLO_BUENAS;  // 1
        int mutantesDeLaSuiteBuena = MUERTOS_SOLO_BUENAS;                            // 22

        double costePorMutantePatologias = costePatologias / mutantesDeLasPatologias;
        double costePorMutanteBuenas = costeSuiteBuena / mutantesDeLaSuiteBuena;

        System.out.printf("  Coste por mutante, suite buena: %.4f%n", costePorMutanteBuenas);
        System.out.printf("  Coste por mutante, patologias : %.4f%n", costePorMutantePatologias);
        System.out.printf("  Relacion: %.0fx mas caro%n",
                costePorMutantePatologias / costePorMutanteBuenas);

        assertTrue(costePorMutantePatologias > costePorMutanteBuenas * 20,
                "Cada mutante que aportan las patologias cuesta al menos veinte veces mas");

        // Esta es la metrica que casi nadie calcula y que convierte una
        // discusion de opiniones ("yo creo que esa prueba sirve") en una
        // decision con numeros.
    }

    @Test
    @DisplayName("03 - La cobertura no distingue una suite buena de una inutil")
    void laCoberturaNoDistingue() {
        // Las patologias EJECUTAN el codigo: suben la cobertura de linea igual
        // que cualquier prueba de verdad, porque JaCoCo cuenta lineas
        // ejecutadas y no le importa si alguien miro el resultado.
        //
        // Compruebelo usted mismo abriendo target/site/jacoco/index.html
        // despues de `mvn verify`.
        //
        // La mutacion, en cambio, si distingue: una prueba sin aserciones no
        // puede matar un mutante, porque matar un mutante significa
        // exactamente "alguna asercion se dio cuenta del cambio".
        //
        // De ahi la regla practica del taller:
        //
        //   La cobertura dice que codigo se EJECUTO.
        //   La mutacion dice que codigo esta PROTEGIDO.
        //   Solo la segunda es un argumento para conservar una prueba.
        //
        // Una prueba sin aserciones no puede, por construccion, matar ningun
        // mutante. Es la forma corta de decir que no protege nada.
        assertTrue(MUERTOS_SUITE_COMPLETA < MUTANTES_GENERADOS,
                "Quedan mutantes vivos: la suite no lo cubre todo pese a la cobertura alta");
    }

    @Test
    @DisplayName("04 - EJERCICIO: queda un mutante vivo en una regla de negocio")
    void quedaUnMutanteVivoEnUnaReglaDeNegocio() {
        // De los dos supervivientes finales, uno esta en un getter (da igual)
        // y el otro esta en la logica:
        //
        //   Registry.java:43   if (p.getAge() < 0 || p.getAge() > EDAD_MAXIMA)
        //   mutador: ConditionalsBoundary   ->   convierte `< 0` en `<= 0`
        //
        // Con esa mutacion, una persona de edad 0 devolveria INVALID_AGE en
        // vez de UNDERAGE. Ninguna prueba se entera, porque NINGUNA usa la
        // edad 0.
        //
        // Es el valor frontera que se olvida siempre: se prueba -1 (invalida)
        // y se prueba 17 (menor), y se salta el 0, que esta justo en el borde
        // entre las dos reglas.
        //
        // ESTE ES EL EJERCICIO DEL MODULO 2. Anada la prueba que mata este
        // mutante, vuelva a ejecutar PIT y compruebe que el score sube. Se
        // deja sin hacer a proposito: encontrarlo leyendo el informe de PIT es
        // la habilidad que se entrena.
        //
        // Fijese en el contraste: 23 pruebas en verde, cobertura alta, y una
        // regla de negocio sin proteger. Ninguna metrica de las habituales lo
        // habria dicho.
        assertEquals(2, MUTANTES_GENERADOS - MUERTOS_SUITE_COMPLETA,
                "Deberian quedar dos supervivientes: un getter y la frontera de edad");
    }
}
