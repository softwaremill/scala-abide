package scala.tools.abide.traversal

import scala.tools.abide._
import scala.reflect.internal._
import scala.reflect.internal.traversal._

object FusingTraversalAnalyzerGenerator extends AnalyzerGenerator {
  def generateAnalyzer(universe : SymbolTable, rules : List[Rule]) : FusingTraversalAnalyzer = {
    val traversalRules = rules.map(_ match {
      case t : TraversalRule => t
      case rule => scala.sys.error("Unexpected rule type for TraversalAnalyzer : " + rule.getClass)
    })

    new FusingTraversalAnalyzer(universe, traversalRules)
  }

  val subsumes : Set[AnalyzerGenerator] = Set(NaiveTraversalAnalyzerGenerator)
}

class FusingTraversalAnalyzer(val universe : SymbolTable, rules : List[TraversalRule]) extends Analyzer {
  import universe._

  private val fused : Option[TraversalFusion { val universe : FusingTraversalAnalyzer.this.universe.type }] =
    if (rules.isEmpty) None else Some(Fuse(universe)(rules.map { rule =>
      if (rule.context.universe != universe)
        scala.sys.error("Missmatch between analyzer and rule universe")

      rule.asInstanceOf[Traversal { val universe : FusingTraversalAnalyzer.this.universe.type }]
    } : _*))

  def apply(tree : Tree) : List[Warning] = fused match {
    case Some(traverser) =>
      traverser.traverse(tree)
      rules.flatMap(_.state.warnings).toList
    case None => Nil
  }
}
