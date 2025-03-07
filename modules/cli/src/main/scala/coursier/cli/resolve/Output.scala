package coursier.cli.resolve

import java.io.PrintStream

import coursier.cli.params.OutputParams
import coursier.core.{Dependency, Resolution}
import coursier.graph.Conflict
import coursier.params.ResolutionParams
import coursier.parse.{JavaOrScalaDependency, JavaOrScalaModule}
import coursier.util.{ModuleMatcher, Print}

object Output {

  def errPrintln(s: String) = Console.err.println(s)

  def printDependencies(
    outputParams: OutputParams,
    resolutionParams: ResolutionParams,
    deps: Seq[Dependency],
    stdout: PrintStream,
    stderr: PrintStream
  ): Unit =
    if (outputParams.verbosity >= 1) {
      stderr.println(
        s"  Dependencies:\n" +
          Print.dependenciesUnknownConfigs(
            deps,
            Map.empty,
            printExclusions = outputParams.verbosity >= 2
          )
      )

      if (resolutionParams.forceVersion.nonEmpty) {
        stderr.println("  Force versions:")
        for ((mod, ver) <- resolutionParams.forceVersion.toVector.sortBy { case (mod, _) => mod.toString })
          stderr.println(s"$mod:$ver")
      }
    }

  def printResolutionResult(
    printResultStdout: Boolean,
    params: ResolveParams,
    scalaVersion: String,
    platformOpt: Option[String],
    res: Resolution,
    stdout: PrintStream,
    stderr: PrintStream,
    colors: Boolean
  ): Unit =
    if (printResultStdout || params.output.verbosity >= 1 || params.anyTree || params.conflicts) {
      if ((printResultStdout && params.output.verbosity >= 1) || params.output.verbosity >= 2 || params.anyTree)
        stderr.println(s"  Result:")

      val withExclusions = params.output.verbosity >= 1

      val depsStr =
        if (params.whatDependsOn.nonEmpty) {
          val matchers = params.whatDependsOn
            .map(_.module(JavaOrScalaModule.scalaBinaryVersion(scalaVersion), scalaVersion))
            .map(ModuleMatcher(_))
          Print.dependencyTree(
            res,
            roots = res.minDependencies.filter(f => matchers.exists(m => m.matches(f.module))).toSeq,
            printExclusions = withExclusions,
            reverse = true,
            colors = colors
          )
        } else if (params.reverseTree || params.tree)
          Print.dependencyTree(
            res,
            printExclusions = withExclusions,
            reverse = params.reverseTree,
            colors = colors
          )
        else if (params.conflicts) {
          val conflicts = Conflict(res)
          val messages = Print.conflicts(conflicts)
          if (messages.isEmpty) {
            if ((printResultStdout && params.output.verbosity >= 1) || params.output.verbosity >= 2)
              stderr.println("No conflict found.")
            ""
          } else
            messages.mkString("\n")
        } else
          Print.dependenciesUnknownConfigs(
            if (params.classpathOrder) res.orderedDependencies else res.minDependencies.toVector,
            res.projectCache.mapValues { case (_, p) => p },
            printExclusions = withExclusions,
            reorder = !params.classpathOrder
          )

      if (depsStr.nonEmpty) {
        if (printResultStdout)
          stdout.println(depsStr)
        else
          stderr.println(depsStr)
      }
    }

}
