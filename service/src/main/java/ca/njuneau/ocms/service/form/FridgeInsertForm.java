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
import spark.Request;

/**
 * Form for entering a fridge row in the database.
 */
public class FridgeInsertForm {

  /**
   * Form parameter "name" with restrictive pattern
   */
  @NotBlank
  @Pattern(regexp = "[a-zA-Z0-3 \\-_\"',\\.]+")
  private final String name;

  /**
   * Form parameter "date-expiry" with primitive date and time format
   */
  @NotNull
  @StringLocalDateTime
  private final String dateExpiry;

  public FridgeInsertForm(final Request request) {
    this.name = request.queryParams("name");
    this.dateExpiry = request.queryParams("date-expiry");
  }

  /**
   * @return The fridge row's name
   */
  public String getName() {
    return name;
  }

  /**
   * @return An ISO-8601 timestamp indicating the date at which the fridge item expires
   */
  public String getDateExpiry() {
    return dateExpiry;
  }

}
