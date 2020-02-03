package scife
package enumeration
package benchmarks

import dependent._
import memoization._
import scife.{ enumeration => e }
import scife.util._

import scife.util.logging._

import structures._
import TreeShapes._

import org.scalatest._
import org.scalameter.api._

import scala.language.existentials

import scife.enumeration.benchmarks.java._

object JavaScifeGeneration {
  import scife.enumeration.memoization.MemoizationScope
  
  // var hack: List[JavaTest] = List()

  def apply(): JavaScifeGeneration = {
    new JavaScifeGeneration(scife.enumeration.benchmarks.iterators.JavaGeneration.DEFAULT_SPEC)
  } // apply

  def walkThrough(size: Int, memo: MemoizationScope): Long = {
    // hack = List()
    import scife.enumeration.Enum
    import scife.enumeration.dependent.Depend
    import scife.enumeration.benchmarks.BTreeTest

    var numStructures: Long = 0

    val dep: Depend[Int, JavaTest] =
      apply.constructEnumerator(memo)

    val enum = dep.apply(size)
    enum.foreach(s => {
      // hack = s :: hack
      numStructures += 1
    })

    numStructures
  }
} // JavaScifeGeneration

class JavaScifeGeneration(val spec: List[ClassSpec]) extends StructuresBenchmark[Depend[Int, JavaTest]] {
  // for a given class, these are the parents
  val hierarchy: scala.collection.immutable.Map[ClassName, Set[ClassName]] =
    spec.map(s => (s.name, s.extendsImplements)).toMap

  type Env = scala.collection.immutable.Map[Variable, Type]
  type EnumType = Depend[Int, JavaTest]

  def measureCode(tdEnum: EnumType) = {
    { (size: Int) =>
      val enum = tdEnum.getEnum((size))
      for (i <- 0 until enum.size) enum(i)
    }
  }

  def warmUp(inEnum: EnumType, maxSize: Int) {
    for (size <- 1 to maxSize) {
      val enum = inEnum.getEnum((size))
      for (i <- 0 until enum.size) enum(i)
    }
  }

  def parentChild(parent: ClassName, child: ClassName): Boolean = {
    hierarchy.get(child) match {
      case Some(directParents) => {
	if (directParents.contains(parent)) {
	  true
	} else {
	  directParents.exists(directParent => parentChild(parent, directParent))
	}
      }
      case None => false
    }
  } // parentChild

  def debug(msg: String) {
    //println(msg)
  }
  
  def typesCompatibleParentChild(parent: Type, child: Type): Boolean = {
    // debug("TYPES COMPATIBLE PARENT CHILD: " + (parent, child))
    (parent, child) match {
      case (`child`, `parent`) => true
      case (ClassType(parentName), ClassType(childName)) => parentChild(parentName, childName)
      case _ => false
    }
  } // typesCompatibleParentChild

