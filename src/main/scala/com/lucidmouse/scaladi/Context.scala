package com.lucidmouse.scaladi

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
  def get[T](id: String): T = { ContextHolder.chosenContext.get(id) }.asInstanceOf[T]
}

object Context extends Context

class UnknownIdException(id: String) extends Exception("Object idetidied by ID = '" + id + "' could not have been found!")
