case class Coord(x: Double, y: Double, z: Double) {
  def +(other: Coord): Coord = Coord(x + other.x, y + other.y, z + other.z)

  def -(other: Coord): Coord = Coord(x - other.x, y - other.y, z - other.z)

  def dot(other: Coord): Double = x * other.x + y * other.y + z * other.z

  def magnitude: Double = Math.sqrt(this.dot(this))

  def normalize: Coord = {
    val m = magnitude
    if (m == 0) this else Coord(x / m, y / m, z / m)
  }

  def *(scalar: Double): Coord = Coord(x * scalar, y * scalar, z * scalar)

  def cross(other: Coord): Coord = Coord(
    y * other.z - z * other.y,
    z * other.x - x * other.z,
    x * other.y - y * other.x
  )
}

object Coord {
  val ZERO: Coord = Coord(0, 0, 0)
  
  // Utility methods
  def distance(from: Coord, to: Coord): Double = (to - from).magnitude
  
  def midpoint(a: Coord, b: Coord): Coord = Coord(
    (a.x + b.x) / 2,
    (a.y + b.y) / 2,
    (a.z + b.z) / 2
  )
  
  def lerp(a: Coord, b: Coord, t: Double): Coord = {
    require(t >= 0.0 && t <= 1.0, "Interpolation factor must be between 0 and 1")
    Coord(
      a.x + (b.x - a.x) * t,
      a.y + (b.y - a.y) * t,
      a.z + (b.z - a.z) * t
    )
  }
}
