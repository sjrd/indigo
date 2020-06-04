lazy val publishSettings = {
  import xerial.sbt.Sonatype._
  Seq(
    publishTo := sonatypePublishToBundle.value,
    publishMavenStyle := true,
    sonatypeProfileName := "io.indigoengine",
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    sonatypeProjectHosting := Some(GitHubHosting("PurpleKingdomGames", "indigo", "indigo@purplekingdomgames.com")),
    developers := List(
      Developer(id = "davesmith00000", name = "David Smith", email = "indigo@purplekingdomgames.com", url = url("https://github.com/davesmith00000"))
    )
  )
}

// Plugin
lazy val sbtIndigo =
  (project in file("."))
    .settings(publishSettings: _*)
    .settings(
      version := IndigoVersion.getVersion,
      scalaVersion := "2.12.10", // This is a plugin! Only 2.12 is supported!
      organization := "io.indigoengine",
      scalacOptions ++= Scalac212Options.scala212Compile
    )
    .settings(
      name := "sbt-indigo",
      sbtPlugin := true,
      libraryDependencies ++= Seq(
        "commons-io" % "commons-io" % "2.6"
      ),
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-core",
        "io.circe" %%% "circe-generic",
        "io.circe" %%% "circe-parser"
      ).map(_ % "0.13.0")
    )
    .enablePlugins(ScalaJSPlugin)
