package ega.api.egafinance.mapper;

import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.entity.Compte;
import org.mapstruct.*;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CompteMapper {

    Compte toCompte(CompteInput compteInput);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCompteFromInput(CompteInput source,@MappingTarget Compte target);
}
