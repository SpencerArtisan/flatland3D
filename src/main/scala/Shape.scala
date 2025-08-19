trait Shape {
  def id: Int
  def center: Coord
  def occupiesSpaceAt(coord: Coord): Boolean

  // Default: estimate local-space normal via occupancy gradient around the local point
  def surfaceNormalAt(local: Coord): Coord = {
    val eps = 0.5
    val gx = (if (occupiesSpaceAt(Coord(local.x + eps, local.y, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x - eps, local.y, local.z))) 1 else 0)
    val gy = (if (occupiesSpaceAt(Coord(local.x, local.y + eps, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x, local.y - eps, local.z))) 1 else 0)
    val gz = (if (occupiesSpaceAt(Coord(local.x, local.y, local.z + eps))) 1 else 0) -
      (if (occupiesSpaceAt(Coord(local.x, local.y, local.z - eps))) 1 else 0)
    val grad = Coord(gx, gy, gz)
    if (grad.magnitude == 0) Coord(0, 0, 1) else grad.normalize
  }
}

case class Box(id: Int, width: Double, height: Double, depth: Double) extends Shape {
  val center: Coord = Coord(width / 2, height / 2, depth / 2)

  def occupiesSpaceAt(coord: Coord): Boolean = {
    // Local coordinates are centered around the box center
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
    // Use no tolerance to ensure the box only occupies space at its exact boundaries
    val tolerance = 0.0  // No tolerance to fix multiple Z-level issue
    val xWithin = coord.x >= -halfWidth - tolerance && coord.x <= halfWidth + tolerance
    val yWithin = coord.y >= -halfHeight - tolerance && coord.y <= halfHeight + tolerance
    val zWithin = coord.z >= -halfDepth - tolerance && coord.z <= halfDepth + tolerance
    
    xWithin && yWithin && zWithin
  }

  override def surfaceNormalAt(local: Coord): Coord = {
    // Local coordinates are centered around the box center
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
    // Calculate distances to each face (positive = inside, negative = outside)
    val dx0 = local.x + halfWidth  // Distance to -X face
    val dx1 = halfWidth - local.x  // Distance to +X face
    val dy0 = local.y + halfHeight // Distance to -Y face
    val dy1 = halfHeight - local.y // Distance to +Y face
    val dz0 = local.z + halfDepth  // Distance to -Z face
    val dz1 = halfDepth - local.z  // Distance to +Z face
    
    // Use a more robust approach: find the face with the smallest distance
    // and add some tolerance to ensure consistent results
    val tolerance = 0.5  // Increased tolerance for more consistent results
    val distances = Seq(
      (dx0, Coord(-1, 0, 0)),      // -X face
      (dx1, Coord(1, 0, 0)),       // +X face
      (dy0, Coord(0, -1, 0)),      // -Y face
      (dy1, Coord(0, 1, 0)),       // +Y face
      (dz0, Coord(0, 0, -1)),      // -Z face
      (dz1, Coord(0, 0, 1))        // +Z face
    )
    
    // Find the face with the smallest distance, with some tolerance for consistency
    val minDist = distances.minBy(_._1)._1
    val closestFaces = distances.filter(d => Math.abs(d._1 - minDist) < tolerance)
    
    // If multiple faces are equally close, prefer the one that's most "inside" the box
    val bestFace = closestFaces.maxBy(_._1)
    bestFace._2
  }
}
