package coursier.parse

import coursier.core.{Module, ModuleName, Organization, Reconciliation}
import coursier.util.{ModuleMatcher, ModuleMatchers, ValidationNel}
import coursier.util.Traverse._

object ReconciliationParser {
  def reconciliation(input: Seq[String], scalaVersionOrDefault: String): ValidationNel[String, Seq[(ModuleMatchers, Reconciliation)]] =
    DependencyParser.moduleVersions(input, scalaVersionOrDefault).flatMap { elems =>
      elems.validationNelTraverse {
        case (m, v) =>
          ValidationNel.fromEither(reconciliation(m, v))
      }
    }

  private def reconciliation(module: Module, v: String): Either[String, (ModuleMatchers, Reconciliation)] = {
    val m =
      if (module.organization == Organization("*") && module.name == ModuleName("*")) ModuleMatchers.all
      else ModuleMatchers(exclude = Set(ModuleMatcher.all), include = Set(ModuleMatcher(module)))
    v match {
      case "default" => Right(m -> Reconciliation.Default)
      case "relaxed" => Right(m -> Reconciliation.Relaxed)
      case "strict" => Right(m -> Reconciliation.Strict)
      case _ => Left(s"Unknown reconciliation '$v'")
    }
  }
}
