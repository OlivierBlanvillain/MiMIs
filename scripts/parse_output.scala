sealed trait DataStructure
case object BST extends DataStructure
case object RBT extends DataStructure
case object Heap extends DataStructure
case object BTree extends DataStructure
case object Riff extends DataStructure
case object Java extends DataStructure

object Cell {
  import java.text.{NumberFormat, DecimalFormat}
  val intFormatter = NumberFormat.getIntegerInstance
  val speedupFormatter = new DecimalFormat("###,###,###.00")

  def printNum(me: Long, other: Long, divBy: Long = 1) {
    if (me == other) {
      print("\\hog (1.0x) ")
    } else if (me < other) {
      val speedup = other.asInstanceOf[Double] / me.asInstanceOf[Double]
      print("\\hog (")
      print(speedupFormatter.format(speedup))
      print("x) ")
    }
    print(intFormatter.format(me))
  }

  def printStreamGenRate(rate: Option[Cell]) {
    rate match {
      case Some(Time(time)) => print(intFormatter.format(time))
      case Some(OOM) => print("OOM")
      case Some(Timeout) => print("Timeout")
      case None => print("N/A")
      case _ => {
	assert(false)
      }
    }
  }

  def printCell(me: Option[Cell], other: Option[Cell]) {
    (me, other) match {
      case (Some(Time(meTime)), Some(Time(otherTime))) => {
        printNum(meTime, otherTime)
      }
      case (Some(MemUsage(meUsage)), Some(MemUsage(otherUsage))) => {
        printNum(meUsage, otherUsage, 1024)
      }
      case (Some(Time(meTime)), Some(OOM | Timeout)) => {
        print("\\hog ")
        print(intFormatter.format(meTime))
      }
      case (Some(Time(meTime)), None) => {
        print(intFormatter.format(meTime))
      }
      case (Some(MemUsage(meUsage)), None | Some(OOM | Timeout)) => {
        print("\\hog ")
        print(intFormatter.format(meUsage))
      }
      case (Some(OOM), _) => {
        print("OOM")
      }
      case (Some(Timeout), _) => {
        print("Timeout")
      }
      case (None, _) => {
        print("N/A")
      }
      case _ => {
        assert(false)
      }
    }
  }
}
sealed trait Cell {
  def isSuccessful: Boolean
}
case object OOM extends Cell {
  def isSuccessful: Boolean = false
}
case object Timeout extends Cell {
  def isSuccessful: Boolean = false
}
case class Time(ms: Long) extends Cell {
  def isSuccessful: Boolean = true
}
case class MemUsage(kb: Long) extends Cell {
  def isSuccessful: Boolean = true
}

object Table {
  type Bound = Int

  def printRow(dataStructureName: String, mapping: Map[Bound, Row]) {
    val printRows = mapping.filter(_._2.atLeastOneSuccessful)
    val numRows = printRows.size
    println("\\hline")
    print("\\multirow{" + numRows + "}{*}{\\textbf{" + dataStructureName + "}}")
    printRows.keys.toSeq.sorted.foreach(bound => {
      print(" & " + bound + " & \\unk & ")
      mapping(bound).printRow()
    })
  }
}
import Table._

case class Table(
  mapping: Map[DataStructure, Map[Bound, Row]]) {

  def getError(dataStructure: DataStructure, bound: Bound, f: Row => Cell): Cell = {
    f(getRow(dataStructure, bound)) match {
      case res@(OOM | Timeout) => res
      case other => {
	assert(false)
	null
      }
    }
  }

  def getRow(dataStructure: DataStructure, bound: Bound): Row = {
    mapping(dataStructure)(bound)
  }

  def update(
    dataStructure: DataStructure,
    bound: Bound,
    row: Row): Table = {
    
    if (!mapping.contains(dataStructure)) {
      copy(mapping = mapping + (dataStructure -> Map(bound -> row)))
    } else {
      val newRow = mapping(dataStructure).get(bound).map(_.mergeWith(row)).getOrElse(row)
      val newInner = mapping(dataStructure) + (bound -> newRow)
      copy(mapping = mapping + (dataStructure -> newInner))
    }
  }

  def printTable() {
    import Table.printRow
    mapping.get(BST).foreach(inner =>
      printRow("BSTs", inner))
    mapping.get(RBT).foreach(inner =>
      printRow("RBTs", inner))
    mapping.get(Heap).foreach(inner =>
      printRow("Heaps", inner))
    mapping.get(BTree).foreach(inner =>
      printRow("B-Trees", inner))
    mapping.get(Riff).foreach(inner =>
      printRow("Riff", inner))
    mapping.get(Java).foreach(inner =>
      printRow("Java", inner))
  }      
} // Table

