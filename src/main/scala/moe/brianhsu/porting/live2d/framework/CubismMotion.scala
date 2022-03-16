package moe.brianhsu.porting.live2d.framework

import CubismMotion.{EffectNameEyeBlink, EffectNameLipSync}
import moe.brianhsu.live2d.adapter.gateway.avatar.motion.AvatarMotionDataReader
import moe.brianhsu.live2d.enitiy.avatar.effect.{EffectOperation, FallbackParameterValueUpdate, ParameterValueUpdate}
import moe.brianhsu.live2d.enitiy.avatar.motion.{Motion, MotionEvent}
import moe.brianhsu.live2d.enitiy.avatar.motion.data.CurveTarget.{Model, Parameter, PartOpacity}
import moe.brianhsu.live2d.enitiy.avatar.motion.data.{MotionCurve, MotionData}
import moe.brianhsu.live2d.enitiy.avatar.settings.detail.MotionSetting
import moe.brianhsu.live2d.enitiy.math.Easing
import moe.brianhsu.live2d.enitiy.model.Live2DModel

object CubismMotion {
  private val EffectNameEyeBlink = "EyeBlink"
  private val EffectNameLipSync  = "LipSync"

  def apply(motionInfo: MotionSetting, eyeBlinkParameterIds: List[String], lipSyncParameterIds: List[String]): CubismMotion = {
    val cubismMotion = new CubismMotion(
      new AvatarMotionDataReader(motionInfo).loadMotionData(),
      isLoop = false, isLoopFadeIn = false,
      Option(motionInfo.meta.duration).filter(_ > 0.0f),
      motionInfo.fadeInTime.filter(_ >= 0),
      motionInfo.fadeOutTime.filter(_ >= 0).orElse(Some(1.0f))
    )
    cubismMotion.setEffectIds(eyeBlinkParameterIds, lipSyncParameterIds)
    cubismMotion
  }

}

