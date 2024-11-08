case class Dot(id: Int) extends Shape {
  val center: Coord = Coord.ZERO

  def occupiesSpaceAt(coord: Coord): Boolean =
    coord == center
}
