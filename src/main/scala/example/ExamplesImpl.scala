package example

import io.grpc.stub.StreamObserver

import java.util.logging.{Level, Logger}
import scala.concurrent.Future

class ExamplesImpl extends ExamplesGrpc.Examples {
  val logger = Logger.getLogger(this.getClass.getSimpleName)

  override def sayHello(req: HelloRequest) = {
    val reply = HelloReply(message = "Hello " + req.name)
    logger.info("sending response: " + reply.message)
    Future.successful(reply)
  }

  override def serverStreamExample(request: Seed, responseObserver: StreamObserver[JustANumber]): Unit = {
    logger.info("In server streaming example.")
    val random  = new scala.util.Random(request.seed)
    (1 to 10).foreach { _ =>
      val response = JustANumber(random.nextInt(100))
      logger.info(s"Sending back the random number: ${response.number}")
      responseObserver.onNext(response)
      //doesn't seem to work without the sleep
      Thread.sleep(1000)
    }
    logger.info(s"Done sending back random numbers.")
    responseObserver.onCompleted()
  }

  override def clientStreamExample(responseObserver: StreamObserver[ReplySum]): StreamObserver[JustANumber] = {
    var sum: Int = 0

    val requestObserver = new StreamObserver[JustANumber] {
      override def onNext(req: JustANumber) = {
        sum  = sum + req.number
        logger.info(s"The current sum is: $sum")
      }
      override def onError(t: Throwable) = {
        logger.info(s"There has been an error: ${t.getMessage}")
        throw t
      }
      override def onCompleted() = {
        logger.info("There are no more numbers coming. I am now responding with the sum.")
        responseObserver.onNext(new ReplySum(sum))
        responseObserver.onCompleted()
      }
    }

    requestObserver
  }

  override def bidirectionalStreamingExample(responseObserver: StreamObserver[JustANumber]): StreamObserver[JustANumber] = {
    var sum: Int = 0

    val requestObserver = new StreamObserver[JustANumber] {
      override def onNext(req: JustANumber) = {
        sum = sum + req.number
        responseObserver.onNext(JustANumber(sum))
        logger.info(s"The current total is: $sum")
      }

      override def onError(t: Throwable) = {
        logger.info(s"There has been an error: ${t.getMessage}")
        throw t
      }

      override def onCompleted() = {
        logger.info("There are no more numbers coming. I am completing the response observer now.")
        responseObserver.onCompleted()
      }
    }

    requestObserver
  }
}