object Row {
  val empty = Row(None, None, None, None, None, None, None)

  def merge[A](op1: Option[A], op2: Option[A]): Option[A] = {
    (op1, op2) match {
      case (Some(a), Some(b)) => {
	if (a == b) {
	  Some(a)
	} else {
	  assert(false)
	  None
	}
      }
      case (s@Some(_), None) => s
      case (None, s@Some(_)) => s
      case (None, None) => None
    }
  }
}

case class Row(
  streamsGenRate: Option[Cell],
  senniClp: Option[Cell],
  kyleClp: Option[Cell],
  scifeGenRate: Option[Cell],
  mimiGenRate: Option[Cell],
  scifeMemUsage: Option[Cell],
  mimiMemUsage: Option[Cell]) {

  def atLeastOneSuccessful: Boolean = {
    def successful(op: Option[Cell]): Boolean = {
      op.map(_.isSuccessful).getOrElse(false)
    }
    Seq(
      streamsGenRate,
      senniClp,
      kyleClp,
      scifeGenRate,
      mimiGenRate,
      scifeMemUsage,
      mimiMemUsage).exists(successful)
  }

  def mergeWith(other: Row): Row = {
    import Row.merge
    Row(merge(streamsGenRate, other.streamsGenRate),
	merge(senniClp, other.senniClp),
	merge(kyleClp, other.kyleClp),
	merge(scifeGenRate, other.scifeGenRate),
	merge(mimiGenRate, other.mimiGenRate),
	merge(scifeMemUsage, other.scifeMemUsage),
	merge(mimiMemUsage, other.mimiMemUsage))
  }

  def printRow() {
    import Cell.printCell

    def printSep() {
      print(" & ")
    }

    Cell.printStreamGenRate(streamsGenRate)
    printSep()
    printCell(senniClp, kyleClp)
    printSep()
    printCell(kyleClp, senniClp)
    printSep()
    printCell(scifeGenRate, mimiGenRate)
    printSep()
    printCell(mimiGenRate, scifeGenRate)
    printSep()
    printCell(scifeMemUsage, mimiMemUsage)
    printSep()
    printCell(mimiMemUsage, scifeMemUsage)
    println(" \\\\")
  }
} // Row

object ParseOutput {
  type Parser = (Table, List[String]) => Option[(Table, List[String])]
  val HeaderRegex = """^([^;]+); ([^;]+); (\d+)$""".r
  val NumStructuresRegex = """^Num structures: (\d+)$""".r
  val TimeRegex = """^(\d+) ms""".r
  val SpaceRegex = """^(\d+) kB$""".r
  val SkipRegex = """^Skipping bound (\d+) for (.+) due to previously-detected error$""".r
  val ClpTimeRegex = """^([0-9\.]+)user.*$""".r

  def parseException(lines: List[String]): Option[(Cell, List[String])] = {
    lines match {
      case head :: tail => {
	lazy val rest = tail.dropWhile(_.startsWith("\t"))
	if (head.contains("TimeoutException")) {
	  Some((Timeout, rest))
	} else if (head.contains("OutOfMemoryError")) {
	  // can have a timeout after OOM
	  parseException(rest) match {
	    case Some((_, finalRest)) => Some((OOM, finalRest))
	    case None => Some((OOM, rest))
	  }
	} else {
	  None
	}
      }
      case _ => None
    }
  }

  def parseError(
    benchmarkName: String,
    generatorName: String,
    dataStructure: DataStructure,
    f: Cell => Row): Parser = {

    (table, lines) => {
      lines match {
	case HeaderRegex(`benchmarkName`, `generatorName`, bound) :: tail => {
	  for {
	    (cell, "Error detected" :: rest) <- parseException(tail)
	  } yield {
	    val newTable =
	      table.update(
		dataStructure,
		bound.toInt,
		f(cell))
	    (newTable, rest)
	  }
	}
	case _ => None
      }
    }
  }

