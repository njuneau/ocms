package ca.njuneau.ocms.service.compat;

import java.io.IOException;
import java.io.PrintWriter;

import io.prometheus.client.servlet.common.adapter.HttpServletResponseAdapter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * A Jakarta EE version of {@link io.prometheus.client.Adapter.HttpServletResponseAdapterImpl}
 */
public class JakartaResponseAdapter implements HttpServletResponseAdapter {

  private final HttpServletResponse delegate;

  public JakartaResponseAdapter(final HttpServletResponse delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getStatus() {
    return delegate.getStatus();
  }

  @Override
  public void setStatus(final int httpStatusCode) {
    delegate.setStatus(httpStatusCode);
  }

  @Override
  public void setContentType(final String contentType) {
    delegate.setContentType(contentType);

  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return delegate.getWriter();
  }

}
