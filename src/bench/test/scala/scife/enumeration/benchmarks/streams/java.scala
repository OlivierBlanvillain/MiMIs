package scife.enumeration.benchmarks.streams

import scife.enumeration.benchmarks.java._

import scala.collection.immutable.Stream

// Generates Java expressions and statements with the intention
// of testing APIs.  Assumes there are existing classes around.
// Only interested in creating new objects and calling methods.
// Does not handle generics.

object StreamJavaGeneration {
  def disjuncts[A](streams: Stream[A]*): Stream[A] = {
    streams.reduceRight(_ ++ _)
  }

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val bound = args(0).toInt
      val generator = apply()
      println("NUM ELEMENTS: " + generator.makeTest(bound).toList.size)
    }
  } // main

  def apply(): StreamJavaGeneration = {
    new StreamJavaGeneration(scife.enumeration.benchmarks.iterators.JavaGeneration.DEFAULT_SPEC)
  } // apply
}
import StreamJavaGeneration._

class StreamJavaGeneration(val spec: List[ClassSpec]) {
  import scife.enumeration.benchmarks.iterators.JavaGeneration.{
    Env,
    MIN_NUM_STATEMENTS,
    MAX_NUM_STATEMENTS
  }

  // for a given class, these are the parents
  val hierarchy: Map[ClassName, Set[ClassName]] =
    spec.map(s => (s.name, s.extendsImplements)).toMap

  def allParents(of: ClassName): Stream[ClassName] = {
    hierarchy.get(of) match {
      case Some(directParents) => {
	val asStream = directParents.toStream
	disjuncts(asStream,
		  asStream.flatMap(allParents))
      }
      case None => Stream.Empty
    }
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

  def makeExpressionsOfTypes(size: Int, env: Env, types: List[Type]): Stream[List[Expression]] = {
    types match {
      case Nil => Stream(List())
      case head :: tail => {
	for {
	  (expression, expressionType) <- makeExpression(size, env)
	  // given expression can be a subtype of what we expect
	  if typesCompatibleParentChild(head, expressionType)
	  rest <- makeExpressionsOfTypes(size, env, tail)
	} yield expression :: rest
      }
    }
  } // makeExpressionsOfTypes

  def makeCall(size: Int, env: Env, voidOk: Boolean): Stream[(Call, Option[Type])] = {
    for {
      (base, ClassType(myClassName)) <- makeExpression(size, env)
      methodClassName <- (myClassName #:: allParents(myClassName))
      MethodSpec(methodName, paramTypes, returnType) <- spec.find(_.name == methodClassName).get.methods.toStream
      if returnType.isDefined || voidOk
      params <- makeExpressionsOfTypes(size, env, paramTypes)
    } yield (Call(base, methodName, params), returnType)
  } // makeCall
  
  def makeExpression(size: Int, env: Env): Stream[(Expression, Type)] = {
    val baseCases: Stream[(Expression, Type)] =
      ((IntLiteral(0), IntType) #::
       (BooleanLiteral(false), BooleanType) #::
       (DoubleLiteral(0.0), DoubleType) #::
       (StringLiteral("foo"), ClassType(ClassName("String"))) #::
       env.toStream.map(pair => (VariableExpression(pair._1): Expression, pair._2)))

    if (size <= 0) {
      baseCases
    } else {
      disjuncts(baseCases,
		// new
		for {
		  ClassSpec(className, _, constructors, _) <- spec.toStream
		  ConstructorSpec(paramTypes) <- constructors.toStream
		  params <- makeExpressionsOfTypes(size - 1, env, paramTypes)
		} yield (NewExpression(className, params), ClassType(className)),
		// call - non-void
		for {
		  (call, Some(returnType)) <- makeCall(size - 1, env, false)
		} yield (CallExpression(call), returnType))
    }
  } // makeExpression
				 
  def makeStatement(size: Int, env: Env, statementNum: Int): Stream[(Statement, Env)] = {
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
  } // makeStatement

  def makeStatements(size: Int, env: Env, numStatements: Int): Stream[List[Statement]] = {
    if (numStatements < 0) {
      Stream.Empty
    } else if (numStatements == 0) {
      Stream(List())
    } else {
      assert(numStatements > 0)
      for {
	(statement, newEnv) <- makeStatement(size, env, numStatements)
	rest <- makeStatements(size, newEnv, numStatements - 1)
      } yield statement :: rest
    }
  } // makeStatements

  def makeTest(size: Int): Stream[JavaTest] = {
    for {
      numStatements <- MIN_NUM_STATEMENTS.to(MAX_NUM_STATEMENTS).toStream
      statements <- makeStatements(size, Map(), numStatements)
    } yield JavaTest(statements.toSeq)
  } // makeTest
} // JavaGeneration

