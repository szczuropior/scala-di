package com.lucidmouse.scala.di.data

/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 19:47
 */

object ContextHolder {
  @volatile var chosenContext: ContextData = EmptyContext

  def choseContext(context: ContextData) {
    chosenContext = context
  }

  protected def eraseContextInformation() {
    chosenContext = EmptyContext
  }
}
