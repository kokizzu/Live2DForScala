package moe.brianhsu.porting.live2d.framework.math

import moe.brianhsu.porting.live2d.framework.math

class ViewPortMatrixCalculator {

  private val ViewScale = 1.0f
  private val ViewMaxScale = 2.0f
  private val ViewMinScale = 0.8f
  private val ViewLogicalLeft = -1.0f
  private val ViewLogicalRight = 1.0f
  private val ViewLogicalMaxLeft = -2.0f
  private val ViewLogicalMaxRight = 2.0f
  private val ViewLogicalMaxTop = -2.0f
  private val ViewLogicalMaxBottom = 2.0f

  private var deviceToScreen: Matrix4x4 = new Matrix4x4
  private var viewMatrix: ViewMatrix = new ViewMatrix(Rectangle(), Rectangle(), 0, 0)
  private var surfaceWidth: Int = 0
  private var surfaceHeight: Int = 0

  def getDeviceToScreen: Matrix4x4 = deviceToScreen
  def getViewMatrix: Matrix4x4 = viewMatrix

  def updateViewPort(width: Int, height: Int): Unit = {
    this.surfaceWidth = width
    this.surfaceHeight = height

    if ((surfaceWidth != 0) && (surfaceHeight != 0)) {
      val ratio = surfaceWidth.toFloat / surfaceHeight.toFloat
      val left = -ratio
      val right = ratio
      val top = ViewLogicalLeft
      val bottom = ViewLogicalRight

      updateDeviceToScreen(left, right, top, bottom)
      updateViewMatrix(left, right, top, bottom)
    }

  }

  private def updateViewMatrix(left: Float, right: Float, top: Float, bottom: Float): Unit = {
    this.viewMatrix = new ViewMatrix(
      Rectangle(left, right, right - left, bottom - top),
      Rectangle(
        ViewLogicalMaxLeft, ViewLogicalMaxRight,
        ViewLogicalMaxRight - ViewLogicalMaxLeft,
        ViewLogicalMaxBottom - ViewLogicalMaxTop
      ),
      ViewMaxScale, ViewMinScale
    ).scale(ViewScale, ViewScale).asInstanceOf[ViewMatrix]
  }

  def updateDeviceToScreen(left: Float, right: Float, top: Float, bottom: Float): Unit = {
    val (scaleRelativeX, scaleRelativeY) = if (surfaceWidth > surfaceHeight) {
      val screenW: Float = (right - left).abs
      (screenW / surfaceWidth, -screenW / surfaceWidth)
    } else {
      val screenH: Float = (bottom - top).abs
      (screenH / surfaceHeight, -screenH / surfaceHeight)
    }

    deviceToScreen =
      (new Matrix4x4)
        .scaleRelative(scaleRelativeX, scaleRelativeY)
        .translateRelative(-surfaceWidth * 0.5f, -surfaceHeight * 0.5f)
  }

}
