package ca.njuneau.ocms.service;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import spark.servlet.SparkApplication;
import spark.servlet.SparkFilter;

/**
 * This implementation uses a pre-instanciated app so we don't have to resort to DI or statics all
 * over the place
 */
public class CustomSparkFilter extends SparkFilter {

  private final SparkApplication[] sparkApplications;

  /**
   * @param sparkApplications A set of pre-constructed, but uninitialized applications
   */
  public CustomSparkFilter(final SparkApplication[] sparkApplications) {
    this.sparkApplications = sparkApplications;
  }

  @Override
  protected SparkApplication[] getApplications(final FilterConfig filterConfig) throws ServletException {
    return sparkApplications;
  }

}
