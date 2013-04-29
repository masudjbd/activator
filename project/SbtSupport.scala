import sbt._
import Keys._

object SbtSupport {
  val sbtLaunchJarUrl = SettingKey[String]("sbt-launch-jar-url")
  val jansiJarUrl = SettingKey[String]("jansi-jar-url")
  val jansiJarLocation = SettingKey[File]("jansi-jar-location")
  val jansiJar = TaskKey[File]("jansi-jar")
  val sbtLaunchJarLocation = SettingKey[File]("sbt-launch-jar-location")  
  val sbtLaunchJar = TaskKey[File]("sbt-launch-jar", "Resolves SBT launch jar")

  def snapshotDownloadUrl(v: String) = "http://private-repo.typesafe.com/typesafe/ivy-snapshots/org.scala-sbt/sbt-launch/"+v+"/sbt-launch.jar"
  def currentDownloadUrl(v: String) = "http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/"+v+"/sbt-launch.jar"
  def oldDownloadUrl(v: String) = "http://repo.typesafe.com/typesafe/ivy-releases/org.scala-tools.sbt/sbt-launch/"+v+"/sbt-launch.jar"

  def downloadUrlForVersion(v: String) = (v split "[^\\d]" filter (_ matches "[\\d]+") map (_.toInt)) match {
    case Array(0, 11, x, _*) if x >= 3 => currentDownloadUrl(v)
    case Array(0, y, _*) if y >= 12    => currentDownloadUrl(v)
    case _                             => oldDownloadUrl(v)
  }

  def downloadFile(uri: String, file: File): File = {
    import dispatch._
    if(!file.exists) {
       // oddly, some places require us to create the file before writing...
       IO.touch(file)
       val writer = new java.io.BufferedOutputStream(new java.io.FileOutputStream(file))
       try Http(url(uri) >>> writer)
       finally writer.close()
    }
    // TODO - GPG Trust validation.
    file
  }

  val buildSettings: Seq[Setting[_]] = Seq(
    // TODO - Configure different SBT version...
    //sbtLaunchJarUrl <<= sbtVersion apply downloadUrlForVersion,
    // TODO - We use a snpashot launcher for now for the new jline...
    sbtLaunchJarUrl := snapshotDownloadUrl(Dependencies.sbtSnapshotVersion),
    sbtLaunchJarLocation <<= baseDirectory (_ / "target" / "sbt" / "sbt-launch.jar"),
    sbtLaunchJar <<= (sbtLaunchJarUrl, sbtLaunchJarLocation) map downloadFile,
    // TODO - pull jansi from Ivy.
    jansiJarUrl := "http://repo.fusesource.com/nexus/content/groups/public/org/fusesource/jansi/jansi/1.7/jansi-1.7.jar",
    jansiJarLocation <<= baseDirectory (_ / "target" / "sbt" / "jansi.jar"),
    jansiJar <<= (jansiJarUrl, jansiJarLocation) map downloadFile
  )

  val settings: Seq[Setting[_]] = Seq(
    resourceGenerators in Compile <+= (sbtLaunchJar.task map (Seq apply _))
  )
}
