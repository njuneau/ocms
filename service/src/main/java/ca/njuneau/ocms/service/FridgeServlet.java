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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.njuneau.ocms.model.FridgeDAO;
import ca.njuneau.ocms.model.FridgeRow;
import ca.njuneau.ocms.service.form.FridgeInsertForm;
import ca.njuneau.ocms.service.handling.Handle;
import ca.njuneau.ocms.service.handling.Handler;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
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
  private static final DateTimeFormatter RESPONSE_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final FridgeDAO fridgeDao;
  private final Validator validator;
  private final JsonBuilderFactory jsonBuilderFactory;
  private final List<Handler> handlers;

  /**
   * @param fridgeDao The fridge dao, connected to JDBI
   * @param validator The Jakarta bean validator
   * @param jsonBuilderFactory The Jakarta JSON builder factory
   */
  public FridgeServlet(final FridgeDAO fridgeDao, final Validator validator, final JsonBuilderFactory jsonBuilderFactory) {
    this.fridgeDao = fridgeDao;
    this.validator = validator;
    this.jsonBuilderFactory = jsonBuilderFactory;

    // Introspect servlet for handler methods
    this.handlers = new ArrayList<Handler>();
    for (final Method declaredMethod : getClass().getDeclaredMethods()) {
      for (final Handle handle : declaredMethod.getDeclaredAnnotationsByType(Handle.class)) {
        try {
          handlers.add(new Handler(handle.path(), handle.method(), declaredMethod));
        } catch (final IllegalArgumentException | NullPointerException e) {
          LOG.error("Invalid handler mapping on []", declaredMethod, e);
        }
      }
    }
    Collections.sort(handlers);
    Collections.reverse(handlers);
  }

  @Override
  protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    response.setContentType(CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    // Very primitive dispatcher
    final String path = request.getPathInfo();
    final String method = request.getMethod();
    boolean handled = false;
    for (final Handler handler : handlers) {
      if (path.startsWith(handler.getPath()) && method.equals(handler.getMethod())) {
        try {
          handler.getHandler().invoke(this, request, response);
        } catch (final Exception e) {
          LOG.error("Handler invocation failure", e);
          handleInternalError(response);
        }
        handled = true;
        break;
      }
    }

    if (!handled) {
      handleNotFound(response);
    }
  }

  /**
   * Handles the root path ("/") GET. Obtain all items in the fridge
   *
   * @param request The servlet request
   * @param response The servlet response
   *
   * @throws IOException
   */
  @Handle(path = "/", method = "GET")
  private void handleGetRoot(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final List<FridgeRow> rows = fridgeDao.getFridgeRows();
    final JsonArrayBuilder arrayBuilder = jsonBuilderFactory.createArrayBuilder();
    for (final FridgeRow row : rows) {
      arrayBuilder.add(row.toJson(jsonBuilderFactory.createObjectBuilder(), RESPONSE_DATE_TIME_FORMATTER));
    }
    response.setStatus(200);
    response.getOutputStream().print(arrayBuilder.build().toString());
  }

  /**
   * Handles the root path ("/") POST. Inserts an item in the fridge
   *
   * @param request The servlet request
   * @param response The servlet response
   */
  @Handle(path = "/", method = "POST")
  private void handlePostRoot(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final var form = new FridgeInsertForm(request);
    final Set<ConstraintViolation<FridgeInsertForm>> formErrors = validator.validate(form);

    if (formErrors.isEmpty()) {
      // Form validated successfully. Insert in the database.
      try {
        final OffsetDateTime dateExpiry = LocalDateTime
            .parse(form.getDateExpiry(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(ZoneOffset.UTC)
            .toOffsetDateTime();
        final UUID rowId = UUID.randomUUID();
        fridgeDao.insertFridgeRow(rowId, form.getName(), dateExpiry);

        final FridgeRow insertedRow = fridgeDao.getFrideRow(rowId);
        response.setStatus(201);
        response.getOutputStream().print(
            insertedRow.toJson(jsonBuilderFactory.createObjectBuilder(), RESPONSE_DATE_TIME_FORMATTER).toString());
      } catch (final Exception e) {
        LOG.error("Error while inserting in the DB", e);
        response.setStatus(500);
        response.getOutputStream().print(
            createJsonErrorBuilder(500, "Could not insert in the DB").build().toString());
      }

    } else {
      // Form contains errors. Return 400 bad request with constraint violation messages
      final JsonArrayBuilder errorMessages = jsonBuilderFactory.createArrayBuilder();
      for (final ConstraintViolation<FridgeInsertForm> formError : formErrors) {
        errorMessages.add(
          jsonBuilderFactory.createObjectBuilder()
            .add("path", formError.getPropertyPath().toString())
            .add("message", formError.getMessage()
          )
        );
      }
      final JsonObject jsonResponse = createJsonErrorBuilder(400, "Form contains errors")
          .add("validationMessages", errorMessages)
          .build();
      response.setStatus(400);
      response.getOutputStream().print(jsonResponse.toString());
    }
  }

  /**
   * Answers with a "internal server error"
   *
   * @param response The servlet response
   */
  private void handleInternalError(final HttpServletResponse response) throws IOException {
    response.setStatus(500);
    response.getOutputStream().print(createJsonErrorBuilder(500, "Internal server error").build().toString());
  }

  /**
   * Answers with a "not found" error
   *
   * @param response The servlet response
   */
  private void handleNotFound(final HttpServletResponse response) throws IOException {
    response.setStatus(404);
    response.getOutputStream().print(createJsonErrorBuilder(404, "Not found").build().toString());
  }

  /**
   * @param errorCode The error code
   * @param message The error message
   * @return The base JSON object builder to use for error responses
   */
  private JsonObjectBuilder createJsonErrorBuilder(final int errorCode, final String message) {
    return jsonBuilderFactory.createObjectBuilder()
        .add("error", errorCode)
        .add("message", message);
  }

}
