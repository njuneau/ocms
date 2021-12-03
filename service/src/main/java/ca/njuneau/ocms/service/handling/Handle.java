package ca.njuneau.ocms.service.handling;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a method should be able to handle an incoming servlet request
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Handle {

  /**
   * @return The path pattern the request should have
   */
  public String pathPattern();

  /**
   * @return The HTTP method the handler responds to
   */
  public String method();

}
