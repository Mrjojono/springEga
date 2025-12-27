package ega.api.egafinance.mapper;

import ega.api.egafinance.dto.CompteInput;
import ega.api.egafinance.dto.CompteUpdateInput;
import ega.api.egafinance.entity.Compte;
import org.mapstruct.*;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CompteMapper {

    Compte toCompte(CompteInput compteInput);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCompteFromInput(CompteUpdateInput source, @MappingTarget Compte target);
}
