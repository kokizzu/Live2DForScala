package moe.brianhsu.porting.live2d.demo.sprite

import moe.brianhsu.porting.live2d.renderer.opengl.TextureManager.TextureInfo
import moe.brianhsu.porting.live2d.adapter.{DrawCanvasInfo, OpenGL}

class PowerSprite(drawCanvasInfo: DrawCanvasInfo, textureInfo: TextureInfo, shader: SpriteShader)
                 (implicit private val gl: OpenGL) extends LAppSprite(drawCanvasInfo, textureInfo, shader) {

  override protected def calculatePosition(): Position = {
    val windowWidth = drawCanvasInfo.currentSurfaceWidth
    Position(
      windowWidth - textureInfo.width * 0.5f,
      textureInfo.height * 0.5f,
      textureInfo.width.toFloat,
      textureInfo.height.toFloat
    )
  }
}