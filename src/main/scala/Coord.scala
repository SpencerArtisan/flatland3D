
case class Coord(x: Double, y: Double) {
  def +(other: Coord): Coord = Coord(x + other.x, y + other.y)

  def -(other: Coord): Coord = Coord(x - other.x, y - other.y)

  def isWithin(rectangle: Rectangle): Boolean = {
    val xWithin = x >= 0 && x < rectangle.width
    val yWithin = y >= 0 && y < rectangle.height
    xWithin && yWithin
  }

  def rotate(radians: Double): Coord = {
    val sinRadians = Math.sin(radians)
    val cosRadians = Math.cos(radians)

    Coord(x * cosRadians - y * sinRadians, x * sinRadians + y * cosRadians)
  }
}

object Coord {
  val ZERO: Coord = Coord(0, 0)
}
