package shiftplan.publish;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SMTPConfigTest {

    @Test
    void testGetConfigParams() throws IOException {

        SMTPConfig cfg = SMTPConfig.getConfigParams();

        assertAll("SMTPConfig wird getestet",
                () -> assertEquals("mail.gmx.net", cfg.getSmtpServer()),
                () -> assertEquals(587, cfg.getSmtpPort()),
                () -> assertTrue(cfg.isStartTLSEnabled()),
                () -> assertEquals("stephan.geert.schneider@gmx.de", cfg.getUserId()),
                () -> assertEquals("Stephan Schneider", cfg.getUserName()),
                () -> assertEquals("Schichtplan für 2023", cfg.getSubject()),
                () -> assertEquals("Hallo zusammen,<br><br>im Anhang ist der neue Schichtplan.", cfg.getMessage())
        );
    }

}