package com.lucidmouse.scala.di


import collection.mutable.HashMap
import concurrent.Lock
import com.lucidmouse.scala.concurrent.Synchronized


/**
 * Created by: michal ludwinowicz
 * 12.05.12, 14:24
 */

//+ nazwy do kontekswow, zeby np jesli zdefiniuje ctx "test" - nie nadpisal on wartosci w ctx "main"
//+ lazy ladowanie skladnikow objekcotw
//+ lazy ladowanie : obiekc zamiast funkcji


/**
 * Class that can be used to add object to given context (identified by ctxName)
 * @param ctxName name of the context (all objects added to context will be added to context of given name)
 */
private class ContextCreator(val ctxName: String, val parentContextName: String = "") {
  implicit def string2CtxElt(id: String): CtxElt = new CtxElt(id, ctxName, parentContextName)
  //implicit def any2Function(any: Any): ()=>Any = ()=>any


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
  def :>(obj: ()=>Any) = prototype(obj)
  /** Creates Lazy Singleton (there will be only one object instance, created on 1st get operation). */
  def ~>(obj: ()=>Any) = lazySingleton(obj)
  /** Creates Singleton (there will be only one object instance). */
  def ->(obj: Any) = singleton(obj)
}




