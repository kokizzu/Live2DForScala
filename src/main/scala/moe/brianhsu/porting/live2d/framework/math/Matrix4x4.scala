package moe.brianhsu.porting.live2d.framework.math

object Matrix4x4 {
  val NumberOfElements: Int = 4 * 4

  def staticMultiply(a: Array[Float], b: Array[Float], dst: Array[Float]): Unit = {
    val c = Array[Float](
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f
    )

    val n = 4

    for (i <- 0 until n) {
      for (j <- 0 until n) {
        for (k <- 0 until n) {
          c(j + i * 4) += a(k + i * 4) * b(j + k * 4)
        }
      }
    }

    for (i <- 0 until NumberOfElements) {
      dst(i) = c(i)
    }

  }
}
class Matrix4x4 {

  import Matrix4x4.NumberOfElements

  protected val tr: Array[Float] = createIdentity()

  private def createIdentity(): Array[Float] = Array[Float](
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f
  )

  def matrixArray: Array[Float] = tr

  def translateRelative(x: Float, y: Float): Matrix4x4 = {
    val tr1 = Array[Float](
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,
      x,    y,    0.0f, 1.0f
    )

    multiply(tr1, this.tr, this.tr)
    this
  }

  def translate(x: Float, y: Float): Matrix4x4 = {
    this.tr(12) = x
    this.tr(13) = y
    this
  }

  def scaleRelative(x: Float, y: Float): Matrix4x4 = {
    val tr1 = Array(
      x,      0.0f,   0.0f, 0.0f,
      0.0f,   y,      0.0f, 0.0f,
      0.0f,   0.0f,   1.0f, 0.0f,
      0.0f,   0.0f,   0.0f, 1.0f
    )

    multiply(tr1, this.tr, this.tr)
    this
  }

  def scale(x: Float, y: Float): Matrix4x4 = {
    this.tr(0) = x
    this.tr(5) = y
    this
  }

  def transformX(src: Float): Float = this.tr(0) * src + this.tr(12)

  def invertTransformX(src: Float): Float = (src - this.tr(12)) / this.tr(0)

  def transformY(src: Float): Float = this.tr(5) * src + this.tr(13)

  def invertTransformY(src: Float): Float = (src - this.tr(13)) / this.tr(5)

  def setMatrix(tr: Array[Float]): Matrix4x4 = {
    for (i <- 0 until NumberOfElements) {
      this.tr(i) = tr(i)
    }
    this
  }

  def scaleX: Float = this.tr(0)
  def scaleY: Float = this.tr(5)
  def translateX: Float = this.tr(12)
  def translateY: Float = this.tr(13)

  def translateX(x: Float): Matrix4x4 = {
    this.tr(12) = x
    this
  }

  def translateY(y: Float): Matrix4x4 = {
    this.tr(13) = y
    this
  }

  def multiplyByMatrix(matrix4x4: Matrix4x4): Matrix4x4 = {
    multiply(matrix4x4.matrixArray, this.tr, this.tr)
    this
  }

  def multiply(a: Array[Float], b: Array[Float], dst: Array[Float]): Unit = {
    val c = Array[Float](
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 0.0f
    )

    val n = 4

    for (i <- 0 until n) {
      for (j <- 0 until n) {
        for (k <- 0 until n) {
          c(j + i * 4) += a(k + i * 4) * b(j + k * 4)
        }
      }
    }

    for (i <- 0 until NumberOfElements) {
      dst(i) = c(i)
    }

  }

}
