package com.lucidmouse.scaladi.test

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FlatSpec}
import org.scalatest.junit.JUnitRunner
import com.lucidmouse.scaladi.data.{ContextHolder}
import com.lucidmouse.scaladi._
import org.junit.runner.RunWith
import collection.mutable


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
      "2" lazySingleton{ "two" }
      "3" prototype { new String("three") }
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
    val ctx = new ContextConfiguration { "o" singleton new CountingObject("o") }
    checkObjectsQuantity(ctx, objectId = "o", expectedAmountBeforeGet = 1,
                             expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Lazy singleton" should "be created only once (on 1st get)" in {
    val ctx = new ContextConfiguration { "o" lazySingleton { new CountingObject("o") } }
    checkObjectsQuantity(ctx, objectId = "o", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }


  "Prototype" should "be created per each get request" in {
    val ctx = new ContextConfiguration { "o" prototype { new CountingObject("o") } }
    checkObjectsQuantity(ctx, objectId = "o", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 2)
  }

  "InvalidIdException" should "be thrown on attempt to add already existing id" in {
    object Ctx1 extends ContextConfiguration {
      //having
      "1" singleton "one"
      "2" lazySingleton{ "two" }
      "3" prototype { new String("three") }
      //when + then
      evaluating { "1" singleton "one" } should produce [InvalidIdException]
      evaluating { "1" lazySingleton{ "one" } } should produce [InvalidIdException]
      evaluating { "1" prototype { new String("one") } } should produce [InvalidIdException]
      evaluating { "2" singleton "two" } should produce [InvalidIdException]
      evaluating { "2" lazySingleton{ "two" } } should produce [InvalidIdException]
      evaluating { "2" prototype { new String("two") } } should produce [InvalidIdException]
      evaluating { "3" singleton "three" } should produce [InvalidIdException]
      evaluating { "3" lazySingleton{ "three" } } should produce [InvalidIdException]
      evaluating { "3" prototype { new String("three") } } should produce [InvalidIdException]
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
    evaluating { Ctx1User.getObject("2") } should produce [InvalidIdException]
  }


  "Child context object" should "be returned when asking for Id overrides$ by given context" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1" singleton "one"
      "2" singleton "two"
    }
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
      overrides id "1" singleton "ONE!"
    }
    CtxChild setAsCurrentContext
    //when
    object ChildContextUser extends Context {
      def getOne : String = get("1").asInstanceOf[String]
    }
    // then
    ChildContextUser.getOne should equal ("ONE!")
  }


  "Parent context object" should "be returned when asking for Id NOT overrides$ by child context" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1" singleton "one"
      "2" singleton "two"
    }
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
      overrides id "1" singleton "ONE!"
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
      overrides id "1" prototype { "one parent" }
      "4" prototype { "four parent" }
    }
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
      overrides id "2" lazySingleton { "two child" }
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
      "component" lazySingleton { new CountingObject("container") }
      "container" lazySingleton { new Container(get("component")) }
    }
    checkObjectsQuantity(ctx, objectId = "container", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }

  "Component of Prototype container" should "be initialized lazy when is prototype itself" in {
    val ctx = new ContextConfiguration {
      "component" prototype { new CountingObject("container") }
      "container" prototype { new Container(get("component")) }
    }
    checkObjectsQuantity(ctx, objectId = "container", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 2)
  }

  "Component of Lazy Singleton container" should "be initialized fast when is regular singleton" in {
    val ctx = new ContextConfiguration {
      "component" singleton new CountingObject("container")
      "container" lazySingleton { new Container(get("component")) }
    }
    checkObjectsQuantity(ctx, objectId = "container", expectedAmountBeforeGet = 1,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
  }

  "Component of Singleton container" should "be initialized fast and only once even if is lazy and prototype" in {
    val ctx = new ContextConfiguration {
      "component" prototype { new CountingObject("container") }
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
      "2" prototype (2)
    }
    object Ctx3 extends ContextConfiguration {
      "3" lazySingleton ("3")
    }
    object Ctx4 extends ContextConfiguration {
      "4" singleton "4"
    }
    object Ctx123456 extends ContextConfiguration(Ctx12, Ctx3, Ctx4) {
      "5" prototype (5)
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

  "When context1 extends context2 containing object with identical id, get()" should "return context1 object" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1" singleton "1-CtxParent"
      "2" prototype ("2-CtxParent")
      "3" singleton "3-CtxParent"
    }
    object CtxChild extends ContextConfiguration(extendedContexts = CtxParent) {
      overrides id "1" singleton "1-CtxGranchild$"
      overrides id "2" prototype ("2-CtxGranchild$")
      "4" prototype ("4-CtxGranchild$")
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
    ContextUser.get1 should equal ("1-CtxGranchild$")
    ContextUser.get2 should equal ("2-CtxGranchild$")
    ContextUser.get3 should equal ("3-CtxParent")
    ContextUser.get4 should equal ("4-CtxGranchild$")
  }

  "When ctx extends ctx containing identical ids but with scope differences, get()" should "use child object scopes" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1" singleton "1"
      "2" prototype ("2")
      "3" lazySingleton ("3")
      "4" singleton "4"
      "5" prototype ("5")
      "6" lazySingleton ("6")
    }
    object CtxChild extends ContextConfiguration(CtxParent) {
      overrides id "1" prototype (new CountingObject("1"))
      overrides id "2" singleton new CountingObject("2")
      overrides id "3" singleton new CountingObject("3")
      overrides id "4" lazySingleton (new CountingObject("4"))
      overrides id "5" lazySingleton (new CountingObject("5"))
      overrides id "6" lazySingleton (new CountingObject("6"))
    }
    object CtxGranchild extends ContextConfiguration(CtxChild) {
      ^("6") prototype (new CountingObject("6"))
    }
    //when
    CtxGranchild setAsCurrentContext()
    // then
    checkObjectsQuantity(CtxGranchild, objectId = "1", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 2)
    checkObjectsQuantity(CtxGranchild, objectId = "2", expectedAmountBeforeGet = 1,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
    checkObjectsQuantity(CtxGranchild, objectId = "3", expectedAmountBeforeGet = 1,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
    checkObjectsQuantity(CtxGranchild, objectId = "4", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
    checkObjectsQuantity(CtxGranchild, objectId = "5", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 1)
    checkObjectsQuantity(CtxGranchild, objectId = "6", expectedAmountBeforeGet = 0,
      expectedAmountAfter1stGet = 1, expectedAmountAfter2ndGet = 2)
  }

  "When context have 2 parents with identical id, creating it" should "throw exception" in {
    //having
    object CtxParent_1_1_Singleton extends ContextConfiguration { "1" singleton "1.1" }
    object CtxParent_1_2_Singleton extends ContextConfiguration { "1" singleton "1.2" }
    object CtxParent_2_1_Prototype extends ContextConfiguration { "2" prototype ("2.1") }
    object CtxParent_2_2_Prototype extends ContextConfiguration { "2" prototype ("2.2") }
    object CtxParent_3_1_LazySingleton extends ContextConfiguration { "3" lazySingleton ("3.1") }
    object CtxParent_3_2_LazySingleton extends ContextConfiguration { "3" lazySingleton ("3.2") }
    //when
    object Ctx1Child extends ContextConfiguration(CtxParent_1_1_Singleton, CtxParent_1_2_Singleton)
    object Ctx2Child extends ContextConfiguration(CtxParent_2_1_Prototype, CtxParent_2_2_Prototype)
    object Ctx3Child extends ContextConfiguration(CtxParent_3_1_LazySingleton, CtxParent_3_2_LazySingleton)
    // then
    evaluating { Ctx1Child } should produce [InvalidOverridingException]
    evaluating { Ctx2Child } should produce [InvalidOverridingException]
    evaluating { Ctx3Child } should produce [InvalidOverridingException]
  }

  "When context have 2 parents with identical id and different scopes, creating it" should "throw exception" in {
    //having
    object CtxParent_Singleton extends ContextConfiguration { "1" singleton "1.s" }
    object CtxParent_Prototype extends ContextConfiguration { "1" prototype ("1.p") }
    object CtxParent_LazySingleton extends ContextConfiguration { "1" lazySingleton ("1.ls") }
    //when
    object Ctx_s_p extends ContextConfiguration(CtxParent_Singleton, CtxParent_Prototype)
    object Ctx_s_ls extends ContextConfiguration(CtxParent_Singleton, CtxParent_LazySingleton)
    object Ctx_ls_p extends ContextConfiguration(CtxParent_LazySingleton, CtxParent_Prototype)
    object Ctx_ls_s extends ContextConfiguration(CtxParent_LazySingleton, CtxParent_Singleton)
    object Ctx_p_s extends ContextConfiguration(CtxParent_Prototype, CtxParent_Singleton)
    object Ctx_p_ls extends ContextConfiguration(CtxParent_Prototype, CtxParent_LazySingleton)
    // then
    evaluating { Ctx_s_p } should produce [InvalidOverridingException]
    evaluating { Ctx_s_ls } should produce [InvalidOverridingException]
    evaluating { Ctx_ls_p } should produce [InvalidOverridingException]
    evaluating { Ctx_ls_s } should produce [InvalidOverridingException]
    evaluating { Ctx_p_s } should produce [InvalidOverridingException]
    evaluating { Ctx_p_ls } should produce [InvalidOverridingException]
  }

  "When ctx declares overriding but do not overrides parent's object ID, exception" should "be thrown" in {
    //having
    object CtxParent extends ContextConfiguration {
      "1:parent" singleton "1"
      "2:parent" prototype ("2")
      "3:parent" lazySingleton ("3")
    }
    //when
    object CtxChild1 extends ContextConfiguration(CtxParent) {
      overrides id "1:child" singleton "1"
    }
    object CtxChild2 extends ContextConfiguration(CtxParent) {
      overrides id "2:child" prototype {"2"}
    }
    object CtxChild3 extends ContextConfiguration(CtxParent) {
      overrides id "3:child" lazySingleton {"3"}
    }
    // then
    evaluating { CtxChild1 } should produce [InvalidOverridingException]
    evaluating { CtxChild2 } should produce [InvalidOverridingException]
    evaluating { CtxChild3 } should produce [InvalidOverridingException]
  }

  // ----- helper functions -----

  def checkObjectsQuantity(ctx: ContextConfiguration, objectId: String, expectedAmountBeforeGet: Int,
                           expectedAmountAfter1stGet: Int, expectedAmountAfter2ndGet: Int) {
    //having
    ctx setAsCurrentContext

    //when
    val actualAmountBeforeGet = Counter.getVal(objectId)
    object CtxUser extends Context {
      get(objectId)
      val actualAmountAfter1stGet = Counter.getVal(objectId)
      get(objectId)
      val actualAmountAfter2ndGet = Counter.getVal(objectId)
    }

    //then
    actualAmountBeforeGet should equal (expectedAmountBeforeGet)
    CtxUser.actualAmountAfter1stGet should equal (expectedAmountAfter1stGet)
    CtxUser.actualAmountAfter2ndGet should equal (expectedAmountAfter2ndGet)
  }
}


//  ------ helper classes ------

object Counter {
  val counters = new mutable.HashMap[String, Int]
  def increase(name: String) { counters(name) = counters.getOrElse(name, 0) + 1 }
  def getVal(name: String) = counters.getOrElse(name, 0)
  def reset() { counters.clear }
}

class CountingObject(id: String) {
  Counter.increase(id)
}


class Container(val counter: CountingObject) {
}

