package models;

import lombok.Getter;

@Getter
public class UserWithPassword {
    private final CreateUserResponse user;
    private final String password;

    public UserWithPassword(CreateUserResponse user, String password) {
        this.user = user;
        this.password = password;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public String getName() {
        return user.getName();
    }

    public String getRole() {
        return user.getRole();
    }

    public int getId() {
        return (int) user.getId();
    }
}
