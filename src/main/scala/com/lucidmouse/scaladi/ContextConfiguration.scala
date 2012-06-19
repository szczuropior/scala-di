package com.lucidmouse.scaladi

import data.{ContextDataOverrider, ContextDataUpdater, ContextHolder, ContextData}


/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 20:55
 */

/**
 * This class should be used to addFunction object to given context (identified by ctxName).
 * @param extendedContexts parent of _this_ context : all objects contained in parent (and parent's parents) not declared
 *                      implicitly by _this_ context will be accessible from _this_ context
 * @throws OverridingIDsInContextParentsException when parents of context contain identical ID
 * @throws AlreadyExistingIdException when ID is overridden without providing 'overrides id' keyword
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

  object overrides {
    /** Tells that @id ID is overriding ID of the context's parent. */
    def id (id: String) = new ContextDataOverrider(id, context)
  }

  /**
   * Tells that @id ID is overriding ID of the context's parent. Is short version of 'overrides id'.
   * @param id overridden ID
   * @return
   */
  def ^(id: String) = new ContextDataOverrider(id, context)
}

class InvalidIdException(msg: String) extends Exception(msg)

class InvalidOverridingException(msg: String) extends Exception(msg)