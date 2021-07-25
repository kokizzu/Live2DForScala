package moe.brianhsu.porting.live2d.framework.model

import com.sun.jna.ptr.FloatByReference
import moe.brianhsu.live2d.enitiy.core.{CsmVector, CubismCore}
import moe.brianhsu.live2d.enitiy.core.types.{CPointerToMoc, CPointerToModel, ModelAlignment}
import moe.brianhsu.live2d.enitiy.core.memory.MemoryInfo
import moe.brianhsu.porting.live2d.framework
import moe.brianhsu.porting.live2d.framework.MocInfo
import moe.brianhsu.porting.live2d.framework.exception.{DrawableInitException, MocNotRevivedException, ParameterInitException, PartInitException, TextureSizeMismatchException}
import moe.brianhsu.porting.live2d.framework.math.ModelMatrix
import moe.brianhsu.porting.live2d.framework.model.drawable.{ConstantFlags, Drawable, DynamicFlags, VertexInfo}

/**
 * The Live 2D model that represent an .moc file.
 *
 * Library user should NEVER create instance of this class directly, it should be obtain
 * from a ``Cubism`` class.
 *
 * @param mocInfo   The moc file information
 * @param core      The core library of Cubism
 */
class Live2DModel(mocInfo: MocInfo, textureFiles: List[String])(core: CubismCore) {


  private var savedParameters: Map[String, Float] = Map.empty
  private lazy val revivedMoc: CPointerToMoc = reviveMoc()
  private lazy val modelSize: Int =  core.cubismAPI.csmGetSizeofModel(this.revivedMoc)
  private lazy val modelMemoryInfo: MemoryInfo = core.memoryAllocator.allocate(this.modelSize, ModelAlignment)
  protected lazy val cubismModel: CPointerToModel = createCubsimModel()

  lazy val modelMatrix: ModelMatrix = new ModelMatrix(canvasInfo.width, canvasInfo.height)

  def getTextureFileByIndex(index: Int): String = textureFiles(index)

  def isUsingMasking: Boolean = drawables.values.exists(d => d.masks.nonEmpty)

  def getDrawableByIndex(drawableIndex: Int): Drawable = drawablesByIndex(drawableIndex)

  def isUsingMask: Boolean = drawables.values.exists(x => x.masks.nonEmpty)

  private def createCubsimModel(): CPointerToModel = {
    val model = core.cubismAPI.csmInitializeModelInPlace(
      this.revivedMoc,
      this.modelMemoryInfo.alignedMemory,
      this.modelSize
    )

    if (textureFiles.size != getTextureCountFromModel(model)) {
      throw new TextureSizeMismatchException
    }

    model
  }

  private def getTextureCountFromModel(model: CPointerToModel): Int = {
    val drawableCounts = core.cubismAPI.csmGetDrawableCount(model)
    val textureIndexList = core.cubismAPI.csmGetDrawableTextureIndices(model)
    val maxIndex = (0 until drawableCounts).map(i => textureIndexList(i)).max
    maxIndex + 1
  }

  lazy val parameterList: List[Parameter] = createParameterList()

  /**
   * Parameters of this model
   *
   * This is a map that key is the parameterId, and value is corresponding Parameter object.
   *
   * @throws ParameterInitException when could not get valid parameters from Live 2D Cubism Model
   */
  lazy val parameters: Map[String, Parameter] = createParameters()

  lazy val partsList: List[Part] = createPartList()
  var nonExistParamIndexToValue: Map[Int, Float] = Map.empty
  var nonExistParamIdToIndex: Map[String, Int] = Map.empty

  def setParameterValueUsingIndex(index: Int, value: Float): Unit = {

    if (index >= 0 && index < parameters.size) {
      setParameterValue(parameterList(index).id, value)
    } else {
      nonExistParamIndexToValue += (index -> value)
    }
  }
  def getParameterValueUsingIndex(paramIndex: Int): Float = {
    if (nonExistParamIndexToValue.contains(paramIndex)) {
      nonExistParamIndexToValue(paramIndex)
    } else {
      parameterList(paramIndex).current
    }
  }

  def setPartOpacityUsingIndex(index: Int, value: Float): Unit = {
    if (index >= 0 && index < parts.size) {
      partsList(index).setOpacity(value)
    }
  }

  /**
   * Parts of this model
   *
   * This is a map that key is the partId, and value is corresponding Part object.
   *
   * @throws PartInitException when could not get valid parts from Live 2D Cubism Model
   */
  lazy val parts: Map[String, Part] = createParts()

  /**
   * Drawable of this model.
   *
   * This is a map that key is the drawableId, and value is corresponding Drawable object.
   *
   */
  lazy val drawables: Map[String, Drawable] = createDrawable()

  lazy val drawablesByIndex: List[Drawable] = drawables.values.toList.sortBy(_.index)


