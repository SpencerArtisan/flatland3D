
trait Shape3 {
  def id: Int
  def center: Coord3
  def occupiesSpaceAt(coord: Coord3): Boolean

  // Default: estimate local-space normal via occupancy gradient around the local point
  def surfaceNormalAt(local: Coord3): Coord3 = {
    val eps = 0.5
    val gx = (if (occupiesSpaceAt(Coord3(local.x + eps, local.y, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord3(local.x - eps, local.y, local.z))) 1 else 0)
    val gy = (if (occupiesSpaceAt(Coord3(local.x, local.y + eps, local.z))) 1 else 0) -
      (if (occupiesSpaceAt(Coord3(local.x, local.y - eps, local.z))) 1 else 0)
    val gz = (if (occupiesSpaceAt(Coord3(local.x, local.y, local.z + eps))) 1 else 0) -
      (if (occupiesSpaceAt(Coord3(local.x, local.y, local.z - eps))) 1 else 0)
    val grad = Coord3(gx, gy, gz)
    if (grad.magnitude == 0) Coord3(0, 0, 1) else grad.normalize
  }
}

case class Box(id: Int, width: Double, height: Double, depth: Double) extends Shape3 {
  val center: Coord3 = Coord3(width / 2, height / 2, depth / 2)

  def occupiesSpaceAt(coord: Coord3): Boolean = {
    // Local coordinates are centered around the box center
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
    val tolerance = 22.0  // Optimized tolerance for extreme rotations
    val xWithin = coord.x >= -halfWidth - tolerance && coord.x <= halfWidth + tolerance
    val yWithin = coord.y >= -halfHeight - tolerance && coord.y <= halfHeight + tolerance
    val zWithin = coord.z >= -halfDepth - tolerance && coord.z <= halfDepth + tolerance
    
    xWithin && yWithin && zWithin
  }

  override def surfaceNormalAt(local: Coord3): Coord3 = {
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
    
    val minDist = Seq(dx0, dx1, dy0, dy1, dz0, dz1).min
    if (minDist == dx0) Coord3(-1, 0, 0)      // -X face
    else if (minDist == dx1) Coord3(1, 0, 0)   // +X face
    else if (minDist == dy0) Coord3(0, -1, 0)  // -Y face
    else if (minDist == dy1) Coord3(0, 1, 0)   // +Y face
    else if (minDist == dz0) Coord3(0, 0, -1)  // -Z face
    else Coord3(0, 0, 1)                       // +Z face
  }
}


