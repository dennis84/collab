package collab

object Rand {

  import scala.util.Random

  private val chars: IndexedSeq[Char] = (('0' to '9') ++ ('a' to 'z'))
  private val nbChars = chars.size

  def nextString(len: Int) = List.fill(len)(nextChar).mkString

  def nextChar = chars(Random nextInt nbChars)
}
