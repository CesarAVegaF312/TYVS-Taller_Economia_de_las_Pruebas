package edu.unisabana.tyvs.suite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * MODULO 1 — MEDIR EL COSTE REAL DE LA SUITE
 *
 * La pregunta de la que parte el taller: cuanto le cuesta a usted su suite de
 * pruebas?
 *
 * Casi nadie lo sabe. Se sabe el porcentaje de cobertura, que es una metrica
 * del lado del valor (y mala), y no se sabe nada del lado del coste. Y una
 * suite tiene coste: tiempo de ejecucion en cada commit, tiempo de
 * mantenimiento cada vez que cambia el codigo, y confianza perdida cada vez
 * que falla sin motivo.
 *
 * Este analizador lee los informes que Surefire ya genera en cada ejecucion
 * (target/surefire-reports/TEST-*.xml) y produce el dato que falta: cuanto
 * tarda cada clase y cada prueba, ordenado de mayor a menor.
 *
 * No hace falta ninguna herramienta nueva. El dato ya estaba ahi; lo que no
 * habia era la costumbre de mirarlo.
 *
 * Ejecucion:
 *   mvn test
 *   mvn exec:java -Dexec.mainClass=edu.unisabana.tyvs.suite.AnalizadorDeSuite
 */
public final class AnalizadorDeSuite {

    /** Una prueba con su tiempo. */
    public static final class Prueba {
        public final String clase;
        public final String nombre;
        public final double segundos;

        public Prueba(String clase, String nombre, double segundos) {
            this.clase = clase;
            this.nombre = nombre;
            this.segundos = segundos;
        }

        public String nombreCorto() {
            int i = clase.lastIndexOf('.');
            return (i < 0 ? clase : clase.substring(i + 1)) + "#" + nombre;
        }
    }

    private static final Pattern CASO = Pattern.compile(
            "<testcase[^>]*?name=\"([^\"]+)\"[^>]*?classname=\"([^\"]+)\"[^>]*?time=\"([^\"]+)\"");

    private AnalizadorDeSuite() {
    }

    /** Lee todos los TEST-*.xml de un directorio de informes de Surefire. */
    public static List<Prueba> leerInformes(Path directorio) throws IOException {
        List<Prueba> pruebas = new ArrayList<>();

        if (!Files.isDirectory(directorio)) {
            return pruebas;
        }

        try (Stream<Path> archivos = Files.list(directorio)) {
            List<Path> xml = archivos
                    .filter(p -> p.getFileName().toString().startsWith("TEST-"))
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .toList();

            for (Path archivo : xml) {
                pruebas.addAll(analizar(
                        new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8)));
            }
        }
        return pruebas;
    }

    /**
     * Extrae los casos de un XML de Surefire.
     *
     * Se usa una expresion regular y no un parser de XML a proposito: el
     * formato es fijo, la dependencia es cero y el codigo cabe en una
     * pantalla. Para un informe de Surefire es una decision razonable; para
     * XML arbitrario seria un error.
     */
    public static List<Prueba> analizar(String xml) {
        List<Prueba> pruebas = new ArrayList<>();
        Matcher m = CASO.matcher(xml);

        while (m.find()) {
            pruebas.add(new Prueba(m.group(2), m.group(1), Double.parseDouble(m.group(3))));
        }
        return pruebas;
    }

    /** Suma de tiempos de una lista. */
    public static double total(List<Prueba> pruebas) {
        return pruebas.stream().mapToDouble(p -> p.segundos).sum();
    }

    /**
     * Genera el informe de coste.
     *
     * La columna importante es la ultima: que porcentaje del tiempo total
     * consume cada prueba. Es la que convierte "la suite tarda un rato" en
     * "tres pruebas se llevan la mitad del tiempo", que ya es accionable.
     */
    public static String informe(List<Prueba> pruebas) {
        if (pruebas.isEmpty()) {
            return "No hay informes de Surefire. Ejecute `mvn test` primero.";
        }

        double total = total(pruebas);
        StringBuilder sb = new StringBuilder();

        sb.append(String.format(Locale.ROOT, "%n  COSTE DE LA SUITE: %d pruebas, %.3f s en total%n", pruebas.size(), total));
        sb.append("  ").append("-".repeat(74)).append(System.lineSeparator());
        sb.append(String.format(Locale.ROOT, "  %-52s %9s %10s%n", "PRUEBA", "SEGUNDOS", "% DEL TOTAL"));
        sb.append("  ").append("-".repeat(74)).append(System.lineSeparator());

        pruebas.stream()
                .sorted(Comparator.comparingDouble((Prueba p) -> p.segundos).reversed())
                .forEach(p -> sb.append(String.format(Locale.ROOT, "  %-52s %9.3f %9.1f%%%n",
                        p.nombreCorto(), p.segundos,
                        total == 0 ? 0 : p.segundos / total * 100)));

        // El corte que de verdad interesa: cuanto cuesta el paquete de
        // patologias frente al resto.
        double patologias = total(pruebas.stream()
                .filter(p -> p.clase.contains(".patologias."))
                .toList());

        if (patologias > 0) {
            sb.append("  ").append("-".repeat(74)).append(System.lineSeparator());
            sb.append(String.format(Locale.ROOT, "  Paquete `patologias`: %.3f s  (%.1f%% del tiempo total)%n",
                    patologias, patologias / total * 100));
            sb.append(String.format(Locale.ROOT, "  Resto de la suite   : %.3f s  (%.1f%%)%n",
                    total - patologias, (total - patologias) / total * 100));
            sb.append(System.lineSeparator());
            sb.append("  Compare ese porcentaje con lo que aportan al mutation score.").append(System.lineSeparator());
        }

        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        Path directorio = Paths.get(
                args.length > 0 ? args[0] : "target/surefire-reports");

        System.out.println(informe(leerInformes(directorio)));
    }
}
