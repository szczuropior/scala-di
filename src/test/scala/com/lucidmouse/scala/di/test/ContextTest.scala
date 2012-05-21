package com.lucidmouse.scala.di.test

import org.junit.runner.RunWith

import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import com.lucidmouse.scala.di.{Context, ContextConfiguration}
import com.lucidmouse.scala.di.data.{UnknownIdException, AlreadyExistingIdException}


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

  "AlreadyExistingIdException" should "be thrown on attempt to add already existing id" in {
    object Ctx1 extends ContextConfiguration {
      //having
      "1" singleton "one"
      "2" lazySingleton{ () => "two" }
      "3" prototype { () => new String("three") }
      //when + then
      evaluating { "1" singleton "one" } should produce [AlreadyExistingIdException]
      evaluating { "1" lazySingleton { () => "one" } } should produce [AlreadyExistingIdException]
      evaluating { "1" prototype { () => new String("one") } } should produce [AlreadyExistingIdException]
      evaluating { "2" singleton "two" } should produce [AlreadyExistingIdException]
      evaluating { "2" lazySingleton{ () => "two" } } should produce [AlreadyExistingIdException]
      evaluating { "2" prototype { () => new String("two") } } should produce [AlreadyExistingIdException]
      evaluating { "3" singleton "three" } should produce [AlreadyExistingIdException]
      evaluating { "3" lazySingleton { () => "three" } } should produce [AlreadyExistingIdException]
      evaluating { "3" prototype { () => new String("three") } } should produce [AlreadyExistingIdException]
    }
    Ctx1 setAsCurrentContext
  }

  "UnknownIdException" should "be thrown on attempt to get not existing object" in {
    //having
    object Ctx1 extends ContextConfiguration {
      "1" singleton "one"
    }
    Ctx1 setAsCurrentContext
    //when
    object Ctx1User extends Context {
      def getObject(id: String) = get(id).asInstanceOf[String]
    }
    // then
    evaluating { Ctx1User.getObject("2") } should produce [UnknownIdException]
  }


  "Child context object" should "be returned when asking for Id overriden by given context" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1" singleton "one"
      "2" singleton "two"
    }
    object CtxChild extends ContextConfiguration(parentContext = CtxParent) {
      "1" singleton "ONE!"
    }
    CtxChild setAsCurrentContext
    //when
    object ChildContextUser extends Context {
      def getOne : String = get("1").asInstanceOf[String]
    }
    // then
    ChildContextUser.getOne should equal ("ONE!")
  }


  "Parent context object" should "be returned when asking for Id NOT overriden by child context" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1" singleton "one"
      "2" singleton "two"
    }
    object CtxChild extends ContextConfiguration(parentContext = CtxParent) {
      "1" singleton "ONE!"
    }
    CtxChild setAsCurrentContext
    //when
    object ChildContextUser extends Context {
      def getTwo : String = get("2").asInstanceOf[String]
    }
    // then
    ChildContextUser.getTwo should equal ("two")
  }

    "Possible youngest context object" should "be returned when asking for given Id" in {
    //having
    object CtxGrandparent extends ContextConfiguration {
      "1" singleton "one grandparent"
      "2" singleton "two grandparent"
      "3" singleton "three grandparent"
    }
    object CtxParent extends ContextConfiguration(parentContext = CtxGrandparent) {
      "1" prototype { ()=>"one parent" }
      "4" prototype { ()=>"four parent" }
    }
    object CtxChild extends ContextConfiguration(parentContext = CtxParent) {
      "2" lazySingleton { ()=>"two child" }
    }
    object CtxGrandchild extends ContextConfiguration(parentContext = CtxChild) {
      "5" singleton "five grandchild"
    }
    CtxGrandchild setAsCurrentContext
    //when
    object ContextUser extends Context {
      def getObject(id: String) : String = get(id).asInstanceOf[String]
    }
    // then
    ContextUser.getObject("1") should equal ("one parent")
    ContextUser.getObject("2") should equal ("two child")
    ContextUser.getObject("3") should equal ("three grandparent")
    ContextUser.getObject("4") should equal ("four parent")
    ContextUser.getObject("5") should equal ("five grandchild")
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


