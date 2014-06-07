package org.duffqiu.rest.common

abstract class RestStyle(style : String)

case object REST_STYLE extends RestStyle("REST")

case object RPC_STYLE extends RestStyle("RPC")