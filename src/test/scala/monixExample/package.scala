import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

package object monixExample {

  implicit class TaskAwait[T](task: Task[T]) {

    def awaitTask(implicit scheduler: Scheduler): T =
      await()

    def await(timeout: FiniteDuration = 1.second)(implicit scheduler: Scheduler): T =
      Await.result(task.runToFuture, timeout)
  }

}
