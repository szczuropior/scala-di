package com.lucidmouse.scala.di

import data.{NewContextElement, ContextHolder, ContextData}


/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 20:55
 */

/**
 * This class should be used to addFunction object to given context (identified by ctxName).
 * @param parentContext parent of _this_ context : all objects contained in parent (and parent's parents) not declared
 *                      implicitly by _this_ context will be accessible from _this_ context
 */
class ContextConfiguration(val parentContext: ContextConfiguration = DummyContextCreator) extends NotNull {
  val context: ContextData = if (parentContext != null) new ContextData(parentContext.context) else new ContextData()
  implicit def string2CtxElt(id: String) = new NewContextElement(id, this.context)

  def setAsCurrentContext() {ContextHolder choseContext(this.context)}

  def get[T](id: String): T = {
    val availableCtx = if (ContextHolder.contextHasBeenChosen()) ContextHolder.chosenContext else context
    availableCtx.get(id).asInstanceOf[T]
  }


  //TODO: use or not : protected def get(id: String): Any = { Context getContextOrCreateOne(ctxName, parentContextName) get(id) }
  //TODO IMPLEMENT: def getLazy(id: String): ()=>Any = {...}
}



private object DummyContextCreator extends ContextConfiguration {
  override val context = new ContextData()
}