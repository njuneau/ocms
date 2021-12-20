package ca.njuneau.ocms.service.compat;

import io.prometheus.client.servlet.common.adapter.FilterConfigAdapter;

import jakarta.servlet.FilterConfig;

/**
 * A Jakarta EE version of {@link io.prometheus.client.Adapter.FilterConfigAdapterImpl}
 */
public class JakartaFilterConfigAdapter implements FilterConfigAdapter {

  private final FilterConfig delegate;

  public JakartaFilterConfigAdapter(final FilterConfig delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getInitParameter(final String name) {
    return delegate.getInitParameter(name);
  }

}
