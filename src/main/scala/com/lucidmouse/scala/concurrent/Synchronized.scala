package com.lucidmouse.scala.concurrent

import concurrent.Lock

/**
 * Created by: michal
 * 15.05.12, 20:20
 */


trait Synchronized {
  val lock = new Lock

  def synchronized(f: () => Unit) = {
    try {
      lock.acquire()
      f()
    } finally {
      lock.release()
    }
  }
}
