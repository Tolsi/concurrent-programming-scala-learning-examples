package org.learningconcurrency
package exercises
package ch2

import scala.collection.mutable

object Ex10 extends App {

  class PriorityTaskPool(val p:Int, val important: Int) {

    implicit val ord: Ordering[(Int,() => Unit)] = Ordering.by(_._1)

    private val tasks = mutable.PriorityQueue[(Int,() => Unit)]()

    @volatile
    private var terminated = false

    def asynchronous(priority: Int)(task: => Unit):Unit = tasks synchronized {
      tasks.enqueue((priority,() => task))
      tasks.notify
    }

    class Worker extends Thread {

      def poll() = tasks.synchronized {
        while (tasks.isEmpty) {
          tasks.wait()
        }
        log(s"queue: " + tasks.foldLeft("")((s,t)=>s"$s${t._1},"))
        tasks.dequeue()
      }

      override def run() = {
        while (true) {
          poll() match {
            case (p, task) if (p > important) || (!terminated) => task()
            case _  =>
          }
        }
      }
    }

    def shutdown() = tasks.synchronized {
      terminated = true
      tasks.notify()
    }

    (1 to p).map((i) => new Worker()).map(_.start)

  }

  val tasks = new PriorityTaskPool(10, 300)

  (1 to 1000).foreach((i) => {
    val a = (Math.random*1000).toInt
    tasks.asynchronous(a)({log(s"<- $a")})
  })

  Thread.sleep(1)
  tasks.shutdown()

}
