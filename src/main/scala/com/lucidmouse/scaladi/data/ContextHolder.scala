package com.lucidmouse.scaladi.data

/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 19:47
 */

private[scaladi] object ContextHolder {
  @volatile var chosenContext: ContextData = InvalidContext
  @volatile private var ctxHasBeenChosen = false

  def choseGlobalContext(context: ContextData) {
    chosenContext = context
    ctxHasBeenChosen = true
  }

  def eraseGlobalContextInformation() {
    chosenContext = InvalidContext
    ctxHasBeenChosen = false
  }

  def contextHasBeenChosen() = ctxHasBeenChosen
}
