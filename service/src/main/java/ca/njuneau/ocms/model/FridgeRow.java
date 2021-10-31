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
public class FridgeRow {
  private final UUID id;
  private final String name;
  private final OffsetDateTime dateEntered;
  private final OffsetDateTime dateExpiry;

  /**
   * @param id The row's primary key
   * @param name The fridge item's name
   * @param dateEntered The date at which the item was put in the fridge
   * @param dateExpiry The item's expiration date
   */
  public FridgeRow(final UUID id, final String name, final OffsetDateTime dateEntered, final OffsetDateTime dateExpiry) {
    this.id = id;
    this.name = name;
    this.dateEntered = dateEntered;
    this.dateExpiry = dateExpiry;
  }

  /**
   * @return The row's primary key
   */
  public UUID getId() {
    return id;
  }

  /**
   * @return The fridge item's name
   */
  public String getName() {
    return name;
  }

  /**
   * @return The date at which the item was put in the fridge
   */
  public OffsetDateTime getDateEntered() {
    return dateEntered;
  }

  /**
   * @return The item's expiration date
   */
  public OffsetDateTime getDateExpiry() {
    return dateExpiry;
  }

  /**
   * Converts this object to JSON
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
