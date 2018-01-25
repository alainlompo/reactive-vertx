### Integrating embedded hsqldb with Logging frameworks

 HSQLDB does not integrate well with loggers when embedded. <br/>
 By default it tries to reconfigure the logging system in place, <br/>
 so we need to disable it by passing a -Dhsqldb.reconfig_logging=false property to the Java Virtual Machine when executing applications.
