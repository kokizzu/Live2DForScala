package moe.brianhsu.live2d.adapter.gateway.avatar.physics

import moe.brianhsu.live2d.boundary.gateway.avatar.physics.PhysicsReader
import moe.brianhsu.live2d.enitiy.avatar.physics.{CubismPhysicsInput, CubismPhysicsNormalization, CubismPhysicsOutput, CubismPhysicsParameter, CubismPhysicsParticle, CubismPhysicsRig, CubismPhysicsSubRig, TargetType}
import moe.brianhsu.live2d.enitiy.avatar.physics.CubismPhysicsType.{Angle, X, Y}
import moe.brianhsu.live2d.enitiy.avatar.settings.Settings
import moe.brianhsu.live2d.enitiy.avatar.settings.detail.PhysicsSetting
import moe.brianhsu.live2d.enitiy.avatar.settings.detail.PhysicsSetting.{Input, Normalization, Output, Setting, Vertex}
import moe.brianhsu.live2d.enitiy.math.EuclideanVector
import moe.brianhsu.porting.live2d.physics.CubismPhysics.Options
import moe.brianhsu.porting.live2d.physics.{CubismPhysics, GetInputAngleFromNormalizedParameterValue, GetInputTranslationXFromNormalizedParameterValue, GetInputTranslationYFromNormalizedParameterValue, GetOutputAngle, GetOutputScaleAngle, GetOutputScaleTranslationX, GetOutputScaleTranslationY, GetOutputTranslationX, GetOutputTranslationY}

class AvatarPhysicsReader(avatarSettings: Settings) extends PhysicsReader {
  /**
   * Load and create Pose effect
   *
   * @return `None` if there is no physics setting, or `Some[CubismPhysics]` contains
   *         the [[moe.brianhsu.live2d.enitiy.avatar.effect.impl.Pose]] effect.
   */
  override def loadPhysics: Option[CubismPhysics] = {
    avatarSettings.physics.map(x => createCubismPhysics(x))
  }

  private def createCubismPhysics(physicsSetting: PhysicsSetting): CubismPhysics = {
    val cubismRig = createRig(physicsSetting)
    val options: Options = Options(
      EuclideanVector(0.0f, -1.0f),
      EuclideanVector(10.0f, 10.0f)
    )

    new CubismPhysics(cubismRig, options)
  }

  private def createRig(json: PhysicsSetting): CubismPhysicsRig = {

    val settings: List[CubismPhysicsSubRig] = json.physicsSettings.map { setting =>
      createSubRig(
        setting.normalization,
        setting.input.map(createInput),
        setting.output.map(createOutput),
        createParticleList(setting.vertices)
      )
    }

    CubismPhysicsRig(
      settings,
      EuclideanVector(
        json.meta.effectiveForces.gravity.x,
        json.meta.effectiveForces.gravity.y
      ),
      EuclideanVector(
        json.meta.effectiveForces.wind.x,
        json.meta.effectiveForces.wind.y
      )
    )
  }

  private def createParticleList(vertexSettings: List[Vertex]): List[CubismPhysicsParticle] = {
    var previousParticle: Option[CubismPhysicsParticle] = None
    var resultList: List[CubismPhysicsParticle] = Nil

    for (vertexSetting <- vertexSettings) {
      val particle = createParticle(previousParticle, vertexSetting)
      resultList ::= particle
      previousParticle = Some(particle)
    }

    resultList.reverse
  }

  private def createParticle(previousParticle: Option[CubismPhysicsParticle],
    vertexSetting: Vertex): CubismPhysicsParticle = {
    val initialPosition = previousParticle match {
      case None => EuclideanVector(0.0f, 0.0f)
      case Some(particle) =>
        val radius = EuclideanVector(0.0f, vertexSetting.radius)
        particle.initialPosition + radius
    }

    val position = if (previousParticle.isEmpty) {
      EuclideanVector(vertexSetting.position.x, vertexSetting.position.y)
    } else {
      initialPosition
    }

    val lastPosition = initialPosition

    CubismPhysicsParticle(
      vertexSetting.mobility, vertexSetting.delay,
      vertexSetting.acceleration, vertexSetting.radius,
      initialPosition, position, lastPosition,
      EuclideanVector(0.0f, 1.0f),
      EuclideanVector(0.0f, 0.0f)
    )
  }

  private def createOutput(outputSetting: Output): CubismPhysicsOutput = {
    val (outputType, translationFunction, scaleFunction) = outputSetting.`type` match {
      case "X" => (X, GetOutputTranslationX, GetOutputScaleTranslationX)
      case "Y" => (Y, GetOutputTranslationY, GetOutputScaleTranslationY)
      case "Angle" => (Angle, GetOutputAngle, GetOutputScaleAngle)
      case _ => throw new UnsupportedOperationException("Unsupported output type")
    }

    CubismPhysicsOutput(
      CubismPhysicsParameter(
        outputSetting.destination.id,
        TargetType.Parameter
      ),
      outputSetting.vertexIndex,
      outputSetting.scale,
      outputSetting.weight,
      outputType,
      outputSetting.reflect,
      translationFunction,
      scaleFunction
    )
  }

  private def createInput(input: Input): CubismPhysicsInput = {
    val (inputType, normalizeFunction) = input.`type` match {
      case "X" => (X, GetInputTranslationXFromNormalizedParameterValue)
      case "Y" => (Y, GetInputTranslationYFromNormalizedParameterValue)
      case "Angle" => (Angle, GetInputAngleFromNormalizedParameterValue)
      case _ => throw new UnsupportedOperationException("Unsupported input type")
    }

    CubismPhysicsInput(
      CubismPhysicsParameter(input.source.id, TargetType.Parameter),
      inputType,
      input.weight,
      input.reflect,
      normalizeFunction
    )
  }

  private def createSubRig(normalization: Normalization,
                           inputsInSetting: List[CubismPhysicsInput],
                           outputsInSetting: List[CubismPhysicsOutput],
                           particleInSetting: List[CubismPhysicsParticle]): CubismPhysicsSubRig = {

    CubismPhysicsSubRig(
      CubismPhysicsNormalization(
        normalization.position.minimum,
        normalization.position.maximum,
        normalization.position.default
      ),
      CubismPhysicsNormalization(
        normalization.angle.minimum,
        normalization.angle.maximum,
        normalization.angle.default
      ),
      inputsInSetting, outputsInSetting, particleInSetting
    )
  }

}
