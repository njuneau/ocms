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

package ca.njuneau.ocms.model;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * A refrigirator row
 */
public record FridgeRow(
  /**
   * The row's primary key
   */
  UUID id,

  /**
   * The fridge item's name
   */
  String name,

  /**
   * The date at which the item was put in the fridge
   */
  OffsetDateTime dateEntered,

  /**
   * The item's expiration date
   */
  OffsetDateTime dateExpiry) {

  /**
   * Converts this record to JSON
   * @param builder The JSON builder to use when creating the JSON object
   * @param dateTimeFormatter The formatter to use for outputting dates
   * @return The JSON representation of the fridge item
   */
  public final JsonObject toJson(final JsonObjectBuilder builder, final DateTimeFormatter dateTimeFormatter) {
    return builder
        .add("id", id.toString())
        .add("name", name)
        .add("dateEntered", dateTimeFormatter.format(dateEntered))
        .add("dateExpiry", dateTimeFormatter.format(dateExpiry))
        .build();
  }
}
