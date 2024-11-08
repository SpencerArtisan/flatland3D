
case class Rectangle(id: Int, width: Double, height: Double) extends Shape {
  val center: Coord = Coord(width / 2, height / 2)

  def occupiesSpaceAt(coord: Coord): Boolean =
    coord.isWithin(this)
}
