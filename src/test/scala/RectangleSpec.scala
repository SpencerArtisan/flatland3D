import org.scalatest.flatspec._
import org.scalatest.matchers._

class RectangleSpec extends AnyFlatSpec with should.Matchers {

  "A rectangle" should "occupy a fixed width and height" in {
    val rectangle = Rectangle(100, 2, 1)
    rectangle.occupiesSpaceAt(Coord(0, 0)) should be (true)
    rectangle.occupiesSpaceAt(Coord(1, 0)) should be (true)
    rectangle.occupiesSpaceAt(Coord(2, 0)) should be (false)
    rectangle.occupiesSpaceAt(Coord(0, 1)) should be (false)
    rectangle.occupiesSpaceAt(Coord(1, 1)) should be (false)
    rectangle.occupiesSpaceAt(Coord(2, 1)) should be (false)
  }
}