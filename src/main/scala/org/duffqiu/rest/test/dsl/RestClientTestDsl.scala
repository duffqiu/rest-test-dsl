package org.duffqiu.rest.test.dsl

import scala.collection.convert.WrapAsScala.iterableAsScalaIterable
import scala.collection.convert.WrapAsScala.mapAsScalaMap
import scala.language.implicitConversions
import scala.language.postfixOps

import org.duffqiu.rest.common.RestClient
import org.duffqiu.rest.common.RestClientConfig
import org.duffqiu.rest.common.RestOperation
import org.duffqiu.rest.common.RestRequest
import org.duffqiu.rest.common.RestResource
import org.duffqiu.rest.common.RestResponse
import org.duffqiu.rest.common.RestResult
import org.duffqiu.rest.test.dsl.RestCommonImplicits.string2RestResponse
import org.scalatest.Assertions
import org.scalatest.concurrent

import dispatch.Defaults.executor
import dispatch.Http
import dispatch.implyRequestHandlerTuple

object RestClientTestDsl extends concurrent.ScalaFutures with RestClientConfig with Assertions {

    type Client = RestClient
    type WithClientResource = (Client, RestResource)
    type WithClientResourceOperation = (Client, RestResource, RestOperation)
    type WithClientResourceOperationRequest[A] = (Client, RestResource, RestOperation, A)
    type WithClientResourceOperationRequestResult[A] = (Client, RestResource, RestOperation, A, RestResult)
    type Response4Test = RestResponse => Unit

    class ClientHelper(client: Client) {
        def on(port: Int) = {
            new RestClient(client.name, client.hostName, port)
        }

        def ask_for(resource: RestResource) = (client, resource)

        def end = Unit
    }

    class ClientResourceHelper(wcr: WithClientResource) {
        def to(operation: RestOperation) = (wcr._1, wcr._2, operation)
    }

    class ClientOperationHelper(wcro: WithClientResourceOperation) {
        def by[A](request: A) = (wcro._1, wcro._2, wcro._3, request)
    }

    class ClientRequestHelper[A](wcror: WithClientResourceOperationRequest[A]) {
        def should(result: RestResult) = (wcror._1, wcror._2, wcror._3, wcror._4, result)
    }

    class ClientResultHelper[A](wcrorr: WithClientResourceOperationRequestResult[A]) {
        def and_with(fun: Response4Test) = (wcrorr._1, wcrorr._2, wcrorr._3, wcrorr._4, wcrorr._5, fun)
    }

    implicit def string2RestClientHelper(name: String) = new RestClient(name)
    implicit def client2ClientHelper(client: Client) = new ClientHelper(client)
    implicit def withClientResource(wcr: WithClientResource) = new ClientResourceHelper(wcr)
    implicit def withClientOperation(wcro: WithClientResourceOperation) = new ClientOperationHelper(wcro)
    implicit def withClientRequest[A <: RestRequest](wcror: WithClientResourceOperationRequest[A]) = new ClientRequestHelper[A](wcror)
    implicit def withClientResult[A <: RestRequest, B <: RestResponse](wcrorr: WithClientResourceOperationRequestResult[A]) = new ClientResultHelper[A](wcrorr)

    implicit def Tuple2Client[A <: RestRequest](t: (Client, RestResource, RestOperation, A, RestResult, Response4Test)): ClientHelper = {
        t match {
            case ((client, resource, operation, request, result, resp2test)) => {

                val req = client.buildHttpRequest(resource, operation, request)

                whenReady(Http(req > { response => response })) {
                    response =>
                        {
                            val statusCode = response.getStatusCode()
                            val body = response.getResponseBody()
                            val httpHeaders = mapAsScalaMap(response.getHeaders())

                            val headerPara = httpHeaders map {
                                case (key, value) => {
                                    val valueList = iterableAsScalaIterable(value)
                                    //limitation, moco can't support a key with a list value in http header, but dispatch support.
                                    (key, valueList.head)
                                }
                            }

                            result.shouldMatch(statusCode) match {
                                case true => {
                                    val restResponse = ("RestResponse", statusCode) <:< (headerPara toMap) <<< body
                                    resp2test(restResponse)
                                }
                                case _ => fail("Expect result is not matched. Expect: " + result() + ", but get: "
                                    + statusCode + ", body is " + Option(body).filter(_.trim().nonEmpty).getOrElse("Empty"))
                            }
                        }
                }
            }
        }

        new ClientHelper(t._1)
    }

}
