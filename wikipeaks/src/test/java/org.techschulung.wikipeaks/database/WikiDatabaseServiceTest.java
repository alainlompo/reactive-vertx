package org.techschulung.wikipeaks.database;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.techschulung.wikipeaks.database.WikiDatabaseService;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by alompo on 05.04.18.
 */
@RunWith(VertxUnitRunner.class)
public class WikiDatabaseServiceTest {
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

    @After
    public void finish(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void crud_operations_are_OK(TestContext context) {
        Async async = context.async();

        sut.createPage("Test", "Some content", context.asyncAssertSuccess(v1 -> {
            sut.fetchPage("Test", context.asyncAssertSuccess(json1 -> {
                context.assertTrue(json1.getBoolean("found"));
                context.assertTrue(json1.containsKey("id"));
                context.assertEquals("Some content", json1.getString("rawContent"));

                sut.savePage(json1.getInteger("id"), "Yo!", context.asyncAssertSuccess(v2 -> {
                   sut.fetchAllPages(context.asyncAssertSuccess(array1 -> {
                      context.assertEquals(1, array1.size());

                       sut.fetchPage("Test", context.asyncAssertSuccess(json2 -> {
                           context.assertEquals("Yo!", json2.getString("rawContent"));

                           sut.deletePage(json1.getInteger("id"), v3 -> {

                               sut.fetchAllPages(context.asyncAssertSuccess(array2 -> {
                                    context.assertTrue(array2.isEmpty());
                                   async.complete();
                               }));
                           });
                       }));
                   }));
                }));
            }));
        }));
        async.awaitSuccess(5000);
    }

    @Test
    public void contentPlaceHolfer_fuerTest() {
        assertThat(true).isTrue();
    }

}
