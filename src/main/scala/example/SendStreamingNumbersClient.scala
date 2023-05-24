package example

import example.ExamplesGrpc.ExamplesStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import io.grpc.stub.StreamObserver

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

object SendStreamingNumbersClient {
  def nonBlockingClient(): SendStreamingNumbersClient = {
    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build
    val stub: ExamplesStub = ExamplesGrpc.stub(channel)
    new SendStreamingNumbersClient(channel, stub)
  }

  def main(args: Array[String]) {
    val client = nonBlockingClient()
    try {
      client.sendNumbers()
    } finally {
      client.shutdown()
    }
  }
}

class SendStreamingNumbersClient(
  private val channel: ManagedChannel,
  private val stub: ExamplesStub
) {
  val logger = Logger.getLogger("SendNumbersStreamingClient")

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  val respObsv: StreamObserver[ReplySum] = new StreamObserver[ReplySum] {
    override def onNext(reply: ReplySum) =
      logger.info(s"The sum is: ${reply.sum.toString}.")

    override def onError(t: Throwable) =
      logger.log(Level.SEVERE, t.getMessage, t)

    override def onCompleted() =
      logger.info("The whole exchange is complete.")
  }

  def sendNumbers() = {
    try {
      val requestObsv: StreamObserver[JustANumber] = stub.clientStreamExample(respObsv)
      (1 to 10).foreach { num =>
        requestObsv.onNext(JustANumber(num))
      }
      requestObsv.onCompleted()
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.SEVERE, "RPC failed: {0}", e.getStatus)
    }
  }
}