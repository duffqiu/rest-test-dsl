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

object RestServer {
    private[common] final val DEFAULT_NAME = "MocoServer"
    private[common] final val DEFAULT_PORT = 8080
}

class RestServer(val name: String = RestServer.DEFAULT_NAME, val port: Int = RestServer.DEFAULT_PORT) {
    val hit = requestHit() // not used when using log
    val server = port match { case 0 => httpserver(hit); case _ => httpserver(port, hit) }

    val mocoRun: Runner = runner(server)

    def startup = {
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

        val contentHandler = seq(text(response.bodyJson));

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
            case _ => and(by(uri(fullPath)), by(Moco.method(operationName)), json(text(request.bodyJson)))
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
