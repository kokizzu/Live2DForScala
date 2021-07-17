package moe.brianhsu.live2d.renderer.opengl

import moe.brianhsu.live2d.adapter.OpenGL
import moe.brianhsu.live2d.framework.math.Matrix4x4
import moe.brianhsu.live2d.framework.model.drawable.ConstantFlags.BlendMode
import moe.brianhsu.live2d.framework.model.drawable.VertexInfo
import moe.brianhsu.live2d.framework.model.{Avatar, Live2DModel}
import moe.brianhsu.live2d.renderer.opengl.clipping.{ClippingContext, ClippingManager}
import moe.brianhsu.live2d.renderer.opengl.shader.ShaderRenderer

class Renderer(model: Live2DModel)(implicit gl: OpenGL) {

  import gl._

  private var projection: Option[Matrix4x4] = None
  private val textureManager = new TextureManager
  private val shaderRenderer = new ShaderRenderer
  private val profile = new Profile()
  private var isCulling: Boolean = false
  private var clippingContextBufferForMask: Option[ClippingContext] = None
  private var clippingContextBufferForDraw: Option[ClippingContext] = None
  private val clippingManagerHolder: Option[ClippingManager] = model.isUsingMasking match {
    case true => Some(new ClippingManager(model, textureManager))
    case false => None
  }

  private[renderer] val offscreenBufferHolder: Option[OffscreenFrame] = clippingManagerHolder.map(manager => new OffscreenFrame(manager.clippingMaskBufferSize, manager.clippingMaskBufferSize))

  def getProjection: Option[Matrix4x4] = projection
  def getClippingContextBufferForDraw: Option[ClippingContext] = clippingContextBufferForDraw
  def getClippingContextBufferForMask: Option[ClippingContext] = clippingContextBufferForMask
  def setClippingContextBufferForMask(clip: Option[ClippingContext]): Unit = {
    clippingContextBufferForMask = clip
  }

  def setClippingContextBufferForDraw(context: Option[ClippingContext]): Unit = {
    clippingContextBufferForDraw = context
  }

  def setIsCulling(isCulling: Boolean): Unit = {
    this.isCulling = isCulling
  }

  def setProjection(projection: Matrix4x4): Unit = {
    this.projection = Some(projection)
  }

  def draw(avatar: Avatar, projection: Matrix4x4): Unit = {
      for(model <- avatar.modelHolder) {
        projection.multiplyByMatrix(model.modelMatrix)
        this.setProjection(projection)

        this.profile.save()
        this.drawModel()
        this.profile.restore()
      }
  }

  def preDraw(): Unit = {
    gl.glDisable(GL_SCISSOR_TEST)
    gl.glDisable(GL_STENCIL_TEST)
    gl.glDisable(GL_DEPTH_TEST)
    gl.glEnable(GL_BLEND)
    gl.glColorMask(red = true, green = true, blue = true, alpha = true)
    gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    gl.glBindBuffer(GL_ARRAY_BUFFER, 0) //前にバッファがバインドされていたら破棄する必要がある
  }

  def postDraw(): Unit = {}

  def drawMesh(drawTextureId: Int, vertexInfo: VertexInfo,
               opacity: Float, colorBlendMode: BlendMode, invertedMask: Boolean): Unit ={

    isCulling match {
      case true  => gl.glEnable(GL_CULL_FACE)
      case false => gl.glDisable(GL_CULL_FACE)
    }

    gl.glFrontFace(GL_CCW)

    val modelColorRGBA = getClippingContextBufferForMask match {
      case None => TextureColor(1.0f, 1.0f, 1.0f, opacity)
      case _    => TextureColor()
    }

    shaderRenderer.render(
      this, drawTextureId,
      vertexInfo.getVertexArrayDirectBuffer,
      vertexInfo.getUvArrayDirectBuffer,
      colorBlendMode,
      modelColorRGBA,
      projection.getOrElse(new Matrix4x4),
      invertedMask
    )

    gl.glDrawElements(GL_TRIANGLES, vertexInfo.numberOfTriangleIndex, GL_UNSIGNED_SHORT, vertexInfo.getIndexArrayDirectBuffer)

    gl.glUseProgram(0)
    setClippingContextBufferForDraw(None)
    setClippingContextBufferForMask(None)
  }

  def drawModel(): Unit = {
    clippingManagerHolder.foreach { manager =>
      preDraw()
      manager.setupClippingContext(this, profile.getLastFBO, profile.gatLastViewPort)
    }

    preDraw()

    val sortedDrawable = model.sortedDrawables
    for (drawable <- sortedDrawable.filter(_.dynamicFlags.isVisible)) {

      val clipContext: Option[ClippingContext] = clippingManagerHolder.flatMap(_.getClippingContextByDrawable(drawable))
      setClippingContextBufferForDraw(clipContext)

      setIsCulling(drawable.isCulling)
      val textureFile = model.getTextureFileByIndex(drawable.textureIndex)
      val textureInfo = textureManager.loadTexture(textureFile)

      drawMesh(
        textureInfo.textureId,
        drawable.vertexInfo,
        drawable.opacity,
        drawable.constantFlags.blendMode,
        drawable.constantFlags.isInvertedMask
      )
    }
    postDraw()
  }

}