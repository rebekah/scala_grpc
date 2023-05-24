package example

import example.ExamplesGrpc.ExamplesStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import io.grpc.stub.StreamObserver
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

object BidirectionalStreamingNumbersClient {
  def nonBlockingClient(): BidirectionalStreamingNumbersClient = {
    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build
    val stub: ExamplesStub = ExamplesGrpc.stub(channel)
    new BidirectionalStreamingNumbersClient(channel, stub)
  }

  def main(args: Array[String]) {
    val client = nonBlockingClient()
    try {
      client.getIncrementalTotals()
    } finally {
      client.shutdown()
    }
  }
}

class BidirectionalStreamingNumbersClient(
  private val channel: ManagedChannel,
  private val stub: ExamplesStub
) {
  val logger = Logger.getLogger("BidirectionalStreamingNumbersClient")

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  var total: Int = 0

  val respObsv: StreamObserver[JustANumber] = new StreamObserver[JustANumber] {
    override def onNext(reply: JustANumber) = {
      total = reply.number
      logger.info(s"The current total is: ${total.toString}.")
    }

    override def onError(t: Throwable) =
      logger.log(Level.SEVERE, t.getMessage, t)

    override def onCompleted() =
      logger.info(s"The whole exchange is complete. The final total is: $total.")
  }

  def getIncrementalTotals() = {
    try {
      val requestObsv: StreamObserver[JustANumber] = stub.bidirectionalStreamingExample(respObsv)
      (1 to 10).foreach { num =>
        requestObsv.onNext(JustANumber(num))
        //this is necessary or it doesn't seem to work
        Thread.sleep(1000)
      }
      requestObsv.onCompleted()
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.SEVERE, "RPC failed: {0}", e.getStatus)
    }
  }
}
