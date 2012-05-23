package com.lucidmouse.scala.di.data

/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 19:47
 */

object ContextHolder {
  @volatile var chosenContext: ContextData = EmptyContext
  @volatile private var ctxHasBeenChosen = false

  def choseContext(context: ContextData) {
    chosenContext = context
    ctxHasBeenChosen = true
  }

  def eraseContextInformation() {
    chosenContext = EmptyContext
    ctxHasBeenChosen = false
  }

  def contextHasBeenChosen() = ctxHasBeenChosen
}
