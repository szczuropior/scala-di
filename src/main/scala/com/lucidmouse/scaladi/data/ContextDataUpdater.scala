package com.lucidmouse.scaladi.data

/**
  * Created by: m.ludwinowicz[a]gmail.com
  * 20.05.12, 19:47
  */

private[scaladi] class ContextDataUpdater(id: String, ctx: ContextData) {
  protected val overrides = false;

  /** Creates Prototype: new object will be created on each Context.get() operation. */
     def prototype(x: =>Any) { ctx addPrototype(id, x, overrides) }

  /** Creates Singleton (there will be only one object instance returned by Context.get()). */
     def singleton(x: Any) { ctx addSingleton(id, x, overrides) }

  /** Creates Lazy Singleton (there will be only one object instance, created on 1st Context.get() operation). */
     def lazySingleton(x: =>Any) { ctx addLazySingleton(id, x, overrides) }

  /** Creates Prototype: new object will be created on each Context.get() operation. */
     def :>(obj: =>Any) { prototype(obj) }

  /** Creates Lazy Singleton (there will be only one object instance, created on 1st get operation). */
     def ~>(obj: =>Any) { lazySingleton(obj) }

  /** Creates Singleton (there will be only one object instance). */
     def ->(obj: Any) { singleton(obj) }
}




