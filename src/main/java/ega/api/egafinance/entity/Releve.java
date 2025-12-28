package ega.api.egafinance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Releve {
    private Compte compte;
    private BigDecimal soldeInitial;
    private BigDecimal soldeFinal;
    private List<Transaction> transactions;
}

