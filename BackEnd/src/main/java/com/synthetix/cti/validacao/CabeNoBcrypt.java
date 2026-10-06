package com.synthetix.cti.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * O BCrypt so le 72 BYTES e recusa o resto com erro. Letra acentuada ocupa 2 bytes, entao o limite
 * de 72 caracteres do @Pattern nao basta.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CabeNoBcrypt.Validador.class)
public @interface CabeNoBcrypt {

	String message() default "A senha passa do limite do BCrypt; use menos letras acentuadas";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validador implements ConstraintValidator<CabeNoBcrypt, String> {

		@Override
		public boolean isValid(String senha, ConstraintValidatorContext contexto) {
			return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= 72;
		}

	}

}
