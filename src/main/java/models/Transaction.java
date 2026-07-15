package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {
    private Integer id;
    private Double amount;
    private String type;
    private String timestamp;
    private Integer relatedAccountId;
}
