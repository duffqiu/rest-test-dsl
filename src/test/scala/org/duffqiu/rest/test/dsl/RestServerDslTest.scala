package org.duffqiu.rest.test.dsl

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.language.postfixOps

import org.duffqiu.rest.common.CLIENTERROR
import org.duffqiu.rest.common.CREATE
import org.duffqiu.rest.common.DELETE
import org.duffqiu.rest.common.LOCAL_HOST
import org.duffqiu.rest.common.QUERY
import org.duffqiu.rest.common.REST_STYLE
import org.duffqiu.rest.common.RPC_STYLE
import org.duffqiu.rest.common.RestBody
import org.duffqiu.rest.common.RestClientConfig
import org.duffqiu.rest.common.RestRequest
import org.duffqiu.rest.common.RestResponse
import org.duffqiu.rest.common.RestServer
import org.duffqiu.rest.common.RestClient
import org.duffqiu.rest.common.SUCCESS
import org.duffqiu.rest.test.actor.RUN_REST_SERVER
import org.duffqiu.rest.test.actor.RestClientMasterActor
import org.duffqiu.rest.test.actor.RestClientWorkActor
import org.duffqiu.rest.test.actor.RestServerActor
import org.duffqiu.rest.test.actor.RestTestResourceBatchMatchMsg
import org.duffqiu.rest.test.actor.RestTestTaskBatchMsg
import org.duffqiu.rest.test.dsl.RestClientTestDsl.client2ClientHelper
import org.duffqiu.rest.test.dsl.RestClientTestDsl.string2RestClientHelper
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientOperation
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientRequest
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientResource
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientResult
import org.duffqiu.rest.test.dsl.RestCommonImplicits.restStyle2ResourceHelper
import org.duffqiu.rest.test.dsl.RestCommonImplicits.string2RestRequest
import org.duffqiu.rest.test.dsl.RestCommonImplicits.string2RestResponse
import org.duffqiu.rest.test.dsl.RestServerTestDsl.server2ServerHelper
import org.duffqiu.rest.test.dsl.RestServerTestDsl.string2RestServerHelper
import org.duffqiu.rest.test.dsl.RestServerTestDsl.withServerOperation
import org.duffqiu.rest.test.dsl.RestServerTestDsl.withServerRequest
import org.duffqiu.rest.test.dsl.RestServerTestDsl.withServerResource
import org.duffqiu.rest.test.dsl.RestServerTestDsl.responseToRequest2Response
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.Finders
import org.scalatest.FunSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers
import org.scalatest.concurrent
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import net.liftweb.json.DefaultFormats
import net.liftweb.json.parse

case class VService(serviceName: String = "name1", subscriptionStatus: String = "ok")
case class IMSI_RequestBody(vimsi: String = "+12121", msisdn: String = "+86233232", imsi: String = "+234234232432",
                            service: VService = VService()) extends RestBody

