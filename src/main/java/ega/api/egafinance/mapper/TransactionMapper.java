package ega.api.egafinance.mapper;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Transaction;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {
    Transaction toTransaction(TransactionInput transactionInput);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTransactionFromInput(TransactionInput source, @MappingTarget Transaction target);
}
