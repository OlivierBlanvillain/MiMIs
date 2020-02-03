package scife.enumeration.benchmarks.iterators.simplescala

// ---BEGIN SYNTACTIC DEFINITION---
case class Variable(name: String)
case class FunctionName(name: String)
case class ConstructorName(name: String)
case class UserDefinedTypeName(name: String)
case class Nat(n: Int)

sealed trait Type
case object StringType extends Type
case object BooleanType extends Type
case object IntegerType extends Type
case object UnitType extends Type
case class FunctionType(tau1: Type, tau2: Type) extends Type
case class TupleType(taus: List[Type]) extends Type
case class UserType(un: UserDefinedTypeName) extends Type

sealed trait Exp
case class VariableExp(x: Variable) extends Exp
case class StringExp(str: String) extends Exp
case class BooleanExp(b: Boolean) extends Exp
case class IntExp(i: Int) extends Exp
case object UnitExp extends Exp
case class BinopExp(e1: Exp, op: Binop, e2: Exp) extends Exp
case class FunctionExp(x: Variable, tau: Type, e: Exp) extends Exp
case class AnonCallExp(e1: Exp, e2: Exp) extends Exp
case class NamedCallExp(fn: FunctionName, e: Exp) extends Exp
case class IfExp(e1: Exp, e2: Exp, e3: Exp) extends Exp
case class BlockExp(vals: List[Val], e: Exp) extends Exp
case class TupleExp(es: List[Exp]) extends Exp
case class AccessExp(e: Exp, n: Nat) extends Exp
case class ConstructorExp(cn: ConstructorName, e: Exp) extends Exp
case class MatchExp(e: Exp, cases: List[Case]) extends Exp

case class Val(x: Variable, e: Exp)

sealed trait Case
case class ConstructorCase(cn: ConstructorName, x: Variable, e: Exp) extends Case
case class TupCase(xs: List[Variable], e: Exp) extends Case

case class UserDefinedTypeDef(un: UserDefinedTypeName, cdefs: Seq[ConstructorDefinition])

case class ConstructorDefinition(cn: ConstructorName, tau: Type)

sealed trait Binop
case object BinopPlus extends Binop
case object BinopMinus extends Binop
case object BinopTimes extends Binop
case object BinopDiv extends Binop
case object BinopAnd extends Binop
case object BinopOr extends Binop
case object BinopLT extends Binop
case object BinopLTE extends Binop

case class Def(fn: FunctionName, x: Variable, tau1: Type, tau2: Type, e: Exp)

case class Program(tdefs: Seq[UserDefinedTypeDef], defs: Seq[Def], e: Exp)
// ---END SYNTACTIC DEFINITION---

object SimpleScala {
  type Env = Map[Variable, Type]

  val MIN_NUM_TUPLE = 2
  val MAX_NUM_TUPLE = 4

  val MIN_NUM_VALS = 0
  val MAX_NUM_VALS = 2

  val MIN_NUM_CONSTRUCTORS = 1
  val MAX_NUM_CONSTRUCTORS = 3

  val MIN_NUM_USERDEFS = 0
  val MAX_NUM_USERDEFS = 2

  val MIN_NUM_FUNCTION_DEFS = 0
  val MAX_NUM_FUNCTION_DEFS = 2

  def disjuncts[A](iterators: Iterator[A]*): Iterator[A] = {
    iterators.reduceRight(_ ++ _)
  } // disjuncts

  def makeListChain[A, B](size: Int, b: B, mkA: B => Iterator[(A, B)]): Iterator[(List[A], B)] = {
    if (size < 0) {
      Iterator()
    } else if (size == 0) {
      Iterator((List(), b))
    } else {
      assert(size > 0)
      for {
	(a, newB) <- mkA(b)
	(rest, finalB) <- makeListChain(size - 1, newB, mkA)
      } yield (a :: rest, finalB)
    }
  } // makeListChain

  def makeNumberedList[A](size: Int, mkA: Int => Iterator[A]): Iterator[List[A]] = {
    makeListChain(size, 0, (num: Int) => mkA(num).map(a => (a, num + 1))).map(_._1)
  } // makeNumberedList

  def makeList[A](size: Int, mkA: () => Iterator[A]): Iterator[List[A]] = {
    makeListChain(size, (), (_: Unit) => mkA().map(a => (a, ()))).map(_._1)
  } // makeList

  def mmap[A, B](items: List[A], f: A => Iterator[B]): Iterator[List[B]] = {
    items match {
      case Nil => Iterator(Nil)
      case a :: as => {
	for {
	  b <- f(a)
	  bs <- mmap(as, f)
	} yield b :: bs
      }
    }
  } // mmap

  val initialWithUser = new WithUser(List(), List())

