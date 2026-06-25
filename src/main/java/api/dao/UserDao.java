package api.dao;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserDao {
    private Long id;
    private String username;
    private String passwordHash;  // зашифрованный пароль из БД
    private String role;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}