/*
 *  Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *  Copyright (c) 2017 INSA Lyon, CITI Laboratory.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.techschulung.wikipeaks;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  // tag::sql-fields[]
  private static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key, Name varchar(255) unique, Content clob)";
  private static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?"; // <1>
  private static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
  private static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
  private static final String SQL_ALL_PAGES = "select Name from Pages";
  private static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";
  private static final String SQL_SEARCH_PAGES = "Select * from Pages where Content like ?";
  // end::sql-fields[]

  public static final String TITLE_KEY = "title";
  private static final String EMPTY_PAGE_MARKDOWN =
          "# A new page\n" +
                  "\n" +
                  "Feel-free to write in Markdown!\n";

  // tag::db-and-logger[]
  private JDBCClient dbClient;
  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  // end::db-and-logger[]

  private static final String LOCATION_HEADER = "Location";
  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  // tag::prepareDatabase[]
  private Future<Void> prepareDatabase() {
    Future<Void> future = Future.future();

    dbClient = JDBCClient.createShared(vertx, new JsonObject()  // <1>
      .put("url", "jdbc:hsqldb:file:db/wiki")   // <2>
      .put("driver_class", "org.hsqldb.jdbcDriver")   // <3>
      .put("max_pool_size", 30));   // <4>

    dbClient.getConnection(ar -> {    // <5>
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        future.fail(ar.cause());    // <6>
      } else {
        SQLConnection connection = ar.result();   // <7>
        connection.execute(SQL_CREATE_PAGES_TABLE, create -> {
          connection.close();   // <8>
          if (create.failed()) {
            LOGGER.error("Database preparation error", create.cause());
            future.fail(create.cause());
          } else {
            future.complete();  // <9>
          }
        });
      }
    });

    return future;
  }
  // end::prepareDatabase[]

  // tag::startHttpServer[]
  private Future<Void> startHttpServer() {
    Future<Void> future = Future.future();
    HttpServer server = vertx.createHttpServer();   // <1>

    Router router = Router.router(vertx);   // <2>
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler); // <3>
    router.post().handler(BodyHandler.create());  // <4>
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);
    router.post("/search").handler(this::pagesSearchHandler);

    server
      .requestHandler(router::accept)   // <5>
      .listen(9292, ar -> {   // <6>
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port 9292");
          future.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          future.fail(ar.cause());
        }
      });

    return future;
  }
  // end::startHttpServer[]


  // tag::pageDeletionHandler[]
  private void pageDeletionHandler(RoutingContext context) {
    String id = context.request().getParam("id");
    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection connection = car.result();
        connection.updateWithParams(SQL_DELETE_PAGE, new JsonArray().add(id), res -> {
          connection.close();
          if (res.succeeded()) {
            context.response().setStatusCode(303);
            context.response().putHeader(LOCATION_HEADER, "/");
            context.response().end();
          } else {
            context.fail(res.cause());
          }
        });
      } else {
        context.fail(car.cause());
      }
    });
  }
  // end::pageDeletionHandler[]

  // tag::pageCreateHandler[]
  private void pageCreateHandler(RoutingContext context) {
    String pageName = context.request().getParam("name");
    String location = "/wiki/" + pageName;
    if (pageName == null || pageName.isEmpty()) {
      location = "/";
    }
    context.response().setStatusCode(303);
    context.response().putHeader(LOCATION_HEADER, location);
    context.response().end();
  }
  // end::pageCreateHandler[]

  // tag::indexHandler[]
  private void indexHandler(RoutingContext context) {
    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection connection = car.result();
        connection.query(SQL_ALL_PAGES, res -> {
          connection.close();

          if (res.succeeded()) {
            List<String> pages = res.result() // <1>
              .getResults()
              .stream()
              .map(json -> json.getString(0))
              .sorted()
              .collect(Collectors.toList());

            context.put(TITLE_KEY, "Wikipeaks home");  // <2>
            context.put("pages", pages);
            templateEngine.render(context, "templates", "/index.ftl", ar -> {   // <3>
              if (ar.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().end(ar.result());  // <4>
              } else {
                context.fail(ar.cause());
              }
            });

          } else {
            context.fail(res.cause());  // <5>
          }
        });
      } else {
        context.fail(car.cause());
      }
    });
  }
  // end::indexHandler[]

  // tag::pageUpdateHandler[]
  private void pageUpdateHandler(RoutingContext context) {
    String id = context.request().getParam("id");   // <1>
    String title = context.request().getParam(TITLE_KEY);
    String markdown = context.request().getParam("markdown");
    boolean newPage = "yes".equals(context.request().getParam("newPage"));  // <2>

    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection connection = car.result();
        String sql = newPage ? SQL_CREATE_PAGE : SQL_SAVE_PAGE;
        JsonArray params = new JsonArray();   // <3>
        if (newPage) {
          params.add(title).add(markdown);
        } else {
          params.add(markdown).add(id);
        }
        connection.updateWithParams(sql, params, res -> {   // <4>
          connection.close();
          if (res.succeeded()) {
            context.response().setStatusCode(303);    // <5>
            context.response().putHeader(LOCATION_HEADER, "/wiki/" + title);
            context.response().end();
          } else {
            context.fail(res.cause());
          }
        });
      } else {
        context.fail(car.cause());
      }
    });
  }
  // end::pageUpdateHandler[]

  // tag::pagesSearchHandler[]
  private void pagesSearchHandler(RoutingContext context) {

    String searchText = context.request().getParam("searchText");
    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection connection = car.result();
        connection.queryWithParams(SQL_SEARCH_PAGES, new JsonArray().add("%" + searchText + "%"), fetch -> {
          connection.close();

          if (fetch.succeeded()) {
            List<PageContentPart> pageContentParts
                    = fetch.result().getResults()
                    .stream()
                    .map(this::mapContentPart)
                    .map(pageContentPart -> reduced(pageContentPart, searchText))
                    .collect(Collectors.toList());

            LOGGER.debug("Found {} pages that matches the {} search criteria", pageContentParts.size(), searchText);

            context.put(TITLE_KEY, "Search results....");
            context.put("pageContentParts", pageContentParts);
            context.put("searchText", searchText);

            templateEngine.render(context, "templates", "/search.ftl", ar -> {
              if (ar.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().end(ar.result());
              } else {
                context.fail(ar.cause());
              }
            });
          } else {
            context.fail(fetch.cause());
          }
        });
      } else {
        context.fail(car.cause());
      }
    });
  }
  // end::pagesSearchHandler[]

  // tag::pageRenderingHandler[]
  private void pageRenderingHandler(RoutingContext context) {
    String page = context.request().getParam("page");   // <1>

    dbClient.getConnection(car -> {
      if (car.succeeded()) {

        SQLConnection connection = car.result();
        connection.queryWithParams(SQL_GET_PAGE, new JsonArray().add(page), fetch -> {  // <2>
          connection.close();
          if (fetch.succeeded()) {

            JsonArray row = fetch.result().getResults()
              .stream()
              .findFirst()
              .orElseGet(() -> new JsonArray().add(-1).add(EMPTY_PAGE_MARKDOWN));
            Integer id = row.getInteger(0);
            String rawContent = row.getString(1);

            context.put("title", page);
            context.put("id", id);
            context.put("newPage", fetch.result().getResults().isEmpty() ? "yes" : "no");
            context.put("rawContent", rawContent);
            context.put("content", Processor.process(rawContent));  // <3>
            context.put("timestamp", new Date().toString());

            templateEngine.render(context, "templates", "/page.ftl", ar -> {
              if (ar.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().end(ar.result());
              } else {
                context.fail(ar.cause());
              }
            });
          } else {
            context.fail(fetch.cause());
          }
        });

      } else {
        context.fail(car.cause());
      }
    });
  }
  // end::pageRenderingHandler[]

  // tag::start[]
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
    steps.setHandler(startFuture.completer());
  }
  // end::start[]

  public void anotherStart(Future<Void> startFuture) {
    // tag::another-start[]
    Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
    steps.setHandler(ar -> {  // <1>
      if (ar.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });
    // end::another-start[]
  }

  private PageContentPart mapContentPart(JsonArray jsonArray) {
    return new PageContentPart(jsonArray.getString(1), jsonArray.getString(2));
  }

  private PageContentPart reduced(PageContentPart pageContentPart, String searchCriteria)  {
    if (pageContentPart == null) {
      return null;
    }

    pageContentPart.setContentPart(
            extractPart(pageContentPart.getContentPart(), searchCriteria, 7, 50).orElse("")
    );

    return pageContentPart;
  }

  /**
   *
   * @param source
   * @param part
   * @param extractionLenght: should be strictly superior to part's length
   * @param upFrontLength: that small part before the searched component. upFrontLength + part.length should be
   *                     strictly inferior to extractionLength
   * @return The searched piece of String with its environning text
   */
  private Optional<String> extractPart(String source, String part, int extractionLenght, int upFrontLength) {
    if (source == null || source.isEmpty() || part == null || part.isEmpty()) {
      return Optional.empty();
    }

    int position = source.indexOf(part);
    if (position < 0) {
      return Optional.empty();
    }

    if (position - upFrontLength >= 0) {
      return Optional.of(source.substring(position-upFrontLength, position-upFrontLength+extractionLenght) + "...");
    } else if (position + extractionLenght < source.length()) {
      return Optional.of(source.substring(position, position + extractionLenght));
    } else {
      return Optional.of(source.substring(position));
    }
  }
}
