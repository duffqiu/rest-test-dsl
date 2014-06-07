package org.duffqiu.rest.common

abstract class RestOperation(name:String) {
    def apply() = name
}

case object CREATE extends RestOperation("POST")
case object UPDATE extends RestOperation("PUT")
case object QUERY extends RestOperation("GET")
case object DELETE extends RestOperation("DELETE")
