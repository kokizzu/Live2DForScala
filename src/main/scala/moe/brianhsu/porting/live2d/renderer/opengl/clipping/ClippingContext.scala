package moe.brianhsu.porting.live2d.renderer.opengl.clipping

import ClippingContext.channelColors
import moe.brianhsu.live2d.enitiy.math.Rectangle
import moe.brianhsu.live2d.enitiy.math.matrix.GeneralMatrix
import moe.brianhsu.live2d.enitiy.model.drawable.Drawable
import moe.brianhsu.live2d.usecase.renderer.opengl.texture.TextureColor

object ClippingContext {
  private val channelColors = Array(
    TextureColor(1.0f, 0.0f, 0.0f, 0.0f),
    TextureColor(0.0f, 1.0f, 0.0f, 0.0f),
    TextureColor(0.0f, 0.0f, 1.0f, 0.0f),
    TextureColor(0.0f, 0.0f, 0.0f),
  )
}

class ClippingContext(val maskDrawable: List[Drawable], val clippedDrawables: List[Drawable]) {

  private var isUsing: Boolean = false                                ///< 現在の描画状態でマスクの準備が必要ならtrue
  private var layoutChannelNo: Int = 0                       ///< RGBAのいずれのチャンネルにこのクリップを配置するか(0:R , 1:G , 2:B , 3:A)
  private var layoutBounds: Rectangle = Rectangle()                         ///< マスク用チャンネルのどの領域にマスクを入れるか(View座標-1..1, UVは0..1に直す)
  private var allClippedDrawRect: Rectangle = Rectangle()                   ///< このクリッピングで、クリッピングされる全ての描画オブジェクトの囲み矩形（毎回更新）
  private var matrixForMask: GeneralMatrix = new GeneralMatrix ///< マスクの位置計算結果を保持する行列
  private var matrixForDraw: GeneralMatrix = new GeneralMatrix ///< 描画オブジェクトの位置計算結果を保持する行列

  def getMatrixForMask: GeneralMatrix = matrixForMask

  def getMatrixForDraw: GeneralMatrix = matrixForDraw

  def getAllClippedDrawRect: Rectangle = allClippedDrawRect

  def getIsUsing: Boolean = this.isUsing

  def getChannelColor: TextureColor = channelColors(layoutChannelNo)

  def setLayout(channelNo: Int, bounds: Rectangle): Unit = {
    this.layoutChannelNo = channelNo
    this.layoutBounds = bounds
  }

  def getLayoutBounds: Rectangle = layoutBounds

  def calcMatrix(): Unit = {
    val allClippedDrawRect = this.getAllClippedDrawRect
    val layoutBoundsOnTex01 = this.getLayoutBounds

    val margin = 0.05f
    val tmpBoundsOnModel = allClippedDrawRect.expand(
      allClippedDrawRect.width * margin,
      allClippedDrawRect.height * margin
    )

    val scaleX = layoutBoundsOnTex01.width / tmpBoundsOnModel.width
    val scaleY = layoutBoundsOnTex01.height / tmpBoundsOnModel.height

    matrixForMask = calcMaskMatrix(layoutBoundsOnTex01, tmpBoundsOnModel, scaleX, scaleY)
    matrixForDraw = calcDrawMatrix(layoutBoundsOnTex01, tmpBoundsOnModel, scaleX, scaleY)
  }

  private def calcDrawMatrix(layoutBoundsOnTex01: Rectangle, tmpBoundsOnModel: Rectangle,
                             scaleX: Float, scaleY: Float): GeneralMatrix = {
    new GeneralMatrix()
      .translateRelative(layoutBoundsOnTex01.leftX, layoutBoundsOnTex01.bottomY)
      .scaleRelative(scaleX, scaleY)
      .translateRelative(-tmpBoundsOnModel.leftX, -tmpBoundsOnModel.bottomY)
  }

  private def calcMaskMatrix(layoutBoundsOnTex01: Rectangle, tmpBoundsOnModel: Rectangle,
                             scaleX: Float, scaleY: Float): GeneralMatrix = {

    new GeneralMatrix()
      .translateRelative(-1.0f, -1.0f)
      .scaleRelative(2.0f, 2.0f)
      .translateRelative(layoutBoundsOnTex01.leftX, layoutBoundsOnTex01.bottomY)
      .scaleRelative(scaleX, scaleY)
      .translateRelative(-tmpBoundsOnModel.leftX, -tmpBoundsOnModel.bottomY)
  }

  def calcClippedDrawTotalBounds(): Unit = {
    val FLT_MAX = Float.MaxValue
    val FLT_MIN = 0.0f

    val positions = clippedDrawables.flatMap(_.vertexInfo.positions)
    val xPositions = positions.map(_._1)
    val yPositions = positions.map(_._2)

    val clippedDrawTotalMinX = xPositions.minOption.getOrElse(FLT_MAX)
    val clippedDrawTotalMinY = yPositions.minOption.getOrElse(FLT_MAX)
    val clippedDrawTotalMaxX = xPositions.filter(_ > 0).maxOption.getOrElse(FLT_MIN)
    val clippedDrawTotalMaxY = yPositions.filter(_ > 0).maxOption.getOrElse(FLT_MIN)

    if (clippedDrawTotalMinX == FLT_MAX) {
      this.isUsing = false
      this.allClippedDrawRect = Rectangle()
    } else {
      this.isUsing = true
      this.allClippedDrawRect = Rectangle(
        clippedDrawTotalMinX,
        clippedDrawTotalMinY,
        width = clippedDrawTotalMaxX - clippedDrawTotalMinX,
        height = clippedDrawTotalMaxY - clippedDrawTotalMinY
      )
    }
  }

}
