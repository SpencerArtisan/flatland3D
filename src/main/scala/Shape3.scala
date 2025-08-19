
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
    val xWithin = coord.x >= 0 && coord.x < width
    val yWithin = coord.y >= 0 && coord.y < height
    val zWithin = coord.z >= 0 && coord.z < depth
    xWithin && yWithin && zWithin
  }

  override def surfaceNormalAt(local: Coord3): Coord3 = {
    val dx0 = local.x
    val dx1 = width - local.x
    val dy0 = local.y
    val dy1 = height - local.y
    val dz0 = local.z
    val dz1 = depth - local.z
    val minDist = Seq(dx0, dx1, dy0, dy1, dz0, dz1).min
    if (minDist == dx0) Coord3(-1, 0, 0)
    else if (minDist == dx1) Coord3(1, 0, 0)
    else if (minDist == dy0) Coord3(0, -1, 0)
    else if (minDist == dy1) Coord3(0, 1, 0)
    else if (minDist == dz0) Coord3(0, 0, -1)
    else Coord3(0, 0, 1)
  }
}


