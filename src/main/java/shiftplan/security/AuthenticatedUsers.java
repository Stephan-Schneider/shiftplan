package shiftplan.security;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Klasse zur Identifikation authentifizierter User
 * <p>
 * Nachdem ein Benutzer erfolgreich authentifiziert ist, wird das User-Token, dass vom Http-Client mittels eines
 * Customs-Headers übermittelt wird, in die <code>userMap</code>-Tabelle gespeichert,
 * <p>
 * Bei nachfolgenden Anfragen kann anhand des Abgleichs des User-Tokens festgestellt werden, ob sich der User
 * authentifiziert hat.
 * <p>
 * Das User-Token ist maximal <code>maxAge</code> Minuten gültig.
 */
public class AuthenticatedUsers {

    private static final AuthenticatedUsers instance = new AuthenticatedUsers();

    private final Map<String, LocalDateTime> userMap;
    private long maxAge;

    private AuthenticatedUsers() {
        userMap = new ConcurrentHashMap<>();
        maxAge = 30;
    }

    public static AuthenticatedUsers getInstance() {
        return instance;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public void addAuthenticatedUser(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ShiftplanSecurityException("User-Auth-ID fehlt");
        }
        userMap.put(userToken, LocalDateTime.now());
    }

    public boolean isAuthenticated(String userToken) {
        purgeUserMap();
        if (userToken == null) return false;
        return userMap.containsKey(userToken);
    }

    void purgeUserMap() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<String> it = userMap.keySet().iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            String userToken = it.next();
            LocalDateTime timeStamp = userMap.get(userToken);
            if (timeStamp != null && timeStamp.until(now, ChronoUnit.MINUTES) >= maxAge) {
                userMap.remove(userToken);
            }
        }
    }

    public int getUserMapSize() {
        return userMap.size();
    }
}