class RestServerDslTest extends FunSpec with Matchers with BeforeAndAfter
  with GivenWhenThen with concurrent.ScalaFutures with RestClientConfig
  with GeneratorDrivenPropertyChecks {

  var aServer: RestServer = _
  var request4Check: RestRequest = _
  var aClient: RestClient = _

  val TEST_PORT = 8181

  describe("Rest server and client DSL Testing") {

    before {
      aServer = "Server" on TEST_PORT withLog

      aClient = "Client" -> LOCAL_HOST on TEST_PORT

    }

    after {
      aClient.stop
      aServer.stop
    }

    it("Should on 38080 success") {
      val vIMSI = "vIMSI_Manager" on 38080

      vIMSI.serverPort shouldBe 38080

      vIMSI.run

      vIMSI.name shouldBe "vIMSI_Manager"

      vIMSI.stop

    }

    it("Should on 10801 success") {
      val vIMSI = "vIMSI_Manager" on 10801

      vIMSI.serverPort shouldBe 10801

      vIMSI.run

      vIMSI.name shouldBe "vIMSI_Manager"

      vIMSI.stop

    }

    it("Should be OK using rest style resource when query with dsl client") {

      Given("request")

      val resource = REST_STYLE / "/vIMSI"

      val request = "Request" <:< ("Content-Type", "application/json") <<? ("IMSI", "+23232") <<? ("subscriptionStatus", "activated") <</
        ("{vIMSI}", "2323232")

      val requestDouble = RestRequest() <:< ("Content-Type", "application/json")

      val response = ("Response", 200) <<< IMSI_RequestBody()

      When("Prepare server resource and startup")

      (aServer own resource when CREATE given request then response
        and resource when QUERY given requestDouble then response
        and resource when DELETE given request then response run)

      Then("Client call server, the response status shall be SUCCESS")

      aClient ask_for resource to QUERY by request should SUCCESS and_with {
        resp: RestResponse =>
          {
            resp.statusCode shouldBe 200
            resp.bodyJson.contains("msisdn") shouldBe true

            val jsValue = parse(resp.bodyJson)
            implicit val formats = DefaultFormats

            val msisdn = (jsValue \\ "msisdn").extract[String]

            assert(msisdn === "+86233232")
          }
      }

      aClient ask_for resource to QUERY by request should SUCCESS and_with {
        resp: RestResponse =>
          {
            resp.statusCode shouldBe 200
            resp.bodyJson.contains("msisdn") shouldBe true

            val jsValue = parse(resp.bodyJson)
            implicit val formats = DefaultFormats

            val imsi = (jsValue \\ "imsi").extract[String]
            val serviceName = (jsValue \\ "service" \\ "serviceName").extract[String]

            val imsiBody = jsValue.extract[IMSI_RequestBody]
            assert(imsi === "+234234232432")
            assert(serviceName === "name1")
            imsiBody.imsi shouldBe "+234234232432"
          }
      }

    }

    it("Should be OK using rest style resource when create with header, with query parameters, with body by dsl client") {
      Given("request")

      val resource = REST_STYLE / "/vimsi/{vimsi}"

      val request = ("Request" <</ ("{vimsi}", "+2323232") <:< ("Content-Type", "application/json") <:< ("location", "us")
        <<< IMSI_RequestBody() <<? ("imsi", "+23232") <<? ("subscriptionstatus", "activated"))

      val response = ("Response", 200) <<< IMSI_RequestBody()

      When("Prepare server resource and startup")

      aServer own resource when CREATE given request then response run

      Then("Client call server, the response status shall be SUCCESS")

      aClient ask_for resource to CREATE by request should SUCCESS and_with {
        resp: RestResponse =>
          {
            resp.statusCode shouldBe 200
            resp.bodyJson.contains("msisdn") shouldBe true

            val jsValue = parse(resp.bodyJson)
            implicit val formats = DefaultFormats

            val msisdn = (jsValue \\ "msisdn").extract[String]

            assert(msisdn === "+86233232")
          }
      }

    }

    it("Should be OK using rpc style resource when delete with header, with query parameters, with body by dsl client, support server hit check") {
      Given("request")

      val resource = RPC_STYLE / "/vimsi/{vimsi}/delete"

      val request = "Request" <</ ("{vimsi}", "+2323232") <:< ("Content-Type", "application/json") <:< ("location", "us") <<<
        IMSI_RequestBody() <<? ("imsi", "+23232") <<? ("subscriptionstatus", "activated")

      val response = ("Response", 200) <:< ("Content-Type", "application/json") <:< ("location", "us")

      When("Prepare server resource and startup")

      aServer own resource when DELETE given request then response run

      Then("Client call server, the response status shall be SUCCESS")

      aClient ask_for resource to DELETE by request should SUCCESS and_with {
        resp: RestResponse =>
          {
            resp.statusCode shouldBe 200
            aServer shouldHitTimes (resource, CREATE, request, 1)
            aServer shouldHitOnce (resource, CREATE, request)
            aServer shouldHitAtLeast (resource, CREATE, request, 1)
            aServer shouldHitAtMost (resource, CREATE, request, 1)
          }
      }

    }

    it("Should be OK using rest style with request created by generator with colletion pare") {
      Given("request")

      val resource = REST_STYLE / "/vimsi/{vimsi}"

      val reqs = for {
        tel <- Gen.oneOf("tel:", "+")
        vimsiRamdom <- Gen.numStr
        vimsi <- Gen.const(Option(vimsiRamdom) match { case Some(x) if (x.length() > 10 && x.length() < 20) => x; case _ => "89900" })
        imsi <- Gen.numStr
        serviceName <- Gen.oneOf("vowifi", "mca", "vvm")
        subscriptionStatus <- Gen.oneOf("activated", "deactivated")
      } yield ("Request" <</ ("{vimsi}", tel + vimsi) <:< ("Content-Type", "application/json") <:< ("location", "us") <<<
        IMSI_RequestBody(imsi = imsi, service = VService(subscriptionStatus = subscriptionStatus, serviceName = serviceName)) <<?
        ("subscriptionstatus", subscriptionStatus))

      When("Prepare server resource and startup")

      val req_resp: scala.collection.mutable.Map[RestRequest, RestResponse] = new java.util.concurrent.ConcurrentHashMap[RestRequest, RestResponse]

      forAll(reqs, workers(10)) {
        req =>
          {
            val body = req.body match {
              case b: IMSI_RequestBody => b
            }
            val resp = ("Response", 200) <:< ("Content-Type", req >:> ("Content-Type")) <:< ("location", req >:> ("location")) <<<
              IMSI_RequestBody(vimsi = req.>>/("{vimsi}"), imsi = body.imsi, service = VService(subscriptionStatus = req >>?
                ("subscriptionstatus"), serviceName = body.service.serviceName))
            req_resp.put(req, resp)
          }
      }

      req_resp.foldLeft(aServer) {
        (serv, t: (RestRequest, RestResponse)) =>
          serv own resource when DELETE given t._1 then t._2
      } run

      Then("Client call server, the response status shall be SUCCESS with response")

      //can'tuse par when need to use hit methods
      req_resp.par.foreach {
        t =>
          aClient ask_for resource to DELETE by t._1 should SUCCESS and_with {
            resp: RestResponse =>
              {
                resp.statusCode shouldBe 200
                resp.body shouldEqual t._2.bodyJson
              }
          }

      }

    }

    it("Should be OK using rest style with request created by generator with mutiple works") {

      Given("request and corresponding response")

      val resource1 = REST_STYLE / "/vimsi/{vimsi}"

      val reqs = for {
        tel <- Gen.oneOf("tel:", "+")
        vimsiRamdom <- Gen.numStr
        vimsi <- Gen.const(Option(vimsiRamdom) match { case Some(x) if (x.length() > 10 && x.length() < 20) => x; case _ => "89900" })
        imsi <- Gen.numStr
        serviceName <- Gen.oneOf("wifi", "sms", "mms")
        subscriptionStatus <- Gen.oneOf("activated", "deactivated")
      } yield ("Request" <</ ("{vimsi}", tel + vimsi) <:< ("Content-Type", "application/json") <:< ("location", "us") <<<
        IMSI_RequestBody(imsi = imsi, service = VService(subscriptionStatus = subscriptionStatus,
          serviceName = serviceName)) <<? ("subscriptionstatus", subscriptionStatus))

      val req_resp: scala.collection.mutable.Map[RestRequest, RestResponse] = new java.util.concurrent.ConcurrentHashMap[RestRequest, RestResponse]

      forAll(reqs, workers(5)) {
        req =>
          {
            val body = req.body match {
              case b: IMSI_RequestBody => b
            }
            val resp = ("Response", 400) <:< ("Content-Type", req >:> ("Content-Type")) <:<
              ("location", req >:> ("location")) <<< IMSI_RequestBody(vimsi = req.>>/("{vimsi}"), imsi = body.imsi,
                service = VService(subscriptionStatus = req >>? ("subscriptionstatus"), serviceName = body.service.serviceName))
            req_resp.put(req, resp)
          }
      }

      val reqs2 = for {
        tel <- Gen.oneOf("tel:", "+")
        vimsiRamdom <- Gen.numStr
        vimsi <- Gen.const(Option(vimsiRamdom) match { case Some(x) if (x.length() > 10 && x.length() < 20) => x; case _ => "89900" })
        imsi <- Gen.numStr
        serviceName <- Gen.oneOf("wifi", "sms", "mms")
        subscriptionStatus <- Gen.oneOf("activated", "deactivated", "enabled", "disabled")
      } yield ("Request" <</ ("{vimsi}", tel + vimsi) <:< ("Content-Type", "application/json") <:< ("location", "cn") <<<
        IMSI_RequestBody(imsi = imsi, service = VService(subscriptionStatus = subscriptionStatus,
          serviceName = serviceName)) <<? ("subscriptionstatus", subscriptionStatus))

      val req_resp2: scala.collection.mutable.Map[RestRequest, RestResponse] = new java.util.concurrent.ConcurrentHashMap[RestRequest, RestResponse]

      forAll(reqs2, workers(5)) {
        req =>
          {
            val body = req.body match {
              case b: IMSI_RequestBody => b
            }
            val resp = ("Response", 200) <:< ("Content-Type", req >:> ("Content-Type")) <:< ("location", req >:>
              ("location")) <<< IMSI_RequestBody(vimsi = req.>>/("{vimsi}"), imsi = body.imsi,
                service = VService(subscriptionStatus = req >>? ("subscriptionstatus"), serviceName = body.service.serviceName))
            req_resp2.put(req, resp)
          }
      }

      When("Prepare server resource and startup")

      val servActor = new RestServerActor("RestServer", 9999)

      servActor.start

      servActor ! RestTestResourceBatchMatchMsg(resource1, DELETE, req_resp.toMap)
      servActor ! RestTestResourceBatchMatchMsg(resource1, QUERY, req_resp2.toMap)

      //get the RestServer after run
      val restServ = servActor !? RUN_REST_SERVER match {
        case server: RestServer => server
        case _ =>
          fail("[Test] got unknow message from rest server actor")
      }

      And("Start client master and client workers")

      val masterActor = new RestClientMasterActor()
      masterActor.start

      (1 to 20) foreach {
        i =>
          val worker = new RestClientWorkActor("Client" + i, masterActor, restServ, LOCAL_HOST, 9999, {
            case (server, resource1, req, QUERY, resp, resultResponse) =>
              {
                resultResponse.statusCode shouldBe 200
                //                                server shouldHitAtLeast (resource, operation, req, 1)
                resultResponse.body shouldEqual resp.bodyJson
                //                                println("[Test]run check for query")
              }
            case (server, resource1, req, DELETE, resp, resultResponse) =>
              {
                resultResponse.statusCode shouldBe 400
                //                                server shouldHitAtLeast (resource, operation, req, 1)
                resultResponse.body shouldEqual resp.bodyJson
                //                                println("[Test]run check for delete")
              }
            case (_, _, req, _, _, _) => println("[Test]got unknow message: " + req)

          })
          masterActor ! worker
      }

      Then("Client call server, the response status shall be SUCCESS with response")

      masterActor ! RestTestTaskBatchMsg(resource1, DELETE, req_resp.toMap, CLIENTERROR)

      masterActor ! RestTestTaskBatchMsg(resource1, QUERY, req_resp2.toMap, SUCCESS)

      masterActor.stop

      servActor.stop

    }

    it("should be OK when run 100 times with the same client") {
      Given("request")

      val resource = RPC_STYLE / "/vimsi/{vimsi}/delete"

      val request = "Request" <</ ("{vimsi}", "+2323232") <:< ("Content-Type", "application/json") <:<
        ("location", "us") <<< IMSI_RequestBody() <<? ("imsi", "+23232") <<? ("subscriptionstatus", "activated")

      val response = ("Response", 200) <:< ("Content-Type", "application/json") <:< ("location", "us")

      When("Prepare server resource and startup")

      aServer own resource when DELETE given request then response run

      Then("Client call server, the response status shall be SUCCESS")

      (1 to 100) foreach {

        i =>
          aClient ask_for resource to DELETE by request should SUCCESS and_with {
            resp: RestResponse =>
              {
                resp.statusCode shouldBe 200
              }
          }
      }
    }

    it("should be able to support to use xml as body payload") {

      Given("request")

      val resource = REST_STYLE / "/hr/account/create"

      case class Person(name: String, age: Int) {

        def toXmlString(neededDeclaration: Boolean = true): String = {

          val xmlTemplate = <person><name>{ name }</name><age>{ age }</age></person>

          if (neededDeclaration) {
            "<?xml version='1.0' encoding='" + "UTF-8" + "'?>\n" + xmlTemplate
          } else {
            xmlTemplate.toString
          }

        }

        override def toString = toXmlString()

      }

      val request = ("Request" <:< ("Content-Type", "application/xml")
        <:< ("location", "us")
        <<< Person("duff", 37))

      val response = ("Response", 200) <:< ("Content-Type", "application/xml") <:< ("location", "us") <<< Person("duff", 37)

      When("Prepare server resource and startup")

      aServer own resource when CREATE given request then {
        req: RestRequest =>
          {
            response
          }
      } run

      Then("Client call server, the response status shall be SUCCESS")

      aClient ask_for resource to CREATE by request should SUCCESS and_with {
        resp: RestResponse =>
          {
            resp.statusCode shouldBe 200
          }
      }
    }
  }

}
