package moe.brianhsu.porting.live2d.framework.effect

import moe.brianhsu.porting.live2d.framework.model.Live2DModel

trait Effect {
  def updateParameters(model: Live2DModel, deltaTimeSeconds: Float): Unit
}