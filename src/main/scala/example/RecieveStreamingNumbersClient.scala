package example

import example.ExamplesGrpc.ExamplesStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import io.grpc.stub.StreamObserver

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

object ReceiveStreamingNumbersClient {
  def nonBlockingClient(): ReceiveStreamingNumbersClient = {
    //the streaming won't work without the usePlainText() specification - looks like it breaks the syntax checker... but it still runs
    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build
    val stub: ExamplesStub = ExamplesGrpc.stub(channel)
    new ReceiveStreamingNumbersClient(channel, stub)
  }

  def main(args: Array[String]) {
    val client = nonBlockingClient()
    try {
      client.getRandomInts()
    } finally {
      client.shutdown()
    }
  }
}

class ReceiveStreamingNumbersClient(
  private val channel: ManagedChannel,
  private val stub: ExamplesStub
) {
  val logger = Logger.getLogger("ReceiveStreamingNumbersClient")

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  var seqOfRandomInts: Seq[Int] = Seq.empty

  val respObsv: StreamObserver[JustANumber] = new StreamObserver[JustANumber] {
    override def onNext(reply: JustANumber) = {
      seqOfRandomInts = seqOfRandomInts ++ Seq(reply.number)
      logger.info(s"The current sequence is: ${seqOfRandomInts.toString}.")
    }

    override def onError(t: Throwable) =
      logger.log(Level.SEVERE, t.getMessage, t)

    override def onCompleted() =
      logger.info(s"The whole exchange is complete. The final sequence is: ${seqOfRandomInts.toString}")
  }

  def getRandomInts() = {
    try {
      stub.serverStreamExample(Seed(44), respObsv)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.SEVERE, "RPC failed: {0}", e.getStatus)
    }
  }
}
