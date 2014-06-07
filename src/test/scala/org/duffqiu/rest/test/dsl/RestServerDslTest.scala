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

case class VIMSI_VowifiService(serviceName: String = "vowifi", subscriptionStatus: String = "activated")
case class IMSI_RequestBody(vIMSI: String = "+12121", msisdn: String = "+86233232", imsi: String = "+234234232432", service: VIMSI_VowifiService = VIMSI_VowifiService()) extends RestBody

class RestServerDslTest extends FunSpec with Matchers with BeforeAndAfter with GivenWhenThen with concurrent.ScalaFutures with RestClientConfig {

    var aog: RestServer = _
    var request4Check: RestRequest = _

    describe("Rest server and client DSL Testing") {

        before {
            aog = "Server" on 38888

        }

        after {
            Thread.sleep(2000)
            aog.stop
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

        it("Should be OK on 38888 using rest style resource when query with dsl client") {

            Given("request")

            val resource = REST_STYLE / "/vIMSI"

            val request = "Request" <:< ("Content-Type", "application/json") <<? ("IMSI", "+23232") <<? ("subscriptionStatus", "activated") <</ ("{vIMSI}", "2323232")

            val requestDouble = RestRequest() <:< ("Content-Type", "application/json")

            val response = ("Response", 200) <<< IMSI_RequestBody()

            When("Prepare server resource and startup")

            aog own resource when CREATE given request then {
                req: RestRequest => response
            } and resource when QUERY given requestDouble then {
                req: RestRequest => response
            } and resource when DELETE given request then {
                req: RestRequest => response
            } run

            Then("Client call server, the response status shall be SUCCESS")

            val ses = "SES_Client" -> LOCAL_HOST on 38888

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

        it("Should be OK on 38888 using rest style resource when create with header, with query parameters, with body by dsl client") {
            Given("request")

            val resource = REST_STYLE / "/vimsi/{vimsi}"

            val request = "Request" <</ ("{vimsi}", "+2323232") <:< ("Content-Type", "application/json") <:< ("location", "us") <<< IMSI_RequestBody() <<? ("imsi", "+23232") <<? ("subscriptionstatus", "activated")

            val response = ("Response", 200) <<< IMSI_RequestBody()

            When("Prepare server resource and startup")

            aog own resource when CREATE given request then {
                req: RestRequest =>
                    {
                        response
                    }
            } run

            Then("Client call server, the response status shall be SUCCESS")

            val ses = "SES_Client" -> LOCAL_HOST on 38888

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

        it("Should be OK on 38888 using rpc style resource when delete with header, with query parameters, with body by dsl client, support server hit check") {
            Given("request")

            val resource = RPC_STYLE / "/vimsi/{vimsi}/delete"

            val request = "Request" <</ ("{vimsi}", "+2323232") <:< ("Content-Type", "application/json") <:< ("location", "us") <<< IMSI_RequestBody() <<? ("imsi", "+23232") <<? ("subscriptionstatus", "activated")

            val response = ("Response", 200) <:< ("Content-Type", "application/json") <:< ("location", "us")

            When("Prepare server resource and startup")

            aog own resource when DELETE given request then {
                req: RestRequest =>
                    {
                        response
                    }
            } run

            Then("Client call server, the response status shall be SUCCESS")

            val ses = "SES_Client" -> LOCAL_HOST on 38888

            ses ask_for resource to DELETE by request should SUCCESS and_with {
                resp: RestResponse =>
                    {
                        resp.statusCode shouldBe 200
                        aog shouldHitTimes (resource, CREATE, request, 1)
                        aog shouldHitOnce (resource, CREATE, request)
                        aog shouldHitAtLeast (resource, CREATE, request, 1)
                        aog shouldHitAtMost (resource, CREATE, request, 1)
                    }
            } end

        }
    }
}