package ega.api.egafinance.mapper;


import ega.api.egafinance.dto.UserRegisterInput;
import ega.api.egafinance.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    // Map du DTO UserRegisterInput à l'entité User
    User toUser(UserRegisterInput userRegisterInput);

    // Mise à jour partielle d'une entité User existante
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void register(UserRegisterInput source, @MappingTarget User target);
}