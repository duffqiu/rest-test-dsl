package org.duffqiu.rest.common

object RestUtility {

    def asJson(body: AnyRef) = {
        //Using Lift
        import net.liftweb.json.Serialization._
        import net.liftweb.json.DefaultFormats
        implicit val formats = DefaultFormats
        val bodyJsonString = write(body)
        bodyJsonString
    }

}