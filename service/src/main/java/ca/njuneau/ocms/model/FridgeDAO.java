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
import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Fridge database access point
 */
public interface FridgeDAO {

  /**
   * @return All the rows in the fridge
   */
  @SqlQuery("SELECT * FROM fridge")
  @RegisterRowMapper(FridgeRowMapper.class)
  public List<FridgeRow> getFridgeRows();

  @SqlUpdate("INSERT INTO fridge (name, date_entered, date_expiry) VALUES (?, ?, ?)")
  public void insertFridgeRow(final String name, final OffsetDateTime dateEntered, final OffsetDateTime dateExpiry);

}
