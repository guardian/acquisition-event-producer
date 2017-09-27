package com.gu.acquisition.model

import io.circe.Encoder
import io.circe.generic.semiauto._
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}

/**
  * Model for acquisition data passed by the referrer.
  * This should be included in the request to the contribution website as part of the query string: acquisitionData={}
  * The value should be the data encoded using Json in the canonical way, and then percent encoded.
  */
case class ReferrerAcquisitionData(
    campaignCode: Option[String],
    referrerPageviewId: Option[String],
    referrerUrl: Option[String],
    componentId: Option[String],
    componentType: Option[ComponentType],
    source: Option[AcquisitionSource],
    // Used to store the option of the client being in a test on the referring page,
    // that resulted on them landing on the contributions page.
    // e.g. they clicked the contribute link in an Epic AB test.
    abTest: Option[AbTest]
)

object ReferrerAcquisitionData {
  // Ignore IntelliJ - these imports are used!
  import com.gu.acquisition.instances.abTest._
  import com.gu.acquisition.instances.acquisitionSource._
  import com.gu.acquisition.instances.componentType._

  implicit val referrerAcquisitionDataEncoder: Encoder[ReferrerAcquisitionData] = deriveEncoder[ReferrerAcquisitionData]
}