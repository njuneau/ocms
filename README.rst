Ordered control microservice
============================

Description
-----------

This project is an exercise in Java microservices without boot-time indirection. All components are
initialized directly when possible and uses no IOC_ container whatsoever.

It is **not a framework** and **not to be used in production**.

But why?
--------

I try to answer the question:

  In a context where the application and its resources' startup are controlled by the developer, why
  would we want to use an IOC_ container?

Answering this question requires us to look at Java's ecosystem history. Now I don't claim to be an
historian, nor do I claim to hold the truth about everything. I am writing this from the point of
view of my own experience and observations. Feel free to suggest a PR if I am writing things you
believe to be blantatly wrong!

How did we get here
-------------------

The J2EE / Java EE era
''''''''''''''''''''''

My professional experience doesn't span as far back as the first versions of Java EE (historically
abbreviated J2EE for maximum confusion) so I'll start at a familiar point in time, Java EE 6 (2009).

At this point in history, I would argue the three main players in the Web space were Spring
Framework and whatever Java EE sauce you were using. Focusing on IOC, both had their own way of
declaring beans to be injected, buth both relied on the same core deployment principles:

You had an application server, hosted somewhere, managed by someone (maybe not you), on which
developers deployed either WAR archives or, for the enterprise-savvy, EAR archives
[#java-ee-deployment]_.

In an ideal scenario, the application server would:

* Provide communication resources such as:

  * Datasources
  * Messaging broker configurations

* Provide a standard set of libraries that are commonly used by applications deployed on it.

In such a context, using IOC makes a lot of sense: as an application developer, you didn't want to
care how to obtain database connections, to wire yourself up to the authentication layer or how to
register yourself to message brokers. You would instruct, in your application, that you wanted a
certain resource with the expectation that the application server would give it to you.

For example:

.. code:: java

  @Stateless
  public class MyComponent {

    @Resource("the-datasource")
    private DataSource dataSource;

  }

Whether it was XML-based or annotation-based, it became very common for component entry points to
become very... magic [#magic]_ in the way they were initialized.

The developer rebel alliance era
''''''''''''''''''''''''''''''''

As a lot of you likely experienced, it was not uncommon for application servers to be:

* Touched only by order and benediction of a select few
* Outdated as upgrading application servers, its provided libraries or configurations would mean
  potentially breaking a lot of applications.

Without diving into the whys of this being a common occurrence, application deployments were less
and less reliant on appication server resources (especially when it came to libraries). So it became
common practice to ship WARs or EARs with all of the application's dependencies in them, sometimes
overriding even the ones provided by the application server.

Docker containers were not omnipresent *yet* (in 2012), so application still relied on applications
server-provided resources when it came to datasources and the like, but much less so when it came
to libraries.

This trend would eventually lead to the mass use of uber-JARs (they were not new at that time but
not commonplace) and, after some iterations on the concept and the popularization of Docker
containers, runnable uber-JARs that didn't rely on pre-existing application servers for deployment.

Among the key players are:

* `Spring Boot`_
* `Wildfly Swarm`_ (now the defunct Thorntail_)

These two projects seemed to have caused some kind of mass hysteria: it was *the* new way to do
development and, for better or worse, were deployed en-masse very early. Whether it was as
replacement for old application servers or even deployed side-by-side (to much dismay of many system
administrators), "boot-type" applications became the de-facto standard, just as it was also very
trendy to shout "DevOps" all over the place.

However, the underlying technologies remained very similar to the old Java EE stack - somewhere
underneath you still had a servlet continer, a connection pool or a datasource of some kind, and
lots and lots of annotations.

The microservice era
''''''''''''''''''''

This is where we are at the moment. I won't be discussing whether microservices are a sound approach
or not, I'm merely going to state how things seem to look like in terms of deployment.

After application servers were displaced, applications needed a place to live in. Today, the place
of predilection would be Docker containers. As such, the packaging and deployment workflow is more
or less a variation of the following:

* The developers use plugins to produce a JAR that can be executed independently
* The application dependencies are shipped with the application (either inside an uber-JAR or
  a set of files copied onto the resulting Docker image)
* Resources are managed by the application itself
* Application launch (read: ``public static void main(String argv[]) {}`` is in the hands of the
  developer.

And so...
---------

With that in mind, we can go full circle to the initial question: in such a context, what are the
benefits of having an IOC container?

* Resources are under your control
* Application initialization is under your control
* Many components such as HTTP servers are now embeddable [#jetty-server-api]_ [#undertow]_
* The servlet layer is less omnipresent with the advent of reactor-based servers [#spring-webflux]_
  [#vertx]_ , reducing the need for classical Java EE application scaffolding and configuration
  (even that is arguably not that hard)

I believe that in this context, IOC-based initialization and configuration is awkward: there's very
little point of wanting control over the whole packaging and initialization of the application, only
to ironically reverse the flow at the first opportunty:

.. code:: java

  public class MyApplication {

    public static void main(final String[] argv) {
      // Launch IOC container (scan annotations, read XML config, whatevs...)
      ApplicationFramework.run(MyApplication.class);
    }

  }


This repository contains a sample application with the following components:

* A metrics manager
* A database connection pool
* A bean validation facility
* A JSON (de)serialization facility
* An HTTP server

... which are all launched manually, without an IOC container or "all-encompassing" framework.

Launching without full-blown IOC is nothing new. You can take a look at `Vert.x`_ for example.

However, launching individual components manually is less common.

Tradeoffs of that approach
--------------------------

I have this theory, founded on no prior research mind you, that developers (Java ones that is,
present company included) love to create *the* framework that fits like a glove.

Looking at this repository's code, it is conceivable that one would want to put the initialization
code in an abstract class, perhaps introducing some sort of "component" framework. Doing so would be
defeating the whole point : there are *already a lot of component frameworks out there*. Don't do
like I did in the past and start creating your own "rightsized" framework-that-is-not-a-framework.
It will most likely end up like a Spring lookalike, but worse and only known by the ones working in
your organization.

I believe that launching an applications' lower-level components individually with fine-grained
control like in this repository is interesting *when framework-provided defaults start being more
annoying than helpful*.

Anyhow, I hope you had fun reading this and I hope it will spark some thinking and discussion on
your end. Have fun with the code!

How to compile and test the service
-----------------------------------

To compile, you need:

* JDK >= 11
* Maven
* VirtualBox
* Vagrant

Startup the database server like so:

.. code:: sh

  cd vagrant
  vagrant up

Compile the project:

.. code:: sh

  mvn clean package

Launch it:

.. code:: sh

  java -jar service/target/service-0.0.0-SNAPSHOT-assembly/service-0.0.0-SNAPSHOT.jar


You can then go on http://127.0.0.1:8080/fridge/ with your browser. The application is a very very
basic refrigerator content manager. Going to ``/fridge/`` will give you the list of items in your fridge.

To insert content in your fridge:

.. code:: sh

  curl -X 'POST' -d 'name=bread' -d 'date-expiry=2021-11-28T00:00:00' http://127.0.0.1:8080/fridge/

Dates are all UTC.

You can also consult the Prometheus metrics at ``http://127.0.0.1:8080/metrics/``

.. [#java-ee-deployment] https://jakarta.ee/specifications/platform/9/jakarta-platform-spec-9.html#application-assembly-and-deployment
.. [#magic] Magic in a sense that not many people would fully understand how the services' initialization actually ended up working
.. [#jetty-server-api] https://www.eclipse.org/jetty/documentation/jetty-11/programming-guide/index.html#pg-server-http
.. [#undertow] https://undertow.io/
.. [#spring-webflux] https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-fn
.. [#vertx] https://vertx.io/docs/vertx-web/java/#_re_cap_on_vert_x_core_http_servers

.. _IOC: https://en.wikipedia.org/wiki/Inversion_of_control
.. _Spring Boot: https://spring.io/projects/spring-boot
.. _Wildfly Swarm: https://www.wildfly.org/news/2015/05/05/WildFly-Swarm-Released/
.. _Thorntail: https://thorntail.io/posts/the-end-of-an-era/
.. _Vert.x: https://vertx.io
