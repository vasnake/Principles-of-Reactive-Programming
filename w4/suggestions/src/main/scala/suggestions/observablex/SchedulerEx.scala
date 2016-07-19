package suggestions
package observablex

import rx.lang.scala.Scheduler
import rx.schedulers.SwingScheduler

object SchedulerEx {
  
  val SwingEventThreadScheduler: Scheduler =
    rx.lang.scala.JavaConversions.javaSchedulerToScalaScheduler(SwingScheduler.getInstance)

}