  /**
   * This method will access all lazy member fields that load data from the CubismCore C Library,
   * and throws exceptions if there is any corrupted data.
   *
   * @return  The model itself.
   * @throws  DrawableInitException if it cannot construct drawable objects.
   * @throws  MocNotRevivedException if there are errors when reading .moc3 file.
   * @throws  ParameterInitException if it cannot construct parameter objects.
   * @throws  PartInitException if it cannot construct part objects.
   * @throws  TextureSizeMismatchException if the the number of provided texture does not match the information in the model.
   */
  def validAllDataFromNativeLibrary: Live2DModel = {
    this.drawables
    this.parameters
    this.parts
    this
  }

  /**
   * Get the drawables that is sorted by render order.
   *
   * This list is sorted by render order in ascending order.
   */
  def sortedDrawables: List[Drawable] = drawables.values.toList.sortBy(_.renderOrder)

  /**
   * Get the canvas info about this Live 2D Model
   *
   * @return  The canvas info
   */
  def canvasInfo: CanvasInfo = {
    val outSizeInPixel = new CsmVector()
    val outOriginInPixel = new CsmVector()
    val outPixelPerUnit = new FloatByReference()

    core.cubismAPI.csmReadCanvasInfo(this.cubismModel, outSizeInPixel, outOriginInPixel, outPixelPerUnit)

    framework.model.CanvasInfo(
      outSizeInPixel.getX, outSizeInPixel.getY,
      (outOriginInPixel.getX, outOriginInPixel.getY), outPixelPerUnit.getValue)
  }

  def saveParameters(): Unit = {

    for (parameter <- parameters.values) {
      savedParameters += (parameter.id -> parameter.current)
    }

  }

  def loadParameters(): Unit = {
    savedParameters.foreach { case (id, value) =>
      parameters.get(id).foreach(_.update(value))
    }
  }

  /**
   * Update the Live 2D Model and reset all dynamic flags of drawables.
   */
  def update(): Unit = {
    core.cubismAPI.csmUpdateModel(this.cubismModel)
    core.cubismAPI.csmResetDrawableDynamicFlags(this.cubismModel)
  }

  def reset(): Unit = {
    parameters.values.foreach { p => p.update(p.default) }
    update()
  }

  def setParameterValue(parameterId: String, value: Float, weight: Float = 1.0f): Unit = {
    parameters.get(parameterId).foreach { p =>
      val valueFitInRange = (value * weight).max(p.min).min(p.max)

      if (weight == 1) {
        p.update(valueFitInRange)
      } else {
        p.update((p.current * (1 - weight)) + (valueFitInRange * weight))
      }
    }
  }

  def addParameterValue(id: String, value: Float, weight: Float = 1.0f): Unit = {
    parameters.get(id).foreach { p =>
      setParameterValue(id, p.current + (value * weight))
    }
  }

  def multiplyParameterValue(id: String, value: Float, weight: Float): Unit = {
    parameters.get(id).foreach { p =>
      setParameterValue(id, p.current * (1.0f + (value - 1.0f) * weight))
    }

  }

  private def reviveMoc(): CPointerToMoc = {
    val revivedMoc = core.cubismAPI.csmReviveMocInPlace(mocInfo.memory.alignedMemory, mocInfo.originalSize)

    if (revivedMoc == null) {
      throw new MocNotRevivedException
    }

    revivedMoc
  }

  private def createPartList(): List[Part] = {
    val partCount = core.cubismAPI.csmGetPartCount(this.cubismModel)
    val partIds = core.cubismAPI.csmGetPartIds(this.cubismModel)
    val parentIndices = core.cubismAPI.csmGetPartParentPartIndices(this.cubismModel)
    val partOpacities = core.cubismAPI.csmGetPartOpacities(this.cubismModel)

    if (partCount == -1 || partIds == null || parentIndices == null || partOpacities == null) {
      throw new PartInitException
    }

    val range = (0 until partCount).toList

    range.map { i =>
      val opacityPointer = partOpacities.getPointerToFloat(i)
      val partId = partIds(i)
      val parentIndex = parentIndices(i)
      val parentId = parentIndex match {
        case n if n >= 0 && n < partCount => Some(partIds(n))
        case _ => None
      }

      val part = framework.model.Part(opacityPointer, this, partId, parentId)

      part
    }
  }

  private def createParts(): Map[String, Part] = {
    partsList.map(p => p.id -> p).toMap
  }

  def getParameterIndex(id: String): Int = {
    if (nonExistParamIdToIndex.contains(id)) {
      return nonExistParamIdToIndex(id)
    }
    parameterList.zipWithIndex
      .find(_._1.id == id)
      .map(_._2)
      .getOrElse {
        val newIndex = parameterList.size - 1 + nonExistParamIndexToValue.size + 1
        nonExistParamIdToIndex += (id -> newIndex)
        newIndex
      }
  }

  def getPartIndex(id: String): Int = {
    partsList.zipWithIndex
      .find(_._1.id == id)
      .map(_._2)
      .getOrElse(-1)
  }

