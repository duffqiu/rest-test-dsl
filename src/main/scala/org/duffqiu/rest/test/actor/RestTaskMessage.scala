package org.duffqiu.rest.test.actor

import org.duffqiu.rest.common._

case object RUN_REST_SERVER

private[actor] case object BYE
private[actor] case object CLIENT_BYE

case class RestTestResourceMatchMsg(resource: RestResource, req: RestRequest, operation: RestOperation, res: RestResponse)
case class RestTestResourceBatchMatchMsg(resource: RestResource, operation: RestOperation, reqRespMap: Map[RestRequest, RestResponse])

private[actor] case class RestClientExceptionMessage(name: String, exception: Exception)
case class RestTestTaskBatchMsg(resource: RestResource, operation: RestOperation, reqRespMap: Map[RestRequest, RestResponse], expectResult: RestResult)
case class RestTestTaskMessage(resource: RestResource, req: RestRequest, operation: RestOperation, resp: RestResponse, expectResult: RestResult)

