package example

import example.ExamplesGrpc.ExamplesBlockingStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

object HelloWorldClient {
  def blockingClient(host: String, port: Int): HelloWorldClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val blockingStub = ExamplesGrpc.blockingStub(channel)
    new HelloWorldClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = blockingClient("localhost", 50051)
    try {
      val user = args.headOption.getOrElse("world")
      client.greet(user)
    } finally {
      client.shutdown()
    }
  }
}

class HelloWorldClient (
  private val channel: ManagedChannel,
  private val blockingStub: ExamplesBlockingStub
) {
  val logger = Logger.getLogger(classOf[HelloWorldClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def greet(name: String): Unit = {
    logger.info("Will try to greet " + name + " ...")
    try {
      val response = blockingStub.sayHello(HelloRequest(name = name))
      logger.info("Greeting: " + response.message)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.SEVERE, "RPC failed: {0}", e.getStatus)
    }
  }
}