  def parseSkip(
    dataStructure: DataStructure,
    skipProbe: String,
    extractError: Row => Cell,
    withGenRate: (Row, Option[Cell]) => Row,
    withMemUsage: (Row, Option[Cell]) => Row): Parser = {

    (table, lines) => {
      lines match {
	case SkipRegex(bound, `skipProbe`) :: rest => {
	  val curBound = bound.toInt
	  val error = table.getError(
	    dataStructure,
	    curBound - 1,
	    extractError)
	  val newTable =
	    table.update(
	      dataStructure,
	      curBound,
	      withMemUsage(
		withGenRate(Row.empty, Some(error)),
		Some(error)))
	  Some((newTable, rest))
	}
	case _ => None
      }
    }
  }

  def parseScala(
    benchmarkName: String,
    generatorName: String,
    skipProbe: String,
    dataStructure: DataStructure,
    extractError: Row => Cell,
    withGenRate: (Row, Option[Cell]) => Row,
    withMemUsage: (Row, Option[Cell]) => Row): Parser = {

    def mkRow(genRate: Long, memUsage: Long): Row = {
      withMemUsage(
	withGenRate(Row.empty, Some(Time(genRate))),
	Some(MemUsage(memUsage)))
    }

    val handleNormal: Parser = (table, lines) => {
      lines match {
	case HeaderRegex(`benchmarkName`, `generatorName`, bound) :: NumStructuresRegex(_) :: TimeRegex(ms) :: SpaceRegex(kb) :: rest => {
	  val newTable =
	    table.update(
	      dataStructure,
	      bound.toInt,
	      mkRow(ms.toLong, kb.toLong))
	  Some((newTable, rest))
	}
	case _ => None
      }
    }

    val handleSkip: Parser =
      parseSkip(
	dataStructure,
	skipProbe,
	extractError,
	withGenRate,
	withMemUsage)
    
    val handleError: Parser =
      parseError(
	benchmarkName,
	generatorName,
	dataStructure,
	cell =>
	  withMemUsage(
	    withGenRate(Row.empty, Some(cell)),
	    Some(cell)))

    or(handleNormal,
       handleSkip,
       handleError)
  }

  def or2(first: Parser, second: Parser): Parser = {
    (table, lines) => {
      first(table, lines) match {
	case s@Some(_) => s
	case None => second(table, lines)
      }
    }
  }

  def or(parsers: Parser*): Parser = {
    parsers.reduceRight(or2)
  }

  def parseClp(
    benchmarkName: String,
    skipProbe: String,
    dataStructure: DataStructure,
    extractError: Row => Cell,
    withGenRate: (Row, Option[Cell]) => Row): Parser = {

    val handleNormal: Parser = (table, lines) => {
      lines match {
	case HeaderRegex(`benchmarkName`, "CLP", bound) :: ClpTimeRegex(seconds) :: _ :: rest => {
	  val ms = (seconds.toDouble * 1000).asInstanceOf[Long]
	  val newTable =
	    table.update(
	      dataStructure,
	      bound.toInt,
	      withGenRate(Row.empty, Some(Time(ms))))
	  Some((newTable, rest))
	}
	case _ => None
      }
    }

    val handleSkip: Parser =
      parseSkip(
	dataStructure,
	skipProbe,
	extractError,
	withGenRate,
	(row, _) => row)

    val handleError: Parser = (table, lines) => {
      lines match {
	case HeaderRegex(`benchmarkName`, "CLP", bound) :: "Error detected" :: rest => {
	  val newTable =
	    table.update(
	      dataStructure,
	      bound.toInt,
	      withGenRate(Row.empty, Some(Timeout)))
	  Some((newTable, rest))
	}
	case _ => None
      }
    }

    or(handleNormal,
       handleSkip,
       handleError)
  }

  val scifeExtractError: Row => Cell = _.scifeGenRate.get
  val scifeWithGenRate: (Row, Option[Cell]) => Row =
    (row, cell) => row.copy(scifeGenRate = cell)
  val scifeWithMemUsage: (Row, Option[Cell]) => Row  =
    (row, cell) => row.copy(scifeMemUsage = cell)

  val mimiExtractError: Row => Cell = _.mimiGenRate.get
  val mimiWithGenRate: (Row, Option[Cell]) => Row  =
    (row, cell) => row.copy(mimiGenRate = cell)
  val mimiWithMemUsage: (Row, Option[Cell]) => Row  =
    (row, cell) => row.copy(mimiMemUsage = cell)

  val streamExtractError: Row => Cell = _.streamsGenRate.get
  val streamWithGenRate: (Row, Option[Cell]) => Row =
    (row, cell) => row.copy(streamsGenRate = cell)
  val streamWithMemUsage: (Row, Option[Cell]) => Row =
    (row, cell) => row

