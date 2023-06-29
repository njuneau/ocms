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
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.njuneau.ocms.model.FridgeDAO;
import ca.njuneau.ocms.model.FridgeRowMapper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;

import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import io.prometheus.client.jetty.QueuedThreadPoolStatisticsCollector;
import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Program entry point
 */
public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  // Don't do this in prod or I'll be very, very mad.
  private static final String DEFAULT_PG_JDBC_URL = "jdbc:postgresql://127.0.0.1:5432/test";
  private static final String DEFAULT_PG_USER = "test";
  private static final String DEFAULT_PG_PASS = "test";

  private static final int DEFAULT_HTTP_PORT = 8080;

  /**
   * Program entry point
   *
   * @param args Command-line arguments
   */
  public static void main(final String[] args) {
    // Setup command line arguments
    final var cliOptions = new Options();

    final var cliOptionHelp = new Option(
            "h",
            "help",
            false,
            "Display the command line help");
    cliOptions.addOption(cliOptionHelp);

    final var cliOptionPgUrl = new Option(
            "pgurl",
            true,
            "The Postgres JDBC connection URL (defaults to '" + DEFAULT_PG_JDBC_URL + "')");
    cliOptions.addOption(cliOptionPgUrl);

    final var cliOptionPgUser = new Option(
            "pguser",
            true,
            "The Postgres JDBC connection user (defaults to '" + DEFAULT_PG_USER + "')");
    cliOptions.addOption(cliOptionPgUser);

    final var cliOptionPgPassword = new Option(
            "pgpassword",
            true,
            "The Postgres JDBC connection password (defaults to '" + DEFAULT_PG_PASS + "')");
    cliOptions.addOption(cliOptionPgPassword);

    final var cliOptionHttpPort = new Option(
            "httpport",
            true,
            "The HTTP server port (defaults to '" + DEFAULT_HTTP_PORT + "')");
    cliOptions.addOption(cliOptionHttpPort);

    // Parse command line
    final var commandLineParser = new DefaultParser();
    final var helpFormatter = new HelpFormatter();
    CommandLine commandLine;
    try {
      commandLine = commandLineParser.parse(cliOptions, args);
    } catch (final ParseException e) {
      helpFormatter.printHelp("fridge", cliOptions);
      throw new IllegalArgumentException("Invalid command line arguments", e);
    }

    // Get command line arguments
    final String pgJdbcUrl = commandLine.getOptionValue(cliOptionPgUrl, DEFAULT_PG_JDBC_URL);
    final String pgJdbcUser = commandLine.getOptionValue(cliOptionPgUser, DEFAULT_PG_USER);
    final String pgJdbcPassword = commandLine.getOptionValue(cliOptionPgPassword, DEFAULT_PG_PASS);
    int httpPort;
    try {
      httpPort = Integer.parseInt(commandLine.getOptionValue(cliOptionHttpPort, Integer.toString(DEFAULT_HTTP_PORT)));
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("Invalid port number", e);
    }

    // Launch it!
    if (commandLine.hasOption(cliOptionHelp)) {
      helpFormatter.printHelp("fridge", cliOptions);
    } else {
      launchApplication(pgJdbcUrl, pgJdbcUser, pgJdbcPassword, httpPort);
    }
  }

  /**
   * Launches the application
   *
   * @param pgJdbcUrl The Postgres JDBC URL
   * @param pgJdbcUser The Postgres JDBC user
   * @param pgJdbcPassword The Postgres JDBC password
   * @param httpPort The HTTP server port
   */
  public static void launchApplication(
          final String pgJdbcUrl,
          final String pgJdbcUser,
          final String pgJdbcPassword,
          final int httpPort) {
    LOG.info("Setting clock to UTC");
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC.getId()));
    final Clock clock = Clock.systemUTC();

    LOG.info("Initializing metrics");
    DefaultExports.initialize();

    LOG.info("Creating database connection pool");
    final var hikariMetrics = new PrometheusMetricsTrackerFactory();
    final var hikariConfig = new HikariConfig();
    hikariConfig.setMetricsTrackerFactory(hikariMetrics);
    hikariConfig.setJdbcUrl(pgJdbcUrl);
    hikariConfig.setUsername(pgJdbcUser);
    hikariConfig.setPassword(pgJdbcPassword);
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
    final var jettyThreadPoolStatisctics
            = new QueuedThreadPoolStatisticsCollector(jettyThreadPool, jettyThreadPool.getName());
    jettyThreadPoolStatisctics.register();

    final var jettyServer = new Server(jettyThreadPool);
    final var jettyConnector = new ServerConnector(jettyServer);
    jettyConnector.setPort(httpPort);
    jettyServer.addConnector(jettyConnector);
    final var jettyHandlers = new HandlerCollection();

    // Setup the application endpoint
    final var fridgeServletContextHandler = new ServletContextHandler(null, "/fridge");
    final var fridgeServlet = new FridgeApplication(fridgeDao, validator, jsonBuilderFactory);
    final var fridgeErrorHandler = new FridgeErrorHandler(jsonBuilderFactory);
    final var fridgeServletHolder = new ServletHolder(fridgeServlet);
    fridgeServletContextHandler.addServlet(fridgeServletHolder, "/");
    fridgeServletContextHandler.setErrorHandler(fridgeErrorHandler);
    jettyHandlers.addHandler(fridgeServletContextHandler);

    // Setup the metrics endpoint
    final var metricsServletContext = new ServletContextHandler(null, "/metrics");
    metricsServletContext.addServlet(MetricsServlet.class, "/");
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

    // Start Jetty
    try {
      jettyServer.start();
    } catch (final Exception e) {
      LOG.error("Error starting Jetty server", e);
    }

  }

}
