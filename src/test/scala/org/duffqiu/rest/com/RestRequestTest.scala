package org.duffqiu.rest.com

import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.duffqiu.rest.common.EmptyBody
import org.duffqiu.rest.common.RestBody
import org.duffqiu.rest.test.dsl.RestCommonImplicits._
import org.duffqiu.rest.common.RestRequest

sealed case class VIMSI_VowifiService(serviceName: String = "vowifi", subscriptionStatus: String = "activated")
sealed case class IMSI_RequestBody(vIMSI: String = "12121", msisdn: String = "+86233232", imsi: String = "234234232432", service: VIMSI_VowifiService = VIMSI_VowifiService()) extends RestBody

class RestRequestTest extends FunSpec with Matchers {
    describe("Test RestRequest Construction and Methods") {
        it("Should support without parameters") {
            val request = RestRequest()
            request.headerPara.paraType shouldBe "Header"
            request.queryPara.paraType shouldBe "Query"
            request.pathPara.paraType shouldBe "Path"
            request.body shouldBe EmptyBody
        }

        it("Should be able to append parameters(query, path, header)") {
            val request = "Rest request" <</ ("{imsi}", "232323223") <<? ("msisdn", "+8634343") <:< ("Context-Type", "application/json") <<? ("serviceName", "voice")

            request.queryPara("msisdn") shouldBe "+8634343"
            request.queryPara("serviceName") shouldBe "voice"
            request.headerPara("Context-Type") shouldBe "application/json"
            request.pathPara("{imsi}") shouldBe "232323223"
        }

        it("Should support to fetch parameter's value with new operation(<</, <<?, <:<)") {
            val request = "Rest request" <</ ("{imsi}", "232323223") <<? ("msisdn", "+8634343") <:< ("Context-Type", "application/json") <<? ("serviceName", "voice")

            request.>>?("msisdn") shouldBe "+8634343"
            request.>>?("serviceName") shouldBe "voice"
            request.>:>("Context-Type") shouldBe "application/json"
            request.>>/("{imsi}") shouldBe "232323223"

        }

        it("Should support to insert a body and get a body") {
            val request = "Rest request" <<< IMSI_RequestBody()
            val body = request.body
            body shouldBe IMSI_RequestBody()

        }

    }
}