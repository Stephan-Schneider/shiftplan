package shiftplan.publish;

import org.apache.commons.mail.EmailException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EmailDispatchTest {

    @Test
    void sendMail() throws EmailException, IOException {
        EmailDispatch dispatch = new EmailDispatch(null);
        //dispatch.sendMail();
    }
}