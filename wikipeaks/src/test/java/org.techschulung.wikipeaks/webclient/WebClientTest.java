package org.techschulung.wikipeaks.webclient;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.techschulung.wikipeaks.database.WikiDatabaseService;
import org.techschulung.wikipeaks.database.WikiDatabaseVerticle;

/**
 * Created by alompo on 05.04.18.
 */
@RunWith(VertxUnitRunner.class)
public class WebClientTest {
    private Vertx vertx;
    private WikiDatabaseService sut;


    @Before
    public void prepare(TestContext context) throws InterruptedException {
        vertx = Vertx.vertx();

        JsonObject conf = new JsonObject()
                .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
                .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

        vertx.deployVerticle(new WikiDatabaseVerticle(), new DeploymentOptions().setConfig(conf),
                context.asyncAssertSuccess((id ->
                        sut = WikiDatabaseService.createProxy(vertx, WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE))));
    }
    @Test
    public void http_server_runsFine(TestContext context) {
        Async async = context.async();

        vertx.createHttpServer().requestHandler(
                req -> req.response().putHeader("Content-Type", "text/plain").end("Ok")
        )
                .listen(8080, context.asyncAssertSuccess(server -> {
                    WebClient webClient = WebClient.create(vertx);

                    webClient.get(8080, "localhost", "/").send(ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<Buffer> response = ar.result();
                            context.assertTrue(response.headers().contains("Content-Type"));
                            context.assertEquals("text/plain", response.getHeader("Content-Type"));
                            context.assertEquals("Ok", response.body().toString());
                            webClient.close();
                            async.complete();
                        } else {
                            async.resolve(Future.failedFuture(ar.cause()));
                        }
                    });
                }));
    }
}
