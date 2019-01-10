package com.gu.acquisition.services

import java.nio.ByteBuffer

import cats.data.EitherT
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder
import com.amazonaws.services.kinesis.model.{PutRecordRequest, PutRecordResult}
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.model.errors.AnalyticsServiceError.KinesisError
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.thrift.serializer.ThriftSerializer
import okhttp3.HttpUrl

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

sealed trait KinesisServiceConfig {
  val kinesisStreamName: String
}

//Credentials provider is only required by the kinesis client when running in ec2 or locally
case class Ec2OrLocalConfig(credentialsProvider: AWSCredentialsProviderChain, kinesisStreamName: String, ophanEndpoint: Option[HttpUrl] = None) extends KinesisServiceConfig
case class LambdaConfig(kinesisStreamName: String) extends KinesisServiceConfig

private [acquisition] class KinesisService(config: KinesisServiceConfig, region: String = "eu-west-1") extends AnalyticsService {

  private val kinesisClient = {
    val builder = AmazonKinesisAsyncClientBuilder.standard().withRegion(region)
    config match {
      case Ec2OrLocalConfig(provider, _, _) => builder.withCredentials(provider).build
      case _: LambdaConfig => builder.build
    }
  }

  private def putAcquisition(acquisitionSubmission: AcquisitionSubmission): EitherT[Future, AnalyticsServiceError, AcquisitionSubmission] = {
    val promise = Promise[Either[AnalyticsServiceError, AcquisitionSubmission]]()

    val request: PutRecordRequest = {
      val buffer = ByteBuffer.wrap {
        ThriftSerializer.serializeToBytes(acquisitionSubmission.acquisition, userCompressionType = None, thriftBufferInitialSize = None)
      }

      new PutRecordRequest()
        .withStreamName(config.kinesisStreamName)
        .withPartitionKey(acquisitionSubmission.acquisition.identityId.getOrElse(acquisitionSubmission.acquisition.amount.toString))
        .withData(buffer)
    }

    kinesisClient.putRecordAsync(request, new AsyncHandler[PutRecordRequest, PutRecordResult] {
      override def onError(exception: Exception): Unit = exception match {
        case NonFatal(e) => promise.failure(KinesisError(e))
        case fatal => promise.failure(fatal)
      }

      override def onSuccess(request: PutRecordRequest, result: PutRecordResult): Unit = promise.success(Right(acquisitionSubmission))
    })

    EitherT(promise.future)
  }

  def submit[A : AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext): EitherT[Future, AnalyticsServiceError, AcquisitionSubmission] = {
    import AcquisitionSubmissionBuilder.ops._
    import cats.instances.future._

    for {
      submission <- EitherT.fromEither(a.asAcquisitionSubmission)
      result <- putAcquisition(submission)
    } yield result
  }
}
