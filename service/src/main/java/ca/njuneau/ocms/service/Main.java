// This file is part of OCMS.
//
// OCMS is free software: you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// OCMS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
// the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along with OCMS.  If not, see
// <https://www.gnu.org/licenses/>.

package ca.njuneau.ocms.service;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.TimeZone;

import javax.management.MBeanServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.njuneau.ocms.model.FridgeDAO;
import ca.njuneau.ocms.model.FridgeRowMapper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.prometheus.client.dropwizard.DropwizardExports;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Program entry point
 */
public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  // Don't do this in prod or I'll be very, very mad.
  private static final String PG_JDBC_URL = "jdbc:postgresql://192.168.10.100:5432/test";
  private static final String PG_USER     = "test";
  private static final String PG_PASS     = "test";

  private static final int HTTP_PORT      = 8080;

  /**
   * Program entry point
   *
   * @param args Command-line arguments
   */
  public static void main(final String[] args) {
    LOG.info("Setting clock to UTC");
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC.getId()));
    final Clock clock = Clock.systemUTC();

    LOG.info("Initializing metrics registry");
    final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    final var metricsRegistry = new MetricRegistry();
    metricsRegistry.registerAll(new BufferPoolMetricSet(mBeanServer));
    metricsRegistry.registerAll(new ClassLoadingGaugeSet());
    metricsRegistry.registerAll(new GarbageCollectorMetricSet());
    metricsRegistry.registerAll(new JvmAttributeGaugeSet());
    metricsRegistry.registerAll(new MemoryUsageGaugeSet());
    metricsRegistry.registerAll(new ThreadStatesGaugeSet());
    final JmxReporter metricsReporter = JmxReporter.forRegistry(metricsRegistry).build();
    metricsReporter.start();
    final var prometheusExport = new DropwizardExports(metricsRegistry);
    prometheusExport.register();

    LOG.info("Creating database connection pool");
    final var hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(PG_JDBC_URL);
    hikariConfig.setUsername(PG_USER);
    hikariConfig.setPassword(PG_PASS);
    final var hikariDS = new HikariDataSource(hikariConfig);

    LOG.info("Configuring JDBI");
    final var jdbi = Jdbi.create(hikariDS);
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.registerRowMapper(new FridgeRowMapper());
    final FridgeDAO fridgeDao = jdbi.onDemand(FridgeDAO.class);

    LOG.info("Configuring Jakarta JSON");
    final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(Collections.emptyMap());

    LOG.info("Configuring Bean Validator");
    final Validator validator = Validation
        .byDefaultProvider()
        .configure()
        .clockProvider(() -> clock)
        .buildValidatorFactory()
        .getValidator();

    LOG.info("Launching HTTP server");
    final var httpThreadPool = new QueuedThreadPool();
    httpThreadPool.setName("server");
    final var server = new Server(httpThreadPool);
    final var connector = new ServerConnector(server);
    connector.setPort(HTTP_PORT);
    server.addConnector(connector);

    // Setup the servlet
    final var servletContext = new ServletContextHandler();
    servletContext.setContextPath("/");
    final var servletHolder = new ServletHolder(new FridgeServlet(clock, fridgeDao, validator, jsonBuilderFactory));
    servletContext.addServlet(servletHolder, "/*");
    // Disable default error handler
    final var emptyErrorHandler = new ErrorHandler() {
      @Override
      protected void writeErrorPage(final HttpServletRequest request, final Writer writer, final int code, final String message, final boolean showStacks) throws IOException {};
    };
    servletContext.setErrorHandler(emptyErrorHandler);
    server.setHandler(servletContext);

    // Register JVM shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        LOG.info("Stopping Jetty");
        server.stop();
        LOG.info("Stopping Hikari");
        hikariDS.close();
        LOG.info("Stopping metrics reporter");
        metricsReporter.stop();
      } catch (final Exception e) {
        LOG.error("Clean shutdown failure", e);
      }
    }));

    // Start it up!
    try {
      server.start();
    } catch (final Exception e) {
      LOG.error("Error starting Jetty server", e);
    }

  }

}
