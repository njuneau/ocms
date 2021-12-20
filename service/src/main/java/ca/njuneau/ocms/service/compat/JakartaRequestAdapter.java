package ca.njuneau.ocms.service.compat;

import io.prometheus.client.servlet.common.adapter.HttpServletRequestAdapter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A Jakarta EE version of {@link io.prometheus.client.Adapter.HttpServletRequestAdapterImpl}
 */
public class JakartaRequestAdapter implements HttpServletRequestAdapter {

  private final HttpServletRequest delegate;

  public JakartaRequestAdapter(final HttpServletRequest delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getHeader(final String name) {
    return delegate.getHeader(name);
  }

  @Override
  public String getRequestURI() {
    return delegate.getRequestURI();
  }

  @Override
  public String getMethod() {
    return delegate.getMethod();
  }

  @Override
  public String[] getParameterValues(final String name) {
    return delegate.getParameterValues(name);
  }

  @Override
  public String getContextPath() {
    return delegate.getContextPath();
  }

}
