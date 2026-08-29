package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.Gender;
import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LA SUITE BUENA.
 *
 * Siete pruebas, una por regla de negocio, cada una sobre un valor frontera.
 * Rapidas, independientes y con una asercion que de verdad discrimina.
 *
 * Es el punto de comparacion del taller: mide su coste y su valor, y despues
 * compare con las seis patologias del paquete `patologias`.
 *
 * Adelanto del resultado, medido: estas siete pruebas matan practicamente
 * todos los mutantes. Las patologias, que son mas numerosas y mucho mas
 * lentas, no matan ni uno mas.
 */
@DisplayName("Reglas de inscripcion (suite de referencia)")
class RegistryTest {

    private Registry registry;

    @BeforeEach
    void setUp() {
        registry = new Registry();
    }

    private static Person persona(int id, int edad, boolean viva) {
        return new Person("Nombre" + id, id, edad, Gender.FEMALE, viva);
    }

    @Test
    @DisplayName("Una persona adulta y viva queda inscrita")
    void personaValidaQuedaInscrita() {
        assertEquals(RegisterResult.VALID, registry.registerVoter(persona(1, 30, true)));
    }

    @Test
    @DisplayName("Una persona no viva es rechazada")
    void personaNoVivaEsRechazada() {
        assertEquals(RegisterResult.DEAD, registry.registerVoter(persona(2, 30, false)));
    }

    @Test
    @DisplayName("17 anios es menor de edad; 18 no (valor frontera)")
    void fronteraDeLaMayoriaDeEdad() {
        assertEquals(RegisterResult.UNDERAGE, registry.registerVoter(persona(3, 17, true)));
        assertEquals(RegisterResult.VALID, registry.registerVoter(persona(4, 18, true)));
    }

    @Test
    @DisplayName("Una edad negativa o mayor que 120 es invalida (valores frontera)")
    void fronteraDeLaEdadValida() {
        assertEquals(RegisterResult.INVALID_AGE, registry.registerVoter(persona(5, -1, true)));
        assertEquals(RegisterResult.INVALID_AGE, registry.registerVoter(persona(6, 121, true)));
        assertEquals(RegisterResult.VALID, registry.registerVoter(persona(7, 120, true)));
    }

    @Test
    @DisplayName("Un id menor o igual que cero es invalido (valor frontera)")
    void fronteraDelIdentificador() {
        assertEquals(RegisterResult.INVALID, registry.registerVoter(persona(0, 30, true)));
        assertEquals(RegisterResult.INVALID, registry.registerVoter(persona(-5, 30, true)));
        assertEquals(RegisterResult.VALID, registry.registerVoter(persona(1, 30, true)));
    }

    @Test
    @DisplayName("El segundo intento con el mismo id se rechaza")
    void elSegundoIntentoConElMismoIdSeRechaza() {
        assertEquals(RegisterResult.VALID, registry.registerVoter(persona(8, 30, true)));
        assertEquals(RegisterResult.DUPLICATED, registry.registerVoter(persona(8, 45, true)));
        assertEquals(1, registry.totalInscritos());
    }

    @Test
    @DisplayName("Una persona nula es invalida")
    void personaNulaEsInvalida() {
        assertEquals(RegisterResult.INVALID, registry.registerVoter(null));
    }
}
