package com.gu.acquisition.instances

import com.twitter.scrooge.ThriftEnum
import play.api.libs.json._

private[instances] trait ThriftEnumFormat {

  def thriftEnumReads[A <: ThriftEnum](valueOf: String => Option[A]): Reads[A] = Reads { json =>
    json.validate[String].flatMap { raw =>
      valueOf(raw.filter(_ != '_')).map(JsSuccess(_)).getOrElse(JsError(""))
    }
  }

  def thriftEnumWrites[A <: ThriftEnum](originalName: A => String): Writes[A] = Writes { enum =>
    JsString(originalName(enum))
  }
}

