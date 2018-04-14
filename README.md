### Building asynchronous and reactive applications with Eclipse Vert.x

In this repository, we are showcasing Eclipse Vert.x. The repo is organized as independent branches that are intentionally 
left unmerged as each one of them demonstrate some specific aspects of Eclipse Vert.x

#### The repo organisation's big picture

<p>
  <img src="https://github.com/alainlompo/reactive-vertx/blob/master/resources/repo-organisation.jpg?raw=true" />
</p>

The resources folder contains the ppt presentation as well as the corresponding pdf document.

#### Running the apps
In each branch we have one or more mvn module. To run the corresponding app

* Build the module, for example with ```mvn clean install`
* launch the module with ```mvn java -jar target/{module-artifact-id-here}-fat.jar```
* If everthing goes fine the terminal will display a start log indicating the running port also
* Launch your favorite browser and go to ```http://localhost:{the-port}```

#### Sample angular based spa wiki

<img src="https://github.com/alainlompo/reactive-vertx/blob/master/resources/app-showcasing.png?raw=true" />
