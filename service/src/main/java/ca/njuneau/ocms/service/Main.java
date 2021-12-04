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

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.TimeZone;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.njuneau.ocms.model.FridgeDAO;
import ca.njuneau.ocms.model.FridgeRowMapper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import io.prometheus.client.jetty.QueuedThreadPoolStatisticsCollector;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import spark.servlet.SparkApplication;

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

    LOG.info("Initializing metrics");
    DefaultExports.initialize();

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
    final var jettyThreadPool = new QueuedThreadPool();
    jettyThreadPool.setName("jetty");
    final var jettyThreadPoolStatisctics =
        new QueuedThreadPoolStatisticsCollector(jettyThreadPool, jettyThreadPool.getName());
    jettyThreadPoolStatisctics.register();

    final var jettyServer = new Server(jettyThreadPool);
    final var jettyConnector = new ServerConnector(jettyServer);
    jettyConnector.setPort(HTTP_PORT);
    jettyServer.addConnector(jettyConnector);
    final var jettyHandlers = new HandlerCollection();

    // Setup the application endpoint
    final var fridgeServletContext = new ServletContextHandler();
    fridgeServletContext.setContextPath("/fridge/");
    final var fridgeApp = new FridgeApplication(fridgeDao, validator, jsonBuilderFactory);
    final var sparkFilter = new CustomSparkFilter(new SparkApplication[] {fridgeApp});
    final var sparkFilterHolder = new FilterHolder(sparkFilter);
    fridgeServletContext.addFilter(sparkFilterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
    jettyHandlers.addHandler(fridgeServletContext);

    // Setup the metrics endpoint
    final var metricsServletContext = new ServletContextHandler();
    metricsServletContext.setContextPath("/metrics/");
    metricsServletContext.addServlet(MetricsServlet.class, "/*");
    jettyHandlers.addHandler(metricsServletContext);

    jettyServer.setHandler(jettyHandlers);

    // Setup the Jetty metrics
    final var statisticsHandler = new StatisticsHandler();
    statisticsHandler.setHandler(jettyServer.getHandler());
    jettyServer.setHandler(statisticsHandler);
    final var jettyStatisticsCollector = new JettyStatisticsCollector(statisticsHandler);
    jettyStatisticsCollector.register();


    // Register JVM shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        LOG.info("Stopping Jetty");
        jettyServer.stop();
        LOG.info("Stopping Hikari");
        hikariDS.close();
      } catch (final Exception e) {
        LOG.error("Clean shutdown failure", e);
      }
    }));

    // Start it up!
    try {
      jettyServer.start();
    } catch (final Exception e) {
      LOG.error("Error starting Jetty server", e);
    }

  }

}
