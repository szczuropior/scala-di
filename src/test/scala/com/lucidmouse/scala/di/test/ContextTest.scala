package com.lucidmouse.scala.di.test

import org.junit.runner.RunWith

import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import com.lucidmouse.scala.di.{UnknownIdException, Context, ContextCreator}


/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 13.05.12, 12:33
 */

@RunWith(classOf[JUnitRunner])
class ContextTest extends FlatSpec with ShouldMatchers {
  "(test) Method cleaning context" should "remove all added data to context" in {
    object Ctx1 extends ContextCreator("main") {
      "1" singleton "one"
      "2" lazySingleton{ () => "two" }
      "3" prototype { () => new String("three") }
    }
    Ctx1
    Context setContext "main"
    object Ctx1User extends Context {
      get("1") should equal ("one")
      get("2") should equal ("two")
      get("3") should equal ("three")
      val one: String = get("1").asInstanceOf[String]
      one should equal ("one")
    }
    Ctx1User
    Context.removeAllContextsInformation()
    object Ctx2 extends ContextCreator("main") {
      "4" singleton "four"
    }
    Ctx2
    Context setContext "main"
    object Ctx2User extends Context {
      evaluating {get("1")} should produce [UnknownIdException]
      evaluating {get("2")} should produce [UnknownIdException]
      evaluating {get("3")} should produce [UnknownIdException]
      get("4") should equal ("four")
    }
    Ctx2User
  }


  "Singleton" should "be created only once (on adding it to context)" in {
    val creationCounter = new Counter
    new ContextCreator("main") { "o" singleton new CountingObject(creationCounter) }
    checkObjectsQuantity(objectName = "o", creationCounter = creationCounter, expectedAmountBeforeGet = 1,
                             expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Lazy singleton" should "be created only once (on 1st get)" in {
    val creationCounter = new Counter
    new ContextCreator("main") { "o" lazySingleton { () => new CountingObject(creationCounter) } }
    checkObjectsQuantity(objectName = "o", creationCounter = creationCounter, expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Prototype" should "be created per each get request" in {
    val creationCounter = new Counter
    new ContextCreator("main") { "o" prototype { () => new CountingObject(creationCounter) } }
    checkObjectsQuantity(objectName = "o", creationCounter = creationCounter, expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 2)
  }


  // ----- helper functions -----

  def checkObjectsQuantity(objectName: String, creationCounter: Counter, expectedAmountBeforeGet: Int,
                           expectedAmountAfter1stGet: Int, expectedAmountAfter2ndGet: Int) {
    creationCounter.counter should equal (expectedAmountBeforeGet)
    Context setContext "main"
    object CtxUser extends Context {
      get(objectName)
      creationCounter.counter should equal (expectedAmountAfter1stGet)
      get(objectName)
      creationCounter.counter should equal (expectedAmountAfter2ndGet)
    }
    CtxUser
    Context.removeAllContextsInformation()
  }
}


//  ------ helper classes ------

class Counter {
  var counter = 0
  def increase() { counter += 1 }
}

class CountingObject(val counter: Counter) {
  counter.increase()
}


