# Taller de Economía del Suite

Todos los talleres anteriores enseñan a **escribir** pruebas. Este enseña a
**borrarlas**, que es mucho más difícil y casi nadie lo entrena.

La pregunta de partida: **¿cuánto le cuesta a usted su suite de pruebas?**

Casi nadie lo sabe. Se sabe el porcentaje de cobertura —que es una métrica del
lado del valor, y mala— y no se sabe nada del lado del coste. Y una suite
cuesta: tiempo de ejecución en cada *commit*, tiempo de mantenimiento cada vez
que cambia el código, y **confianza perdida** cada vez que falla sin motivo.

Una suite es una inversión. Como toda inversión, puede tener partes que no
rinden, y mantenerlas cuesta dinero.

---

## 🎯 Objetivos

- **Medir el coste real** de una suite, prueba por prueba, con los datos que
  Maven ya genera y nadie mira.
- Distinguir **cobertura** (qué código se ejecutó) de **mutación** (qué código
  está protegido), y saber por qué solo la segunda justifica conservar una
  prueba.
- Reconocer las **seis patologías** que pasan siempre, suben la cobertura y no
  detectarían un defecto.
- Entender por qué una **prueba inestable** es el defecto más caro de una
  suite, y por qué reintentar empeora el problema.
- Tomar y **defender con números** la decisión de borrar pruebas.

---

## Sistema bajo prueba

La `registraduria` de los talleres anteriores, con las reglas ya implementadas.
**El código de producción no es el objeto de estudio: lo es la suite que lo
prueba.**

```text
.
├─ registraduria/
│   ├─ src/main/java/.../
│   │   ├─ domain/                    # las reglas, ya completas
│   │   └─ suite/AnalizadorDeSuite.java   # la herramienta del módulo 1
│   └─ src/test/java/.../
│       ├─ domain/service/RegistryTest.java    # LA SUITE BUENA (7 pruebas)
│       ├─ patologias/                         # LO QUE SE ESTUDIA
│       │   ├─ PatologiasTest.java             # 6 patologías, todas verdes
│       │   └─ InestabilidadTest.java          # módulo 3
│       └─ suite/                              # módulos 1 y 2
└─ defectos_template.md
```

---

## Puesta en marcha

```bash
cd registraduria
mvn clean verify
```

**27 pruebas** en verde (1 omitida a propósito), y al final de la salida el
**informe de coste**: cada prueba, su tiempo y su porcentaje del total.

Después, el experimento del módulo 2:

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
mvn -Psolo-buenas test-compile org.pitest:pitest-maven:mutationCoverage
```

---

## Módulos del taller

### Módulo 1 — Medir el coste ([`AnalizadorDeSuite`](registraduria/src/main/java/edu/unisabana/tyvs/suite/AnalizadorDeSuite.java))

Surefire ya escribe el tiempo de cada prueba en
`target/surefire-reports/TEST-*.xml`. **El dato siempre estuvo ahí; lo que
faltaba era la costumbre de mirarlo.** No hace falta ninguna herramienta nueva:
150 líneas leen esos XML y producen el ranking.

Salida real de una ejecución:

```text
  COSTE DE LA SUITE: 27 pruebas, 1.754 s en total
  PRUEBA                                                SEGUNDOS % DEL TOTAL
  PatologiasTest#pruebaLenta                               1.573      89.7%
  PatologiasTest#pruebaSinAserciones                       0.156       5.3%
  ...
  RegistryTest#personaValidaQuedaInscrita                  0.001       0.0%
  --------------------------------------------------------------------------
  Paquete `patologias`: 1.624 s  (92.6% del tiempo total)
  Resto de la suite   : 0.130 s  (7.4%)
