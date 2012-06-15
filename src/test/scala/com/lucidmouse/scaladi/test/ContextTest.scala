package com.lucidmouse.scaladi.test

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import org.scalatest.junit.JUnitRunner
import com.lucidmouse.scaladi.data.{ContextHolder}
import com.lucidmouse.scaladi.{UnknownIdException, AlreadyExistingIdException, Context, ContextConfiguration}
import org.junit.runner.RunWith


/**
 * Created by: m.ludwinowicz[a]gmail.com
 * 13.05.12, 12:33
 */


@RunWith(classOf[JUnitRunner])
class ContextTest extends FlatSpec with ShouldMatchers with BeforeAndAfterEach {

  override def beforeEach() {
    Counter.reset()
    ContextHolder.eraseGlobalContextInformation()
  }

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
    val ctx = new ContextConfiguration { "o" singleton new CountingObject() }
    checkObjectsQuantity(ctx, objectId = "o", expectedAmountBeforeGet = 1,
                             expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Lazy singleton" should "be created only once (on 1st get)" in {
    val ctx = new ContextConfiguration { "o" lazySingleton { () => new CountingObject() } }
    checkObjectsQuantity(ctx, objectId = "o", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Prototype" should "be created per each get request" in {
    val ctx = new ContextConfiguration { "o" prototype { () => new CountingObject() } }
    checkObjectsQuantity(ctx, objectId = "o", expectedAmountBeforeGet = 0,
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
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
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
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
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
    object CtxParent extends ContextConfiguration(extendedContexts = CtxGrandparent) {
      "1" prototype { ()=>"one parent" }
      "4" prototype { ()=>"four parent" }
    }
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
      "2" lazySingleton { ()=>"two child" }
    }
    object CtxGrandchild extends ContextConfiguration(extendedContexts = CtxChild) {
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


  "Component of Lazy Singleton container" should "be initialized lazy when is lazy itself" in {
    val ctx = new ContextConfiguration {
      "component" lazySingleton { ()=>new CountingObject() }
      "container" lazySingleton { ()=>new Container(get("component")) }
    }
    checkObjectsQuantity(ctx, objectId = "container", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }

  "Component of Prototype container" should "be initialized lazy when is prototype itself" in {
    val ctx = new ContextConfiguration {
      "component" prototype { ()=>new CountingObject() }
      "container" prototype { ()=>new Container(get("component")) }
    }
    checkObjectsQuantity(ctx, objectId = "container", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 2)
  }

  "Component of Lazy Singleton container" should "be initialized fast when is regular singleton" in {
    val ctx = new ContextConfiguration {
      "component" singleton new CountingObject()
      "container" lazySingleton { ()=>new Container(get("component")) }
    }
    checkObjectsQuantity(ctx, objectId = "container", expectedAmountBeforeGet = 1,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }

  "Component of Singleton container" should "be initialized fast and only once even if is lazy and prototype" in {
    val ctx = new ContextConfiguration {
      "component" prototype { ()=>{ new CountingObject() } }
      "container" singleton new Container(get("component").asInstanceOf[CountingObject])
    }
    checkObjectsQuantity(ctx, objectId = "container", expectedAmountBeforeGet = 1,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }

  "Context" should "guess expected object type" in {
    //having
    object Ctx extends ContextConfiguration {
      "1" singleton "1"
      "2" singleton 2
    }
    //when
    Ctx setAsCurrentContext
    object ContextUser extends Context {
      def getOne: String = get("1")
      def getTwo: Int = get("2")
    }
    // then
    ContextUser.getOne should equal ("1")
    ContextUser.getTwo should equal (2)
  }


  "Context extending multiple contexts" should "contain all objects stored by them and objects contained by itself" in {
    //having
    object Ctx12 extends ContextConfiguration {
      "1" singleton "1"
      "2" prototype (()=>2)
    }
    object Ctx3 extends ContextConfiguration {
      "3" lazySingleton (()=>"3")
    }
    object Ctx4 extends ContextConfiguration {
      "4" singleton "4"
    }
    object Ctx123456 extends ContextConfiguration(Ctx12, Ctx3, Ctx4) {
      "5" prototype (()=>5)
      "6" singleton "6"
    }
    //when
    Ctx123456 setAsCurrentContext
    object ContextUser extends Context {
      def get1: String = get("1")
      def get2: Int = get("2")
      def get3: String = get("3")
      def get4: String = get("4")
      def get5: Int = get("5")
      def get6: String = get("6")
    }
    // then
    ContextUser.get1 should equal ("1")
    ContextUser.get2 should equal (2)
    ContextUser.get3 should equal ("3")
    ContextUser.get4 should equal ("4")
    ContextUser.get5 should equal (5)
    ContextUser.get6 should equal ("6")
  }

  "When context1 extends context2 containing object with identical id, context1 object" should "be obtained on get" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1" singleton "1-CtxParent"
      "2" prototype (()=>"2-CtxParent")
      "3" singleton "3-CtxParent"
    }
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
      "1" singleton "1-CtxChild"
      "2" prototype (()=>"2-CtxChild")
      "4" prototype (()=>"4-CtxChild")
    }
    //when
    CtxChild setAsCurrentContext
    object ContextUser extends Context {
      def get1: String = get("1")
      def get2: String = get("2")
      def get3: String = get("3")
      def get4: String = get("4")
    }
    // then
    ContextUser.get1 should equal ("1-CtxChild1")
    ContextUser.get2 should equal ("2-CtxChild")
    ContextUser.get3 should equal ("3-CtxParent")
    ContextUser.get4 should equal ("4-CtxChild")
  }

  "Object stored by last context on multiple extending list" should "be the visible one" in {

  }

  "When ctx1 extends ctx2 and there are objects with identical ids with scopes difference, main object scope" should "be taken into account" in {

  }

  "When ctx extends multiple contexts and there are object scope differences on extension list, the last ctx scope" should "be taken into account" in {

  }

  // ----- helper functions -----

  def checkObjectsQuantity(ctx: ContextConfiguration, objectId: String, expectedAmountBeforeGet: Int,
                           expectedAmountAfter1stGet: Int, expectedAmountAfter2ndGet: Int) {
    //having
    ctx setAsCurrentContext

    //when
    val actualAmountBeforeGet = Counter.counter
    object CtxUser extends Context {
      get(objectId)
      val actualAmountAfter1stGet = Counter.counter
      get(objectId)
      val actualAmountAfter2ndGet = Counter.counter
    }

    //then
    actualAmountBeforeGet should equal (expectedAmountBeforeGet)
    CtxUser.actualAmountAfter1stGet should equal (expectedAmountAfter1stGet)
    CtxUser.actualAmountAfter2ndGet should equal (expectedAmountAfter2ndGet)
  }
}


//  ------ helper classes ------

object Counter {
  var counter = 0
  def increase() { counter += 1 }
  def reset() {counter = 0}
}

class CountingObject() {
  Counter.increase()
}


class Container(val counter: CountingObject) {
}

