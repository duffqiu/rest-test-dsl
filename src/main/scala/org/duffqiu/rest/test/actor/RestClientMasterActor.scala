/**
 *
 */
package org.duffqiu.rest.test.actor

import scala.actors.Actor
import scala.actors.Actor.State.Terminated
import scala.actors.Exit
import scala.actors.TIMEOUT

/**
 * @author macbook
 *
 * Jun 7, 2014
 */

object RestClientMasterActor {
    private final val DEFAULT_INTERVAL = 5000
}

class RestClientMasterActor(interval: Int = RestClientMasterActor.DEFAULT_INTERVAL) extends Actor {

    var workers: List[RestClientWorkActor] = List[RestClientWorkActor]()
    var workIndex = 0
    var exitConfirmCount = 0
    var isExit = false
    var exceptionList: List[RestClientExceptionMessage] = List[RestClientExceptionMessage]()

    override def act(): Unit = {
        trapExit = true
        loopWhile(!isExit) {
            receiveWithin(interval) {
                case BYE => {
                    //					println("[debug]server receive bye")
                    workers.foreach {
                        worker => worker ! CLIENT_BYE
                    }
                    //					println("finish to send bye to all clients")
                }
                case RestTestTaskMessage(resource, req, operation, resp, expectResult) => {
                    getWorker ! RestTestTaskMessage(resource, req, operation, resp, expectResult)
                    //										println("[Client Master Actor] send from master to worker(" + worker.name + "), operation: " + operation + ", expect result: " + expectResult)
                }

                case RestTestTaskBatchMsg(resource, operation, reqRespMap, expectResult) =>
                    //can't use par since getWorker is not thread safe
                    //                    println("[Client Master Actor] receive batch messsage and spit them to send to worker actors")
                    reqRespMap.foreach {
                        t =>
                            getWorker ! RestTestTaskMessage(resource, t._1, operation, t._2, expectResult)
                    }

                case TIMEOUT =>
                //					println("master actor timeout")
                case worker: RestClientWorkActor => {
                    workers = worker :: workers
                    worker.start
                    //					println("add worker: " + worker.name)
                }

                case except: RestClientExceptionMessage => {
                    //					println("[Client Master Actor] got exception: " + except.exception + " from " + except.name)
                    exceptionList = except :: exceptionList
                }

                case Exit(linked, reason) =>
                    exitConfirmCount = exitConfirmCount + 1
                    //					println("client exit because " + reason)
                    if (exitConfirmCount >= workers.length) {
                        //						println("master exit since all client workers are closed")
                        isExit = true
                    }

                case _ =>
                    println("[Client Master Actor]receive unknown message in master worker")

            }
        }
    }

    private[this] def getWorker = {
        workIndex = (workIndex + 1) % workers.length
        workers(workIndex)
    }

    private[this] def shouldNoClientException = {
        if (!exceptionList.isEmpty) {
            exceptionList.foreach(e => throw e.exception)
        }
    }

    def stop: Unit = {
        this ! BYE

        while (this.getState != Terminated) {
            Thread.sleep(interval)
        }

        shouldNoClientException
    }

}
