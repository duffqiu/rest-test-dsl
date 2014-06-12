package org.duffqiu.rest.common

import scala.collection.immutable.Traversable

import dispatch.{ :/ => :/ }
import dispatch.Http
import dispatch.Req
import dispatch.url

class RestClient(val name: String = "RestTestClient", val hostName: RestHost = LOCAL_HOST, val port: Int = 8080) {
    val host = :/(hostName.name, port)
    val client = new Http()

    def ->(hostName: RestHost) = {
        new RestClient(name, hostName, port)
    }

    def buildHttpRequest(resource: RestResource, operation: RestOperation, request: RestRequest) = {

        val fullPath = request.pathPara().foldLeft(resource.path) {
            (input: String, t: (String, String)) => input.replaceAllLiterally(t._1, t._2)
        }

        val reqWithUrl = url(host.url + fullPath)

        val reqWithMethod = resource.style match {
            case REST_STYLE => reqWithUrl.setMethod(operation())
            case _ => reqWithUrl.setMethod("POST")
        }

        val reqWithHeader = request.headerPara().foldLeft(reqWithMethod) {
            (req: Req, t: (String, String)) => req <:< Traversable((t._1, t._2))
        }

        val reqWithBody = request.body match {
            case EmptyBody => reqWithHeader
            case _ => reqWithHeader.setBody(request.bodyJson)
        }

        val reqWithAll = request.queryPara().foldLeft(reqWithBody) {
            (req: Req, t: (String, String)) => req <<? Traversable((t._1, t._2))
        }

        reqWithAll
    }

    def apply() = client

}