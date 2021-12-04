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

import static spark.Spark.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.njuneau.ocms.model.FridgeDAO;
import ca.njuneau.ocms.model.FridgeRow;
import ca.njuneau.ocms.service.form.FridgeInsertForm;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

/**
 * Demo application that speaks to the database. It manages your fridge.
 */
public class FridgeApplication implements SparkApplication {
  private static final Logger LOG = LoggerFactory.getLogger(FridgeApplication.class);

  private static final String CONTENT_TYPE = "application/json";
  private static final DateTimeFormatter RESPONSE_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final FridgeDAO fridgeDao;
  private final Validator validator;
  private final JsonBuilderFactory jsonBuilderFactory;

  /**
   * @param fridgeDao The fridge dao, connected to JDBI
   * @param validator The Jakarta bean validator
   * @param jsonBuilderFactory The Jakarta JSON builder factory
   */
  public FridgeApplication(final FridgeDAO fridgeDao, final Validator validator, final JsonBuilderFactory jsonBuilderFactory) {
    this.fridgeDao = fridgeDao;
    this.validator = validator;
    this.jsonBuilderFactory = jsonBuilderFactory;
  }

  @Override
  public void init() {
    before(this::beforeRequest);
    notFound(this::handleNotFound);
    internalServerError(this::handleInternalError);

    get("/", this::handleGetRoot);
    post("/", this::handlePostRoot);
  }

  /**
   * Executed before each request. Sets the content type to JSON across the board.
   *
   * @param request The HTTP request
   * @param response The HTTP response
   */
  private void beforeRequest(final Request request, final Response response) {
    response.type(CONTENT_TYPE);
  }

  /**
   * Handles the root path ("/") GET. Obtain all items in the fridge
   *
   * @param request The HTTP request
   * @param response The HTTP response
   * @return The response body
   */
  private String handleGetRoot(final Request request, final Response response) {
    final List<FridgeRow> rows = fridgeDao.getFridgeRows();
    final JsonArrayBuilder arrayBuilder = jsonBuilderFactory.createArrayBuilder();
    for (final FridgeRow row : rows) {
      arrayBuilder.add(row.toJson(jsonBuilderFactory.createObjectBuilder(), RESPONSE_DATE_TIME_FORMATTER));
    }
    response.status(200);
    return arrayBuilder.build().toString();
  }

  /**
   * Handles the root path ("/") POST. Inserts an item in the fridge
   *
   * @param request The HTTP request
   * @param response The HTTP response
   * @return The response body
   */
  private String handlePostRoot(final Request request, final Response response) throws IOException {
    final var form = new FridgeInsertForm(request);
    final Set<ConstraintViolation<FridgeInsertForm>> formErrors = validator.validate(form);
    int responseStatus;
    String responseBody;

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
        responseStatus = 201;
        responseBody = insertedRow.toJson(jsonBuilderFactory.createObjectBuilder(), RESPONSE_DATE_TIME_FORMATTER).toString();
      } catch (final Exception e) {
        LOG.error("Error while inserting in the DB", e);
        responseStatus = 500;
        responseBody = createJsonErrorBuilder(500, "Could not insert in the DB").build().toString();
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
      responseStatus = 400;
      responseBody = jsonResponse.toString();
    }

    response.status(responseStatus);
    return responseBody;
  }

  /**
   * Answers with a "internal server error"
   *
   * @param request The HTTP request
   * @param response The HTTP response
   * @return The response body
   */
  private String  handleInternalError(final Request request, final Response response)  {
    response.status(500);
    return createJsonErrorBuilder(500, "Internal server error").build().toString();
  }

  /**
   * Answers with a "not found" error
   *
   * @param request The HTTP request
   * @param response The HTTP response
   * @return The response body
   */
  private String handleNotFound(final Request request, final Response response) {
    response.status(404);
    return createJsonErrorBuilder(404, "Not found").build().toString();
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
