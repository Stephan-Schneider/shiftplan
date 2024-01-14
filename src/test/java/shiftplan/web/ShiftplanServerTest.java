package shiftplan.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ShiftplanServerTest {

    private static final Path appBase = Path.of("/", "home", "stephan", "Apps", "Shiftplan");


    public static void main(String[] args) {
        new ConfigBundle.ConfigBuilder(
                appBase.resolve("generated_data").resolve("shiftplan_serialized.xml").toString(),
                appBase.resolve("XML").toString()
        )
                .templateDir(appBase.resolve("Template").toString())
                .pdfOutDir(appBase.resolve("generated_data").toString())
                .smtpConfigPath(appBase.resolve("mail_config.txt").toString())
                .build();

        ShiftplanServer.createServer(8080);
        System.out.println("Aufruf 'ShiftplanServer.createServer beendet");
    }

    @Test
    void createServer() {
        System.out.println("JUnit-Thead (in createServer): " + Thread.currentThread().getName());
        ShiftplanServer.createServer(8080);

    }
}