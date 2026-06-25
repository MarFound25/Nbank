package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponce extends BaseModel {
    private Integer id;
    private String accountNumber;
    private Double balance;

    @JsonIgnore
    private List<Object> transactions;
}
