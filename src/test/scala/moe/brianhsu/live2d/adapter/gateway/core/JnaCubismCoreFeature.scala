package moe.brianhsu.live2d.adapter.gateway.core

import moe.brianhsu.live2d.enitiy.core.types.CsmLogFunction
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers

class JnaCubismCoreFeature extends AnyFeatureSpec with GivenWhenThen with Matchers {
  Feature("Init with custom logger") {
    Scenario("create cubism with custom logger") {
      Given("A custom logger")
      val logger = new CsmLogFunction {
        override def invoke(message: String): Unit = {}
      }

      When("create a Cubism object with that logger")
      val cubismCore = new JnaCubismCore(logger)

      Then("the logger of cLibrary should be the custom logger")
      cubismCore.cubismAPI.csmGetLogFunction() shouldBe logger
    }
  }
}