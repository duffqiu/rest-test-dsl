package org.duffqiu.rest.test.dsl

import scala.language.postfixOps
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers
import org.scalatest.concurrent
import org.duffqiu.rest.common._
import org.duffqiu.rest.test.dsl.RestClientTestDsl._
import org.duffqiu.rest.test.dsl.RestServerTestDsl._
import org.duffqiu.rest.test.dsl.RestCommonImplicits._
import dispatch.:/
import dispatch.Defaults.executor
import dispatch.Http
import dispatch.implyRequestHandlerTuple
import dispatch.url
import net.liftweb.json.parse
import net.liftweb.json.DefaultFormats
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.duffqiu.rest.test.actor.RestClientMasterActor
import org.duffqiu.rest.test.actor.RestClientWorkActor
import org.duffqiu.rest.test.actor.RestTestTaskMessage
import org.duffqiu.rest.test.actor.RestTestTaskBatchMsg

case class VIMSI_VowifiService(serviceName: String = "vowifi", subscriptionStatus: String = "activated")
case class IMSI_RequestBody(vimsi: String = "+12121", msisdn: String = "+86233232", imsi: String = "+234234232432", service: VIMSI_VowifiService = VIMSI_VowifiService()) extends RestBody

class RestServerDslTest extends FunSpec with Matchers with BeforeAndAfter with GivenWhenThen with concurrent.ScalaFutures with RestClientConfig with GeneratorDrivenPropertyChecks {

    var aServer: RestServer = _
    var request4Check: RestRequest = _

