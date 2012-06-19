package com.lucidmouse.scaladi.data

import collection.mutable.HashMap
import com.lucidmouse.scaladi._

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
      else raiseUnknownIdException(id)
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
      case false => if (containsId(id)) raiseAlreadyExistingIdException(id)
      case true => if (!containsId(id)) raiseInvalidOverridingException(id) else removeId(id)
    }
    addFunction()
  }

  private def containsId(id: String) = (prototypes contains id) || (singletons contains id) || (lazySingletons contains id)

  private def removeId(id: String) = { prototypes remove id; singletons remove id; lazySingletons remove id } //TODO optimise

  private def copyMap[T](from: HashMap[String, T], to: HashMap[String, T]) = synchronized {
    from.foreach { case (key, value) =>
      if (containsId(key)) raiseOverridingIDsInContextParentsException(key)
      else to(key) = value  //no need to remove ID from other scopes, because the ID cannot be stored in the context !
    }
  }

  //exceptions

  private def raiseInvalidOverridingException(id: String) = throw new InvalidOverridingException("Object idetidied by ID = '" +
    id + "' could not be found in parent context though 'overriddes id' modifier has been used.\n" +
    "Please remove 'overriddes id' modifier in the context definition or check whether proper ID has been used.")

  private def raiseOverridingIDsInContextParentsException(id: String) = throw new InvalidOverridingException(
    "Object idetidied by ID = '" + id + "' is contained by at least two parents of the context.\n" +
    "Please, change the value of one of IDs, or use as a parent only one of the contexts with same IDs " +
      "(especially if one of the contexts extends other one).")

  private def raiseAlreadyExistingIdException(id: String) = throw new InvalidIdException("Object idetidied by ID = '" +
    id + "' has already been added to the context!\nIf you actually want to override the object with given ID, " +
    "please use 'overrides id' prefix (overrides id " + id + " ...) while adding the object to the context.")

  private def raiseUnknownIdException(id: String) = throw new InvalidIdException("Object idetidied by ID = '" +
    id + "' could not have been found!")
}


private[scaladi] object InvalidContext extends ContextData(Seq.empty[ContextData]) {
  override def addPrototype(id: String, obj: () => Any, overrides: Boolean) {}

  override def addLazySingleton(id: String, obj: () => Any, overrides: Boolean) {}

  override def addSingleton(id: String, obj: Any, overrides: Boolean) {}

  override def get(id: String): Any = throw new IllegalStateException("No current context has been set!  \n" +
    "Please, run Context.choseGlobalContext(...) to set the current context..")
}