```

Eso convierte *"la suite tarda un rato"* en *"una prueba se lleva el 90% del
tiempo"*, que ya es accionable.

> 💡 El analizador se prueba a sí mismo contra un XML fijo escrito en el test,
> **no** contra los informes reales. Probarlo contra los reales lo haría
> depender de lo rápida que sea la máquina — la patología del módulo 3, y
> habría sido fácil caer en ella.
>
> De hecho, esa prueba **encontró un defecto real**: el analizador usaba
> `String.format` sin `Locale` y escribía `83,1%` en una máquina en español y
> `83.1%` en una en inglés. El informe cambiaba según el idioma del sistema
> operativo. Se arregló forzando `Locale.ROOT` — la corrección correcta era en
> el código, no relajar la aserción.

### Módulo 2 — Valor contra coste ([`ValorVsCosteTest`](registraduria/src/test/java/edu/unisabana/tyvs/suite/ValorVsCosteTest.java))

El experimento central. Se ejecuta PIT dos veces sobre **el mismo código de
producción**, cambiando solo qué pruebas participan.

**Resultado medido, 25 mutantes generados:**

| Suite | Pruebas | Mutantes muertos | Score |
|---|---|---|---|
| Completa | 13 | 23 | **92%** |
| Solo `RegistryTest` | 7 | 22 | **88%** |

> ⚠️ **El resultado no fue el esperado, y por eso es mejor.** La hipótesis al
> escribir el taller era que las patologías no aportarían **nada**. Es falso:
> aportan un mutante. Y ahí está lo interesante, porque la pregunta económica
> real casi nunca es *"¿sirve o no sirve?"* sino **"¿cuánto aporta y a qué
> precio?"**.

Comparando los supervivientes de las dos ejecuciones se identifica exactamente
qué aportan:

```text
Solo RegistryTest, sobreviven 3:
  Registry::registerVoter   changed conditional boundary  (línea 43)
  Person::getGender         replaced return value with null    NO_COVERAGE
  Person::getName           replaced return value with ""      NO_COVERAGE

