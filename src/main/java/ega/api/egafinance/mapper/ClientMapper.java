package ega.api.egafinance.mapper;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.UserRegisterInput;
import ega.api.egafinance.entity.Client;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ClientMapper {

    Client toClient(ClientInput clientInput);

    Client toClientRegister(UserRegisterInput userRegisterInput);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateClientFromInput(ClientInput source, @MappingTarget Client target);
}