/**
 * Copyright (C) 2014 the original author duffqiu@gmail.com
 */
package org.duffqiu.rest.test.actor

import scala.actors.Actor
import scala.actors.Actor.State.Terminated
import scala.actors.Exit
import scala.actors.TIMEOUT

import org.duffqiu.rest.test.dsl.RestServerTestDsl.Tuple2Server
import org.duffqiu.rest.test.dsl.RestServerTestDsl.server2ServerHelper
import org.duffqiu.rest.test.dsl.RestServerTestDsl.string2RestServerHelper
import org.duffqiu.rest.test.dsl.RestServerTestDsl.withServerOperation
import org.duffqiu.rest.test.dsl.RestServerTestDsl.withServerRequest
import org.duffqiu.rest.test.dsl.RestServerTestDsl.withServerResource

object RestServerActor {
    private[actor] final val DEFAULT_NAME = "RestServer"
    private[actor] final val DEFAULT_PORT = 0
    private[actor] final val DEFAULT_INTERVAL = 5000
}

/**
 * @author macbook
 *
 * Jun 15, 2014
 */
class RestServerActor(name: String = RestServerActor.DEFAULT_NAME, port: Int = RestServerActor.DEFAULT_PORT,
                      interval: Int = RestServerActor.DEFAULT_INTERVAL) extends Actor {

    var isExit = false

    val restServ = name on port

    //    def restServer = restServ

    override def act(): Unit = {
        trapExit = true

        loopWhile(!isExit) {
            receiveWithin(interval) {
                case BYE => {
                    restServ.stop
                    isExit = true
                }

                case RestTestResourceMatchMsg(resource, req, operation, resp) => {
                    // println("[Server Actor] config matcher")

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

    def stop: Unit = {
        this ! BYE

        while (this.getState != Terminated) {
            Thread.sleep(interval)
        }
    }
}