    describe("Rest server and client DSL Testing") {

        before {
            aServer = "Server" on 0

        }

        after {
            Thread.sleep(2000)
            aServer.stop
        }

        it("Should on 38080 success") {
            val vIMSI = "vIMSI_Manager" on 38080

            vIMSI.serverPort shouldBe 38080

            vIMSI.run

            vIMSI.name shouldBe "vIMSI_Manager"

            Thread.sleep(2000)
            vIMSI.stop

        }

        it("Should on 10801 success") {
            val vIMSI = "vIMSI_Manager" on 10801

            vIMSI.serverPort shouldBe 10801

            vIMSI.run

            vIMSI.name shouldBe "vIMSI_Manager"

            Thread.sleep(2000)
            vIMSI.stop

        }

        it("Should be OK using rest style resource when query with dsl client") {

            Given("request")

            val resource = REST_STYLE / "/vIMSI"

            val request = "Request" <:< ("Content-Type", "application/json") <<? ("IMSI", "+23232") <<? ("subscriptionStatus", "activated") <</ ("{vIMSI}", "2323232")

            val requestDouble = RestRequest() <:< ("Content-Type", "application/json")

            val response = ("Response", 200) <<< IMSI_RequestBody()

            When("Prepare server resource and startup")

            aServer own resource when CREATE given request then {
                req: RestRequest => response
            } and resource when QUERY given requestDouble then {
                req: RestRequest => response
            } and resource when DELETE given request then {
                req: RestRequest => response
            } run

            Then("Client call server, the response status shall be SUCCESS")

            val ses = "SES_Client" -> LOCAL_HOST on aServer.serverPort

            ses ask_for resource to QUERY by request should SUCCESS and_with {
                resp: RestResponse =>
                    {
                        resp.statusCode shouldBe 200
                        resp.bodyJson.contains("msisdn") shouldBe true

                        val jsValue = parse(resp.bodyJson)
                        implicit val formats = DefaultFormats

                        val msisdn = (jsValue \\ "msisdn").extract[String]

                        assert(msisdn === "+86233232")
                    }
            } end

            ses ask_for resource to QUERY by request should SUCCESS and_with {
                resp: RestResponse =>
                    {
                        resp.statusCode shouldBe 200
                        resp.bodyJson.contains("msisdn") shouldBe true

                        val jsValue = parse(resp.bodyJson)
                        implicit val formats = DefaultFormats
                        import scala.collection.JavaConversions._
                        val imsi = (jsValue \\ "imsi").extract[String]
                        val serviceName = (jsValue \\ "service" \\ "serviceName").extract[String]

                        val imsiBody = jsValue.extract[IMSI_RequestBody]
                        assert(imsi === "+234234232432")
                        assert(serviceName === "vowifi")
                        imsiBody.imsi shouldBe "+234234232432"
                    }
            } end

        }

        it("Should be OK using rest style resource when create with header, with query parameters, with body by dsl client") {
            Given("request")

            val resource = REST_STYLE / "/vimsi/{vimsi}"

            val request = "Request" <</ ("{vimsi}", "+2323232") <:< ("Content-Type", "application/json") <:< ("location", "us") <<< IMSI_RequestBody() <<? ("imsi", "+23232") <<? ("subscriptionstatus", "activated")

            val response = ("Response", 200) <<< IMSI_RequestBody()

            When("Prepare server resource and startup")

            aServer own resource when CREATE given request then {
                req: RestRequest =>
                    {
                        response
                    }
            } run

            Then("Client call server, the response status shall be SUCCESS")

            val ses = "SES_Client" -> LOCAL_HOST on aServer.serverPort

            ses ask_for resource to CREATE by request should SUCCESS and_with {
                resp: RestResponse =>
                    {
                        resp.statusCode shouldBe 200
                        resp.bodyJson.contains("msisdn") shouldBe true

                        val jsValue = parse(resp.bodyJson)
                        implicit val formats = DefaultFormats

                        val msisdn = (jsValue \\ "msisdn").extract[String]

                        assert(msisdn === "+86233232")
                    }
            } end

        }

        it("Should be OK using rpc style resource when delete with header, with query parameters, with body by dsl client, support server hit check") {
            Given("request")

            val resource = RPC_STYLE / "/vimsi/{vimsi}/delete"

            val request = "Request" <</ ("{vimsi}", "+2323232") <:< ("Content-Type", "application/json") <:< ("location", "us") <<< IMSI_RequestBody() <<? ("imsi", "+23232") <<? ("subscriptionstatus", "activated")

            val response = ("Response", 200) <:< ("Content-Type", "application/json") <:< ("location", "us")

            When("Prepare server resource and startup")

            aServer own resource when DELETE given request then {
                req: RestRequest =>
                    {
                        response
                    }
            } run

            Then("Client call server, the response status shall be SUCCESS")

            val ses = "SES_Client" -> LOCAL_HOST on aServer.serverPort

            ses ask_for resource to DELETE by request should SUCCESS and_with {
                resp: RestResponse =>
                    {
                        resp.statusCode shouldBe 200
                        aServer shouldHitTimes (resource, CREATE, request, 1)
                        aServer shouldHitOnce (resource, CREATE, request)
                        aServer shouldHitAtLeast (resource, CREATE, request, 1)
                        aServer shouldHitAtMost (resource, CREATE, request, 1)
                    }
            } end

        }

        it("Should be OK using rest style with request created by generator with colletion pare") {
            Given("request")

            val resource = REST_STYLE / "/vimsi/{vimsi}"

            val reqs = for {
                tel <- Gen.oneOf("tel:", "+")
                vimsiRamdom <- Gen.numStr
                vimsi <- Gen.value(vimsiRamdom match { case x: String if (x != null && x.length() > 10 && x.length() < 20) => x; case _ => "89900" })
                imsi <- Gen.numStr
                serviceName <- Gen.oneOf("vowifi", "mca", "vvm")
                subscriptionStatus <- Gen.oneOf("activated", "deactivated")
            } yield ("Request" <</ ("{vimsi}", tel + vimsi) <:< ("Content-Type", "application/json") <:< ("location", "us") <<< IMSI_RequestBody(imsi = imsi, service = VIMSI_VowifiService(subscriptionStatus = subscriptionStatus, serviceName = serviceName)) <<? ("subscriptionstatus", subscriptionStatus))

            When("Prepare server resource and startup")

            import scala.collection.JavaConversions._
            val req_resp: scala.collection.mutable.Map[RestRequest, RestResponse] = new java.util.concurrent.ConcurrentHashMap[RestRequest, RestResponse]

            forAll(reqs, workers(10)) {
                req =>
                    {
                        val body = req.body match {
                            case b: IMSI_RequestBody => b
                        }
                        val resp = ("Response", 200) <:< ("Content-Type", req >:> ("Content-Type")) <:< ("location", req >:> ("location")) <<< IMSI_RequestBody(vimsi = req.>>/("{vimsi}"), imsi = body.imsi, service = VIMSI_VowifiService(subscriptionStatus = req >>? ("subscriptionstatus"), serviceName = body.service.serviceName))
                        req_resp.put(req, resp)
                    }
            }

            req_resp.foldLeft(aServer) {
                (serv, t: (RestRequest, RestResponse)) =>
                    serv own resource when DELETE given t._1 then { req => t._2 } end
            } run

            val ses = "SES_Client" -> LOCAL_HOST on aServer.serverPort

            Then("Client call server, the response status shall be SUCCESS with response")

            //can'tuse par when need to use hit methods
            for (t <- req_resp.par) {
                ses ask_for resource to DELETE by t._1 should SUCCESS and_with {
                    resp: RestResponse =>
                        {
                            resp.statusCode shouldBe 200
                            resp.body shouldEqual t._2.bodyJson
                        }
                } end

            }

        }

        it("Should be OK using rest style with request created by generator with mutiple works") {

            Given("request and corresponding response")

            val resource1 = REST_STYLE / "/vimsi/{vimsi}"

            val reqs = for {
                tel <- Gen.oneOf("tel:", "+")
                vimsiRamdom <- Gen.numStr
                vimsi <- Gen.const(vimsiRamdom match { case x: String if (x != null && x.length() > 10 && x.length() < 20) => x; case _ => "89900" })
                imsi <- Gen.numStr
                serviceName <- Gen.oneOf("vowifi", "mca", "vvm")
                subscriptionStatus <- Gen.oneOf("activated", "deactivated")
            } yield ("Request" <</ ("{vimsi}", tel + vimsi) <:< ("Content-Type", "application/json") <:< ("location", "us") <<< IMSI_RequestBody(imsi = imsi, service = VIMSI_VowifiService(subscriptionStatus = subscriptionStatus, serviceName = serviceName)) <<? ("subscriptionstatus", subscriptionStatus))

            import scala.collection.JavaConversions._
            val req_resp: scala.collection.mutable.Map[RestRequest, RestResponse] = new java.util.concurrent.ConcurrentHashMap[RestRequest, RestResponse]

            forAll(reqs, workers(5)) {
                req =>
                    {
                        val body = req.body match {
                            case b: IMSI_RequestBody => b
                        }
                        val resp = ("Response", 400) <:< ("Content-Type", req >:> ("Content-Type")) <:< ("location", req >:> ("location")) <<< IMSI_RequestBody(vimsi = req.>>/("{vimsi}"), imsi = body.imsi, service = VIMSI_VowifiService(subscriptionStatus = req >>? ("subscriptionstatus"), serviceName = body.service.serviceName))
                        req_resp.put(req, resp)
                    }
            }

            val reqs2 = for {
                tel <- Gen.oneOf("tel:", "+")
                vimsiRamdom <- Gen.numStr
                vimsi <- Gen.const(vimsiRamdom match { case x: String if (x != null && x.length() > 10 && x.length() < 20) => x; case _ => "89900" })
                imsi <- Gen.numStr
                serviceName <- Gen.oneOf("vowifi", "icloud", "vvm")
                subscriptionStatus <- Gen.oneOf("activated", "deactivated", "enabled", "disabled")
            } yield ("Request" <</ ("{vimsi}", tel + vimsi) <:< ("Content-Type", "application/json") <:< ("location", "cn") <<< IMSI_RequestBody(imsi = imsi, service = VIMSI_VowifiService(subscriptionStatus = subscriptionStatus, serviceName = serviceName)) <<? ("subscriptionstatus", subscriptionStatus))

            val req_resp2: scala.collection.mutable.Map[RestRequest, RestResponse] = new java.util.concurrent.ConcurrentHashMap[RestRequest, RestResponse]

            forAll(reqs2, workers(5)) {
                req =>
                    {
                        val body = req.body match {
                            case b: IMSI_RequestBody => b
                        }
                        val resp = ("Response", 200) <:< ("Content-Type", req >:> ("Content-Type")) <:< ("location", req >:> ("location")) <<< IMSI_RequestBody(vimsi = req.>>/("{vimsi}"), imsi = body.imsi, service = VIMSI_VowifiService(subscriptionStatus = req >>? ("subscriptionstatus"), serviceName = body.service.serviceName))
                        req_resp2.put(req, resp)
                    }
            }

            When("Prepare server resource and startup")

            req_resp.foldLeft(aServer) {
                (serv, t: (RestRequest, RestResponse)) =>
                    serv own resource1 when DELETE given t._1 then { req => t._2 } end
            }

            req_resp2.foldLeft(aServer) {
                (serv, t: (RestRequest, RestResponse)) =>
                    serv own resource1 when QUERY given t._1 then { req => t._2 } end
            } run

            And("Start client master and client workers")

            val masterActor = new RestClientMasterActor()
            masterActor.start

            (1 to 20) foreach {
                i =>
                    val worker = new RestClientWorkActor("Client" + i, masterActor, aServer, LOCAL_HOST, aServer.serverPort, {
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

            println("!!!master actor exit")

        }
    }

}