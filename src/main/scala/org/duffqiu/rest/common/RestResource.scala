package org.duffqiu.rest.common

import scala.collection.Map
import org.scalatest.enablers.Length

case class RestResource(style: RestStyle = REST_STYLE, path: String = "/") {
    def /(pathName: String) = {

        //remove the / in the header
        val pathNameNoHeader = Option(pathName).filter(_.trim().nonEmpty).getOrElse("/").head match {
            case '/' if (pathName.length() > 1) => pathName.substring(1, pathName.length())
            case '/' if (pathName.length() == 1) => ""
            case _ => pathName
        }

        //remove the / in the tail
        val pathNameNoHeadNoTail = Option(pathNameNoHeader).filter(_.trim().nonEmpty).getOrElse("/").last match {
            case '/' if (pathNameNoHeader.length() > 1) => pathNameNoHeader.substring(0, pathNameNoHeader.length() - 1)
            case '/' if (pathNameNoHeader.length() == 1) => ""
            case _ => pathNameNoHeader
        }

        val newPath = (path.length() > 1) match {
            case true => path + "/" + pathNameNoHeadNoTail
            case _ => "/" + pathNameNoHeadNoTail
        }

        new RestResource(style, newPath /*, pathParaKeys*/ )
    }

}