Con todas las pruebas, sobreviven 2: los mismos, menos Person::getGender.
```

**La aportación completa de las seis patologías es matar un mutante en el
_getter_ de un objeto de datos.** No en una regla de negocio: en un getter. Y
lo mata `pruebaDeLaLibreria`, que es justamente la patología "probar la
librería en vez del código propio".

La cuenta:

| | Coste (% del tiempo) | Valor (mutantes) | Coste por mutante |
|---|---|---|---|
| `RegistryTest` | ~7–40% | 22 | 0.018 |
| `patologias` | ~60–93% | 1 | 0.600 |

**Cada mutante que aportan las patologías cuesta más de 30 veces lo que cuesta
un mutante de la suite buena.** Esa es la métrica que casi nadie calcula, y es
la que convierte una discusión de opiniones (*"yo creo que esa prueba sirve"*)
en una decisión con números.

> La conclusión correcta **no** es "las patologías son inútiles". Es "cuestan
> demasiado para lo que dan". Es una decisión de inversión, no de moral.

**Y queda un mutante vivo en una regla de negocio** — es el ejercicio:

```java
Registry.java:43   if (p.getAge() < 0 || p.getAge() > EDAD_MAXIMA)
mutador: ConditionalsBoundary  ->  convierte `< 0` en `<= 0`
```

Con esa mutación, una persona de **edad 0** devolvería `INVALID_AGE` en vez de
`UNDERAGE`. Ninguna prueba se entera porque **ninguna usa la edad 0**: se
prueba el −1 (inválida) y el 17 (menor), y se salta el 0, que está justo en el
borde entre las dos reglas.

27 pruebas en verde, cobertura alta, y una regla de negocio sin proteger.
Ninguna métrica de las habituales lo habría dicho.

### Módulo 3 — Las seis patologías ([`PatologiasTest`](registraduria/src/test/java/edu/unisabana/tyvs/patologias/PatologiasTest.java))

Todas pasan. Todas suben la cobertura. Ninguna detectaría un defecto.

| # | Patología | Por qué existe en proyectos reales |
|---|---|---|
| 1 | **Sin aserciones** | Sube la cobertura igual que una prueba de verdad: JaCoCo cuenta líneas ejecutadas, no resultados comprobados. |
| 1b | **Aserciones decorativas** | `assertNotNull` sobre algo que nunca es nulo. Parece una prueba en regla y no puede fallar. |
| 2 | **Prueba la librería** | Los POJO son la forma más barata de subir el porcentaje cuando hay un objetivo de cobertura obligatorio. |
| 3 | **Lenta sin motivo** | El `sleep` se puso para "estabilizar" algo que fallaba, y nadie se atreve a quitarlo. |
| 4 | **Redundante** | Ocho aserciones sobre el mismo camino. El coste no es el tiempo: es que el día que cambie la regla hay ocho sitios que tocar. |
| 5 | **Se adapta al código** | Pregunta al sistema y afirma lo que le contesta. **Es imposible que falle.** Nace de "arreglar" una prueba copiándole el valor que devolvía el código. |
| 6 | **Acoplada a la implementación** | Se rompe cuando alguien añade un valor al enum, que no rompe a nadie. Es la razón por la que un equipo empieza a temerle a refactorizar. |

> **Regla práctica**: si al borrar una prueba el *mutation score* no baja, esa
> prueba era redundante.

### Módulo 4 — Pruebas inestables ([`InestabilidadTest`](registraduria/src/test/java/edu/unisabana/tyvs/patologias/InestabilidadTest.java))

Una prueba inestable es la que a veces pasa y a veces falla sin que el código
cambie. Es el defecto más caro de una suite, y no por el tiempo perdido:
**destruye lo único que hace útil a una suite, que es que se le crea.**

La secuencia ocurre en semanas:

1. Una prueba falla de vez en cuando.
2. Alguien la relanza y pasa. *"Nada, cosas del CI."*
3. Relanzar ante cualquier rojo se vuelve costumbre.
4. Un día una prueba detecta un defecto **real**…
5. …alguien relanza, pasa por casualidad, y el defecto llega a producción.

En el paso 5 la suite ya no vale nada, y llevaba semanas sin valer nada.

**Sobre los reintentos automáticos.** Casi todo el mundo llega a la misma
solución: configurar el CI para reintentar. Alivia el síntoma y empeora la
enfermedad, porque convierte el paso 3 en política de la empresa. La aritmética
está en la prueba 02: con una tasa de fallo del 15% y tres reintentos, el
**99.6%** de las veces el semáforo está verde y nadie recuerda que la prueba
era inestable. El defecto sigue igual — y la misma configuración tapa también
los fallos **reales** intermitentes, como una condición de carrera de verdad.

El archivo trae una prueba genuinamente inestable, **deshabilitada**. Quítele
el `@Disabled` y ejecute la suite cinco o seis veces: esa experiencia (verde,
verde, rojo, verde) es exactamente la que erosiona la confianza de un equipo.

Tres preguntas para auditar una prueba sospechosa:

1. ¿Depende de la hora, la fecha o una zona horaria?
2. ¿Depende de un número aleatorio, un *hash* o el orden de un `HashMap`?
3. ¿Depende de que otra prueba se haya ejecutado antes?

Si alguna respuesta es "sí", la prueba es inestable **aunque hoy pase**.

---

## PARA ENTREGAR CON ESTE TALLER

### 1) Repositorio

- `mvn clean verify` en verde tras un clon limpio.

### 2) Informe de coste de una suite real (módulo 1)

- Ejecute el analizador sobre **una suite de otro taller** (unitarias,
  integración, seguridad o IA) y pegue el ranking.
- Identifique el **20% de pruebas que consume más tiempo**.
- ¿Está justificado ese tiempo en cada caso? Algunas pruebas lentas lo son con
  razón (Testcontainers levanta un PostgreSQL). Distinguirlas es el trabajo.

### 3) Matar el mutante superviviente (módulo 2)

- Añada la prueba que mata el mutante de `Registry.java:43`.
- Vuelva a ejecutar PIT y **pegue el antes y el después**.
- ¿Cuánto tiempo añadió su prueba a la suite? Calcule su coste por mutante y
  compárelo con las patologías.

### 4) Auditoría de patologías (módulo 3)

- Busque en **otro taller** al menos **tres pruebas** con alguna de las seis
  patologías. Cítelas con archivo y línea.
- Para cada una: ¿la borraría, la arreglaría o la dejaría? Justifique con el
  criterio de coste/valor, no con intuición.
- **Borre una de verdad**, mida el *mutation score* antes y después, y muestre
  que no bajó.

### 5) Una prueba inestable (módulo 4)

- Escriba una prueba inestable **realista** (dependiente del reloj, del orden o
  de la concurrencia), distinta de la del taller.
- Mida su tasa de fallo empíricamente con un detector como el del módulo 4.
- Arréglela. Explique cuál era la causa raíz y por qué su arreglo la elimina en
  vez de esconderla.

### 6) Política de suite (en el Wiki)

Escriba la política que propondría a su equipo. Debe contestar, con criterios
verificables y no con buenas intenciones:

- ¿Cuál es el tiempo máximo aceptable para la suite completa?
- ¿Qué se hace cuando una prueba se detecta como inestable? (Con plazo.)
- ¿Bajo qué condición se borra una prueba, y quién lo autoriza?
- ¿Qué métrica se exige: cobertura, mutación, las dos, ninguna? ¿Por qué?

---

## Rúbrica

| **Criterios de evaluación** | **Indicadores** | **Excelente (5 pts)** | **Bueno (4 pts)** | **Necesita mejorar (3.5 pts)** | **Deficiente (2.5 pts)** | **No cumple (0 pts)** |
|---|---|---|---|---|---|---|
| **Estructura y ejecución** | El proyecto corre con `mvn verify`. | Todo verde tras un clon limpio. | Corre con ajustes menores. | Requiere pasos no documentados. | Falla en varias pruebas. | No ejecuta. |
| **Informe de coste** | Medición sobre una suite real. | Ranking completo, 20% más caro identificado y justificado caso por caso. | Ranking correcto, análisis parcial. | Solo pega la salida. | Sin medición propia. | No entrega. |
| **Mutante superviviente** | Antes y después con evidencia. | Mutante muerto, score comparado, coste de la prueba calculado. | Mutante muerto sin análisis de coste. | Intento sin matarlo. | No ejecuta PIT. | No entrega. |
| **Auditoría de patologías** **(vale por 2)** | Hallazgos reales y decisión justificada. | 3+ patologías citadas con archivo y línea, decisión razonada, una borrada con evidencia de que el score no bajó. | 3 patologías identificadas, decisión sin evidencia. | 1–2 patologías, o mal clasificadas. | Cita ejemplos del propio taller. | No audita. |
| **Prueba inestable** **(vale por 2)** | Diagnóstico y corrección de causa raíz. | Prueba realista, tasa medida empíricamente, causa raíz identificada y eliminada. | Prueba correcta, arreglo sin analizar la causa. | La esconde con reintentos o `sleep`. | Copia el ejemplo del taller. | No entrega. |
| **Política de suite** | Criterios verificables. | Contesta las cuatro preguntas con umbrales y responsables concretos. | Contesta con criterios generales. | Declaración de intenciones sin umbrales. | Mínima. | Sin política. |
| **Documentación y reflexión** | Wiki con análisis. | Reflexión que trata la suite como una inversión, con números. | Clara pero sin profundidad. | Incompleta. | Mínima. | Sin documentación. |

> **Cómo suma**: 9 criterios × 5 pts = **45 puntos**. Son 7 filas, pero
> *Auditoría de patologías* y *Prueba inestable* valen por dos cada una — son
> el núcleo del taller.

| Rango de puntaje | Desempeño |
| ---------------- | --------- |
| 41 – 45 | Excelente dominio técnico y metodológico. |
| 32 – 40 | Buen trabajo con documentación o análisis parcial. |
| 27 – 31 | Cumple con lo básico pero sin profundidad. |
| < 27 | No cumple con los criterios mínimos del taller. |

---

## Créditos y uso académico

Material del curso **Testing y Validación de Software**, Maestría en
Ingeniería de Software, **Universidad de La Sabana**.

Las pruebas del paquete `patologias` son **defectuosas a propósito**. No las
copie a un proyecto real.
