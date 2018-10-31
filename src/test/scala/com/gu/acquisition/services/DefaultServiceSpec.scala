package com.gu.acquisition.services

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import com.gu.acquisition.model.errors.AnalyticsServiceError._
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import okhttp3.OkHttpClient
import org.scalatest.AsyncWordSpec

import scala.concurrent.Future

class DefaultServiceSpec extends AsyncWordSpec with MockitoSugar {

  // Example case class which will be submitted as an acquisition event.
  case class AcquisitionData(id: String)
  object AcquisitionData {
    implicit val builder: AcquisitionSubmissionBuilder[AcquisitionData] = null // Ok being null; never used
  }

  val buildError: AnalyticsServiceError = BuildError("unable to build acquisition")

  val gaService: AcquisitionService = mock[AcquisitionService]
  val ophanService: AcquisitionService = mock[AcquisitionService]

  val service: AcquisitionService = {
    implicit val httpClient: OkHttpClient = mock[OkHttpClient]
    new DefaultAcquisitionService(List(gaService, ophanService))
  }

  "The default acquisition service" when {

    "multiple errors are returned by the constituent services" should {

      "return all errors" in {

        when(gaService.submit(any)(any, any)).thenReturn(EitherT.leftT[Future, AcquisitionSubmission](buildError))

        when(ophanService.submit(any)(any, any)).thenReturn(EitherT.leftT[Future, AcquisitionSubmission](buildError))

        service.submit(AcquisitionData("test-id")).fold(
          err => assert(err == Collection(NonEmptyList(buildError, List(buildError)))),
          _ => fail("errors not accumulated")
        )
      }
    }

    "one constituent service returns an error" should {

      "return the error" in {

        when(gaService.submit(any)(any, any)).thenReturn(EitherT.leftT[Future, AcquisitionSubmission](buildError))

        when(ophanService.submit(any)(any, any)).thenReturn(EitherT.rightT[Future, AnalyticsServiceError](mock[AcquisitionSubmission]))

        service.submit(AcquisitionData("test-id")).fold(
          err => assert(err == buildError),
          _ => fail("errors not accumulated")
        )
      }
    }

    "no constituent service returns an error" should {

      "return a success" in {

        when(gaService.submit(any)(any, any)).thenReturn(EitherT.rightT[Future, AnalyticsServiceError](mock[AcquisitionSubmission]))

        when(ophanService.submit(any)(any, any)).thenReturn(EitherT.rightT[Future, AnalyticsServiceError](mock[AcquisitionSubmission]))

        service.submit(AcquisitionData("test-id")).fold(
          _ => fail("errors accumulated"),
          _ => succeed
        )
      }
    }
  }
}