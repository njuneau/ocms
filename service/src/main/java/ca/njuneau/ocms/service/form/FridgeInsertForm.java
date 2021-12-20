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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Form for entering a fridge row in the database.
 */
public record FridgeInsertForm(
    /**
     * Form parameter "name" with restrictive pattern
     */
    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-3 \\-_\"',\\.]+")
    String name,

    /**
     * Form parameter "date-expiry" with primitive date and time format
     */
    @NotNull
    @StringLocalDateTime
    String dateExpiry) {
}
