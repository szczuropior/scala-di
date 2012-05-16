package com.lucidmouse.scala.di

import collection.mutable.HashMap
import com.lucidmouse.scala.concurrent.Synchronized

trait Context {
  def get(id: String): Any = Context getCurrentContext() get id
}


private object Context extends Synchronized {
  private val allContexts = new HashMap[String, CtxData]()
  private var chosenContext: CtxData = EmptyContext

  def setContext(context: String) {
    synchronized {
      chosenContext = allContexts get(context) getOrElse(EmptyContext)
    }
  }

  def getCurrentContext() = synchronized {
    chosenContext
  }

  def getContextOrCreateOne(name: String, parentContextName: String): CtxData = synchronized {
    if (!allContexts.contains(name)) {
      allContexts put(name, new CtxData(parentContextName))
    }
//    allContexts contains name match {   //<--- !!! THIS BREAKS SCALATEST TEST !!!!!! :/
//      case false => allContexts put(name, new CtxData(parentContextName))
//    }
    allContexts get name get
  }

  def getExistingContextOrEmptyOne(name: String): CtxData = synchronized {
    allContexts get(name) getOrElse(EmptyContext)
  }

  /** Only for tests */
  def removeAllContextsInformation() = synchronized {
    allContexts clear()
    chosenContext = EmptyContext
  }
}


private class CtxData(parentContextName: String) extends Synchronized {
  private val prototypes = new HashMap[String, ()=>Any]()
  private val singletons = new HashMap[String, Any]()
  private val lazySingletons = new HashMap[String, ()=>Any]()
  val parentContext: CtxData = Context.getExistingContextOrEmptyOne(parentContextName)

  def get(id: String): Any = {
    synchronized {
      if (singletons contains id) singletons get id get
      else if (prototypes contains id) createObject(prototypes get id get)
      else if (lazySingletons contains id) {
        val newSingleton = createObject(lazySingletons get id get)
        singletons(id) = newSingleton
        lazySingletons remove id
        newSingleton
      }
      else if (parentContext != EmptyContext) parentContext.get(id)
      else throw new UnknownIdException(id)
    }
  }

  def addPrototype(id: String, obj: ()=>Any) {synchronized{ prototypes(id) = obj }}

  def addLazySingleton(id: String, obj: ()=>Any) {synchronized{ lazySingletons(id) = obj }}

  def addSingleton(id: String, obj: Any) {synchronized{ singletons(id) = obj }}

  private def createObject(creator: ()=>Any): Any = {
    creator()
  }
}

private object EmptyContext extends CtxData("") {
  override def get(id: String): Any = throw new IllegalStateException("No context has been set properly!")
  override def addPrototype(id: String, obj: ()=>Any) {}
  override def addLazySingleton(id: String, obj: ()=>Any) {}
  override def addSingleton(id: String, obj: Any) {}
}