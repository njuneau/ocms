package ca.njuneau.ocms.service.handling;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>A servlet request handler</p>
 * <p>
 *   A handler's comparison is loosely based on the path's complexity : the deeper the path, the
 *   more important the handle is.
 * </p>
 * <p>
 *   You really should not be writing your own request dispatcher. It's just very late.
 * </p>
 */
public class Handler implements Comparable<Handler> {

  private static final Pattern SLASH = Pattern.compile("/");

  private final String path;
  private final String method;
  private final Method handler;
  private final long depth;

  /**
   * Creates a handler
   *
   * @param path The path the request should begin with
   * @param method The HTTP method on which this handler responds
   * @param handler The method to invoke when this handler matches a request
   *
   * @throws IllegalArgumentException If the handler input parameters are invalid
   * @throws NullPointerException If any of the annotation's fields are null
   */
  public Handler(final String path, final String method, final Method handler) {
    Objects.requireNonNull(handler);
    final Parameter[] params = handler.getParameters();
    if (params.length != 2 ||
        !params[0].getType().equals(HttpServletRequest.class) ||
        !params[1].getType().equals(HttpServletResponse.class)) {
      throw new IllegalArgumentException("Handler method with invalid input parameters");
    }
    this.path = Objects.requireNonNull(path);
    this.method = Objects.requireNonNull(method);
    this.handler = handler;
    this.depth = SLASH.matcher(path).results().count();
  }

  /**
   * @return The path the request should begin with
   */
  public String getPath() {
    return path;
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
   * @return The path's depth (amount of slashes in the path)
   */
  public long getDepth() {
    return depth;
  }

  @Override
  public int compareTo(final Handler other) {
    return Long.compare(depth, other.getDepth());
  }


}
