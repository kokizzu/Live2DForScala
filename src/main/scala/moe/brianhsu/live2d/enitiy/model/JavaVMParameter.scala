package moe.brianhsu.live2d.enitiy.model

/**
 * @param id        The parameter id.
 * @param min       The minimum value of this parameter.
 * @param max       The maximum value of this parameter.
 * @param default   The default value of this parameter.
 */
class JavaVMParameter(
  override val id: String,
  override val min: Float = Float.MinValue,
  override val max: Float = Float.MaxValue,
  override val default: Float = 0,
  private var value: Float = 0) extends Parameter {

  /**
   * Get the current value of this parameter.
   *
   * @return The current value of this parameter.
   */
  override def current: Float = value

  /**
   * Update this parameter to a new value.
   *
   * @param value The new value to assign.
   */
  override def doUpdateValue(value: Float): Unit = {
    this.value = value
  }
}
