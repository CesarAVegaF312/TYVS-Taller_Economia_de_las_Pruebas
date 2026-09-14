# Registro de Defectos DE LA SUITE (plantilla)

Ojo con lo que se documenta aqui: en este taller el defecto NO esta en el
codigo de produccion. Esta en la SUITE.

Una prueba defectuosa es la que cuesta mas de lo que aporta. Y "aportar" tiene
una definicion operativa: matar mutantes. Si al borrar una prueba el mutation
score no baja, esa prueba no estaba protegiendo nada.

---

## Formato

### Defecto XX — <clase#metodo>

- **Patologia**: Sin aserciones | Aserciones decorativas | Prueba la libreria |
  Lenta sin motivo | Redundante | Se adapta al codigo | Acoplada a la
  implementacion | Inestable
- **Ubicacion**: archivo y linea
- **Coste medido**: segundos y porcentaje del tiempo total de la suite.
  Del informe del modulo 1, no a ojo.
- **Valor medido**: mutantes que mata en exclusiva. Se obtiene ejecutando PIT
  con y sin ella. Si no lo midio, escriba "no medido" en vez de suponerlo.
- **Que pasaria si el codigo se rompiera**: sea concreto. "Si registerVoter
  devolviera siempre INVALID, esta prueba seguiria verde" es un argumento;
  "no prueba bien" no lo es.
- **Decision**: Borrar | Arreglar | Conservar
- **Justificacion**: en terminos de coste y valor, no de intuicion.
- **Evidencia**: mutation score antes y despues.
- **Estado**: Abierto | Aplicado

---

## Tabla de seguimiento

| ID | Prueba | Patologia | Coste | Valor | Decision | Estado |
|----|--------|-----------|-------|-------|----------|--------|
| SU-01 | ... | ... | ... | ... | ... | Abierto |

---

## Un hallazgo bien redactado

> **Mal:** "Esta prueba no sirve para nada."
>
> **Bien:** "PatologiasTest#pruebaLenta consume 1.573 s, el 89.7% del tiempo total de
> la suite, por cinco Thread.sleep(300) sobre un metodo sincrono. Ejecutando
> PIT con y sin ella el mutation score no cambia (23/25 en los dos casos): no
> mata ningun mutante en exclusiva. Decision: borrar. La suite pasa de 1.754 s
> a 0.181 s sin perder proteccion."

La diferencia es que el segundo se puede defender en una reunion.
