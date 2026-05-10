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
public class AccountResponce extends BaseModel {
    private Integer id;
    private String accountNumber;
    private Double balance;
    private List<Object> transactions;
}
