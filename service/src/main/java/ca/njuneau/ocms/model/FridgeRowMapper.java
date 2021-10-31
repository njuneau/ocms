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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Maps database rows to fridge model objects
 */
public class FridgeRowMapper implements RowMapper<FridgeRow> {

  @Override
  public FridgeRow map(final ResultSet rs, final StatementContext ctx) throws SQLException {
    return new FridgeRow(
        rs.getObject("id", UUID.class),
        rs.getString("name"),
        rs.getObject("date_entered", OffsetDateTime.class),
        rs.getObject("date_expiry", OffsetDateTime.class));
  }

}
