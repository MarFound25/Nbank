package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithToken {
    private Long id;
    private String username;
    private String password;
    private String token;

    public boolean hasValidToken() {
        return token != null && token.startsWith("Basic ");
    }
}
