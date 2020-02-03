package scife.enumeration.benchmarks.iterators

import scife.enumeration.benchmarks.java._

// Generates Java expressions and statements with the intention
// of testing APIs.  Assumes there are existing classes around.
// Only interested in creating new objects and calling methods.
// Does not handle generics.

object JavaGeneration {
  type Env = Map[Variable, Type]

  val MIN_NUM_STATEMENTS = 1
  val MAX_NUM_STATEMENTS = 1

  val objectClass = ClassName("Object")

  val DEFAULT_SPEC: List[ClassSpec] =
    List(ClassSpec(objectClass,
		   Set(),
		   List(ConstructorSpec(List())),
		   List()),
	 ClassSpec(ClassName("CharSequence"),
		   Set(objectClass),
		   List(),
		   List(MethodSpec(MethodName("length"),
				   List(),
				   Some(IntType)))),
	 ClassSpec(ClassName("String"),
		   Set(objectClass, ClassName("CharSequence")),
		   List(ConstructorSpec(List())),
		   List(MethodSpec(MethodName("concat"),
				   List(ClassType(ClassName("String"))),
				   Some(ClassType(ClassName("String")))))),
	 ClassSpec(ClassName("java.util.ArrayList"),
		   Set(objectClass),
		   List(ConstructorSpec(List()),
			ConstructorSpec(List(IntType))),
		   List(MethodSpec(MethodName("size"),
				   List(),
				   Some(IntType)),
			MethodSpec(MethodName("add"),
				   List(ClassType(objectClass)),
				   Some(BooleanType)),
			MethodSpec(MethodName("remove"),
				   List(ClassType(objectClass)),
				   Some(BooleanType)))))

  def disjuncts[A](iterators: Iterator[A]*): Iterator[A] = {
    iterators.reduceRight(_ ++ _)
  } // disjuncts

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val bound = args(0).toInt
      val generator = apply(1000)
      generator.makeTest(bound).foreach(test => println(test.prettyString))
    }
  } // main

  def apply(k: Int): JavaGeneration = {
    new JavaGeneration(DEFAULT_SPEC) {
      def makeCache[K, V]: IteratorCache[K, V] = new RealCacheForceSize(k)
    }
  } // apply
}
import JavaGeneration._

abstract class JavaGeneration(val spec: List[ClassSpec]) {
  // for a given class, these are the parents
  val hierarchy: Map[ClassName, Set[ClassName]] =
    spec.map(s => (s.name, s.extendsImplements)).toMap

  val allParentsCache: IteratorCache[ClassName, ClassName] = makeCache
  val makeExpressionsOfTypesCache: IteratorCache[(Int, Env, List[Type]), List[Expression]] = makeCache
  val makeCallCache: IteratorCache[(Int, Env, Boolean), (Call, Option[Type])] = makeCache
  val makeExpressionCache: IteratorCache[(Int, Env), (Expression, Type)] = makeCache
  val makeStatementCache: IteratorCache[(Int, Env, Int), (Statement, Env)] = makeCache
  val makeStatementsCache: IteratorCache[(Int, Env, Int), List[Statement]] = makeCache
  val makeTestCache: IteratorCache[Int, JavaTest] = makeCache

  def allCaches = (allParentsCache,
		   makeExpressionsOfTypesCache,
		   makeCallCache,
		   makeExpressionCache,
		   makeStatementCache,
		   makeStatementsCache,
		   makeTestCache)

  def makeCache[K, V]: IteratorCache[K, V]

  def allParents(of: ClassName): Iterator[ClassName] = {
    allParentsCache.tryCache(of, {
      hierarchy.get(of) match {
	case Some(directParents) => {
	  disjuncts(directParents.iterator,
		    directParents.iterator.flatMap(allParents))
	}
	case None => Iterator()
      }
    })
  } // sameAndAllParents

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

  def typesCompatibleParentChild(parent: Type, child: Type): Boolean = {
    (parent, child) match {
      case (`child`, `parent`) => true
      case (ClassType(parentName), ClassType(childName)) => parentChild(parentName, childName)
      case _ => false
    }
  } // typesCompatibleParentChild