  class WithUser(val tdefs: List[UserDefinedTypeDef], val defs: List[Def]) {
    def makeType(size: Int): Iterator[Type] = {
      val baseCases: Iterator[Type] = disjuncts(Iterator(StringType),
						Iterator(BooleanType),
						Iterator(IntegerType),
						Iterator(UnitType),
						tdefs.iterator.map(tdef => UserType(tdef.un)))
      if (size <= 0) {
	baseCases
      } else {
	disjuncts(baseCases,
		  // function types
	          for {
		    tau1 <- makeType(size - 1)
		    tau2 <- makeType(size - 2)
		  } yield FunctionType(tau1, tau2),
	          // tuple types
	          for {
		    tupleSize <- MIN_NUM_TUPLE.to(MAX_NUM_TUPLE).iterator
		    elements <- makeList(tupleSize, () => makeType(size - 1))
		  } yield TupleType(elements))
      }
    } // makeType

    def makeVal(size: Int, env: Env): Iterator[(Val, Env)] = {
      for {
	variableName <- Iterator("v1", "v2", "v3")
	variable = Variable(variableName)
	(e, eType) <- makeWellTypedExpression(size, env)
      } yield (Val(variable, e), env + (variable -> eType))
    } // makeVal

    def makeWellTypedExpression(size: Int, env: Env): Iterator[(Exp, Type)] = {
      def makeCase(un: UserDefinedTypeName, cn: ConstructorName, variableType: Type): Iterator[(ConstructorCase, Type)] = {
	for {
	  variableName <- Iterator("c1", "c2", "c3")
	  variable = Variable(variableName)
	  (e, eType) <- makeWellTypedExpression(size - 1, env + (variable -> variableType))
	} yield (ConstructorCase(cn, variable, e), eType)
      } // makeCase

      def makePatternMatch(matchOn: Exp, matchOnType: Type): Iterator[(MatchExp, Type)] = {
	matchOnType match {
	  case TupleType(innerTypes) => {
	    val tupleVars = 0.until(innerTypes.size).map(i => Variable("t" + i)).toList
	    for {
	      (e, eType) <- makeWellTypedExpression(size - 1, env ++ tupleVars.zip(innerTypes))
	    } yield (MatchExp(matchOn, List(TupCase(tupleVars, e))), eType)
	  }
	  case UserType(un) => {
	    for {
	      cases <- mmap(tdefs.find(_.un == un).get.cdefs.toList, (cdef: ConstructorDefinition) => makeCase(un, cdef.cn, cdef.tau))
	      if cases.map(_._2).toSet.size == 1
	    } yield (MatchExp(matchOn, cases.map(_._1)), cases.head._2)
	  }
	  case _ => Iterator()
	}
      } // makePatternMatch

      def makeBinop(op: Binop): Iterator[(BinopExp, Type)] = {
	for {
	  (e1, e1Type) <- makeWellTypedExpression(size - 1, env)
	  (e2, e2Type) <- makeWellTypedExpression(size - 1, env)
	  resultExp = BinopExp(e1, op, e2)
	  resultType <- (e1Type, op, e2Type) match {
	    case (StringType, BinopPlus, StringType) => Iterator(StringType)
	    case (IntegerType, BinopPlus | BinopMinus | BinopTimes | BinopDiv, IntegerType) => Iterator(IntegerType)
	    case (BooleanType, BinopAnd | BinopOr, BooleanType) => Iterator(BooleanType)
	    case (IntegerType, BinopLT | BinopLTE, IntegerType) => Iterator(BooleanType)
	    case _ => Iterator()
	  }
	} yield (resultExp -> resultType)
      } // makeBinop

      val baseCases = disjuncts(// variables
                                env.iterator.map(pair => (VariableExp(pair._1) -> pair._2)),
                                // strings
	                        Iterator((StringExp("foo"), StringType)),
                                // booleans
	                        Iterator((BooleanExp(true), BooleanType)),
                                // integers
	                        Iterator((IntExp(0), IntegerType)),
                                // unit
	                        Iterator((UnitExp, UnitType)))
      if (size <= 0) {
	baseCases
      } else {
	disjuncts(baseCases,
                  // binops
	          for {
		    op <- Iterator(BinopPlus,
		  		   BinopMinus,
		  		   BinopTimes,
				   BinopDiv,
				   BinopAnd,
				   BinopOr,
				   BinopLT,
				   BinopLTE)
		    result <- makeBinop(op)
		  } yield result,
	          // higher-order function creation
                  for {
		    variableName <- Iterator("x", "y", "z")
		    variable = Variable(variableName)
		    variableType <- makeType(size - 1)
		    (body, bodyType) <- makeWellTypedExpression(size - 1, env + (variable -> variableType))
		  } yield (FunctionExp(variable, variableType, body), FunctionType(variableType, bodyType)),
	          // higher-order function call
	          for {
		    (callMe, FunctionType(paramType, returnType)) <- makeWellTypedExpression(size - 1, env)
		    (param, `paramType`) <- makeWellTypedExpression(size - 1, env)
		  } yield (AnonCallExp(callMe, param), returnType),
	          // if
	          for {
		    (e1, BooleanType) <- makeWellTypedExpression(size - 1, env)
		    (e2, resultType) <- makeWellTypedExpression(size - 1, env)
		    (e3, `resultType`) <- makeWellTypedExpression(size - 1, env)
		  } yield (IfExp(e1, e2, e3), resultType),
	          // block
	          for {
		    numVals <- MIN_NUM_VALS.to(MAX_NUM_VALS).iterator
		    (vals, innerEnv) <- makeListChain(numVals, env, (curEnv: Env) => makeVal(size - 1, curEnv))
		    (body, bodyType) <- makeWellTypedExpression(size - 1, innerEnv)
		  } yield (BlockExp(vals, body), bodyType),
	          // tuple creation
                  for {
		    tupleSize <- MIN_NUM_TUPLE.to(MAX_NUM_TUPLE).iterator
		    elements <- makeList(tupleSize, () => makeWellTypedExpression(size - 1, env))
		  } yield (TupleExp(elements.map(_._1)), TupleType(elements.map(_._2))),
	          // tuple access
	          for {
		    (e, TupleType(elementTypes)) <- makeWellTypedExpression(size - 1, env)
		    accessPosition <- 0.until(elementTypes.size).iterator
		  } yield (AccessExp(e, Nat(accessPosition + 1)), elementTypes(accessPosition)),
	          // constructor application
	          for {
		    UserDefinedTypeDef(un, cdefs) <- tdefs.iterator
		    ConstructorDefinition(cn, paramType) <- cdefs.iterator
		    (param, `paramType`) <- makeWellTypedExpression(size - 1, env)
		  } yield (ConstructorExp(cn, param), UserType(un)),
	          // pattern matching
	          for {
		    (matchOn, matchOnType) <- makeWellTypedExpression(size - 1, env)
		    result <- makePatternMatch(matchOn, matchOnType)
		  } yield result)
      }
    } // makeWellTypedExpression

