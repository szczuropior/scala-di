package com.lucidmouse.scala.di

/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 12.05.12, 14:24
 */


/**
 * This class can be used to add object to given context (identified by ctxName).
 * @param contextName name of the context (all objects added to context will be added to context of given name)
 * @param parentContextName name of the context parent : if an object with given ID is not defined in current context C1,
 *                          but is defined in its parent - object contained in parent will be returned when asking C1 about given ID
 */
private class ContextCreator(val contextName: String, val parentContextName: String = "") {
  implicit def string2CtxElt(id: String): CtxElt = new CtxElt(id, contextName, parentContextName)


  //TODO: use or not : protected def get(id: String): Any = { Context getContextOrCreateOne(ctxName, parentContextName) get(id) }
  //TODO IMPLEMENT: def getLazy(id: String): ()=>Any = {...}
}


class UnknownIdException(id: String) extends Exception("Object idetidied by ID = '" + id + "' could not have been found!")

private class CtxElt(val id: String, val ctxName: String, val parentContextName: String) {
  /** Creates Prototype: new object will be created on each Context.get() operation. */
  def prototype(x: ()=>Any) {Context getContextOrCreateOne(ctxName, parentContextName) addPrototype(id, x)}
  /** Creates Singleton (there will be only one object instance returned by Context.get()). */
  def singleton(x: Any) {Context getContextOrCreateOne(ctxName, parentContextName) addSingleton(id, x)}
  /** Creates Lazy Singleton (there will be only one object instance, created on 1st Context.get() operation). */
  def lazySingleton(x: ()=>Any) {Context getContextOrCreateOne(ctxName, parentContextName) addLazySingleton(id, x)}

  /** Creates Prototype: new object will be created on each Context.get() operation. */
  def :>(obj: ()=>Any) { prototype(obj) }
  /** Creates Lazy Singleton (there will be only one object instance, created on 1st get operation). */
  def ~>(obj: ()=>Any) { lazySingleton(obj) }
  /** Creates Singleton (there will be only one object instance). */
  def ->(obj: Any) { singleton(obj) }
}




