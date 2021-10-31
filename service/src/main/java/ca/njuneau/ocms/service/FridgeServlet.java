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

package ca.njuneau.ocms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.njuneau.ocms.model.FridgeDAO;
import ca.njuneau.ocms.model.FridgeRow;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Demo servlet that speaks to the database. It manages your fridge.
 */
public class FridgeServlet extends HttpServlet {
  private static final long serialVersionUID = 2393630406179025527L;
  private static final Logger LOG = LoggerFactory.getLogger(FridgeServlet.class);

  private static final String CONTENT_TYPE = "application/json";
  private static final Pattern PATH_PATTERN_ROOT = Pattern.compile("^/$");

  private static final String HTTP_METHOD_GET = "GET";
  private static final String HTTP_METHOD_POST = "POST";

  private final Clock clock;
  private final FridgeDAO fridgeDao;
  private final Validator validator;
  private final JsonBuilderFactory jsonBuilderFactory;

  /**
   * @param clock The clock to use when obtaining time
   * @param fridgeDao The fridge dao, connected to JDBI
   * @param validator The Jakarta bean validator
   * @param jsonBuilderFactory The Jakarta JSON builder factory
   */
  public FridgeServlet(final Clock clock, final FridgeDAO fridgeDao, final Validator validator, final JsonBuilderFactory jsonBuilderFactory) {
    this.clock = clock;
    this.fridgeDao = fridgeDao;
    this.validator = validator;
    this.jsonBuilderFactory = jsonBuilderFactory;
  }

  @Override
  protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    response.setContentType(CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    // Very primitive regex-based dispatcher
    final String path = request.getPathInfo();
    if (PATH_PATTERN_ROOT.matcher(path).matches()) {
      switch (request.getMethod()) {
        case HTTP_METHOD_GET:
          handleGetRoot(request, response);
        break;
        case HTTP_METHOD_POST:
          handlePostRoot(request, response);
        break;
        default:
          handleNotFound(request, response);
        break;
      }
    } else {
      // Unknown path
      handleNotFound(request, response);
    }
  }

  /**
   * Handles the root path ("/") GET. Obtain all items in the fridge
   *
   * @param request The servlet request
   * @param response The servlet response
   *
   * @throws ServletException
   * @throws IOException
   */
  private void handleGetRoot(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final List<FridgeRow> rows = fridgeDao.getFridgeRows();
    final JsonArrayBuilder arrayBuilder = jsonBuilderFactory.createArrayBuilder();
    for (final FridgeRow row : rows) {
      arrayBuilder.add(row.toJson(jsonBuilderFactory.createObjectBuilder(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
    response.setStatus(200);
    response.getOutputStream().println(arrayBuilder.build().toString());
  }

  /**
   * Handles the root path ("/") POST. Inserts an item in the fridge
   *
   * @param request The servlet request
   * @param response The servlet response
   *
   * @throws ServletException
   * @throws IOException
   */
  private void handlePostRoot(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final var form = new FridgeInsertForm(request);
    final Set<ConstraintViolation<FridgeInsertForm>> formErrors = validator.validate(form);

    if (formErrors.isEmpty()) {
      // Form validated successfully.
      try {
        final OffsetDateTime dateExpiry = LocalDateTime
            .parse(form.getDateExpiry(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(clock.getZone())
            .toOffsetDateTime();
        final OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
        fridgeDao.insertFridgeRow(form.getName(), now, dateExpiry);
        response.setStatus(201);
      } catch (final DateTimeParseException e) {
        // This can happen because I'm too lazy with date time validation in the form
        LOG.warn("Invalid date time parsed. Fix your validator!", e);
        response.setStatus(400);
        response.getOutputStream().println(
          jsonBuilderFactory.createObjectBuilder()
            .add("error", 400)
            .add("message", "Invalid expiry date")
            .build()
            .toString()
        );
      } catch (final Exception e) {
        LOG.error("Error while inserting in the DB", e);
        response.setStatus(500);
        response.getOutputStream().println(jsonBuilderFactory.createObjectBuilder()
            .add("error", 500)
            .add("message", "Could not insert in the DB")
            .build().toString()
        );
      }

    } else {
      // Form contains errors. Return 400 bad request
      final JsonArrayBuilder errorMessages = jsonBuilderFactory.createArrayBuilder();
      for (final ConstraintViolation<FridgeInsertForm> formError : formErrors) {
        errorMessages.add(
          jsonBuilderFactory.createObjectBuilder()
            .add("path", formError.getPropertyPath().toString())
            .add("message", formError.getMessage()
          )
        );
      }
      final JsonObject jsonResponse = jsonBuilderFactory.createObjectBuilder()
          .add("error", 400)
          .add("message", "Form contains errors")
          .add("validationMessages", errorMessages)
          .build();
      response.setStatus(400);
      response.getOutputStream().println(jsonResponse.toString());
    }
  }

  /**
   * Answers with a "not found" error
   *
   * @param request The servlet request
   * @param response The servlet response
   *
   * @throws ServletException
   * @throws IOException
   */
  private void handleNotFound(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JsonObject jsonResponse = jsonBuilderFactory.createObjectBuilder()
        .add("error", 404)
        .add("message", "Not found")
        .build();

    response.getOutputStream().println(jsonResponse.toString());
    response.setStatus(404);
  }

}
