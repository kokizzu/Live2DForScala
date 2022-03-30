package moe.brianhsu.porting

import moe.brianhsu.live2d.adapter.gateway.avatar.settings.json.JsonSettingsReader
import moe.brianhsu.live2d.enitiy.avatar.effect._
import moe.brianhsu.live2d.enitiy.avatar.settings.Settings
import moe.brianhsu.live2d.enitiy.math.EuclideanVector
import moe.brianhsu.live2d.enitiy.model.{JavaVMParameter, Live2DModel}
import moe.brianhsu.porting.live2d.framework.math.MutableData
import moe.brianhsu.porting.live2d.physics.{CubismPhysics, CubismPhysicsParticle, NormalizedPhysicsParameterValueGetter}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.scalamock.scalatest.MockFactory
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{GivenWhenThen, OptionValues, TryValues}

import scala.io.Source
import scala.util.{Random, Using}

class PhysicsFeature extends AnyFeatureSpec with GivenWhenThen with Matchers with TryValues
  with MockFactory with OptionValues with TableDrivenPropertyChecks {
  private implicit val formats: Formats = Serialization.formats(ShortTypeHints(
    List(
      classOf[ParameterValueAdd],
      classOf[ParameterValueMultiply],
      classOf[ParameterValueUpdate],
      classOf[FallbackParameterValueAdd],
      classOf[FallbackParameterValueUpdate],
      classOf[PartOpacityUpdate],
    )
  ))

  Feature("Read pose parts data from Live2D avatar settings") {
    Scenario("s1") {
      Given("A folder path contains json files for Rice Live2D avatar model")
      val folderPath = "src/test/resources/models/Rice"

      When("Create a Physics effect from this Live2D avatar settings")
      val jsonSettingsReader = new JsonSettingsReader(folderPath)
      val settings: Settings = jsonSettingsReader.loadSettings().success.value
      val physicsSetting = settings.physics.value
      val physics = CubismPhysics.create(physicsSetting)

      val testDataFile = Source.fromFile("src/test/resources/expectation/physicsOperations.json")
      val dataPointList = Using.resource(testDataFile) { _.getLines().toList.map(parseLog) }

      dataPointList.foreach { dataPoint =>
        val model = createStubbedModel(dataPoint)
        val operations = physics.evaluate(model, dataPoint.totalElapsedTimeInSeconds, dataPoint.deltaTimeSeconds)
        operations.size shouldBe dataPoint.operations.size
        operations should contain theSameElementsInOrderAs dataPoint.operations
      }
    }
    Scenario("s2") {
      Given("A folder path contains json files for Rice Live2D avatar model")
      val folderPath = "src/test/resources/models/Hiyori"

      When("Create a Physics effect from this Live2D avatar settings")
      val jsonSettingsReader = new JsonSettingsReader(folderPath)
      val settings: Settings = jsonSettingsReader.loadSettings().success.value
      val physicsSetting = settings.physics.value
      val physics = CubismPhysics.create(physicsSetting)

      val testDataFile = Source.fromFile("src/test/resources/expectation/HiyoriPhysic.json")
      val dataPointList = Using.resource(testDataFile) { _.getLines().toList.map(parseLog) }

      dataPointList.foreach { dataPoint =>
        val model = createStubbedModel(dataPoint)
        val operations = physics.evaluate(model, dataPoint.totalElapsedTimeInSeconds, dataPoint.deltaTimeSeconds)
        operations.size shouldBe dataPoint.operations.size
        operations should contain theSameElementsInOrderAs dataPoint.operations
      }
    }

  }

  Feature("UpdateParticles") {
    Scenario("delay is not 0") {
      val strand1 = CubismPhysicsParticle(
        mobility = 1,
        delay = 1,
        acceleration = 1,
        radius = 0,
        initialPosition = EuclideanVector(0.0f),
        position = EuclideanVector(0, 0),
        lastPosition = EuclideanVector(0.0f, 0.0f),
        lastGravity = EuclideanVector(0.0f, 0.0f),
        force = EuclideanVector(0.0f, 0.0f),
        velocity = EuclideanVector(0.0f, 0.0f),
      )

      val strand2 = CubismPhysicsParticle(
        mobility = 0.95f,
        delay = 0.8f,
        acceleration = 1.5f,
        radius = 15,
        initialPosition = EuclideanVector(0.0f),
        position = EuclideanVector(1f, 15),
        lastPosition = EuclideanVector(0.0f, 0.0f),
        lastGravity = EuclideanVector(0.0f, 0.0f),
        force = EuclideanVector(0.0f, 0.0f),
        velocity = EuclideanVector(0.0f, 0.0f),
      )

      val strands = Array(strand1, strand2)

      CubismPhysics.updateParticles(
        strands, strands.size,
        EuclideanVector(0, 0),
        MutableData(0.0f),
        EuclideanVector(0, 0),
        2f,
        0.333f,
        0.1f
      )

      strand1.initialPosition shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.mobility shouldBe 1.0f
      strand1.delay shouldBe 1.0f
      strand1.acceleration shouldBe 1.0f
      strand1.radius shouldBe 0.0f
      strand1.position shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.lastPosition shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.lastGravity shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.force shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.velocity shouldBe EuclideanVector(0.0f, 0.0f)

      strand2.initialPosition shouldBe EuclideanVector(0.0f, 0.0f)
      strand2.mobility shouldBe 0.95f
      strand2.delay shouldBe 0.8f
      strand2.acceleration shouldBe 1.5f
      strand2.radius shouldBe 15.0f
      strand2.position shouldBe EuclideanVector(0.0f, 14.998851f)
      strand2.lastPosition shouldBe EuclideanVector(1.0f, 15.0f)
      strand2.lastGravity shouldBe EuclideanVector(0.0f, 1.0f)
      strand2.force shouldBe EuclideanVector(0.0f, 0.0f)
      strand2.velocity shouldBe EuclideanVector(-0.118868865f, -1.3660143E-4f)
    }

    Scenario("delay is 0") {
      val strand1 = CubismPhysicsParticle(
        mobility = 1,
        delay = 1,
        acceleration = 1,
        radius = 0,
        initialPosition = EuclideanVector(0.0f),
        position = EuclideanVector(0, 0),
        lastPosition = EuclideanVector(0.0f, 0.0f),
        lastGravity = EuclideanVector(0.0f, 0.0f),
        force = EuclideanVector(0.0f, 0.0f),
        velocity = EuclideanVector(0.0f, 0.0f),
      )

      val strand2 = new CubismPhysicsParticle(
        mobility = 0.95f,
        delay = 0f,
        acceleration = 1.5f,
        radius = 15,
        initialPosition = EuclideanVector(0.0f),
        position = EuclideanVector(1f, 15),
        lastPosition = EuclideanVector(0.0f, 0.0f),
        lastGravity = EuclideanVector(0.0f, 0.0f),
        force = EuclideanVector(0.0f, 0.0f),
        velocity = EuclideanVector(0.0f, 0.0f),
      )

      val strands = Array(strand1, strand2)

      CubismPhysics.updateParticles(
        strands, strands.size,
        EuclideanVector(0, 0),
        MutableData(0.0f),
        EuclideanVector(0, 0),
        2f,
        0.0f,
        0.1f
      )

      strand1.initialPosition shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.mobility shouldBe 1.0f
      strand1.delay shouldBe 1.0f
      strand1.acceleration shouldBe 1.0f
      strand1.radius shouldBe 0.0f
      strand1.position shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.lastPosition shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.lastGravity shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.force shouldBe EuclideanVector(0.0f, 0.0f)
      strand1.velocity shouldBe EuclideanVector(0.0f, 0.0f)

      strand2.initialPosition shouldBe EuclideanVector(0.0f, 0.0f)
      strand2.mobility shouldBe 0.95f
      strand2.delay shouldBe 0.0f
      strand2.acceleration shouldBe 1.5f
      strand2.radius shouldBe 15.0f
      strand2.position shouldBe EuclideanVector(0.0f, -14.966778f)
      strand2.lastPosition shouldBe EuclideanVector(1.0f, 15.0f)
      strand2.lastGravity shouldBe EuclideanVector(0.0f, 1.0f)
      strand2.force shouldBe EuclideanVector(0.0f, 0.0f)
      strand2.velocity shouldBe EuclideanVector(0.0f, 0.0f)
    }

  }

  Feature("NormalizeParameterValue") {
    Scenario("Calculate normalized value") {
      val table = Table(
        ("inputValue", "parameterMinimum", "parameterMaximum", "normalizedMinimum", "normalizedMaximum", "normalizedDefault", "isInverted", "result"),
        (50.0f, 0.0f, 100.0f, 0.0f, 100.0f, 50.0f, true, 50.0f),
        (4.22144412994384800000f, 3.13320636749267600000f, -8.29889488220214800000f, 7.22517585754394500000f, 0.14490985870361328000f, 4.32662773132324200000f, true, 7.22517585754394500000f),
        (8.29556274414062500000f, 0.43875598907470703000f, -0.70181751251220700000f, 0.99127674102783200000f, -4.29619216918945300000f, 9.34834861755371100000f, false, -0.99127674102783200000f),
        (3.88695526123046880000f, -2.88563728332519530000f, 7.28557586669921900000f, 6.61606788635253900000f, -6.76763772964477500000f, 7.48964309692382800000f, true, 7.19986248016357400000f),
        (-2.13446855545043950000f, -2.28988265991210940000f, 9.73457527160644500000f, -9.46209049224853500000f, 6.46665763854980500000f, -7.44005775451660200000f, true, -9.40982151031494100000f),
        (-4.54578018188476600000f, -1.54203033447265620000f, 8.00138092041015600000f, -5.77124214172363300000f, -6.37614727020263700000f, 1.68535137176513670000f, false, 6.37614727020263700000f),
        (6.63025283813476600000f, 8.88509941101074200000f, 1.29956817626953120000f, -1.03949165344238280000f, -0.39646911621093750000f, 6.61400794982910200000f, false, -3.77134728431701660000f),
        (8.65293312072753900000f, 2.97424316406250000000f, 3.48930835723876950000f, 4.92040634155273400000f, -5.71320533752441400000f, 8.54444885253906200000f, false, -4.92040634155273400000f),
        (1.35195922851562500000f, 8.31915664672851600000f, 9.27978706359863300000f, -1.32204532623291020000f, 6.48756217956543000000f, 2.56278419494628900000f, false, 1.32204508781433100000f),
        (-7.22521686553955100000f, -3.99886131286621100000f, -8.24843406677246100000f, -8.70619297027587900000f, -3.85145092010498050000f, 0.49726581573486330000f, true, -4.27415323257446300000f),
        (7.34107780456543000000f, 4.17654609680175800000f, -4.74319696426391600000f, -5.09350204467773400000f, -9.08762741088867200000f, -0.40380382537841797000f, true, -5.09350204467773400000f),
        (-6.89597034454345700000f, 8.77605628967285200000f, 3.11721229553222660000f, 2.96432685852050800000f, 5.06023883819580100000f, 2.90299510955810550000f, false, -2.96432685852050800000f),
        (7.03502464294433600000f, 4.90772724151611300000f, -8.40860557556152300000f, 7.07166099548339800000f, 2.06995582580566400000f, -0.17747116088867188000f, true, 7.07166051864624000000f),
        (-7.00501441955566400000f, -1.77604579925537100000f, 5.12517356872558600000f, -2.67909622192382800000f, 3.11296463012695300000f, -0.44972229003906250000f, true, -2.67909622192382800000f),
        (-8.29945182800293000000f, 5.56871318817138700000f, -7.63974761962890600000f, 5.67673301696777300000f, 1.08671760559082030000f, -3.87289524078369140000f, true, 1.08671760559082030000f),
        (-5.91953516006469700000f, -0.99624061584472660000f, -9.96809768676757800000f, -5.63983345031738300000f, 5.98943138122558600000f, -8.20133399963378900000f, false, 7.95159435272216800000f),
        (4.74380016326904300000f, 4.02572250366210900000f, 9.34539413452148400000f, -5.73676586151123050000f, 2.07787895202636700000f, 6.06825256347656250000f, false, 2.54975795745849600000f),
        (1.45040321350097660000f, 0.25236225128173830000f, 2.42634010314941400000f, 6.91045570373535200000f, -5.81144237518310550000f, 4.26078033447265600000f, false, -4.53148412704467800000f),
        (6.53572463989257800000f, -3.74975919723510740000f, -8.18984794616699200000f, -8.42348861694336000000f, -3.23393249511718750000f, -2.31626510620117200000f, false, 3.23393249511718750000f),
        (5.62432384490966800000f, 0.70082759857177730000f, -7.58162736892700200000f, 6.73272895812988300000f, 1.64293193817138670000f, 0.67186832427978520000f, true, 6.73272895812988300000f),
        (-1.93451786041259770000f, 5.97318744659423800000f, 5.46594524383544900000f, -7.03390693664550800000f, -5.15113592147827150000f, 9.51830673217773400000f, false, 7.03390693664550800000f),
        (-6.69598960876464800000f, 4.35857772827148400000f, 2.85850620269775400000f, -2.30805301666259770000f, 1.86712455749511720000f, 3.37705135345459000000f, true, -2.30805301666259770000f),
        (5.84561824798584000000f, 6.43552207946777300000f, -6.41360664367675800000f, -1.78909873962402340000f, 1.43520927429199220000f, -0.50716018676757810000f, false, -1.25686085224151610000f),
        (4.00097370147705100000f, 9.85706138610839800000f, 6.75700950622558600000f, -8.84551525115966800000f, 9.80094718933105500000f, -9.57368087768554700000f, true, -8.84551525115966800000f),
        (-0.86710834503173830000f, -9.59352016448974600000f, 1.38328075408935550000f, -6.33742332458496100000f, -9.60423564910888700000f, 0.34097480773925780000f, false, 3.59910368919372560000f),
        (9.99114608764648400000f, 9.44742965698242200000f, 4.20150279998779300000f, 7.23668861389160200000f, 5.33483409881591800000f, 2.59713363647460940000f, true, 7.23668861389160200000f),
        (1.40194129943847660000f, 2.60881233215332030000f, -1.29775047302246100000f, 1.16022205352783200000f, 3.68802261352539060000f, 6.34927368164062500000f, true, 5.33232593536376950000f),
        (8.33809280395507800000f, 4.67834568023681600000f, -2.85187721252441400000f, 1.59792327880859380000f, -8.34972476959228500000f, 6.25806617736816400000f, false, -1.59792327880859380000f),
        (-8.41329956054687500000f, 4.43086719512939450000f, 2.82111644744873050000f, -1.11528205871582030000f, 5.61332511901855500000f, 0.51094245910644530000f, false, 1.11528205871582030000f),
        (4.88124275207519500000f, -4.46058654785156250000f, -1.43732547760009770000f, 8.09074974060058600000f, 5.98830413818359400000f, -4.65362930297851600000f, true, 8.09074974060058600000f),
        (5.70012569427490200000f, 0.61019039154052730000f, -0.20726108551025390000f, 8.52228927612304700000f, -0.84393405914306640000f, -2.26678752899169900000f, false, -8.52228927612304700000f),
        (-7.89802551269531250000f, 7.53269577026367200000f, 0.29238224029541016000f, -6.49198770523071300000f, -5.60727357864379900000f, -6.52994060516357400000f, true, -6.49198770523071300000f),
        (-3.07692909240722660000f, 2.28876972198486330000f, 9.57832145690918000000f, 4.31225776672363300000f, -8.91790866851806600000f, 0.99125003814697270000f, false, 8.91790866851806600000f),
        (5.70999050140380900000f, 6.69828987121582000000f, -9.32353782653808600000f, -0.59680938720703120000f, -1.00283527374267580000f, -3.12219619750976560000f, false, 0.90836429595947270000f),
        (2.21756076812744140000f, 5.24806880950927700000f, -7.64946365356445300000f, 6.10540390014648400000f, 0.02001190185546875000f, 1.55423736572265620000f, true, 3.96664667129516600000f),
        (4.57698345184326200000f, 5.91145133972168000000f, 3.33262825012207030000f, 4.64446067810058600000f, -1.22636699676513670000f, 4.15968894958496100000f, false, -3.97148227691650400000f),
        (-5.85061454772949200000f, 1.35204887390136720000f, 3.22393894195556640000f, 4.52246475219726600000f, 3.77147579193115230000f, 5.35178661346435550000f, false, -3.77147579193115230000f),
        (-5.20214796066284200000f, 3.20222091674804700000f, -3.75549411773681640000f, 4.44079017639160200000f, 7.20394515991210900000f, -7.19132328033447300000f, true, 4.44079017639160200000f),
        (9.75924301147461000000f, -2.34392738342285160000f, 5.74076652526855500000f, 0.75776576995849610000f, 7.15304946899414100000f, -4.18469047546386700000f, false, -7.15304946899414100000f),
        (8.18874168395996100000f, 9.29671859741211000000f, 9.50702095031738300000f, 2.95728683471679700000f, 3.18843650817871100000f, -0.43420886993408203000f, true, 2.95728683471679700000f),
        (-1.16344070434570310000f, 6.32133102416992200000f, 8.60890388488769500000f, -5.17711257934570300000f, -1.81873703002929690000f, -6.19785785675048800000f, false, 5.17711257934570300000f),
        (7.86498832702636700000f, 6.89836311340332000000f, 9.03853034973144500000f, 3.90011882781982400000f, -7.75863170623779300000f, 4.39531421661376950000f, true, 3.22023963928222660000f),
        (6.46560287475585900000f, 8.15670204162597700000f, 0.82022666931152340000f, 8.60046768188476600000f, 4.90337562561035200000f, 2.71215248107910160000f, false, -5.88588762283325200000f),
        (-4.39426040649414100000f, 0.74823284149169920000f, 7.74916076660156250000f, 8.58474731445312500000f, -4.12624454498291000000f, -9.63885498046875000000f, false, 4.12624454498291000000f),
        (3.73322963714599600000f, 4.14176750183105500000f, 9.60053062438964800000f, 6.81562614440918000000f, 7.99579238891601600000f, -8.95250511169433600000f, false, -6.81562614440918000000f),
        (5.66361236572265600000f, 6.69361686706543000000f, 5.87260341644287100000f, 3.96405029296875000000f, 1.88690948486328120000f, -5.32874488830566400000f, true, 1.88690948486328120000f),
        (-3.75084304809570300000f, 2.36032962799072270000f, -2.73653030395507800000f, -5.14099597930908200000f, -3.45402956008911130000f, 7.13639259338378900000f, true, -5.14099597930908200000f),
        (-7.80300521850585900000f, -8.63446140289306600000f, 0.53286170959472660000f, -4.56812858581543000000f, 8.42617416381836000000f, -8.86049365997314500000f, false, 5.34674501419067400000f),
        (-5.26080131530761700000f, 1.41693210601806640000f, -0.85748577117919920000f, 5.84007453918457000000f, 2.11465072631835940000f, -0.31217575073242190000f, true, 2.11465072631835940000f),
        (-7.10052013397216800000f, -4.71161842346191400000f, 6.46858787536621100000f, 8.80374526977539000000f, -7.85002946853637700000f, -1.38834190368652340000f, true, -7.85002946853637700000f),
        (-2.87101030349731450000f, 2.96365356445312500000f, 5.45241737365722700000f, 2.06610107421875000000f, 3.44803905487060550000f, 9.14391708374023400000f, true, 2.06610107421875000000f),
        (-4.81879711151123050000f, -5.71696758270263700000f, 8.87016487121582000000f, -5.27969741821289100000f, -7.85386180877685550000f, -9.67621707916259800000f, false, 8.07827663421630900000f),
        (1.05516910552978520000f, -2.26807594299316400000f, 0.99730873107910160000f, 0.81823444366455080000f, 1.11257743835449220000f, -4.17326354980468750000f, false, -1.11257743835449220000f),
        (-9.83543586730957000000f, -2.98737049102783200000f, 7.50522422790527300000f, -7.31322860717773400000f, 3.12085628509521500000f, -9.48972797393798800000f, true, -7.31322860717773400000f),
        (2.94566535949707030000f, 1.50158119201660160000f, -5.90023136138916000000f, 3.57814979553222660000f, 6.61083030700683600000f, -8.46888065338134800000f, true, 6.61083126068115200000f),
        (4.67949676513671900000f, -1.05819225311279300000f, 4.84137916564941400000f, -6.89712142944335900000f, 6.54226493835449200000f, 6.95207405090332000000f, false, -6.56475496292114300000f),
        (1.75238227844238280000f, -0.58776855468750000000f, -7.60997676849365200000f, -0.18242645263671875000f, -6.89878940582275400000f, 1.32820320129394530000f, true, -0.18242645263671875000f),
        (6.95076179504394500000f, 0.61912155151367190000f, 7.01118087768554700000f, 4.69326877593994100000f, 5.86302566528320300000f, 8.72069931030273400000f, true, 5.91704845428466800000f),
        (-7.24762535095214800000f, 1.37361335754394530000f, 1.91879177093505860000f, -7.01258754730224600000f, -6.22607326507568400000f, -5.98390960693359400000f, true, -7.01258754730224600000f),
        (-3.67899036407470700000f, -7.83336639404296900000f, -3.28726387023925800000f, -6.69614791870117200000f, 3.25692081451416000000f, 0.20699882507324220000f, false, -2.73131203651428220000f),
        (-9.95363998413086000000f, 4.41294765472412100000f, -0.01834487915039062500f, 2.32381439208984380000f, -0.86393547058105470000f, -2.61364698410034200000f, true, -0.86393547058105470000f),
        (-1.70513248443603520000f, -7.04109764099121100000f, 5.42696571350097700000f, -3.37173843383789060000f, 6.19866371154785200000f, 4.96571445465087900000f, false, -3.76463174819946300000f),
        (-4.03334617614746100000f, 4.97005367279052700000f, 8.08049392700195300000f, -1.06106662750244140000f, 6.68483543395996100000f, 5.37205314636230500000f, true, -1.06106662750244140000f),
        (5.92665481567382800000f, 0.85951900482177730000f, 6.86617088317871100000f, -0.13996028900146484000f, -6.37145757675170900000f, 4.75407123565673800000f, true, 1.39101624488830570000f),
        (5.60811901092529300000f, 9.25723648071289000000f, -2.41962909698486330000f, -2.80997657775878900000f, 7.44065856933593750000f, 4.82148075103759800000f, true, 5.80362892150878900000f),
        (1.19058799743652340000f, 9.62915039062500000000f, -4.75125217437744100000f, 4.09996414184570300000f, 3.65736961364746100000f, -2.11080455780029300000f, true, -1.10933542251586910000f),
        (-4.00584936141967800000f, -1.23743438720703120000f, -2.44222068786621100000f, 4.32868099212646500000f, 4.12245941162109400000f, -4.13345575332641600000f, true, 4.12245893478393550000f),
        (8.42837715148925800000f, 2.39156913757324200000f, 1.64909172058105470000f, 7.66882324218750000000f, -7.53129959106445300000f, 8.24788475036621100000f, true, 7.66882324218750000000f),
        (0.88594627380371090000f, 0.66520595550537110000f, -6.13398933410644500000f, -5.15331649780273400000f, -0.29958915710449220000f, -0.71131896972656250000f, false, 0.29958915710449220000f),
        (-2.49559783935546880000f, -0.85020637512207030000f, 2.74527931213378900000f, -6.35035276412963900000f, -0.39842700958251953000f, 0.93428421020507810000f, true, -6.35035228729248050000f),
        (-4.94752645492553700000f, 6.83289718627929700000f, 1.57745742797851560000f, -7.44089698791503900000f, -0.81876945495605470000f, -7.88019561767578100000f, false, 7.44089698791503900000f),
        (-6.67747879028320300000f, 8.20787429809570300000f, 9.01029396057128900000f, 0.15305709838867188000f, -8.78112220764160200000f, 9.73849487304687500000f, false, 8.78112220764160200000f),
        (-6.58934593200683600000f, -1.38683891296386720000f, 7.95562171936035200000f, 5.90519332885742200000f, -9.40584945678711000000f, 4.32524871826171900000f, true, -9.40585041046142600000f),
        (-8.40399837493896500000f, -2.61223888397216800000f, 6.81112480163574200000f, 1.69412612915039060000f, -1.97977638244628900000f, 7.68556404113769500000f, false, 1.97977638244628900000f),
        (-7.43258380889892600000f, 7.46553230285644500000f, -4.69594240188598600000f, 8.58913803100586000000f, 1.22400760650634770000f, 3.54451942443847660000f, false, -1.22400760650634770000f),
        (3.79825019836425800000f, 8.61936950683593800000f, 4.51869010925293000000f, -2.42511510848999020000f, 0.71711635589599610000f, 6.84082794189453100000f, false, 2.42511558532714840000f),
        (8.37419128417968800000f, 3.35530090332031250000f, -4.63489627838134800000f, -6.01293802261352500000f, -0.15005016326904297000f, 5.24823951721191400000f, false, 0.15005016326904297000f),
        (-2.04368019104003900000f, -9.61177635192871100000f, -5.97320652008056600000f, 9.99030685424804700000f, -7.95802593231201200000f, -4.59275245666503900000f, true, 9.99030685424804700000f),
        (-2.28846788406372070000f, 2.03497982025146500000f, 1.77162361145019530000f, 4.50399875640869100000f, 0.53456497192382810000f, 8.33925437927246100000f, true, 0.53456497192382810000f),
        (-4.14592742919921900000f, 1.79168319702148440000f, -9.90025997161865200000f, 7.20248413085937500000f, 6.03955841064453100000f, -7.21995592117309600000f, true, -7.01210546493530300000f),
        (7.55016136169433600000f, 9.17885971069336000000f, -3.00034523010253900000f, -2.37683534622192400000f, -7.10805416107177700000f, -5.83938980102539100000f, true, -3.30291485786438000000f),
        (7.65915298461914100000f, -4.73968267440795900000f, 1.31784820556640620000f, -1.84649944305419920000f, 6.14575195312500000000f, -6.27488231658935550000f, true, 6.14575195312500000000f),
        (2.01949691772460940000f, 9.32373237609863300000f, 7.35531234741210900000f, 7.24970817565918000000f, 8.47051239013671900000f, -7.01022624969482400000f, false, -7.24970817565918000000f),
        (-4.71603965759277300000f, 6.79227256774902300000f, 9.54289627075195300000f, -4.32487726211547850000f, 5.41538238525390600000f, -6.15328884124755900000f, true, -4.32487726211547850000f),
        (7.19710731506347700000f, 3.54125022888183600000f, -0.90312957763671880000f, 9.48946762084961000000f, -2.76997661590576170000f, 5.98732471466064450000f, true, 9.48946762084961000000f),
        (-6.06356859207153300000f, -8.74242401123046900000f, 2.55729770660400400000f, -3.05453872680664060000f, 1.56287288665771480000f, -8.49264144897461000000f, false, 5.63298988342285200000f),
        (-6.15328073501586900000f, -8.60188865661621100000f, 6.31597137451171900000f, 7.46996116638183600000f, 8.94792366027832000000f, 3.73352622985839840000f, false, -6.24336910247802700000f),
        (5.88297939300537100000f, -6.85063457489013700000f, 8.44209289550781200000f, 2.22990608215332030000f, 2.61209106445312500000f, -4.87462759017944300000f, false, -0.10640811920166016000f),
        (-8.52899551391601600000f, -3.61755609512329100000f, 7.10202789306640600000f, -2.88734531402587900000f, 4.96702003479003900000f, -8.76730823516845700000f, false, 2.88734531402587900000f),
        (-5.67003631591796900000f, 4.83521080017089800000f, 0.79221248626708980000f, -8.22975635528564500000f, -5.36919116973876950000f, 4.45495223999023400000f, true, -8.22975635528564500000f),
        (-4.27462482452392600000f, 4.96860408782959000000f, -6.07278251647949200000f, -6.07306098937988300000f, 4.18074703216552700000f, 3.74814605712890620000f, true, -2.87417316436767600000f),
        (-8.46749496459961000000f, -8.67725276947021500000f, -2.42183589935302730000f, 8.84257125854492200000f, 1.45543003082275400000f, 4.02465629577636700000f, true, 1.62773370742797850000f),
        (3.77925777435302730000f, -6.11551761627197300000f, 5.51335144042968750000f, 9.46397590637207000000f, -7.82221221923828100000f, -9.84351158142089800000f, true, 3.70572185516357400000f),
        (-1.82105636596679690000f, -6.41045188903808600000f, -0.31185054779052734000f, -1.06079101562500000000f, 4.97222042083740200000f, -0.63907051086425780000f, false, -2.19499588012695300000f),
        (6.09153366088867200000f, -8.57068443298339800000f, 5.36309814453125000000f, 7.10018730163574200000f, 4.23930931091308600000f, -5.84251976013183600000f, false, -7.10018730163574200000f),
        (1.35558223724365230000f, -1.91209030151367190000f, 2.62211418151855470000f, -8.62325477600097700000f, 4.72590255737304700000f, -6.75048255920410200000f, true, -1.68545770645141600000f),
        (-1.67088508605957030000f, -9.73393440246582000000f, -2.27818012237548830000f, -8.52776050567627000000f, 0.65820121765136720000f, -9.12511825561523400000f, true, 0.65820121765136720000f),
        (8.14556884765625000000f, -7.89128398895263700000f, -4.09759521484375000000f, -2.65832042694091800000f, 3.36876869201660160000f, -8.72873497009277300000f, true, 3.36876869201660160000f),
        (-4.22880649566650400000f, 2.10523414611816400000f, -5.97039318084716800000f, -9.00813198089599600000f, -3.64102458953857400000f, -0.53145599365234380000f, false, 5.35197877883911100000f),
        (2.70040988922119140000f, -3.92005920410156250000f, -9.84606933593750000000f, -6.69922828674316400000f, 3.44068527221679700000f, 8.21754264831543000000f, false, -3.44068527221679700000f),
        (-8.05912780761718800000f, 5.29548263549804700000f, 3.39865207672119140000f, -7.72486925125122100000f, -8.95555019378662100000f, 9.50727844238281200000f, true, -8.95555114746093800000f)
      )

      forAll(table) { (inputValue, parameterMinimum, parameterMaximum, normalizedMinimum, normalizedMaximum, normalizedDefault, isInverted, expectedResult) =>
        val result = NormalizedPhysicsParameterValueGetter.normalizeParameterValue(
          inputValue, parameterMinimum, parameterMaximum,
          normalizedMinimum, normalizedMaximum, normalizedDefault, isInverted
        )
        result shouldBe expectedResult
      }
    }
  }

  private def parseLog(line: String): LogData = parse(line).extract[LogData]

  private def createStubbedModel(logData: LogData): Live2DModel = {
    val model: Live2DModel = stub[Live2DModel]
    val mockedParameters = logData.parameters.map { case (id, parameter) =>
      id -> new JavaVMParameter(id, parameter.min, parameter.max, parameter.default, parameter.current)
    }

    (() => model.parameters).when().returns(mockedParameters)
    model
  }

  case class CurrentParameter(current: Float, min: Float, max: Float, default: Float)
  case class LogData(
    totalElapsedTimeInSeconds: Float,
    deltaTimeSeconds: Float,
    parameters: Map[String, CurrentParameter],
    operations: List[EffectOperation]
  )

}
