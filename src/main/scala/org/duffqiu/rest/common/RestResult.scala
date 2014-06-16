package org.duffqiu.rest.common

abstract class RestResult(name: String, startCode: Int, endCode: Int) {
    def shouldMatch(statusCode: Int) = {
        statusCode >= startCode && statusCode <= endCode
    }

    def apply() = name + "(" + startCode + "~" + endCode + ")"
}

case object SUCCESS extends RestResult("Success", 200, 299)
case object INFO extends RestResult("Informational", 100, 199)
case object REDIRECTION extends RestResult("Redirection", 300, 399)
case object CLIENTERROR extends RestResult("Client Error", 400, 499)
case object SERVERERROR extends RestResult("Server Error", 500, 599)
case object OTHERERROR extends RestResult("Other Error", 600, 999)
case object ANYRESULTS extends RestResult("Any results", 0, 999)
