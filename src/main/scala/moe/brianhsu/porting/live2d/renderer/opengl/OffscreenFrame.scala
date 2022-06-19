package moe.brianhsu.porting.live2d.renderer.opengl

import moe.brianhsu.live2d.enitiy.opengl.{OpenGLBinding, RichOpenGLBinding}

object OffscreenFrame {

  private var offscreenFrame: Map[OpenGLBinding, OffscreenFrame] = Map.empty
  private implicit val converter: OpenGLBinding => RichOpenGLBinding = RichOpenGLBinding.wrapOpenGLBinding

  def getInstance(displayBufferWidth: Int, displayBufferHeight: Int)(implicit gl: OpenGLBinding): OffscreenFrame = {
    offscreenFrame.get(gl) match {
      case Some(offscreenFrame) => offscreenFrame
      case None =>
        offscreenFrame += (gl -> new OffscreenFrame(displayBufferWidth, displayBufferHeight))
        offscreenFrame(gl)
    }
  }
}

class OffscreenFrame(displayBufferWidth: Int, displayBufferHeight: Int)
                    (implicit gl: OpenGLBinding, wrapper: OpenGLBinding => RichOpenGLBinding) {

  import gl.constants._

  private var originalFrameBufferBinding: Int = 0

  val (textureBufferId, colorBufferId) = createTextureAndColorBuffer()

  def createTextureAndColorBuffer(): (Int, Int) = {
    val textureBufferId = gl.generateFrameBuffers(10).head
    val colorBufferId = gl.generateTextures(10).head

    gl.glBindTexture(GL_TEXTURE_2D, colorBufferId)
    gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, displayBufferWidth, displayBufferHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
    gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    gl.glBindTexture(GL_TEXTURE_2D, 0)

    val originalFrameBuffer = gl.openGLParameters[Int](GL_FRAMEBUFFER_BINDING)
    gl.glBindFramebuffer(GL_FRAMEBUFFER, textureBufferId)
    gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBufferId, 0)
    gl.glBindFramebuffer(GL_FRAMEBUFFER, originalFrameBuffer)

    (textureBufferId, colorBufferId)
  }

  def beginDraw(restoreFBO: Int): Unit = {
    originalFrameBufferBinding = restoreFBO
    gl.glBindFramebuffer(GL_FRAMEBUFFER, textureBufferId)
    gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
    gl.glClear(GL_COLOR_BUFFER_BIT)
  }

  def endDraw(): Unit = {
    gl.glBindFramebuffer(GL_FRAMEBUFFER, originalFrameBufferBinding)
  }

  def destroy(): Unit = {
    gl.glDeleteTextures(1, Array(colorBufferId))
    gl.glDeleteFramebuffers(1, Array(textureBufferId))

  }
}
