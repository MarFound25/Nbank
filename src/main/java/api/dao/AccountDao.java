package api.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDao {
    private Long id;
    private Long userId;
    private Double balance;
    private String accountNumber;
    private LocalDateTime createdAt;

}
