/**
 *
 */
package org.duffqiu.rest.test.actor

import scala.actors.Actor
import org.duffqiu.rest.common.RestRequest
import org.duffqiu.rest.common.RestResponse
import scala.actors.Exit
import scala.actors.TIMEOUT
import scala.actors.Actor.State._

import org.duffqiu.rest.test.dsl.RestServerTestDsl._

/**
 * @author macbook
 *
 * Jun 15, 2014
 */
class RestServerActor(name: String = "RestServer", port: Int = 0, interval: Int = 5000) extends Actor {

    var isExit = false

    val restServ = name on port

    //    def restServer = restServ

    override def act() {
        trapExit = true

        loopWhile(!isExit) {
            receiveWithin(interval) {
                case BYE => {
                    restServ.stop
                    isExit = true
                }

                case RestTestResourceMatchMsg(resource, req, operation, resp) => {
                    //                    println("[Server Actor] config matcher")

                    restServ own resource when operation given req then { req => resp } end
                }

                case RestTestResourceBatchMatchMsg(resource, operation, reqRespMap) => {
                    //                    println("[Server Actor] receive batch messsage and spit them to config matcher")

                    reqRespMap.foreach {
                        t => this ! RestTestResourceMatchMsg(resource, t._1, operation, t._2)

                    }
                }

                case RUN_REST_SERVER => {
                    restServ.run

                    sender ! restServ
                }

                case Exit(link, reason) => println("[Server Actor] receive main process exit")

                case TIMEOUT =>

                case _ => println("[Server Actor]receive unknown message in master worker")
            }
        }
    }

    def stop = {
        this ! BYE

        while (this.getState != Terminated) {
            Thread.sleep(1000)
        }
    }
}