  def constructEnumerator(implicit ms: MemoizationScope) = {
    var makeTest: DependFinite[Int, JavaTest] = null
    var makeStatements: DependFinite[(Int, Env, Int), List[Statement]] = null
    var makeStatement: DependFinite[(Int, Env, Int), (Statement, Env)] = null
    var makeExpression: DependFinite[(Int, Env), (Expression, Type)] = null
    var makeCall: DependFinite[(Int, Env, Boolean), (Call, Option[Type])] = null
    var makeExpressionsOfTypes: DependFinite[(Int, Env, List[Type]), List[Expression]] = null
    var allParents: DependFinite[ClassName, ClassName] = null
    var killSwitch = 0

    allParents = Depend.memoizedFin(
      (self: DependFinite[ClassName, ClassName], of: ClassName) => {
	// debug("ALL PARENTS: " + of)
	hierarchy.get(of) match {
	  case Some(directParents) => {
	    e.Concat(
	      e.Enum(directParents.toList),
	      e.dependent.Chain.single(
		e.Enum(directParents.toList),
		Depend.fin((directParent: ClassName) => {
		  // debug("ALL PARENTS RECURSIVE: " + directParent)
		  self(directParent)
		})))
	  }
	  case None => e.Empty
	}
      })

    makeExpressionsOfTypes = Depend.memoizedFin(
      (self: DependFinite[(Int, Env, List[Type]), List[Expression]], params: (Int, Env, List[Type])) => {
	val (size, env, types) = params
	// debug("MAKE EXPRESSIONS OF TYPES: " + params)
	types match {
	  case Nil => {
	    // debug("MAKE EXPRESSIONS OF TYPES EMPTY")
	    e.Singleton(List())
	  }
	  case head :: tail => {
	    // debug("MAKE EXPRESSIONS OF TYPES NONEMPTY")
	    e.dependent.Chain.single(
	      makeExpression((size, env)),
	      Depend.fin((pair: (Expression, Type)) => {
		// debug("MAKE EXPRESSIONS TYPES COMPATIBLE: " + pair)
		if (typesCompatibleParentChild(head, pair._2)) {
		  self((size, env, tail)).map((rest: List[Expression]) => {
		    // debug("MAKE EXPRESSIONS OF TYPES MAP: " + rest)
		    pair._1 :: rest
		  })
		} else {
		  e.Empty
		}
	      }))
	  }
	}
      })

    makeCall = Depend.memoizedFin(
      (self: DependFinite[(Int, Env, Boolean), (Call, Option[Type])], params: (Int, Env, Boolean)) => {
	val (size, env, voidOk) = params
	// debug("MAKE CALL: " + size)
	e.dependent.Chain.single(
	  makeExpression((size, env)),
	  Depend.fin((pair: (Expression, Type)) => {
	    // debug("MAKE CALL MATCH: " + pair)
	    pair match {
	      case (base, ClassType(myClassName)) => {
		e.dependent.Chain.single(
		  e.Concat(e.Enum(myClassName), allParents(myClassName)),
		  Depend.fin((methodClassName: ClassName) => {
		    // debug("MAKE CALL METHOD CLASS NAME: " + methodClassName)
		    e.dependent.Chain.single(
		      e.Enum(spec.find(_.name == methodClassName).get.methods.toList),
		      Depend.fin((methodSpec: MethodSpec) => {
			// debug("MAKE CALL METHOD SPEC FIN: " + methodSpec)
			if (methodSpec.returnType.isDefined || voidOk) {
			  makeExpressionsOfTypes((size, env, methodSpec.params)).map(
			    (params: List[Expression]) => {
			      // debug("MAKE CALL PARAMS: " + params)
			      (Call(base, methodSpec.name, params), methodSpec.returnType)
			    })
			} else {
			  e.Empty
			}
		      }))
		  }))
	      }
	      case _ => e.Empty
	    }
	  }))
      })

    makeExpression = Depend.memoizedFin(
      (self: DependFinite[(Int, Env), (Expression, Type)], params: (Int, Env)) => {
	val (size, env) = params
	// debug("MAKE EXPRESSION: " + size)

	def baseCases: List[(Expression, Type)] =
	  ((IntLiteral(0), IntType) ::
	   (BooleanLiteral(false), BooleanType) ::
	   (DoubleLiteral(0.0), DoubleType) ::
	   (StringLiteral("foo"), ClassType(ClassName("String"))) ::
	   env.toList.map((pair: (Variable, Type)) => (VariableExpression(pair._1), pair._2)))

	if (size <= 0) {
	  // debug("MAKE EXPRESSION BASE CASES")
	  e.Enum(baseCases)
	} else {
	  e.Concat(
	    e.Concat(
	      e.Enum(baseCases),
	      e.dependent.Chain.single(
		e.Enum(spec),
		Depend.fin((classSpec: ClassSpec) => {
		  // debug("MAKE EXPRESSION CLASS SPEC: " + classSpec)
		  e.dependent.Chain.single(
		    e.Enum(classSpec.constructors),
		    Depend.fin((constructorSpec: ConstructorSpec) => {
		      // debug("MAKE EXPRESSION CONSTRUCTOR SPEC: " + constructorSpec)
		      makeExpressionsOfTypes((size - 1, env, constructorSpec.params)).map(
			(params: List[Expression]) => {
			  // debug("MAKE EXPRESSION PARAMS: " + params)
			  (NewExpression(classSpec.name, params), ClassType(classSpec.name))
			})
		    }))
		}))),
	    e.dependent.Chain.single(
	      makeCall((size - 1, env, false)),
	      Depend.fin((pair: (Call, Option[Type])) => {
		// debug("MAKE EXPRESSION PAIR: " + pair)
		pair match {
		  case (call, Some(returnType)) => {
		    e.Singleton((CallExpression(call), returnType))
		  }
		  case _ => e.Empty
		}
	      })))
	}
      })

    makeStatement = Depend.memoizedFin(
      (self: DependFinite[(Int, Env, Int), (Statement, Env)], params: (Int, Env, Int)) => {
	val (size, env, statementNum) = params
	// debug("MAKE STATEMENT: " + size)
	e.Concat(
	  makeExpression((size, env)).map((pair: (Expression, Type)) => {
	    // debug("MAKE STATEMENT PAIR: " + pair)
	    val (initializer, initializerType) = pair
	    val variable = Variable("x" + statementNum)
	    (VariableDeclaration(initializerType, variable, initializer), env + (variable -> initializerType))
	  }),
	  makeCall((size, env, true)).map((pair: (Call, Option[Type])) => {
	    // debug("MAKE STATEMENT CALL PAIR: " + pair)
	    (CallStatement(pair._1), env)
	  }))
      })

    makeStatements = Depend.memoizedFin(
      (self: DependFinite[(Int, Env, Int), List[Statement]], params: (Int, Env, Int)) => {
	val (size, env, numStatements) = params
	// debug("MAKE STATEMENTS: " + size)
	if (numStatements < 0) {
	  // debug("MAKE STATEMENTS EMPTY")
	  e.Empty
	} else if (numStatements == 0) {
	  // debug("MAKE STATEMENTS 0")
	  e.Singleton(List())
	} else {
	  // debug("MAKE STATEMENTS NON-EMPTY: " + numStatements)
	  assert(numStatements > 0)
	  e.dependent.Chain.single(
	    makeStatement((size, env, numStatements)),
	    Depend.fin((pair: (Statement, Env)) => {
	      // debug("MAKE STATEMENTS PAIR: " + pair)
	      self((size, pair._2, numStatements - 1)).map((rest: List[Statement]) => {
		// debug("MAKE STATEMENTS REST: " + rest)
		pair._1 :: rest
	      })
	    }))
	}
      })

    makeTest = Depend.memoizedFin(
      (self: DependFinite[Int, JavaTest], size: Int) => {
	import scife.enumeration.benchmarks.iterators.JavaGeneration.{MIN_NUM_STATEMENTS, MAX_NUM_STATEMENTS}
	e.dependent.Chain.single(
	  Enum(MIN_NUM_STATEMENTS.to(MAX_NUM_STATEMENTS)),
	  Depend.fin((numStatements: Int) => {
	    // debug("MAKE TEST: " + numStatements)
	    makeStatements((size, scala.collection.immutable.Map(), numStatements)).map(
	      (statements: List[Statement]) => {
		// debug("MAKE TEST STATEMENTS: " + statements)
		JavaTest(statements.toSeq)
	      })
	  }))
      })

    makeTest
  }
} // JavaScifeGeneration

