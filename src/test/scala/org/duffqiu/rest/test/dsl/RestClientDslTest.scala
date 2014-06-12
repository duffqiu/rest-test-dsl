package org.duffqiu.rest.test.dsl

import scala.language.postfixOps
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers
import org.scalatest._
import org.duffqiu.rest.common.RestClientConfig
import org.duffqiu.rest.test.dsl.RestClientTestDsl._
import org.duffqiu.rest.common.RestClient
import org.duffqiu.rest.common.LOCAL_HOST
import org.duffqiu.rest.common.RestResource
import org.duffqiu.rest.common.CREATE
import org.duffqiu.rest.common.RestRequest
import org.duffqiu.rest.common.SUCCESS
import org.duffqiu.rest.common.RestResponse

class RestClientDslTest extends FunSpec with Matchers with BeforeAndAfter with GivenWhenThen with concurrent.ScalaFutures with RestClientConfig {

    describe("Client Init") {

        it("Should support DSL") {
            val ses = "SES_Client" -> LOCAL_HOST on 38080
            ses ask_for (RestResource()) to CREATE by (RestRequest()) should SUCCESS and_with {
                response: RestResponse => Unit
            }
        }

        it("Should to local host 38080") {
            val ses = "SES_Client" -> LOCAL_HOST on 38080

            ses.hostName.name shouldBe "localhost"
            ses.port shouldBe 38080
        }

    }
}