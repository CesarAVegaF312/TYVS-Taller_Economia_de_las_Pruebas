package edu.unisabana.tyvs.domain.model;

/**
 * Resultado posible de un intento de registro de votante.
 *
 * Cada constante corresponde a una clase de equivalencia del dominio:
 * ver la matriz de clases de equivalencia y valores límite en el README.
 */
public enum RegisterResult {
    /** Persona viva, mayor de edad, id válido y no registrada previamente. */
    VALID,
    /** El id ya fue registrado antes. */
    DUPLICATED,
    /** Persona nula o con id inválido (id <= 0). */
    INVALID,
    /** La persona no está viva. */
    DEAD,
    /** Edad dentro del rango 0..17. */
    UNDERAGE,
    /** Edad fuera del rango biológicamente posible (< 0 o > 120). */
    INVALID_AGE
}
