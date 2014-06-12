/**
 *
 */
package org.duffqiu.rest.test.actor

import scala.actors.Actor
import scala.actors.Actor.State.Terminated
import scala.actors.Exit
import scala.actors.TIMEOUT

import org.scalatest.Assertions

/**
 * @author macbook
 *
 * Jun 7, 2014
 */

class RestClientMasterActor() extends Actor with Assertions {

    var workers: List[RestClientWorkActor] = List[RestClientWorkActor]()
    var workIndex = 0

    var exitConfirmCount = 0

    var isExit = false

    var exceptionList: List[RestClientExceptionMessage] = List[RestClientExceptionMessage]()

    override def act() {
        trapExit = true
        loopWhile(!isExit) {
            receiveWithin(6000) {
                case BYE => {

                    //					println("[debug]server receive bye")
                    workers.foreach {
                        worker => worker ! CLIENT_BYE
                    }
                    //					println("finish to send bye to all clients")

                }
                case RestTaskMessage(resource, req, operation, resp, expectResult) => {
                    workIndex = workIndex + 1
                    val worker = getWorker(workIndex % workers.length)
                    worker ! RestTaskMessage(resource, req, operation, resp, expectResult)
                    //										println("[Client Master Actor] send from master to worker(" + worker.name + "), operation: " + operation + ", expect result: " + expectResult)
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

    private[this] def getWorker(index: Int) = {
        workers(index)
    }

    private[this] def shouldNoClientException = {
        if (!exceptionList.isEmpty) {
            exceptionList.foreach {
                e =>
                    throw e.exception
            }
        }
    }

    def stop = {

        this ! BYE

        while (this.getState != Terminated) {
            Thread.sleep(1000)
        }

        shouldNoClientException

    }

}