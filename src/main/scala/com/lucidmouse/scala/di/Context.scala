package com.lucidmouse.scala.di

import data.ContextHolder


/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 12.05.12, 14:24
 */


/**
 * This trait can be used to obtain object contained in current context.
 * It's important to set current context before asking about objects stored in the context.
 * This should be done by executing setAsCurrentContext() method on ContextConfiguration instance.
 */
trait Context {
  def get(id: String): Any = ContextHolder.chosenContext.get(id)
}



