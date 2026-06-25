package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserListResponse extends BaseModel {
    private List<UserItem> users;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserItem {
        private Long id;
        private String username;
        private String password;
        private String name;
        private String role;
        private List<String> accounts;
    }
}