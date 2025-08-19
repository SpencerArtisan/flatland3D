
case class Coord3(x: Double, y: Double, z: Double) {
  def +(other: Coord3): Coord3 = Coord3(x + other.x, y + other.y, z + other.z)

  def -(other: Coord3): Coord3 = Coord3(x - other.x, y - other.y, z - other.z)
}

object Coord3 {
  val ZERO: Coord3 = Coord3(0, 0, 0)
}


