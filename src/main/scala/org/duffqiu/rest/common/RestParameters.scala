package org.duffqiu.rest.common

case class RestParameters(paraType: String, paraMap: Map[String, String] = Map()) {

    def +(t: (String, String)): RestParameters = {
        new RestParameters(paraType, Map(t._1 -> t._2) ++ paraMap)
    }

    def apply(key: String): String = paraMap.get(key) match {
        case None => paraMap.default(key)
        case Some(value) => value
    }

    def ++(map: Map[String, String]): RestParameters = {
        new RestParameters(paraType, paraMap ++ map)
    }

    def apply(): Map[String, String] = paraMap

}

