package ca.njuneau.ocms.service.compat;

import io.prometheus.client.servlet.common.adapter.ServletConfigAdapter;

import jakarta.servlet.ServletConfig;

/**
 * A Jakarta EE version of {@link io.prometheus.client.Adapter.ServletConfigAdapterImpl}
 */
public class JakartaServletConfigAdapter implements ServletConfigAdapter {

  private final ServletConfig delegate;

  public JakartaServletConfigAdapter(final ServletConfig delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getInitParameter(final String name) {
    return delegate.getInitParameter(name);
  }

}
