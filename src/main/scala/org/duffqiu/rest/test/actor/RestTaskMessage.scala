package org.duffqiu.rest.test.actor

import org.duffqiu.rest.common._

private[actor] case object BYE
private[actor] case object CLIENT_BYE

private[actor] case class RestClientExceptionMessage(name: String, exception: Exception)

case class RestTaskMessage(resource: RestResource, req: RestRequest, operation: RestOperation, resp: RestResponse, expectResult: RestResult)

