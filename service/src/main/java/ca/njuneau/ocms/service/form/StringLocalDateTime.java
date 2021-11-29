// This file is part of OCMS.
//
// OCMS is free software: you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// OCMS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
// the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along with OCMS.  If not, see
// <https://www.gnu.org/licenses/>.

package ca.njuneau.ocms.service.form;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Indicates that a field must be a valid ISO-8601 local date-time
 */
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD, TYPE_USE, ANNOTATION_TYPE})
@Constraint(validatedBy = StringLocalDateTimeValidator.class)
@Documented
public @interface StringLocalDateTime {

  /**
   * @return The error message returned when the constraint is violoated
   */
  String message() default "{ca.njuneau.ocms.service.StringLocalDateTime.message}";

  /**
   * @return The validation groups to which this constraint applies
   */
  Class<?>[] groups() default {};

  /**
   * @return A payload optionally attached to the validator
   */
  Class<? extends Payload>[] payload() default {};



}
