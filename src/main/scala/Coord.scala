case class Coord(x: Double, y: Double, z: Double) {
  def +(other: Coord): Coord = Coord(x + other.x, y + other.y, z + other.z)

  def -(other: Coord): Coord = Coord(x - other.x, y - other.y, z - other.z)

  def dot(other: Coord): Double = x * other.x + y * other.y + z * other.z

  def magnitude: Double = Math.sqrt(this.dot(this))

  def normalize: Coord = {
    val m = magnitude
    if (m == 0) this else Coord(x / m, y / m, z / m)
  }

  // Alias for normalize to match common naming conventions
  def normalized: Coord = normalize

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

/**
 * Represents viewport changes from user input.
 * Uses a state-based approach similar to Rotation.
 */
case class ViewportDelta(
  zoomFactor: Double = 1.0,        // 1.0 = no change, >1 = zoom in, <1 = zoom out
  panOffset: Coord = Coord.ZERO    // Offset to pan the viewport center
) {
  require(zoomFactor > 0, "Zoom factor must be positive")
  
  // Check if this represents no change
  def isIdentity: Boolean = zoomFactor == 1.0 && panOffset == Coord.ZERO
  
  // Combine two viewport deltas
  def combine(other: ViewportDelta): ViewportDelta = ViewportDelta(
    zoomFactor = this.zoomFactor * other.zoomFactor,
    panOffset = this.panOffset + other.panOffset
  )
}

object ViewportDelta {
  val IDENTITY = ViewportDelta()
  
  // Common viewport operations
  def zoomIn(factor: Double = 1.2): ViewportDelta = ViewportDelta(zoomFactor = factor)
  def zoomOut(factor: Double = 0.8): ViewportDelta = ViewportDelta(zoomFactor = factor)
  def pan(offset: Coord): ViewportDelta = ViewportDelta(panOffset = offset)
}
