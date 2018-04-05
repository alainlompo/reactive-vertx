package org.techschulung.wikipeaks.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.Map;

/**
 * Created by alompo on 03.04.18.
 */
@ProxyGen
public interface WikiDatabaseService {

    @Fluent
    WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    WikiDatabaseService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    WikiDatabaseService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler);

    static WikiDatabaseService create(JDBCClient dbClient, Map<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler ) {
        return new WikiDatabaseServiceImpl(dbClient, sqlQueries,readyHandler);
    }

    static WikiDatabaseService createProxy(Vertx vertx, String address) {
        return new org.techschulung.wikipeaks.database.WikiDatabaseServiceVertxEBProxy(vertx, address);
    }
}