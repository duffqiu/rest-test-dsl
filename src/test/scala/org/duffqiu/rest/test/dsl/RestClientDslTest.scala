package org.duffqiu.rest.test.dsl

import scala.language.postfixOps

import org.duffqiu.rest.common.CREATE
import org.duffqiu.rest.common.LOCAL_HOST
import org.duffqiu.rest.common.RestClientConfig
import org.duffqiu.rest.common.RestRequest
import org.duffqiu.rest.common.RestResource
import org.duffqiu.rest.common.RestResponse
import org.duffqiu.rest.common.SUCCESS
import org.duffqiu.rest.test.dsl.RestClientTestDsl.client2ClientHelper
import org.duffqiu.rest.test.dsl.RestClientTestDsl.string2RestClientHelper
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientOperation
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientRequest
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientResource
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientResult
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers
import org.scalatest.concurrent

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