class CubismMotion(motionData: MotionData,
                   override val isLoop: Boolean = false,
                   override val isLoopFadeIn: Boolean = false,
                   override val durationInSeconds: Option[Float],
                   override val fadeInTimeInSeconds: Option[Float],
                   override val fadeOutTimeInSeconds: Option[Float]) extends Motion {
  var _weight: Float = 1.0f
  var _isLoop: Boolean = false                            ///< ループするか?
  var _isLoopFadeIn: Boolean = true                      ///< ループ時にフェードインが有効かどうかのフラグ。初期値では有効。
  var _lastWeight: Float = 0.0f                        ///< 最後に設定された重み

  var _eyeBlinkParameterIds: List[String] = Nil   ///< 自動まばたきを適用するパラメータIDハンドルのリスト。  モデル（モデルセッティング）とパラメータを対応付ける。
  var _lipSyncParameterIds: List[String] = Nil    ///< リップシンクを適用するパラメータIDハンドルのリスト。  モデル（モデルセッティング）とパラメータを対応付ける。

  var _modelCurveIdEyeBlink: String = null               ///< モデルが持つ自動まばたき用パラメータIDのハンドル。  モデルとモーションを対応付ける。
  var _modelCurveIdLipSync: String = null                ///< モデルが持つリップシンク用パラメータIDのハンドル。  モデルとモーションを対応付ける。


  /**
   * 自動エフェクトがかかっているパラメータIDリストの設定
   *
   * 自動エフェクトがかかっているパラメータIDリストを設定する。
   *
   * @param   eyeBlinkParameterIds    自動まばたきがかかっているパラメータIDのリスト
   * @param   lipSyncParameterIds     リップシンクがかかっているパラメータIDのリスト
   */
  def setEffectIds(eyeBlinkParameterIds: List[String], lipSyncParameterIds: List[String]): Unit = {
    this._eyeBlinkParameterIds = eyeBlinkParameterIds
    this._lipSyncParameterIds = lipSyncParameterIds
  }

  override def events: List[MotionEvent] = this.motionData.events.toList

  override def calculateOperations(model: Live2DModel, totalElapsedTimeInSeconds: Float, deltaTimeInSeconds: Float,
                                   weight: Float,
                                   startTimeInSeconds: Float,
                                   fadeInStartTimeInSeconds: Float,
                                   endTimeInSeconds: Option[Float]): List[EffectOperation] = {

    var operations: List[EffectOperation] = Nil

    if (_modelCurveIdEyeBlink == null) {
      _modelCurveIdEyeBlink = EffectNameEyeBlink
    }

    if (_modelCurveIdLipSync == null) {
      _modelCurveIdLipSync = EffectNameLipSync
    }

    val timeOffsetSeconds: Float = totalElapsedTimeInSeconds - startTimeInSeconds
    var lipSyncValue: Float = Float.MaxValue
    var eyeBlinkValue = Float.MaxValue

    //まばたき、リップシンクのうちモーションの適用を検出するためのビット（maxFlagCount個まで
    val MaxTargetSize: Int = 64
    var lipSyncFlags: Int = 0
    var eyeBlinkFlags: Int = 0

    //瞬き、リップシンクのターゲット数が上限を超えている場合
    if (_eyeBlinkParameterIds.size > MaxTargetSize) {
      println(s"too many eye blink targets : ${_eyeBlinkParameterIds.size}")
    }
    if (_lipSyncParameterIds.size > MaxTargetSize) {
      println(s"too many lip sync targets : ${_lipSyncParameterIds.size}")
    }

    val tmpFadeIn: Float = {
      fadeInTimeInSeconds.filter(_ > 0.0f)
        .map(fadeInTime => Easing.sine((totalElapsedTimeInSeconds - startTimeInSeconds) / fadeInTime))
        .getOrElse(1.0f)
    }

    val tmpFadeOut = (for {
      _ <- endTimeInSeconds
      fadeOutTime <- fadeOutTimeInSeconds if fadeOutTime > 0.0f
    } yield {
      Easing.sine((endTimeInSeconds.get - totalElapsedTimeInSeconds) / fadeOutTime)
    }).getOrElse(1.0f)

    var value: Float = 0.0f

    // 'Repeat' time as necessary.
    var time: Float = timeOffsetSeconds

    if (_isLoop) {
      while (time > motionData.duration) {
        time -= motionData.duration
      }
    }
    // Evaluate model curves.
    var c: Int = 0
    val curves = motionData.curves
    while(c < motionData.curveCount && curves(c).targetType == Model) {
      // Evaluate curve and call handler.
      value = evaluateCurve(motionData, curves(c), time)

      if (curves(c).id == _modelCurveIdEyeBlink) {
        eyeBlinkValue = value
      } else if (curves(c).id == _modelCurveIdLipSync) {
        lipSyncValue = value
      }
      c += 1
    }
    var parameterMotionCurveCount = 0

    while(c < motionData.curveCount && curves(c).targetType == Parameter) {
      parameterMotionCurveCount += 1
      val sourceValue: Float = model.parameters(curves(c).id).current

      // Evaluate curve and apply value.
      value = evaluateCurve(motionData, curves(c), time)
      if (eyeBlinkValue != Float.MaxValue) {
        var isBreak: Boolean = false
        for (i <- _eyeBlinkParameterIds.indices if i < MaxTargetSize && !isBreak) {
          if (_eyeBlinkParameterIds(i) == curves(c).id) {
            value *= eyeBlinkValue
            eyeBlinkFlags |= (1 << i)
            isBreak = true
          }
        }
      }
      if (lipSyncValue != Float.MaxValue) {
        var isBreak: Boolean = false
        for (i <- _lipSyncParameterIds.indices if i < MaxTargetSize && !isBreak) {
          if (_lipSyncParameterIds(i) == curves(c).id)
          {
            value += lipSyncValue
            lipSyncFlags |= (1 << i)
            isBreak = true
          }
        }
      }

      var v: Float = 0
      // パラメータごとのフェード
      if (curves(c).fadeInTime < 0.0f && curves(c).fadeOutTime < 0.0f)
      {
        //モーションのフェードを適用
        v = sourceValue + (value - sourceValue) * weight
      } else {
        // パラメータに対してフェードインかフェードアウトが設定してある場合はそちらを適用
        var fin: Float = 0
        var fout: Float = 0
        if (curves(c).fadeInTime < 0.0f) {
          fin = tmpFadeIn
        } else {
          fin = if (curves(c).fadeInTime == 0.0f) {
            1.0f
          } else {
            Easing.sine((totalElapsedTimeInSeconds - fadeInStartTimeInSeconds) / curves(c).fadeInTime)
          }

        }

        if (curves(c).fadeOutTime < 0.0f) {
          fout = tmpFadeOut
        } else {
          fout = if (curves(c).fadeOutTime == 0.0f || endTimeInSeconds.isEmpty) {
            1.0f
          } else {
            Easing.sine((endTimeInSeconds.get - totalElapsedTimeInSeconds) / curves(c).fadeOutTime)
          }
        }
        val paramWeight: Float = _weight * fin * fout

        // パラメータごとのフェードを適用
        v = sourceValue + (value - sourceValue) * paramWeight
      }
      operations ::= FallbackParameterValueUpdate(curves(c).id, v)
      c += 1
    }

    {
      if (eyeBlinkValue != Float.MaxValue) {
        for (i <- _eyeBlinkParameterIds.indices if i < MaxTargetSize) {
          val sourceValue = model.parameters(_eyeBlinkParameterIds(i)).current
          //モーションでの上書きがあった時にはまばたきは適用しない
          if (((eyeBlinkFlags >> i) & 0x01) != 0) {
            //continue;
          } else {

            val v = sourceValue + (eyeBlinkValue - sourceValue) * weight
            model.parameters.get(_eyeBlinkParameterIds(i)).foreach { p =>
              operations ::= ParameterValueUpdate(p.id, v)
            }
          }
        }
      }

      if (lipSyncValue != Float.MaxValue) {
        for (i <- _lipSyncParameterIds.indices if i < MaxTargetSize) {
          val sourceValue = model.parameters(_lipSyncParameterIds(i)).current
          //モーションでの上書きがあった時にはリップシンクは適用しない
          if (((lipSyncFlags >> i) & 0x01) != 0) {
            //continue;
          } else {
            val v = sourceValue + (lipSyncValue - sourceValue) * weight
            model.parameters.get(_lipSyncParameterIds(i)).foreach { p =>
              operations ::= ParameterValueUpdate(p.id, v)
            }
          }
        }
      }
    }

    while (c < motionData.curveCount && curves(c).targetType == PartOpacity) {
      // Evaluate curve and apply value.
      value = evaluateCurve(motionData, curves(c), time)
      operations ::= FallbackParameterValueUpdate(curves(c).id, value)
      c += 1
    }

    _lastWeight = weight
    operations
  }

  private def evaluateCurve(motionData: MotionData, curve: MotionCurve, time: Float): Float = {

    var target: Int = -1
    val totalSegmentCount: Int = curve.baseSegmentIndex + curve.segmentCount
    var pointPosition: Int = 0
    var isBreak: Boolean = false

    for (i <- curve.baseSegmentIndex until totalSegmentCount if !isBreak) {
      // Get first point of next segment.
      pointPosition = motionData.segments(i).basePointIndex + motionData.segments(i).segmentType.pointCount

      // Break if time lies within current segment.
      if (motionData.points(pointPosition).time > time) {
        target = i
        isBreak = true
      }
    }

    if (target == -1) {
      return motionData.points(pointPosition).value
    }

    val segment = motionData.segments(target)
    segment.segmentType.evaluate(motionData.points.drop(segment.basePointIndex), time)
  }

}
