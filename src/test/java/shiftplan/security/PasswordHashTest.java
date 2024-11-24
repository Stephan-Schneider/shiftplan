package shiftplan.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import shiftplan.web.PrefsIdentityManager;

import java.security.NoSuchAlgorithmException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHashTest {

    private String input;

    @ParameterizedTest
    @MethodSource("getValues")
    void toHexString(String stored, String provided) throws NoSuchAlgorithmException {
        stored = PasswordHash.toHexString(PasswordHash.getSHA(stored));
        provided = PasswordHash.toHexString(PasswordHash.getSHA(provided));

        assertEquals(stored, provided);
    }

    private static Stream<Arguments> getValues() {
        return Stream.of(
                Arguments.of("passwort_1", "passwort_1"),
                Arguments.of("töp!säcröt", "töp!säcröt"),
                Arguments.of("123456", "123456")
        );
    }

    @Test
    void whenStringValueOfThenOK() {
        final char[] chars = {'d', 'e', 'b', 'b', 'o', '_', '1', '5', '3'};
        String string = String.valueOf(chars);
        assertEquals("debbo_153", string);
    }

    @Test
    void createCredentials() throws NoSuchAlgorithmException, BackingStoreException {
        // Login-credentials für den Test-Server hinterlegen
        Preferences users = Preferences.userNodeForPackage(PrefsIdentityManager.class);
        String user = "Schicht-Exp";
        String passwd = "debbo_153";
        String passwd_hash = PasswordHash.toHexString(PasswordHash.getSHA(passwd));
        users.put(user, passwd_hash);
        users.flush();
        users.sync();
    }
}