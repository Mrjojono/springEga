package ega.api.egafinance.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IbanValidator.class)
@Documented
public @interface ValidIban {
    String message() default "Le numéro IBAN n'est pas valide";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}