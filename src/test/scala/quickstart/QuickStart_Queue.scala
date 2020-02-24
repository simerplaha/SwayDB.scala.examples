package quickstart

import scala.concurrent.duration._

object QuickStart_Queue extends App {

  import swaydb._
  import swaydb.serializers.Default._

  val queue = memory.Queue[Int]().get

  queue.push(elem = 1)
  queue.push(elem = 2, expireAfter = 0.millisecond) //expire now
  queue.push(elem = 3)

  assert(queue.popOrNull() == 1)
  assert(queue.popOrNull() == 3) //3 because 2 is expired.
}
