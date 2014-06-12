package org.duffqiu.rest.com

import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.duffqiu.rest.common.RestResource
import org.duffqiu.rest.common.REST_STYLE

import org.duffqiu.rest.test.dsl.RestCommonImplicits._

class RestResourceTest extends FunSpec with Matchers {
    describe("Test RestResource Construction and Methods") {
        it("Should support without parameters") {
            val resource = RestResource()
            resource.style shouldBe REST_STYLE
            assert(resource.path === "/")
        }

        it("Should support implicit create resource with style") {
            val resource = REST_STYLE / "/vimsi"
            resource.style shouldBe REST_STYLE
            assert(resource.path === "/vimsi")

        }

        it("Should create a new resource if provide new path") {
            val resource = REST_STYLE / "/vimsi"
            val newRes = resource / "/imsi"
            assert(newRes.path === "/vimsi/imsi")
            assert(resource !== newRes)
        }

        it("Should be able to append a path parameter") {
            val resource = REST_STYLE / "/vimsi/{vimis}"
            val newRes = resource / "msisdn/"
            assert(newRes.path === "/vimsi/{vimis}/msisdn")

        }

        it("Should support the / in header and tail") {
            val resource = REST_STYLE / "/vimsi/{vimis}/"
            val newRes = resource / "/imsi/{imsi}/"
            assert(newRes.path === "/vimsi/{vimis}/imsi/{imsi}")
        }

        it("Should support empty or / only") {
            val resource = REST_STYLE / ""
            val newRes = resource / "/"
            assert(resource.path === "/")
            assert(newRes.path === "/")
        }
    }
}