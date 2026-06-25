package generators;

import models.CreateUserRequest;
import models.UserRole;

public class RandomModelGenerator {

    public static <T> T generate(Class<T> modelClass) {
        if (modelClass == CreateUserRequest.class) {
            return (T) CreateUserRequest.builder()
                    .username(RandomData.getUsername())
                    .password(RandomData.getPassword())
                    .name(RandomData.getUsername())
                    .role(UserRole.USER.toString())
                    .build();
        }
        throw new IllegalArgumentException("Unknown model class: " + modelClass);
    }
}