// Axis-aligned bounding box for efficient intersection testing
case class BoundingBox(min: Coord, max: Coord) {
  def center: Coord = (min + max) * 0.5
  def size: Coord = max - min
  
  // Test if a ray intersects this box
  def intersectRay(origin: Coord, direction: Coord): Boolean = {
    var tmin = Double.NegativeInfinity
    var tmax = Double.PositiveInfinity
    
    // Test intersection with each axis slab
    for (axis <- 0 until 3) {
      val invD = 1.0 / direction.get(axis)
      var t0 = (min.get(axis) - origin.get(axis)) * invD
      var t1 = (max.get(axis) - origin.get(axis)) * invD
      
      // Swap if necessary to ensure t0 is intersection with near plane
      if (invD < 0) {
        val temp = t0
        t0 = t1
        t1 = temp
      }
      
      tmin = math.max(tmin, t0)
      tmax = math.min(tmax, t1)
      
      if (tmax <= tmin) return false
    }
    
    true
  }
  
  // Combine two boxes
  def union(other: BoundingBox): BoundingBox = {
    BoundingBox(
      Coord(
        math.min(min.x, other.min.x),
        math.min(min.y, other.min.y),
        math.min(min.z, other.min.z)
      ),
      Coord(
        math.max(max.x, other.max.x),
        math.max(max.y, other.max.y),
        math.max(max.z, other.max.z)
      )
    )
  }
  
  // Surface area for SAH calculations
  def surfaceArea: Double = {
    val d = size
    2.0 * (d.x * d.y + d.y * d.z + d.z * d.x)
  }
}

object BoundingBox {
  // Compute bounding box for a triangle
  def fromTriangle(triangle: Triangle): BoundingBox = {
    val min = Coord(
      math.min(math.min(triangle.v0.x, triangle.v1.x), triangle.v2.x),
      math.min(math.min(triangle.v0.y, triangle.v1.y), triangle.v2.y),
      math.min(math.min(triangle.v0.z, triangle.v1.z), triangle.v2.z)
    )
    val max = Coord(
      math.max(math.max(triangle.v0.x, triangle.v1.x), triangle.v2.x),
      math.max(math.max(triangle.v0.y, triangle.v1.y), triangle.v2.y),
      math.max(math.max(triangle.v0.z, triangle.v1.z), triangle.v2.z)
    )
    BoundingBox(min, max)
  }
  
  // Compute bounding box for a sequence of triangles
  def fromTriangles(triangles: Seq[Triangle]): BoundingBox = {
    require(triangles.nonEmpty, "Cannot create bounding box from empty triangle sequence")
    triangles.map(fromTriangle).reduce(_.union(_))
  }
}
