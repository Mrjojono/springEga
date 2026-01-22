package ega.api.egafinance.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.iban4j.IbanUtil;


public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    @Override
    public void initialize(ValidIban constraintAnnotation) {
        // Initialisation
    }

    @Override
    public boolean isValid(String iban, ConstraintValidatorContext context) {
        if (iban == null || iban.isBlank()) {
            return true;
        }
        try {
            IbanUtil.validate(iban);
            return true;
        } catch (Exception e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Le numéro IBAN '" + iban + "' n'est pas valide: " + e.getMessage()
            ).addConstraintViolation();
            return false;
        }
    }
}