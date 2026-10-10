package storage.cloud.cloudstorage.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NotBlank(message = "Username should not be null and must contain at least one non-whitespace character")
@Size(min = 5, max = 20, message = "User name must be between {min} and {max} characters")
@Pattern(regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$", message = "Invalid username")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ValidUserName {
    String message() default "Invalid username";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
