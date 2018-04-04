package org.techschulung.wikipeaks;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.techschulung.wikipeaks.database.WikiDatabaseService;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by alompo on 30.01.18.
 */
public class HttpServerVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
    public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

    private static final String EMPTY_PAGE_MARKDOWN =
            "# A new page\n" +
                    "\n" +
                    "Feel-free to write in Markdown!\n";

    private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

    private String wikiDbQueue = "wikidb.queue";
    private WikiDatabaseService dbService;

    public static final String TITLE_KEY = "title";


    @Override
    public void start(Future<Void> startFuture) throws Exception {

        wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");
        dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.get("/").handler(this::indexHandler);
        router.get("/wiki/:page").handler(this::pageRenderingHandler);
        router.post().handler(BodyHandler.create());
        router.post("/save").handler(this::pageUpdateHandler);
        router.post("/create").handler(this::pageCreateHandler);
        router.post("/delete").handler(this::pageDeletionHandler);
        router.post("/search").handler(this::pagesSearchHandler);

        int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 9292);
        server
                .requestHandler(router::accept)
                .listen(portNumber, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server running on port " + portNumber);
                        startFuture.complete();
                    } else {
                        LOGGER.error("Could not start a HTTP server", ar.cause());
                        startFuture.fail(ar.cause());
                    }
                });
    }

    private void indexHandler(RoutingContext context) {

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "all-pages");

        vertx.eventBus().send(wikiDbQueue, new JsonObject(), options, reply -> {
            if (reply.succeeded()) {
                JsonObject body = (JsonObject) reply.result().body();
                context.put("title", "Wiki home");
                context.put("pages", body.getJsonArray("pages").getList());
                templateEngine.render(context, "templates", "/index.ftl", ar -> {
                    if (ar.succeeded()) {
                        context.response().putHeader("Content-Type", "text/html");
                        context.response().end(ar.result());
                    } else {
                        context.fail(ar.cause());
                    }
                });
            } else {
                context.fail(reply.cause());
            }
        });
    }

    private PageContentPart mapContentPartWrapper(String jsonArray) {
        return PageContentPart.parseJson(jsonArray);
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

    private void pagesSearchHandler(RoutingContext context) {

        String searchText = context.request().getParam("searchText");
        DeliveryOptions options = new DeliveryOptions().addHeader("action", "search-all-pages");
        JsonObject request = new JsonObject().put("searchText", "%" + searchText + "%");

        vertx.eventBus().send(wikiDbQueue, request, options, reply -> {
            if (reply.succeeded()) {

                JsonObject body = (JsonObject) reply.result().body();

                JsonArray pageContentPartsContainer = body.getJsonArray("pageContentParts");

                // TODO: all this part needs more refactoring
                final Function<String, PageContentPart> objectToContentPartTransformer = this::mapContentPartWrapper;
                final Function<PageContentPart, PageContentPart> contentPartReducer = contentPart -> reduced(contentPart, searchText);

                List<PageContentPart>  pageContentParts
                        = (List<PageContentPart>) pageContentPartsContainer.getList()
                        .stream()
                        .map(objectToContentPartTransformer)
                        .map(contentPartReducer)
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
                context.fail(reply.cause());
            }
        });
    }

    private void pageRenderingHandler(RoutingContext context) {

        String requestedPage = context.request().getParam("page");
        JsonObject request = new JsonObject().put("page", requestedPage);

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-page");
        vertx.eventBus().send(wikiDbQueue, request, options, reply -> {

            if (reply.succeeded()) {
                JsonObject body = (JsonObject) reply.result().body();

                boolean found = body.getBoolean("found");
                String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);
                context.put("title", requestedPage);
                context.put("id", body.getInteger("id", -1));
                context.put("newPage", found ? "no" : "yes");
                context.put("rawContent", rawContent);
                context.put("content", Processor.process(rawContent));
                context.put("timestamp", new Date().toString());

                templateEngine.render(context, "templates","/page.ftl", ar -> {
                    if (ar.succeeded()) {
                        context.response().putHeader("Content-Type", "text/html");
                        context.response().end(ar.result());
                    } else {
                        context.fail(ar.cause());
                    }
                });

            } else {
                context.fail(reply.cause());
            }
        });
    }

    private void pageUpdateHandler(RoutingContext context) {

        String title = context.request().getParam("title");
        JsonObject request = new JsonObject()
                .put("id", context.request().getParam("id"))
                .put("title", title)
                .put("markdown", context.request().getParam("markdown"));

        DeliveryOptions options = new DeliveryOptions();
        if ("yes".equals(context.request().getParam("newPage"))) {
            options.addHeader("action", "create-page");
        } else {
            options.addHeader("action", "save-page");
        }

        vertx.eventBus().send(wikiDbQueue, request, options, reply -> {
            if (reply.succeeded()) {
                context.response().setStatusCode(303);
                context.response().putHeader("Location", "/wiki/" + title);
                context.response().end();
            } else {
                context.fail(reply.cause());
            }
        });
    }

    private void pageCreateHandler(RoutingContext context) {
        String pageName = context.request().getParam("name");
        String location = "/wiki/" + pageName;
        if (pageName == null || pageName.isEmpty()) {
            location = "/";
        }
        context.response().setStatusCode(303);
        context.response().putHeader("Location", location);
        context.response().end();
    }

    private void pageDeletionHandler(RoutingContext context) {
        String id = context.request().getParam("id");
        JsonObject request = new JsonObject().put("id", id);
        DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete-page");
        vertx.eventBus().send(wikiDbQueue, request, options, reply -> {
            if (reply.succeeded()) {
                context.response().setStatusCode(303);
                context.response().putHeader("Location", "/");
                context.response().end();
            } else {
                context.fail(reply.cause());
            }
        });
    }
}