    def makeTDef(size: Int, name: UserDefinedTypeName): Iterator[UserDefinedTypeDef] = {
      def makeCDef(name: ConstructorName): Iterator[ConstructorDefinition] = {
	for {
	  tau <- makeType(size)
	} yield ConstructorDefinition(name, tau)
      } // makeCDef

      for {
	numConstructors <- MIN_NUM_CONSTRUCTORS.to(MAX_NUM_CONSTRUCTORS).iterator
	constructors <- makeNumberedList(numConstructors, num => makeCDef(ConstructorName(name.name + "_C" + num)))
      } yield UserDefinedTypeDef(name, constructors.toSeq)
    } // makeTDef

    def addTDef(size: Int, name: UserDefinedTypeName): Iterator[WithUser] = {
      for {
	add <- makeTDef(size, name)
      } yield new WithUser(add :: tdefs, defs)
    } // addTDef

    def addTDefTimes(size: Int, numToAdd: Int): Iterator[WithUser] = {
      if (numToAdd <= 0) {
	Iterator(this)
      } else {
	for {
	  temp <- addTDef(size, UserDefinedTypeName("T" + numToAdd))
	  result <- temp.addTDefTimes(size, numToAdd - 1)
	} yield result
      }
    } // addTDefTimes

    def makeDef(size: Int, name: FunctionName): Iterator[Def] = {
      val variable = Variable("d")
      for {
	paramType <- makeType(size)
	(e, returnType) <- makeWellTypedExpression(size, Map(variable -> paramType))
      } yield Def(name, variable, paramType, returnType, e)
    } // makeDef

    def addDef(size: Int, name: FunctionName): Iterator[WithUser] = {
      for {
	theDef <- makeDef(size, name)
      } yield new WithUser(tdefs, theDef :: defs)
    } // addDef

    def addDefTimes(size: Int, numToAdd: Int): Iterator[WithUser] = {
      if (numToAdd <= 0) {
	Iterator(this)
      } else {
	for {
	  temp <- addDef(size, FunctionName("f" + numToAdd))
	  result <- temp.addDefTimes(size, numToAdd - 1)
	} yield result
      }
    } // addDefTimes
  } // WithUser

  def makeWithUser(size: Int): Iterator[WithUser] = {
    for {
      numTDefs <- MIN_NUM_USERDEFS.to(MAX_NUM_USERDEFS).iterator
      withUser1 <- initialWithUser.addTDefTimes(size, numTDefs)
      numDefs <- MIN_NUM_FUNCTION_DEFS.to(MAX_NUM_FUNCTION_DEFS).iterator
      withUser2 <- withUser1.addDefTimes(size, numDefs)
    } yield withUser2
  } // makeWithUser

  def makeWellTypedProgram(size: Int): Iterator[(Program, Type)] = {
    for {
      withUser <- makeWithUser(size)
      (e, eType) <- withUser.makeWellTypedExpression(size, Map())
    } yield (Program(withUser.tdefs, withUser.defs, e) -> eType)
  } // makeWellTypedProgram

  def main(args: Array[String]) {
    if (args.length != 1) {
      println("Needs a size bound")
    } else {
      val bound = args(0).toInt
      var size: Long = 0
      makeWellTypedProgram(bound).foreach(_ => size += 1)
      println("NUM PROGRAMS: " + size)
      // makeWellTypedProgram(bound).foreach(
      // 	{ case (program, theType) => println("-----\n" + program + "\n" + theType) })
    }
  } // main
} // SimpleScala

