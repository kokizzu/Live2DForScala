name := "Live2D Cubism For Scala"

organization := "moe.brianhsu.live2d"

scalaVersion := "2.13.5"

scalacOptions := Seq("-deprecation", "-Ywarn-unused")

Compile / doc / scalacOptions ++= Seq("-private")

val lwjglVersion = "3.2.3"

libraryDependencies ++= Seq(
    "net.java.dev.jna" % "jna" % "5.7.0",
    "org.json4s" %% "json4s-native" % "4.0.1",
//    "org.jogamp.gluegen" % "gluegen-rt" % "2.3.2",
//    "org.jogamp.gluegen" % "gluegen-rt-main" % "2.3.2",
//    "org.jogamp.jogl" % "jogl-all" % "2.3.2",
//    "org.jogamp.jogl" % "jogl-all-main" % "2.3.2",
    "org.scalatest" %% "scalatest" % "3.2.5" % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.5.0" % Test
)

fork := true

