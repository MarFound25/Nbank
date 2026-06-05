package api.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;  // Добавьте этот импорт

import java.time.LocalDateTime;
import java.util.List;  // Добавьте этот импорт

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