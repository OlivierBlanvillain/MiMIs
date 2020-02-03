package scife.enumeration.benchmarks.java

// ---BEGIN SYNTACTIC DEFINITION---
case class Variable(name: String)
case class MethodName(name: String)
case class ClassName(name: String)

sealed trait Type {
  def prettyString: String
}
case object IntType extends Type {
  def prettyString: String = "int"
}
case object BooleanType extends Type {
  def prettyString: String = "boolean"
}
case object DoubleType extends Type {
  def prettyString: String = "double"
}
case class ClassType(name: ClassName) extends Type {
  def prettyString: String = name.name
}

case class Call(base: Expression, name: MethodName, params: Seq[Expression]) {
  def prettyString: String = base.prettyString + "." + name.name + Expression.prettyString(params)
}

object Expression {
  def prettyString(params: Seq[Expression]): String = {
    "(" + params.map(_.prettyString).mkString(", ") + ")"
  }
}

sealed trait Expression {
  def prettyString: String
}
case class VariableExpression(variable: Variable) extends Expression {
  def prettyString: String = variable.name
}
case class IntLiteral(value: Int) extends Expression {
  def prettyString: String = value.toString
}
case class BooleanLiteral(value: Boolean) extends Expression {
  def prettyString: String = value.toString
}
case class DoubleLiteral(value: Double) extends Expression {
  def prettyString: String = value.toString
}
case class StringLiteral(value: String) extends Expression {
  def prettyString: String = "\"" + value + "\""
}
case class NewExpression(name: ClassName, params: Seq[Expression]) extends Expression {
  def prettyString: String = "new " + name.name + Expression.prettyString(params)
}
case class CallExpression(call: Call) extends Expression {
  def prettyString: String = call.prettyString
}

sealed trait Statement {
  def prettyString: String
}
case class VariableDeclaration(theType: Type, name: Variable, initializer: Expression) extends Statement {
  def prettyString: String = theType.prettyString + " " + name.name + " = " + initializer.prettyString + ";"
}
case class CallStatement(call: Call) extends Statement {
  def prettyString: String = call.prettyString + ";"
}

case class JavaTest(statements: Seq[Statement]) {
  def prettyString: String = {
    "public class Test {\n" +
    "    public static void main(String[] args) {\n" +
    "        " +
    statements.map(_.prettyString).mkString("\n        ") + "\n" +
    "    }\n" +
    "}\n"
  } // prettyString
} // JavaTest
// ---END SYNTACTIC DEFINITION---

// ---BEGIN STUFF FOR SPECIFYING LIBRARIES OF INTEREST---
case class ClassSpec(name: ClassName,
		     extendsImplements: Set[ClassName],
		     constructors: List[ConstructorSpec],
		     methods: List[MethodSpec])
case class ConstructorSpec(params: List[Type])
// return type of None indicates void
case class MethodSpec(name: MethodName, params: List[Type], returnType: Option[Type])
// ---END STUFF FOR SPECIFYING LIBRARIES OF INTEREST---
