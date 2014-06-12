package org.duffqiu.rest.common

import org.scalatest.concurrent._
import org.scalatest.time._

trait RestClientConfig extends ScalaFutures {
    implicit val config = PatienceConfig(scaled(Span(10, Seconds)), scaled(Span(1, Seconds)))

}