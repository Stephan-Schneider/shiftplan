package shiftplan.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticatedUsersTest {

    private AuthenticatedUsers authUsers;
    private final long oneMinute = 60000L;

    @BeforeEach
    void setUp() {
        authUsers = AuthenticatedUsers.getInstance();
    }

    @ParameterizedTest
    @MethodSource("getTokens")
    void addAuthenticatedUser(String token, int size) {
        authUsers.addAuthenticatedUser(token);
        assertEquals(size, authUsers.getUserMapSize());
    }

    @Test
    void isAuthenticated() {
        String token = createRandomString();
        authUsers.addAuthenticatedUser(token);
        assertTrue(authUsers.isAuthenticated(token));
    }

    @Test
    void purgeUserMap() throws InterruptedException {
        authUsers.setMaxAge(1);
        authUsers.addAuthenticatedUser(createRandomString());
        Thread.sleep(oneMinute);
        authUsers.addAuthenticatedUser(createRandomString());
        Thread.sleep(oneMinute);
        authUsers.addAuthenticatedUser(createRandomString());
        Thread.sleep(oneMinute + 10000L);

        System.out.println("Anzahl der Elemente in userMap (vor purge): " + authUsers.getUserMapSize());
        System.out.println("purgeUserMap wird jetzt ausgeführt");
        authUsers.purgeUserMap();

        assertEquals(0, authUsers.getUserMapSize());
    }



    private static Stream<Arguments> getTokens() {
        return Stream.of(
                Arguments.of(createRandomString(), 1),
                Arguments.of(createRandomString(), 2),
                Arguments.of(createRandomString(), 3)
        );
    }


    private static String createRandomString() {
        int leftLimit = 97; // Buchstabe 'a'
        int rightLimit = 122; // Buchstabe 'z'
        int length = 36;
        Random random = new Random();

        String result =  random.ints(leftLimit, rightLimit +1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();
        System.out.println(result);
        return result;

    }
}