  val senniExtractError: Row => Cell = _.senniClp.get
  val senniWithGenRate: (Row, Option[Cell]) => Row  =
    (row, cell) => row.copy(senniClp = cell)

  val kyleExtractError: Row => Cell = _.kyleClp.get
  val kyleWithGenRate: (Row, Option[Cell]) => Row  =
    (row, cell) => row.copy(kyleClp = cell)

  def parseStream(benchmarkName: String, dataStructure: DataStructure): Parser = {
    parseScala(
      benchmarkName,
      "Stream",
      "Streams",
      dataStructure,
      streamExtractError,
      streamWithGenRate,
      streamWithMemUsage)
  }

  def parseScife(benchmarkName: String, dataStructure: DataStructure): Parser = {
    parseScala(
      benchmarkName,
      "SciFe",
      "SciFe",
      dataStructure,
      scifeExtractError,
      scifeWithGenRate,
      scifeWithMemUsage)
  }

  def parseMimi(benchmarkName: String, dataStructure: DataStructure): Parser = {
    parseScala(
      benchmarkName,
      "MiMI",
      "MiMIs",
      dataStructure,
      mimiExtractError,
      mimiWithGenRate,
      mimiWithMemUsage)
  }

  def parseSenni(benchmarkName: String, dataStructure: DataStructure): Parser = {
    parseClp(
      benchmarkName,
      "Senni CLP",
      dataStructure,
      senniExtractError,
      senniWithGenRate)
  }

  def parseKyle(benchmarkName: String, dataStructure: DataStructure): Parser = {
    parseClp(
      benchmarkName,
      "Kyle CLP",
      dataStructure,
      kyleExtractError,
      kyleWithGenRate)
  }

  val parseOneBst: Parser =
    or(parseScife("BSTs", BST),
       parseMimi("BSTs", BST),
       parseStream("BSTs", BST),
       parseKyle("kyle_bst", BST),
       parseSenni("senni_bst", BST))

  val parseOneRbt: Parser =
    or(parseScife("RBTs", RBT),
       parseMimi("RBTs", RBT),
       parseStream("RBTs", RBT),
       parseKyle("kyle_rb", RBT),
       parseSenni("senni_rb", RBT))

  val parseOneHeap: Parser =
    or(parseScife("Heaps", Heap),
       parseMimi("Heaps", Heap),
       parseKyle("kyle_heap", Heap),
       parseStream("Heaps", Heap))

  val parseOneBTree: Parser =
    or(parseScife("B-Trees", BTree),
       parseMimi("B-Trees", BTree),
       parseKyle("kyle_btree", BTree),
       parseStream("B-Trees", BTree))

  val parseOneRiff: Parser =
    or(parseScife("Riff", Riff),
       parseMimi("Riff", Riff),
       parseKyle("kyle_riff", Riff),
       parseStream("Riff", Riff))

  val parseOneJava: Parser =
    or(parseScife("Java", Java),
       parseMimi("Java", Java),
       parseStream("Java", Java))

  def parseAll(lines: List[String]): Option[Table] = {
    @scala.annotation.tailrec
    def loop(table: Table, parsers: List[Parser], lines: List[String]): Option[Table] = {
      (lines, parsers) match {
	case (_ :: _, headParser :: restParsers) => {
	  headParser(table, lines) match {
	    case Some((newTable, newLines)) => loop(newTable, parsers, newLines)
	    case None => loop(table, restParsers, lines)
	  }
	}
	case (_, _ :: _ :: Nil) => {
	  println("DID NOT PARSE EVERYTHING")
	  None
	}
	case (Nil, Nil) | (Nil, _ :: Nil) => {
	  Some(table)
	}
	case (head :: _, Nil) => {
	  println("PARSE ERROR AT: " + head)
	  None
	}
	case _ => {
	  assert(false)
	  None
	}
      }
    }

    loop(Table(Map()),
	 List(parseOneBst,
	      parseOneRbt,
	      parseOneHeap,
	      parseOneBTree,
	      parseOneRiff,
	      parseOneJava),
	 lines)
  }

  def usage() {
    println("Takes the file containing data to parse")
  }

  def main(args: Array[String]) {
    if (args.length != 1) {
      usage()
    } else {
      import scala.io.Source
      parseAll(Source.fromFile(args(0)).getLines.toList).foreach(_.printTable())
    }
  }
} // ParseOutput
