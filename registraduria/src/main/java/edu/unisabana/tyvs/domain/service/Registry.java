package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Reglas de inscripcion de votantes, YA IMPLEMENTADAS.
 *
 * A diferencia del taller de pruebas unitarias, donde esta clase es el
 * ejercicio, aqui es un dato del problema. Lo que se estudia en este taller no
 * es el codigo de produccion: es la SUITE que lo prueba.
 *
 * Se necesita una implementacion completa porque las pruebas de mutacion no
 * dicen nada sobre codigo a medio escribir: hacen falta ramas de verdad para
 * poder mutarlas.
 *
 * Orden de evaluacion (importa, y por eso esta escrito):
 *   1. persona nula        -> INVALID
 *   2. id <= 0             -> INVALID
 *   3. edad fuera de rango -> INVALID_AGE
 *   4. no viva             -> DEAD
 *   5. menor de edad       -> UNDERAGE
 *   6. id ya inscrito      -> DUPLICATED
 *   7. resto               -> VALID
 */
public class Registry {

    public static final int EDAD_MINIMA = 18;
    public static final int EDAD_MAXIMA = 120;

    private final Set<Integer> inscritos = new HashSet<>();

    public RegisterResult registerVoter(Person p) {
        if (p == null) {
            return RegisterResult.INVALID;
        }
        if (p.getId() <= 0) {
            return RegisterResult.INVALID;
        }
        if (p.getAge() < 0 || p.getAge() > EDAD_MAXIMA) {
            return RegisterResult.INVALID_AGE;
        }
        if (!p.isAlive()) {
            return RegisterResult.DEAD;
        }
        if (p.getAge() < EDAD_MINIMA) {
            return RegisterResult.UNDERAGE;
        }
        if (!inscritos.add(p.getId())) {
            return RegisterResult.DUPLICATED;
        }
        return RegisterResult.VALID;
    }

    /** Cuantas personas hay inscritas. */
    public int totalInscritos() {
        return inscritos.size();
    }
}
