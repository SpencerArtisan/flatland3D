
trait Shape3 {
  def id: Int
  def center: Coord3
  def occupiesSpaceAt(coord: Coord3): Boolean
}

case class Box(id: Int, width: Double, height: Double, depth: Double) extends Shape3 {
  val center: Coord3 = Coord3(width / 2, height / 2, depth / 2)

  def occupiesSpaceAt(coord: Coord3): Boolean = {
    val xWithin = coord.x >= 0 && coord.x < width
    val yWithin = coord.y >= 0 && coord.y < height
    val zWithin = coord.z >= 0 && coord.z < depth
    xWithin && yWithin && zWithin
  }
}


