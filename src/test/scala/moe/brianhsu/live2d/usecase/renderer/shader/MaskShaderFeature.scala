package moe.brianhsu.live2d.usecase.renderer.shader

import moe.brianhsu.live2d.enitiy.opengl.OpenGLBinding
import org.scalamock.scalatest.MockFactory
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers

class MaskShaderFeature extends AnyFeatureSpec with Matchers with GivenWhenThen with MockFactory {

  Feature("Source code of Shader") {
    Scenario("Has main method in vertex shader") {
      Given("A implicit stubbed OpenGL binding")
      implicit val openGLBinding: OpenGLBinding = stub[OpenGLBinding]

      When("Create a MaskedShader")
      val shader = new MaskedShader()

      Then("The vertex source code has main method")
      shader.vertexShaderSource should include ("void main() {")
    }

    Scenario("Has main method in fragment shader") {
      Given("A implicit stubbed OpenGL binding")
      implicit val openGLBinding: OpenGLBinding = stub[OpenGLBinding]

      When("Create a MaskedShader")
      val shader = new MaskedShader()

      Then("The fragment source code has main method")
      shader.fragmentShaderSource should include ("void main() {")
    }

  }

}
