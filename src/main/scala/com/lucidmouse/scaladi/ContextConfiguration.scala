package com.lucidmouse.scaladi

import data.{ContextDataUpdater, ContextHolder, ContextData}


/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 20:55
 */

/**
 * This class should be used to addFunction object to given context (identified by ctxName).
 * @param extendedContexts parent of _this_ context : all objects contained in parent (and parent's parents) not declared
 *                      implicitly by _this_ context will be accessible from _this_ context
 */
class ContextConfiguration(extendedContexts: ContextConfiguration*) extends NotNull {
  val context: ContextData =
    if (extendedContexts != null) new ContextData(extendedContexts map(_.context))
    else new ContextData(Seq.empty[ContextData])
  implicit def string2CtxElt(id: String) = new ContextDataUpdater(id, this.context)


  def extend(contexts: ContextConfiguration*) {}

  def setAsCurrentContext() {ContextHolder choseGlobalContext this.context}

  def get[T](id: String): T = {
    val availableCtx = if (ContextHolder.contextHasBeenChosen()) ContextHolder.chosenContext else context
    availableCtx.get(id).asInstanceOf[T]
  }
}

class AlreadyExistingIdException(id: String) extends Exception("Object idetidied by ID = '" + id + "' has already been added to the context!")