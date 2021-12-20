//This file is part of OCMS.
//
//OCMS is free software: you can redistribute it and/or modify it under the terms of the GNU
//General Public License as published by the Free Software Foundation, either version 3 of the
//License, or (at your option) any later version.
//
//OCMS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
//the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
//Public License for more details.
//
//You should have received a copy of the GNU General Public License along with OCMS.  If not, see
//<https://www.gnu.org/licenses/>.
package ca.njuneau.ocms.service;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import jakarta.json.JsonBuilderFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles out-of-servlet errors
 */
public class FridgeErrorHandler extends ErrorHandler {

  private static final String CONTENT_TYPE = "application/json";

  private final JsonBuilderFactory jsonBuilderFactory;

  /**
   * @param jsonBuilderFactory The Jakarta JSON builder factory
   */
  public FridgeErrorHandler(final JsonBuilderFactory jsonBuilderFactory) {
    this.jsonBuilderFactory = jsonBuilderFactory;
  }

  @Override
  public void handle(
      final String target,
      final Request baseRequest,
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException, ServletException {
    response.setContentType(CONTENT_TYPE);
    response.getOutputStream().print(
      jsonBuilderFactory
        .createObjectBuilder()
        .add("code", response.getStatus())
        .add("message", "Error")
        .build()
        .toString()
    );
  }
}
