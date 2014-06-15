package org.duffqiu.rest.common

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write

object RestUtility {

    def asJson(body: AnyRef) = {
        //Using Lift

        implicit val formats = DefaultFormats
        val bodyJsonString = write(body)
        bodyJsonString
    }

}

