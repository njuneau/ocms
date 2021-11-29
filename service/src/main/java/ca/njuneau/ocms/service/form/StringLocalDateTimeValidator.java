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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementation for the {@link StringLocalDateTime} validator
 */
public class StringLocalDateTimeValidator implements ConstraintValidator<StringLocalDateTime, String> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    boolean valid;

    if (value != null) {
      try {
        FORMATTER.parse(value);
        valid = true;
      } catch (final DateTimeParseException e) {
        valid = false;
      }
    } else {
      // Null value validation is handled by @NotNull in bean validation.
      valid = true;
    }

    return valid;
  }

}
