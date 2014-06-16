package org.duffqiu.rest.common

import org.duffqiu.rest.common.RestUtility.asJson

object RestResponse {
	private final val DEFAULT_STATUS_CODE = 200
}

case class RestResponse(statusCode: Int = RestResponse.DEFAULT_STATUS_CODE, headerPara: RestParameters = RestParameters("Header"), body: AnyRef = EmptyBody) {
	val bodyJson: String = body match {
		case s: String => s
		case _ => asJson(body)
	}

	//get header parameter's value
	def >:>(key: String) = headerPara(key)

	//append header parameter value
	def <:<(req: String, resp: String) = new RestResponse(statusCode, headerPara + (req -> resp), body)

	//append body
	def <<<(newBody: AnyRef) = new RestResponse(statusCode, headerPara, newBody)

	//apend header map
	def <:<(map: Map[String, String]) = new RestResponse(statusCode, headerPara ++ map, body)

}
