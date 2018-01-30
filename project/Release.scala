import sbt.Keys.version
import sbt._
import sbtrelease.{ReleaseStateTransformations, Version}

import scala.sys.process

object Release {

  import sbtrelease.ReleasePlugin.autoImport._

  private val gitRemote = "origin"

  private val mergeReleaseOntoMaster = Def.setting {
    val vcs = releaseVcs.value.get

    val rcVersion = releaseVersion.value(version.value)
    val releaseTargetVersion = versions.releaseTarget(rcVersion)
    val releaseBranch = s"release/$releaseTargetVersion"

    ReleaseStep(action = { st =>
      vcs.cmd("merge", "--no-ff", releaseBranch) !! st.log.asInstanceOf[process.ProcessLogger]
      st
    })
  }

  private val mergeReleaseOntoDevelop = Def.setting {
    val vcs = releaseVcs.value.get

    val rcVersion = releaseVersion.value(version.value)
    val releaseTargetVersion = versions.releaseTarget(rcVersion)
    val releaseBranch = s"release/$releaseTargetVersion"

    ReleaseStep(action = { st =>
      vcs.cmd("merge", "--no-ff", releaseBranch) !! st.log.asInstanceOf[process.ProcessLogger]
      st
    })
  }

  private val checkoutDevelop = Def.setting {
    val vcs = releaseVcs.value.get
    ReleaseStep(action = { st =>
      vcs.cmd("checkout", "develop") !! st.log.asInstanceOf[process.ProcessLogger]
      st
    })
  }

  private val checkoutMaster = Def.setting {
    val vcs = releaseVcs.value.get
    ReleaseStep(action = { st =>
      vcs.cmd("checkout", "master") !! st.log.asInstanceOf[process.ProcessLogger]
      st
    })
  }

  private val pushBranches = Def.setting {
    val vcs = releaseVcs.value.get

    ReleaseStep(action = { st =>
      vcs.cmd("push", gitRemote, "master") !! st.log.asInstanceOf[process.ProcessLogger]
      vcs.cmd("push", gitRemote, "develop") !! st.log.asInstanceOf[process.ProcessLogger]
      vcs.cmd("push", gitRemote, "--tags") !! st.log.asInstanceOf[process.ProcessLogger]
      st
    })
  }

  val createReleaseBranch = Def.setting {
    val vcs = releaseVcs.value.get

    val rcVersion = releaseVersion.value(version.value)
    val releaseTargetVersion = versions.releaseTarget(rcVersion)
    val releaseBranch = s"release/$releaseTargetVersion"

    ReleaseStep(
      action = { st =>
        vcs.cmd("checkout", "-b", releaseBranch, "develop") !! st.log.asInstanceOf[process.ProcessLogger]
        st
      })
  }

  private val removeReleaseBranch = Def.setting {
    val vcs = releaseVcs.value.get

    val rcVersion = releaseVersion.value(version.value)
    val releaseTargetVersion = versions.releaseTarget(rcVersion)
    val releaseBranch = s"release/$releaseTargetVersion"

    ReleaseStep(action = { st =>
      vcs.cmd("branch", "-D", releaseBranch) !! st.log.asInstanceOf[process.ProcessLogger]

      st
    })
  }

  private object versions {
    def releaseTarget(rcVersion: String): String = {
      Version(rcVersion).getOrElse(sbtrelease.versionFormatError)
        .withoutQualifier
        .string
    }
  }

  private object git {
    private def checkBranch(name: Def.Initialize[String]) = Def.setting {
      val vcs = releaseVcs.value.get
      ReleaseStep(
        action = { st =>
          if (vcs.currentBranch != name.value)
            sys.error(s"The command should be issued when on branch $name")
          st
        })
    }

    def checkIsOnDevelop = checkBranch(Def.setting("develop"))

  }

  private val releaseMaster = Def.setting {
    Seq[ReleaseStep](
      git.checkIsOnDevelop.value,
      ReleaseStateTransformations.checkSnapshotDependencies,
      ReleaseStateTransformations.inquireVersions,
      ReleaseStateTransformations.runClean,
      ReleaseStateTransformations.runTest,
      // Create a release
      createReleaseBranch.value,
      ReleaseStateTransformations.setReleaseVersion,
      ReleaseStateTransformations.commitReleaseVersion,
      // Finalize a release branch
      checkoutMaster.value,
      mergeReleaseOntoMaster.value,
      ReleaseStateTransformations.tagRelease,
      checkoutDevelop.value,
      mergeReleaseOntoDevelop.value,
      removeReleaseBranch.value,
      // Push the merged feature branch
      ReleaseStateTransformations.setNextVersion,
      ReleaseStateTransformations.commitNextVersion,
      pushBranches.value
    )
  }


  val settings = Seq(
    releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}",
    releaseTagName := (version in ThisBuild).value,
    releaseProcess := releaseMaster.value
  )

}
