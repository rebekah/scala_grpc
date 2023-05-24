package example

import java.util.logging.Logger
import io.grpc.{Server, ServerBuilder}
import scala.concurrent.ExecutionContext

object ExamplesServer {
  val executionContext = ExecutionContext.global
  val logger = Logger.getLogger("HelloWorldServer")

  val port = 50051

  def main(args: Array[String]): Unit = {
    val server: Server = ServerBuilder
      .forPort(port)
      .addService(ExamplesGrpc.bindService(new ExamplesImpl, executionContext))
      .build
      .start()
    logger.info(s"starting Grpc server on port: ${port}")
    sys.addShutdownHook {
      server.shutdown()
    }
    server.awaitTermination()
  }
}
