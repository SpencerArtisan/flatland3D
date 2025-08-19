case class Coord(x: Double, y: Double, z: Double) {
  def +(other: Coord): Coord = Coord(x + other.x, y + other.y, z + other.z)

  def -(other: Coord): Coord = Coord(x - other.x, y - other.y, z - other.z)

  def dot(other: Coord): Double = x * other.x + y * other.y + z * other.z

  def magnitude: Double = Math.sqrt(this.dot(this))

  def normalize: Coord = {
    val m = magnitude
    if (m == 0) this else Coord(x / m, y / m, z / m)
  }
}

object Coord {
  val ZERO: Coord = Coord(0, 0, 0)
}
