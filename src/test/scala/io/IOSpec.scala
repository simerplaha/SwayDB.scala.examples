package io

import java.rmi.RemoteException

import org.scalatest.{Matchers, WordSpec}
import swaydb.IO

class IOSpec extends WordSpec with Matchers {

  "type-safe errors" in {
    //Define typed errors that can occur in our application in one place.
    sealed trait MyApplicationErrors
    //suppose the application connects to a database and we need a typed error for failed connections
    //to a remote database.
    case class DatabaseConnectionError(message: String) extends MyApplicationErrors

    //Defined an ExceptionHandler that converts all typed exceptions to typed Errors.
    implicit val exceptionHandler =
      new IO.ExceptionHandler[MyApplicationErrors] {
        override def toException(error: MyApplicationErrors): Throwable =
          error match {
            case DatabaseConnectionError(message) =>
              new RemoteException(message)
          }

        override def toError(exception: Throwable): MyApplicationErrors =
          exception match {
            case exception: RemoteException =>
              DatabaseConnectionError(exception.getMessage)
          }
      }

    /**
      * Perform IO somewhere in the application that connects to the database.
      * Here all errors will be typed.
      */
    val connected: IO[MyApplicationErrors, Boolean] =
      IO {
        //suppose here we try to connect to the database but it throws an Exception.
        throw new RemoteException("Failed to connect.")
      } recover {
        //in recovery all error will be typed.
        case DatabaseConnectionError(message) =>
          //do some recovery. Connect to a different remote location.
          true //return true after recovery.
      }

    connected.get shouldBe true
  }

  "IO as Either - never exception" in {
    //IO can also be used as Scala's Either by defining neverException.
    //This will behave now like Scala's Either.
    implicit val exceptionHandler = IO.ExceptionHandler.neverException[Int]

    val right = IO[Int, String]("Some string")
    right.isRight shouldBe true
    right.isLeft shouldBe false
    right.get shouldBe "Some string"

    val left = IO.Left[Int, Boolean](12345)
    left.isRight shouldBe false
    left.isLeft shouldBe true
    left.value shouldBe 12345
  }

  "IO as Try" in {
    val io: IO[Throwable, String] =
      IO {
        "perform some IO"
      }

    io.get shouldBe "perform some IO"
  }
}
