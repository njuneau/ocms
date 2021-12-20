package ca.njuneau.ocms.service.compat;

import java.io.IOException;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Predicate;
import io.prometheus.client.servlet.common.exporter.Exporter;
import io.prometheus.client.servlet.common.exporter.ServletConfigurationException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Jakarta EE version of {@link io.prometheus.client.exporter.MetricsServlet}
 */
public class JakartaMetricsServlet extends HttpServlet {
  private static final long serialVersionUID = -6790231988713964524L;

  private final Exporter exporter;

  public JakartaMetricsServlet() {
    this(CollectorRegistry.defaultRegistry, null);
  }

  public JakartaMetricsServlet(final Predicate<String> sampleNameFilter) {
    this(CollectorRegistry.defaultRegistry, sampleNameFilter);
  }

  public JakartaMetricsServlet(final CollectorRegistry registry) {
    this(registry, null);
  }

  public JakartaMetricsServlet(final CollectorRegistry registry, final Predicate<String> sampleNameFilter) {
    exporter = new Exporter(registry, sampleNameFilter);
  }

  @Override
  public void init(final ServletConfig servletConfig) throws ServletException {
    try {
      exporter.init(new JakartaServletConfigAdapter(servletConfig));
    } catch (final ServletConfigurationException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    exporter.doGet(new JakartaRequestAdapter(req), new JakartaResponseAdapter(resp));
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    exporter.doPost(new JakartaRequestAdapter(req), new JakartaResponseAdapter(resp));
  }

}
