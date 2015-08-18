package bootstrap.liftweb

import net.liftweb.common.Loggable
import net.liftweb.util.{ LoggingAutoConfigurer, Props }
import io.undertow.{Undertow, Handlers}
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.FilterInfo

object Start extends App with Loggable {

  LoggingAutoConfigurer().apply()

  logger.info("run.mode: " + Props.modeName)
  logger.trace("system environment: " + sys.env)
  logger.trace("system props: " + sys.props)
  logger.info("liftweb props: " + Props.props)
  logger.info("args: " + args.toList)

  startLift()
  
  def startLift(): Unit = {
    logger.info("starting Lift server")

    val port = {
      val prop = Props.get("jetty.port", "8080")
      val str = if(prop startsWith "$") System.getenv(prop substring 1) else prop
      str.toInt
    }

    logger.info(s"port number is $port")

    val webappDir: String = Option(this.getClass.getClassLoader.getResource("webapp"))
      .map(_.toExternalForm)
      .filter(_.contains("jar:file:")) // this is a hack to distinguish in-jar mode from "expanded"
      .getOrElse("target/webapp")

    logger.info(s"webappDir: $webappDir")

    val servletBuilder = Servlets.deployment()
      .setClassLoader(this.getClass.getClassLoader())
      .setContextPath("/lift")
      .setDeploymentName("lift.war")
      .addFilter(new FilterInfo("Lift Filter", classOf[net.liftweb.http.LiftFilter]))

    val manager = Servlets.defaultContainer().addDeployment(servletBuilder)
    manager.deploy();
    val path = Handlers.path(Handlers.redirect("/lift"))
      .addPrefixPath("/lift", manager.start());

    val server = Undertow.builder()
      .addHttpListener(port, "localhost")
      .setHandler(path)
      .build();
    server.start();
  }

}
