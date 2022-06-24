package moe.brianhsu.live2d.boundary.gateway.renderer

trait DrawCanvasInfoReader {
  def currentCanvasWidth: Int
  def currentCanvasHeight: Int
  def currentSurfaceWidth: Int
  def currentSurfaceHeight: Int

}
