package org.duffqiu.rest.test.dsl

import scala.language.implicitConversions
import org.duffqiu.rest.common.RestResource
import org.duffqiu.rest.common.RestStyle
import org.duffqiu.rest.common.RestRequest
import org.duffqiu.rest.common.RestResponse

object RestCommonImplicits {
    implicit def restStyle2ResourceHelper(style: RestStyle) = new RestResource(style)
    implicit def string2RestRequest(s: String): RestRequest = new RestRequest()
    implicit def string2RestResponse(t: (String, Int)): RestResponse = new RestResponse(t._2)
}