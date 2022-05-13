package moe.brianhsu.live2d.adapter.gateway.renderer.swt

import moe.brianhsu.live2d.boundary.gateway.renderer.DrawCanvasInfoReader
import org.eclipse.swt.opengl.GLCanvas

class SWTOpenGLCanvasInfoReader(canvas: GLCanvas) extends DrawCanvasInfoReader {
  override def currentCanvasWidth: Int = canvas.getBounds.width
  override def currentCanvasHeight: Int = canvas.getBounds.height
}
