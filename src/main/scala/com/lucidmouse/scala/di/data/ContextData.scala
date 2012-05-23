package com.lucidmouse.scala.di.data

import collection.mutable.HashMap

/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 19:09
 */

class ContextData(val parentCtxData: ContextData = EmptyContext) extends NotNull {
  private val prototypes = new HashMap[String, ()=>Any]()
  private val singletons = new HashMap[String, Any]()
  private val lazySingletons = new HashMap[String, ()=>Any]()

  def get(id: String): Any = {
    synchronized {
      if (singletons contains id) singletons get(id) get
      else if (prototypes contains id) createObject(prototypes get id get)
      else if (lazySingletons contains id) {
        val newSingleton = createObject(lazySingletons get id get)
        singletons(id) = newSingleton
        lazySingletons remove id
        newSingleton
      }
      else parentCtxData.get(id)  //parentCtxData can't be null, because ContextData is NotNull
    }
  }

  def addPrototype(id: String, creator: ()=>Any) { addObjectToContext(id, ()=>{prototypes(id) = creator}) }

  def addLazySingleton(id: String, creator: ()=>Any) { addObjectToContext(id, ()=>{lazySingletons(id) = creator}) }

  def addSingleton(id: String, obj: Any) { addObjectToContext(id, ()=>{singletons(id) = obj}) }

  private def createObject(creator: ()=>Any): Any = {
    creator()
  }

  private def addObjectToContext(id: String, addFunction: ()=>Unit) {
    synchronized {
      if ((prototypes contains id) || (singletons contains id) || (lazySingletons contains id)) {
        throw new AlreadyExistingIdException(id)
      }
      addFunction()
    }
  }
}


private class EmptyContext extends ContextData() {
  override def addPrototype(id: String, obj: () => Any) {}

  override def addLazySingleton(id: String, obj: () => Any) {}

  override def addSingleton(id: String, obj: Any) {}
}

private object InvalidContext extends EmptyContext {
  override def get(id: String): Any = throw new IllegalStateException("No current context has been set!  \n" +
    "Please, run Context.choseContext(...) to set the current context..")
}


private object EmptyContext extends EmptyContext {
  override def get(id: String): Any = throw new UnknownIdException(id)
}


class UnknownIdException(id: String) extends Exception("Object idetidied by ID = '" + id + "' could not have been found!")

class AlreadyExistingIdException(id: String) extends Exception("Object idetidied by ID = '" + id + "' has already been added to the context!")