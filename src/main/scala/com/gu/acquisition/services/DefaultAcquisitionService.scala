package com.gu.acquisition.services

import cats.data.EitherT
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import okhttp3.OkHttpClient

import scala.concurrent.{ExecutionContext, Future}

class DefaultAcquisitionService(services: List[AnalyticsService])(implicit client: OkHttpClient) extends AcquisitionService {

  override def submit[A: AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext) = {
    EitherT {
      val submitOps = services.map(_.submit(a).value)
      Future.sequence(submitOps).map(mergeEithers)
    }
  }

  // Return the AcquisitionSubmission only if there are no errors, otherwise the full List[AnalyticsServiceError]
  def mergeEithers(eithers: List[Either[AnalyticsServiceError, AcquisitionSubmission]]): Either[List[AnalyticsServiceError], AcquisitionSubmission] = {
    val errors: List[AnalyticsServiceError] = eithers.flatMap(_.swap.toOption)
    val submission: Option[AcquisitionSubmission] = eithers.collectFirst { case Right(s) => s }

    (errors, submission) match {
      case (Nil, Some(s)) => Right(s)
      case _ => Left(errors)
    }
  }
}
