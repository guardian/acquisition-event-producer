package com.gu.acquisition.model

import ophan.thrift.event.Acquisition
import play.api.libs.json.{Reads, Writes, Json => PlayJson}
import simulacrum.typeclass

case class OphanIds(pageviewId: String, visitId: Option[String], browserId: Option[String])

object OphanIds {
  import io.circe._
  import io.circe.generic.semiauto._

  implicit val reads: Reads[OphanIds] = PlayJson.reads[OphanIds]
  implicit val writes: Writes[OphanIds] = PlayJson.writes[OphanIds]

  implicit val encoder: Encoder[OphanIds] = deriveEncoder[OphanIds]
  implicit val decoder: Decoder[OphanIds] = deriveDecoder[OphanIds]
}

/**
  * Encapsulates all the data required to submit an acquisition to Ophan.
  */
case class AcquisitionSubmission(ophanIds: OphanIds, acquisition: Acquisition)

/**
  * Type class for creating an acquisition submission from an arbitrary data type.
  */
@typeclass trait AcquisitionSubmissionBuilder[A] {

  import cats.syntax.either._

  def buildOphanIds(a: A): Either[String, OphanIds]

  def buildAcquisition(a: A): Either[String, Acquisition]

  def asAcquisitionSubmission(a: A): Either[String, AcquisitionSubmission] =
    for {
      ophanIds <- buildOphanIds(a)
      acquisition <- buildAcquisition(a)
    } yield AcquisitionSubmission(ophanIds, acquisition)
}