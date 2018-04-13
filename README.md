### The place to learn all about vert.x

#### Branch feature/refactoring-services

This branch is completely runnable and stays so and autonomous
It does not contain the refactoring into services with proxies and code generation
The next branch does that

#### Branch feature/vertx-services

This branch is derived from feature/refactoring-services. Here we refactor the code to create Db service and proxy and
to effectively use them


#### Branch feature/security.

Here we start by creating and adding a self signed certificate in our java keystore using the keytool command line tool:

```
keytool -genkey -alias test -keyalg RSA -keystore server-keystore.jks -keysize 2048 -validity 360 -dname CN=localhost -keypass secret -storepass secret
```
