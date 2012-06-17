package com.lucidmouse.scaladi.data

import collection.mutable.HashMap
import com.lucidmouse.scaladi.{InvalidOverridingException, AlreadyExistingIdException, UnknownIdException}

/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 20.05.12, 19:09
 */

private[scaladi] class ContextData(parentContextsData: Seq[ContextData]) extends NotNull {
  private val prototypes = new HashMap[String, ()=>Any]()
  private val singletons = new HashMap[String, Any]()
  private val lazySingletons = new HashMap[String, ()=>Any]()

  if (parentContextsData != null) for (parentCtx <- parentContextsData) {
    copyMap(parentCtx.prototypes, prototypes)
    copyMap(parentCtx.singletons, singletons)
    copyMap(parentCtx.lazySingletons, lazySingletons)
  }


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
      else throw new UnknownIdException(id)
    }
  }

  def addPrototype(id: String, creator: ()=>Any, overrides: Boolean) { addObjectToContext(id, ()=>{ prototypes(id) = creator }, overrides) }

  def addLazySingleton(id: String, creator: ()=>Any, overrides: Boolean) { addObjectToContext(id, ()=>{ lazySingletons(id) = creator }, overrides) }

  def addSingleton(id: String, obj: Any, overrides: Boolean) { addObjectToContext(id, ()=>{ singletons(id) = obj }, overrides) }

  private def createObject(creator: ()=>Any): Any = {
    creator()
  }

  private def addObjectToContext(id: String, addFunction: ()=>Unit, isOverriding: Boolean) = synchronized {
    isOverriding match {
      case false => if (containsId(id)) throw new AlreadyExistingIdException(id)
      case true => if (!containsId(id)) throw new InvalidOverridingException(id) else removeId(id)
    }
    addFunction()
  }

  private def containsId(id: String) = (prototypes contains id) || (singletons contains id) || (lazySingletons contains id)

  private def removeId(id: String) = { prototypes remove id; singletons remove id; lazySingletons remove id } //TODO optimise

  private def copyMap[T](from: HashMap[String, T], to: HashMap[String, T]) = synchronized {
    from.foreach { case (key, value) => to update (key, value) }
  }

//  private def copyMap[T](from: Map[String, T], to: Map[String, T]) = synchronized {
//    from.foldLeft(to) { case (map, (key, value)) => map updated (key, value) }
//  }
}


private[scaladi] object InvalidContext extends ContextData(Seq.empty[ContextData]) {
  override def addPrototype(id: String, obj: () => Any, overrides: Boolean) {}

  override def addLazySingleton(id: String, obj: () => Any, overrides: Boolean) {}

  override def addSingleton(id: String, obj: Any, overrides: Boolean) {}

  override def get(id: String): Any = throw new IllegalStateException("No current context has been set!  \n" +
    "Please, run Context.choseGlobalContext(...) to set the current context..")
}
