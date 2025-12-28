package ega.api.egafinance.service;

import ega.api.egafinance.dto.ClientInput;
import ega.api.egafinance.dto.UserRegisterInput;
import ega.api.egafinance.entity.User;

public interface IAuthService {

    public User Register(UserRegisterInput userRegisterInput);
}
