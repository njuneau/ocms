package ca.njuneau.ocms.service.handling;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * <p>A servlet request handler</p>
 * <p>
 *   You really should not be writing your own request dispatcher. It's just very late.
 * </p>
 */
public class Handler {

  private final Pattern pathPattern;
  private final String method;
  private final Method handler;
  private final int handlerParameterCount;
  private final int handlerParameterPositionRequest;
  private final int handlerParameterPositionResponse;
  private final int handlerParameterPositionMatcher;

  /**
   * Creates a handler
   *
   * @param pathPattern The HTTP request path pattern
   * @param method The HTTP method on which this handler responds
   * @param handler The method to invoke when this handler matches a request
   *
   * @throws IllegalArgumentException If the handler input parameters are invalid
   * @throws NullPointerException If any of the annotation's fields are null
   */
  public Handler(final Pattern pathPattern, final String method, final Method handler) {
    Objects.requireNonNull(handler);
    final Parameter[] params = handler.getParameters();
    this.handlerParameterCount = params.length;

    // Maker sure handler input params are valid
    if (handlerParameterCount == 2 || handlerParameterCount == 3) {
      int httpRequestPosition = -1;
      int httpResponsePosition = -1;
      int matcherPosition = -1;
      for (int i = 0; i < this.handlerParameterCount; i++) {
        final Class<?> paramType = params[i].getType();
        if (ServletRequest.class.isAssignableFrom(paramType)) {
          httpRequestPosition = i;
        } else if (ServletResponse.class.isAssignableFrom(paramType)) {
          httpResponsePosition = i;
        } else if (Matcher.class.isAssignableFrom(paramType)) {
          matcherPosition = i;
        }
      }

      if (httpRequestPosition >= 0 && httpResponsePosition >= 0) {
        this.handlerParameterPositionRequest = httpRequestPosition;
        this.handlerParameterPositionResponse = httpResponsePosition;
        this.handlerParameterPositionMatcher = matcherPosition;
      } else {
        throw new IllegalArgumentException("Could not find ServletRequest or SerlvetResponse parameters");
      }
    } else {
      throw new IllegalArgumentException("Handler methods must have one SerlvetRequest parameter, one ServletResponse parameter and optionally one Matcher parameter.");
    }

    this.pathPattern = Objects.requireNonNull(pathPattern);
    this.method = Objects.requireNonNull(method);
    this.handler = handler;
  }

  /**
   * @return The HTTP request path pattern
   */
  public Pattern getPathPattern() {
    return pathPattern;
  }

  /**
   * @return The HTTP method this handler responds to
   */
  public String getMethod() {
    return method;
  }

  /**
   * @return The method to invoke when this handler matches a request
   */
  public Method getHandler() {
    return handler;
  }

  /**
   * @return The amount of parameters the handler has
   */
  public int getHandlerParameterCount() {
    return handlerParameterCount;
  }

  /**
   * @return The position of the ServletRequest parameter
   */
  public int getHandlerParameterPositionRequest() {
    return handlerParameterPositionRequest;
  }

  /**
   * @return The position of the ServletResponse parameter
   */
  public int getHandlerParameterPositionResponse() {
    return handlerParameterPositionResponse;
  }

  /**
   * @return True if the handler method has a Matcher parameter
   */
  public boolean hasMatcherParameter() {
    return handlerParameterPositionMatcher >= 0;
  }

  /**
   * @return The position of the Matcher parameter. Validate the presence of this parameter with
   *         {@link #hasMatcherParameter()}.
   */
  public int getHandlerParameterPositionMatcher() {
    return handlerParameterPositionMatcher;
  }


}
