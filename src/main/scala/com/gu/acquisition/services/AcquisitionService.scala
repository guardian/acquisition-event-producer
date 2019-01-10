package com.gu.acquisition.services

import cats.data.EitherT
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import okhttp3.{HttpUrl, OkHttpClient}

import scala.concurrent.{ExecutionContext, Future}

trait AcquisitionService {
  def submit[A : AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext): EitherT[Future, List[AnalyticsServiceError], AcquisitionSubmission]
}

object AcquisitionService {

  def allServices(kinesisConfig: KinesisServiceConfig, ophanEndpoint: Option[HttpUrl] = None)(implicit client: OkHttpClient) = new DefaultAcquisitionService(List(
    new OphanService(ophanEndpoint),
    new GAService(),
    new KinesisService(kinesisConfig)
  ))

  def noKinesis(ophanEndpoint: Option[HttpUrl])(implicit client: OkHttpClient) = new DefaultAcquisitionService(List(
    new OphanService(ophanEndpoint),
    new GAService()
  ))
}
