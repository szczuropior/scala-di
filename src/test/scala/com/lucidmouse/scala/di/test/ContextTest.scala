package com.lucidmouse.scala.di.test

import org.junit.runner.RunWith

import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import com.lucidmouse.scala.di.{Context, ContextConfiguration}


/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 13.05.12, 12:33
 */

@RunWith(classOf[JUnitRunner])
class ContextTest extends FlatSpec with ShouldMatchers {
  "Object added by ContextConfiguration" should "be able to be obtained by Context" in {
    //having
    object Ctx1 extends ContextConfiguration {
      "1" singleton "one"
      "2" lazySingleton{ () => "two" }
      "3" prototype { () => new String("three") }
    }
    Ctx1 setAsCurrentContext

    //when
    object Ctx1User extends Context {
      val one: String = get("1").asInstanceOf[String]
      val two: String = get("2").asInstanceOf[String]
      val three: String = get("3").asInstanceOf[String]

    }

    //then
    Ctx1User.one should equal ("one")
    Ctx1User.two should equal ("two")
    Ctx1User.three should equal ("three")
  }


  "Singleton" should "be created only once (on adding it to context)" in {
    val creationCounter = new Counter
    val ctx = new ContextConfiguration() { "o" singleton new CountingObject(creationCounter) }
    checkObjectsQuantity(ctx, objectId = "o", creationCounter = creationCounter, expectedAmountBeforeGet = 1,
                             expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Lazy singleton" should "be created only once (on 1st get)" in {
    val creationCounter = new Counter
    val ctx = new ContextConfiguration() { "o" lazySingleton { () => new CountingObject(creationCounter) } }
    checkObjectsQuantity(ctx, objectId = "o", creationCounter = creationCounter, expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Prototype" should "be created per each get request" in {
    val creationCounter = new Counter
    val ctx = new ContextConfiguration() { "o" prototype { () => new CountingObject(creationCounter) } }
    checkObjectsQuantity(ctx, objectId = "o", creationCounter = creationCounter, expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 2)
  }


  // ----- helper functions -----

  def checkObjectsQuantity(ctx: ContextConfiguration, objectId: String, creationCounter: Counter,
                           expectedAmountBeforeGet: Int, expectedAmountAfter1stGet: Int, expectedAmountAfter2ndGet: Int) {
    //having
    ctx setAsCurrentContext

    //when
    val actualAmountBeforeGet = creationCounter.counter
    object CtxUser extends Context {
      get(objectId)
      val actualAmountAfter1stGet = creationCounter.counter
      get(objectId)
      val actualAmountAfter2ndGet = creationCounter.counter
    }

    //then
    actualAmountBeforeGet should equal (expectedAmountBeforeGet)
    CtxUser.actualAmountAfter1stGet should equal (expectedAmountAfter1stGet)
    CtxUser.actualAmountAfter2ndGet should equal (expectedAmountAfter2ndGet)
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


