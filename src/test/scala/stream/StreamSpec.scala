package stream

import org.scalatest.{Matchers, WordSpec}
import swaydb.{Bag, Stream}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

class StreamSpec extends WordSpec with Matchers {

  def stream[BAG[_]](implicit bag: Bag[BAG]) =
    Stream
      .range(1, 1000)
      .map(_ - 1)
      .filter(_ % 2 == 0)

  "stream Bag.Less" in {
    implicit val bag = Bag.less

    val sum = stream.foldLeft(0)(_ + _)
    sum shouldBe 249500
  }

  "stream Try" in {
    implicit val bag = Bag.tryBag

    val sum: Try[Int] = stream.foldLeft(0)(_ + _)
    sum.get shouldBe 249500
  }

  "stream Future" in {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val bag = Bag.future

    val sum: Future[Int] = stream.foldLeft(0)(_ + _)
    Await.result(sum, 1.second) shouldBe 249500
  }

  "stream Monix Task" in {
    import monix.eval._
    implicit val scheduler = monix.execution.Scheduler.global
    import swaydb.monix.Bag._ //import monix tag to support Task.

    val sum: Task[Int] = stream.foldLeft(0)(_ + _)
    Await.result(sum.runToFuture, 1.second) shouldBe 249500
  }

  "stream ZIO Task" in {
    import zio.Task
    import zio.Runtime

    implicit val runtime = Runtime.default
    import swaydb.zio.Bag._ //import zio tag to support Task.

    val sum: Task[Int] = stream.foldLeft(0)(_ + _)
    Await.result(runtime.unsafeRunToFuture(sum), 1.second) shouldBe 249500
  }
}
