package com.gu.acquisition.services

import cats.data.EitherT
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import okhttp3.OkHttpClient

import scala.concurrent.{ExecutionContext, Future}

trait AcquisitionService {

  def submit[A : AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext): EitherT[Future, AnalyticsServiceError, AcquisitionSubmission]
}

object AcquisitionService {

  def prod(implicit client: OkHttpClient): DefaultAcquisitionService = DefaultAcquisitionService()
}
