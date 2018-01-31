import sbt.Keys.version
import sbt._
import sbtrelease.{ReleaseStateTransformations, Version}

/**
  * Based on https://github.com/fagossa/sbt-release-test
  *
  */
object Release {

  import sbtrelease.ReleasePlugin.autoImport._

  private val gitRemote = "origin"

  private val mergeReleaseOntoMaster = Def.setting {
    val vcs = releaseVcs.value.get

    val rcVersion = releaseVersion.value(version.value)
    val releaseTargetVersion = versions.releaseTarget(rcVersion)
    val releaseBranch = s"release/$releaseTargetVersion"

    ReleaseStep(action = { st =>
      vcs.cmd("merge", "--no-ff", releaseBranch) !! st.log
      st
    })
  }

  private val mergeReleaseOntoDevelop = Def.setting {
    val vcs = releaseVcs.value.get

    val rcVersion = releaseVersion.value(version.value)
    val releaseTargetVersion = versions.releaseTarget(rcVersion)
    val releaseBranch = s"release/$releaseTargetVersion"

    ReleaseStep(action = { st =>
      vcs.cmd("merge", "--no-ff", releaseBranch) !! st.log
      st
    })
  }

  private val checkoutDevelop = Def.setting {
    val vcs = releaseVcs.value.get
    ReleaseStep(action = { st =>
      vcs.cmd("checkout", "develop") !! st.log
      st
    })
  }

  private val checkoutMaster = Def.setting {
    val vcs = releaseVcs.value.get
    ReleaseStep(action = { st =>
      vcs.cmd("checkout", "master") !! st.log
      st
    })
  }

  private val pushBranches = Def.setting {
    val vcs = releaseVcs.value.get

    ReleaseStep(action = { st =>
      vcs.cmd("push", gitRemote, "master") !! st.log
      vcs.cmd("push", gitRemote, "develop") !! st.log
      vcs.cmd("push", gitRemote, "--tags") !! st.log
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
        vcs.cmd("checkout", "-b", releaseBranch, "develop") !! st.log
        st
      })
  }

  private val removeReleaseBranch = Def.setting {
    val vcs = releaseVcs.value.get

    val rcVersion = releaseVersion.value(version.value)
    val releaseTargetVersion = versions.releaseTarget(rcVersion)
    val releaseBranch = s"release/$releaseTargetVersion"

    ReleaseStep(action = { st =>
      vcs.cmd("branch", "-D", releaseBranch) !! st.log
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

  lazy val runIntegrationTest: ReleaseStep = ReleaseStep(
    action = { st: State =>
        import sbt.Keys.{thisProjectRef, test}

        val extracted = Project.extract(st)
        val ref = extracted.get(thisProjectRef)
        extracted.runAggregated(test in IntegrationTest in ref, st)
    },
    enableCrossBuild = true
  )

  private val releaseMaster = Def.setting {
    Seq[ReleaseStep](
      git.checkIsOnDevelop.value,
      ReleaseStateTransformations.checkSnapshotDependencies,
      ReleaseStateTransformations.inquireVersions,
      ReleaseStateTransformations.runClean,
      ReleaseStateTransformations.runTest,
      runIntegrationTest,
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
      ReleaseStateTransformations.commitNextVersion//,
      //pushBranches.value
    )
  }


  val settings = Seq(
    releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}",
    releaseTagName := s"release-${(version in ThisBuild).value}",
    releaseProcess := releaseMaster.value
  )

}
