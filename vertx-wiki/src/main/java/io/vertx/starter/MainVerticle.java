package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {

  /*@Override
  public void start() {
    vertx.createHttpServer()
        .requestHandler(req -> req.response().end("Hello Vert.x!"))
        .listen(8080);
  }*/

  /**
   * By having each method returning a Future object, the implementation of the start method
   * becomes a composition
   * @param startFuture
   * @throws Exception
     */
  @Override
  public void start(Future<Void> startFuture) throws Exception{
    Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
    steps.setHandler(startFuture.completer());
  }

  private Future<Void> prepareDatabase() {
    Future<Void> future = Future.future();
    // (...)
    return future;
  }

  private Future<Void> startHttpServer() {
    Future<Void> future = Future.future();
    // (...)
    return future;
  }

}
