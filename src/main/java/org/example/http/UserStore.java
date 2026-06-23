package org.example.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UserStore {

    private final Map<String, String> passwordsByUser = new HashMap<>();

    public static UserStore withDemoUser() {
        return new UserStore().add("admin", "admin");
    }

    public UserStore add(String username, String password) {
        passwordsByUser.put(username, password);
        return this;
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        return Objects.equals(passwordsByUser.get(username), password);
    }
}
