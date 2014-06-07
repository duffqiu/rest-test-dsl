package org.duffqiu.rest.common

import org.duffqiu.rest.common.RestUtility._

case class RestRequest(headerPara: RestParameters = RestParameters("Header"), pathPara: RestParameters = RestParameters("Path"),
                       queryPara: RestParameters = RestParameters("Query"), body: RestBody = EmptyBody) {

    //refer to http://www.flotsam.nl/dispatch-periodic-table.html

    //append path parameter value
    def <</(t: (String, String)) = new RestRequest(headerPara, pathPara + (t._1 -> t._2), queryPara, body)

    //append query parameter value
    def <<?(t: (String, String)) = new RestRequest(headerPara, pathPara, queryPara + (t._1 -> t._2), body)

    //append header parameter value
    def <:<(t: (String, String)) = new RestRequest(headerPara + (t._1 -> t._2), pathPara, queryPara, body)

    //apend header map
    def <:<(map: Map[String, String]) = new RestRequest(headerPara ++ map, pathPara, queryPara, body)

    //append body
    def <<<(newBody: RestBody) = new RestRequest(headerPara, pathPara, queryPara, newBody)

    //get path parameter's value
    def >>/(key: String) = pathPara(key)

    //get query parameter's value
    def >>?(key: String) = queryPara(key)

    //get header parameter's value
    def >:>(key: String) = headerPara(key)

    val bodyJson: String = asJson(body)

}