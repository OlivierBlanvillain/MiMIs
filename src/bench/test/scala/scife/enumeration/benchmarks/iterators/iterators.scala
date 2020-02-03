package scife.enumeration.benchmarks.iterators

object Iterators {
  import scala.language.implicitConversions

  implicit def anyToIterator[A](a: A): Iterator[A] = {
    Seq(a).iterator
  }

  implicit class WithOr[A](val base: Iterator[A]) {
    private sealed trait State
    private case object OnLeft extends State
    private case object OnRight extends State
    private case object Empty extends State

    // effectively does ++, but lazily
    def or[B >: A](otherRaw: => Iterator[B]): Iterator[B] = {
      lazy val other = otherRaw

      new Iterator[B] {
	private var state: State = OnLeft

	def hasNext: Boolean = {
	  state match {
	    case OnLeft => {
	      if (base.hasNext) {
		true
	      } else {
		state = OnRight
		hasNext
	      }
	    }
	    case OnRight => {
	      if (other.hasNext) {
		true
	      } else {
		state = Empty
		hasNext
	      }
	    }
	    case Empty => false
	  }
	} // hasNext

	def next(): B = {
	  if (!hasNext) {
	    throw new NoSuchElementException("Empty Or")
	  } else {
	    state match {
	      case OnLeft => base.next()
	      case OnRight => other.next()
	      case Empty => {
		assert(false)
		throw new NoSuchElementException("Empty Or")
	      }
	    }
	  }
	} // next
      }
    } // or
  } // WithOr
} // Iterators

object EmptyIterator extends Iterator[Nothing] {
  def hasNext: Boolean = false
  def next(): Nothing = throw new NoSuchElementException("EmptyIterator.next")
} // EmptyIterator
