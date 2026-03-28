package shiftplan.web;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.security.PasswordHash;
import shiftplan.security.SecretStore;
import shiftplan.security.ShiftplanSecurityException;

import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

public class PrefsIdentityManager implements IdentityManager {

    private static final Logger logger = LogManager.getLogger(PrefsIdentityManager.class);

    PrefsIdentityManager() {}
    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String userName, Credential credential) {
        Account account = getAccount(userName);
        if (account != null && verifyCredential(credential)) {
            return account;
        }
        return null;
    }

    @Override
    public Account verify(Credential credential) {
        return null;
    }

    private boolean verifyCredential(Credential credential) {
        if (credential instanceof PasswordCredential) {
            String providedPassword = String.valueOf(((PasswordCredential) credential).getPassword());
            try {
                providedPassword = PasswordHash.toHexString(PasswordHash.getSHA(providedPassword));
                logger.debug("Übermitteltes HexString-Passwort: {}", providedPassword);
                String storedPassword = SecretStore.secrets.getPassword();
                logger.debug("Hinterlegtes Passwort: {}", storedPassword);
                logger.info("Hash-Werte gleich? {}", storedPassword.equals(providedPassword) ? "ja" : "nein");
                return storedPassword.equals(providedPassword);
            } catch (NoSuchAlgorithmException e) {
                throw new ShiftplanSecurityException(e.getMessage());
            }
        }
        return false;
    }

    private Account getAccount(final String userName) {
        if (userName != null && userName.equals(SecretStore.secrets.getUserName())) {
            return new Account() {

                private final Principal principal = new Principal() {
                    @Override
                    public String getName() {
                        return userName;
                    }
                };

                @Override
                public Principal getPrincipal() {
                    return principal;
                }

                @Override
                public Set<String> getRoles() {
                    return Collections.emptySet();
                }
            };
        }
        return null;
    }
}
