package shiftplan.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class ShiftplanServerTest {

    private static final Path appBase = Path.of("/", "home", "stephan", "tmp", "Web");


    public static void main(String[] args) {
        new ConfigBundle.ConfigBuilder(
                appBase.resolve("generated_data").resolve("shiftplan.json").toString(),
                appBase.resolve("generated_data").resolve("shiftplan_serialized.xml").toString(),
                appBase.resolve("XML").toString()
        )
                .templateDir(appBase.resolve("Template").toString())
                .generatedDataDir(appBase.resolve("generated_data").toString())
                .webResourcesBasePath("/home/stephan/tmp/dist")
                .build();

        ShiftplanServer.createServer("0.0.0.0", 8080);
        System.out.println("Aufruf 'ShiftplanServer.createServer beendet");
    }

    @Test
    void createServer() {
        System.out.println("JUnit-Thead (in createServer): " + Thread.currentThread().getName());
        ShiftplanServer.createServer("localhost", 8080);

    }
}