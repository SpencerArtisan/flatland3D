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
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
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
    val halfWidth = width / 2
    val halfHeight = height / 2
    val halfDepth = depth / 2
    
    findClosestFaceNormal(local, halfWidth, halfHeight, halfDepth)
  }
  
  private def findClosestFaceNormal(point: Coord, halfWidth: Double, halfHeight: Double, halfDepth: Double): Coord = {
    val tolerance = 1.0
    
    if (Math.abs(point.x - halfWidth) < tolerance) Coord(1, 0, 0)
    else if (Math.abs(point.x + halfWidth) < tolerance) Coord(-1, 0, 0)
    else if (Math.abs(point.y - halfHeight) < tolerance) Coord(0, 1, 0) 
    else if (Math.abs(point.y + halfHeight) < tolerance) Coord(0, -1, 0)
    else if (Math.abs(point.z - halfDepth) < tolerance) Coord(0, 0, 1)
    else if (Math.abs(point.z + halfDepth) < tolerance) Coord(0, 0, -1)
    else {
      val distancesToFaces = Seq(
        (Math.abs(point.x - halfWidth), Coord(1, 0, 0)),
        (Math.abs(point.x + halfWidth), Coord(-1, 0, 0)),
        (Math.abs(point.y - halfHeight), Coord(0, 1, 0)),
        (Math.abs(point.y + halfHeight), Coord(0, -1, 0)),
        (Math.abs(point.z - halfDepth), Coord(0, 0, 1)),
        (Math.abs(point.z + halfDepth), Coord(0, 0, -1))
      )
      distancesToFaces.minBy(_._1)._2
    }
  }

}
