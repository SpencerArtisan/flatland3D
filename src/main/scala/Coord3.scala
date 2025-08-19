
case class Coord3(x: Double, y: Double, z: Double) {
  def +(other: Coord3): Coord3 = Coord3(x + other.x, y + other.y, z + other.z)

  def -(other: Coord3): Coord3 = Coord3(x - other.x, y - other.y, z - other.z)

  def dot(other: Coord3): Double = x * other.x + y * other.y + z * other.z

  def magnitude: Double = Math.sqrt(this.dot(this))

  def normalize: Coord3 = {
    val m = magnitude
    if (m == 0) this else Coord3(x / m, y / m, z / m)
  }
}

object Coord3 {
  val ZERO: Coord3 = Coord3(0, 0, 0)
}


