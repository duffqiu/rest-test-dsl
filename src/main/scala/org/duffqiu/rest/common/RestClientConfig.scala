package org.duffqiu.rest.common

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Seconds
import org.scalatest.time.Span

object RestClientConfig {
  private final val TIMEOUT = 30
  private final val INTERVAL = 1
}

trait RestClientConfig extends ScalaFutures {
  implicit val config = PatienceConfig(scaled(Span(RestClientConfig.TIMEOUT, Seconds)), scaled(Span(RestClientConfig.INTERVAL, Seconds)))

}