  private def createParameterList(): List[Parameter] = {
    val parametersCount = core.cubismAPI.csmGetParameterCount(this.cubismModel)
    val parametersIds = core.cubismAPI.csmGetParameterIds(this.cubismModel)
    val currentValues = core.cubismAPI.csmGetParameterValues(this.cubismModel)
    val defaultValues = core.cubismAPI.csmGetParameterDefaultValues(this.cubismModel)
    val minValues = core.cubismAPI.csmGetParameterMinimumValues(this.cubismModel)
    val maxValues = core.cubismAPI.csmGetParameterMaximumValues(this.cubismModel)

    if (parametersCount == -1 || parametersIds == null || currentValues == null ||
      defaultValues == null || minValues == null || maxValues == null) {
      throw new ParameterInitException
    }

    val range = (0 until parametersCount).toList

    range.map { i =>
      val id = parametersIds(i)
      val minValue = minValues(i)
      val maxValue = maxValues(i)
      val defaultValue = defaultValues(i)
      val currentValuePointer = currentValues.getPointerToFloat(i)
      val parameter = Parameter(currentValuePointer, this, id, minValue, maxValue, defaultValue)
      parameter
    }

  }

  private def createParameters(): Map[String, Parameter] = {
    parameterList.map(p => p.id -> p).toMap
  }

  private def createDrawable(): Map[String, Drawable] = {
    val drawableCounts = core.cubismAPI.csmGetDrawableCount(this.cubismModel)
    val drawableIdList = core.cubismAPI.csmGetDrawableIds(this.cubismModel)
    val constantFlagsList = core.cubismAPI.csmGetDrawableConstantFlags(this.cubismModel)
    val dynamicFlagsList = core.cubismAPI.csmGetDrawableDynamicFlags(this.cubismModel)
    val textureIndexList = core.cubismAPI.csmGetDrawableTextureIndices(this.cubismModel)
    val drawOrderList = core.cubismAPI.csmGetDrawableDrawOrders(this.cubismModel)
    val renderOrderList = core.cubismAPI.csmGetDrawableRenderOrders(this.cubismModel)
    val opacityList = core.cubismAPI.csmGetDrawableOpacities(this.cubismModel)

    if (drawableCounts == -1 || drawableIdList == null || constantFlagsList == null ||
      dynamicFlagsList == null || textureIndexList == null || drawOrderList == null ||
      renderOrderList == null || opacityList == null) {
      throw new DrawableInitException
    }

    // Mask Related
    val maskCountList = core.cubismAPI.csmGetDrawableMaskCounts(this.cubismModel)
    val masksList = core.cubismAPI.csmGetDrawableMasks(this.cubismModel)

    if (maskCountList == null || masksList == null) {
      throw new DrawableInitException
    }

    // Vertex Related
    val indexCountList = core.cubismAPI.csmGetDrawableIndexCounts(this.cubismModel)
    val indexList = core.cubismAPI.csmGetDrawableIndices(this.cubismModel)
    val vertexCountList = core.cubismAPI.csmGetDrawableVertexCounts(this.cubismModel)
    val positionList =  core.cubismAPI.csmGetDrawableVertexPositions(this.cubismModel)
    val textureCoordinateList =  core.cubismAPI.csmGetDrawableVertexUvs(this.cubismModel)

    if (indexCountList == null || indexList == null || vertexCountList == null ||
        positionList == null || textureCoordinateList == null) {
      throw new DrawableInitException
    }


    val range = (0 until drawableCounts).toList

    range.map { i =>
      val drawableId = drawableIdList(i)
      val constantFlags = ConstantFlags(constantFlagsList(i))
      val dynamicFlags = DynamicFlags(dynamicFlagsList.getPointerToByte(i))
      val textureIndex = textureIndexList(i)
      val drawOrderPointer = drawOrderList.getPointerToInt(i)
      val renderOrderPointer = renderOrderList.getPointerToInt(i)
      val opacityPointer = opacityList.getPointerToFloat(i)
      val maskCount = maskCountList(i)
      val masks = (0 until maskCount).toList.map(j => masksList(i)(j))
      val vertexInfo = VertexInfo(
        vertexCountList(i),
        indexCountList(i),
        positionList(i),
        textureCoordinateList(i),
        indexList(i)
      )

      val drawable = Drawable(
        this, drawableId, i, constantFlags, dynamicFlags, textureIndex, masks,
        vertexInfo, drawOrderPointer, renderOrderPointer, opacityPointer
      )

      drawableId -> drawable
    }.toMap
  }

  def isHit(drawableId: String, pointX: Float, pointY: Float): Boolean = {
    val isHitHolder = drawables.get(drawableId).map { drawable =>
      val vertices = drawable.vertexInfo.positions

      var left: Float = vertices(0)._1
      var right: Float = vertices(0)._1
      var top = vertices(0)._2
      var bottom = vertices(0)._2

      for (vertex <- vertices.drop(1)) {
        val (x, y) = vertex
        if (x < left) {
          left = x; // Min x
        }

        if (x > right) {
          right = x; // Max x
        }

        if (y < top) {
          top = y; // Min y
        }

        if (y > bottom) {
          bottom = y; // Max y
        }
      }
      val tx = modelMatrix.invertTransformX(pointX)
      val ty = modelMatrix.invertTransformY(pointY)

      (left <= tx) &&
        (tx <= right) &&
        (top <= ty) &&
        (ty <= bottom)
    }
    isHitHolder.getOrElse(false)
  }
}