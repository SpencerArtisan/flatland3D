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
    
    // Ensure the box only occupies space at integer coordinates to prevent multiple Z-level rendering
    // Round coordinates to nearest integer to avoid floating-point precision issues
    val roundedCoord = Coord(
      Math.round(coord.x).toDouble,
      Math.round(coord.y).toDouble,
      Math.round(coord.z).toDouble
    )
    
    val xWithin = roundedCoord.x >= -halfWidth && roundedCoord.x <= halfWidth
    val yWithin = roundedCoord.y >= -halfHeight && roundedCoord.y <= halfHeight
    val zWithin = roundedCoord.z >= -halfDepth && roundedCoord.z <= halfDepth
    
    xWithin && yWithin && zWithin
  }

  override def surfaceNormalAt(local: Coord): Coord = {
    val dominantAxis = findDominantAxis(local)
    outwardNormalForAxis(dominantAxis, local)
  }
  
  private def findDominantAxis(local: Coord): String = {
    val absX = Math.abs(local.x)
    val absY = Math.abs(local.y)
    val absZ = Math.abs(local.z)
    
    if (absX >= absY && absX >= absZ) "x"
    else if (absY >= absZ) "y"
    else "z"
  }
  
  private def outwardNormalForAxis(axis: String, local: Coord): Coord = axis match {
    case "x" => if (local.x > 0) Coord(1, 0, 0) else Coord(-1, 0, 0)
    case "y" => if (local.y > 0) Coord(0, 1, 0) else Coord(0, -1, 0)
    case "z" => if (local.z > 0) Coord(0, 0, 1) else Coord(0, 0, -1)
  }
}
