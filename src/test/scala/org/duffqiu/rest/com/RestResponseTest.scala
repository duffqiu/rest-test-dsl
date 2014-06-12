package org.duffqiu.rest.com

import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.duffqiu.rest.common.RestResponse
import org.duffqiu.rest.common.EmptyBody
import org.duffqiu.rest.common.RestBody
import org.duffqiu.rest.test.dsl.RestCommonImplicits._

sealed case class VowifiService(serviceName: String = "vowifi", subscriptionStatus: String = "activated")
sealed case class IMSI_ResponseBody(vIMSI: String = "12121", msisdn: String = "+86233232", imsi: String = "234234232432", service: VowifiService = VowifiService()) extends RestBody

class RestResponseTest extends FunSpec with Matchers {
    describe("Test RestReponse Construction and Methods") {
        it("Should support without parameters") {
            val response = RestResponse()
            response.headerPara.paraType shouldBe "Header"
            response.statusCode shouldBe 200
            response.body shouldBe EmptyBody
        }

        it("Should be able to append parameters(statusCode, header, body)") {
            val response = ("Rest Response", 201) <:< ("Context-Type", "application/json") <<< IMSI_ResponseBody() <:< ("location", "US")
            response >:> ("Context-Type") shouldBe "application/json"
            response.body shouldBe IMSI_ResponseBody()
            response >:> ("location") shouldBe "US"
            response.statusCode shouldBe 201

        }

    }
}