  def makeExpressionsOfTypes(size: Int, env: Env, types: List[Type]): Iterator[List[Expression]] = {
    makeExpressionsOfTypesCache.tryCache((size, env, types), {
      types match {
	case Nil => Iterator(List())
	case head :: tail => {
	  for {
	    (expression, expressionType) <- makeExpression(size, env)
	    // given expression can be a subtype of what we expect
	    if typesCompatibleParentChild(head, expressionType)
	    rest <- makeExpressionsOfTypes(size, env, tail)
	  } yield expression :: rest
	}
      }
    })
  } // makeExpressionsOfTypes

  def makeCall(size: Int, env: Env, voidOk: Boolean): Iterator[(Call, Option[Type])] = {
    makeCallCache.tryCache((size, env, voidOk), {
      for {
	(base, ClassType(myClassName)) <- makeExpression(size, env)
	methodClassName <- disjuncts(Iterator(myClassName), allParents(myClassName))
	MethodSpec(methodName, paramTypes, returnType) <- spec.find(_.name == methodClassName).get.methods.iterator
	if returnType.isDefined || voidOk
	params <- makeExpressionsOfTypes(size, env, paramTypes)
      } yield (Call(base, methodName, params), returnType)
    })
  } // makeCall
  
  def makeExpression(size: Int, env: Env): Iterator[(Expression, Type)] = {
    makeExpressionCache.tryCache((size, env), {
      val baseCases: Iterator[(Expression, Type)] =
	disjuncts(Iterator((IntLiteral(0), IntType)),
		  Iterator((BooleanLiteral(false), BooleanType)),
		  Iterator((DoubleLiteral(0.0), DoubleType)),
		  Iterator((StringLiteral("foo"), ClassType(ClassName("String")))),
		  env.iterator.map(pair => (VariableExpression(pair._1), pair._2)))

      if (size <= 0) {
	baseCases
      } else {
	disjuncts(baseCases,
		  // new
		  for {
		    ClassSpec(className, _, constructors, _) <- spec.iterator
		    ConstructorSpec(paramTypes) <- constructors.iterator
		    params <- makeExpressionsOfTypes(size - 1, env, paramTypes)
		  } yield (NewExpression(className, params), ClassType(className)),
		  // call - non-void
		  for {
		    (call, Some(returnType)) <- makeCall(size - 1, env, false)
		  } yield (CallExpression(call), returnType))
      }
    })
  } // makeExpression
				 
  def makeStatement(size: Int, env: Env, statementNum: Int): Iterator[(Statement, Env)] = {
    makeStatementCache.tryCache((size, env, statementNum), {
      disjuncts(// variable declaration
                for {
		  (initializer, initializerType) <- makeExpression(size, env)
		} yield {
		  val variable = Variable("x" + statementNum)
		  (VariableDeclaration(initializerType, variable, initializer), env + (variable -> initializerType))
		},
                // call - possibly void
                for {
		  (call, _) <- makeCall(size, env, true)
		} yield (CallStatement(call), env))
    })
  } // makeStatement

  def makeStatements(size: Int, env: Env, numStatements: Int): Iterator[List[Statement]] = {
    makeStatementsCache.tryCache((size, env, numStatements), {
      if (numStatements < 0) {
	Iterator()
      } else if (numStatements == 0) {
	Iterator(List())
      } else {
	assert(numStatements > 0)
	for {
	  (statement, newEnv) <- makeStatement(size, env, numStatements)
	  rest <- makeStatements(size, newEnv, numStatements - 1)
	} yield statement :: rest
      }
    })
  } // makeStatements

  def makeTest(size: Int): Iterator[JavaTest] = {
    makeTestCache.tryCache(size, {
      for {
	numStatements <- MIN_NUM_STATEMENTS.to(MAX_NUM_STATEMENTS).iterator
	statements <- makeStatements(size, Map(), numStatements)
      } yield JavaTest(statements.toSeq)
    })
  } // makeTest
} // JavaGeneration

