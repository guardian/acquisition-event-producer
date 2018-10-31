package com.gu.acquisition.services

import cats.Applicative
import cats.data.{EitherT, Nested, NonEmptyList, Validated}
import cats.implicits._
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import okhttp3.OkHttpClient

import scala.concurrent.{ExecutionContext, Future}

class DefaultAcquisitionService private[services] (services: List[AcquisitionService])(implicit client: OkHttpClient) extends AcquisitionService {

  import DefaultAcquisitionService._

  override def submit[A : AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext): EitherT[Future, AnalyticsServiceError, AcquisitionSubmission] = {
    // Make the requests concurrently, accumulating any errors.
    // Sadly the type parameters are required by the Scala compiler :(
    val result = services.traverse[ValidatedT[Future, NonEmptyList[AnalyticsServiceError], ?], AcquisitionSubmission](_.submit(a).toNestedValidatedNel)
    EitherT(result.value.map(_.toEither)).bimap(
      errors => if (errors.size == 1) errors.head else AnalyticsServiceError.Collection(errors),
      _.head
    )
  }
}

object DefaultAcquisitionService {

  def apply()(implicit client: OkHttpClient): DefaultAcquisitionService =
    new DefaultAcquisitionService(List(new OphanService, new GAService))

  // Utility type alias.
  // There is no ValidatedT data type in cats,
  // since Validated is an applicative,
  // and applicatives stack - obviously! ;)
  // https://github.com/typelevel/cats/issues/1818
  type ValidatedT[F[_], A, B] = Nested[F, Validated[A, ?], B]

  // For some reason, compiler can't find an implicit applicative without creating this utility method (?)
  implicit def validatedTApplicative(implicit ec: ExecutionContext): Applicative[ValidatedT[Future, NonEmptyList[AnalyticsServiceError], ?]] =
    Nested.catsDataApplicativeForNested[Future, Validated[NonEmptyList[AnalyticsServiceError], ?]]
}