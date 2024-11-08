case class Scene(occupiedCells: Seq[Seq[Boolean]]) {
  def render(blockChar: Char = '*', blankChar: Char = '.', xScale: Int = 1): String =
    occupiedCells
      .map(_.map(occupied => (if (occupied) blockChar else blankChar).toString * xScale).mkString)
      .mkString("\n")
}

object Scene {
  def from(world: World): Scene = {
    val rows = 0 until world.height
    val columns = 0 until world.width

    Scene(rows
      .map(row => columns
        .map(column => Coord(column, row))
        .map(cellCoord => world.exists(_.occupiesSpaceAt(cellCoord)))))
  }
}