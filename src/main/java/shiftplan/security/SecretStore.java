package shiftplan.security;

public class SecretStore {

    public static Secrets secrets;

    private SecretStore() {}

    public static void createSecretStore(String userName, String password) {
        secrets = new Secrets(userName, password);
    }

    public static class Secrets {
        private final String userName;
        private final String password;

        private Secrets(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }
        public String getUserName() {
            return userName;
        }
        public String getPassword() {
            return password;
        }
    }
}
