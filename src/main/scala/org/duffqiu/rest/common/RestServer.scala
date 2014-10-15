package org.duffqiu.rest.common

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.language.postfixOps

import com.github.dreamhead.moco.Moco
import com.github.dreamhead.moco.Moco.and
import com.github.dreamhead.moco.Moco.by
import com.github.dreamhead.moco.Moco.header
import com.github.dreamhead.moco.Moco.httpserver
import com.github.dreamhead.moco.Moco.json
import com.github.dreamhead.moco.Moco.query
import com.github.dreamhead.moco.Moco.seq
import com.github.dreamhead.moco.Moco.text
import com.github.dreamhead.moco.Moco.uri
import com.github.dreamhead.moco.MocoRequestHit.atLeast
import com.github.dreamhead.moco.MocoRequestHit.atMost
import com.github.dreamhead.moco.MocoRequestHit.never
import com.github.dreamhead.moco.MocoRequestHit.once
import com.github.dreamhead.moco.MocoRequestHit.requestHit
import com.github.dreamhead.moco.MocoRequestHit.times
import com.github.dreamhead.moco.Runner
import com.github.dreamhead.moco.Runner.runner
import com.github.dreamhead.moco.handler.AndResponseHandler
import com.github.dreamhead.moco.handler.StatusCodeResponseHandler
import com.github.dreamhead.moco.Moco.log

object RestServer {
  private[common] final val DEFAULT_NAME = "MocoServer"
  private[common] final val DEFAULT_PORT = 8080
}

class RestServer(val name: String = RestServer.DEFAULT_NAME, port: Int = RestServer.DEFAULT_PORT, needLog: Boolean = false) {
  val hit = requestHit() // not used when using log

  val server = (port, needLog) match {

    case (0, true) => httpserver(RestServer.DEFAULT_PORT, hit, log("moco.log"))

    case (0, false) => httpserver(RestServer.DEFAULT_PORT, hit)

    case (xPort, true) if validPort(xPort) => httpserver(xPort, hit, log("moco.log"))

    case (xPort, false) if validPort(xPort) => httpserver(xPort, hit)

    case _ => throw new Exception("Illegel port value")

  }

  val mocoRun: Runner = runner(server)

  private def validPort(port: Int) = {
    if (port > 0 && port < 65536) {
      true
    } else {
      false
    }
  }

  def withLog = new RestServer(name, port, true)

  def run = {
    mocoRun.start()
  }

  def stop = {
    mocoRun.stop()
  }

  def on(port: Int) = {
    new RestServer(name, port)
  }

  def serverPort = {
    server.port
  }

  def apply() = server

  def configMock(resource: RestResource, operation: RestOperation, request: RestRequest, response: RestResponse): Unit = {
    val handlers = buildMocoHandlers(response)

    val matcher = buildMocoMatcher(resource, operation, request)

    server.request(matcher).response(handlers)

  }

  def shouldHitTimes(resource: RestResource, operation: RestOperation, request: RestRequest, hitTimes: Int): Unit = {

    val matcher = buildMocoMatcher(resource, operation, request)

    //doesn't support hit matcher on method if rest style is RPC_Style since the method is always POST
    hit.synchronized {
      hit.verify(matcher, times(hitTimes))
    }

  }

  def shouldHitOnce(resource: RestResource, operation: RestOperation, request: RestRequest): Unit = {

    val matcher = buildMocoMatcher(resource, operation, request)
    hit.synchronized {
      hit.verify(matcher, once)
    }
  }

  def shouldHitNever(resource: RestResource, operation: RestOperation, request: RestRequest): Unit = {

    val matcher = buildMocoMatcher(resource, operation, request)
    hit.synchronized {
      hit.verify(matcher, never)
    }
  }

  def shouldHitAtMost(resource: RestResource, operation: RestOperation, request: RestRequest, hitTimes: Int): Unit = {

    val matcher = buildMocoMatcher(resource, operation, request)

    hit.synchronized {
      hit.verify(matcher, atMost(hitTimes))
    }
  }

  def shouldHitAtLeast(resource: RestResource, operation: RestOperation, request: RestRequest, hitTimes: Int): Unit = {

    val matcher = buildMocoMatcher(resource, operation, request)

    hit.synchronized {
      hit.verify(matcher, atLeast(hitTimes))
    }
  }

  private def buildMocoHandlers(response: RestResponse) = {
    val statusHandler = new StatusCodeResponseHandler(response.statusCode)
    val headerHandlerList = response.headerPara() map { case (key, value) => header(key, value) } toList

    //val contentHandler = seq(text(response.bodyJson));

    val bodyString = response.body match {
      case restBody: RestBody => response.bodyJson
      case x: Any => x.toString()
    }

    val contentHandler = seq(text(bodyString));

    val responseHandler = statusHandler :: contentHandler :: headerHandlerList

    val handlers = new AndResponseHandler(responseHandler.asJava)

    handlers

  }

  private def buildMocoMatcher(resource: RestResource, operation: RestOperation, request: RestRequest) = {
    val operationName = resource.style match {
      case REST_STYLE => operation()
      case _ => "POST"
    }

    val fullPath = request.pathPara().foldLeft(resource.path) {
      (input: String, t: (String, String)) => input.replaceAllLiterally(t._1, t._2)
    }

    val matcherWithBody = request.body match {
      case EmptyBody => and(by(uri(fullPath)), by(Moco.method(operationName)))
      case restBody: RestBody => and(by(uri(fullPath)), by(Moco.method(operationName)), json(text(request.bodyJson)))
      case x: Any => and(by(uri(fullPath)), by(Moco.method(operationName)), by(text(x.toString)))
    }

    val matcherWithHeader = request.headerPara().foldLeft(matcherWithBody) {
      (m, t) => and(m, Moco.eq(header(t._1), t._2))
    }

    val matcher = request.queryPara().foldLeft(matcherWithHeader) {
      (m, t) => and(m, Moco.eq(query(t._1), t._2))
    }

    matcher

  }

}
