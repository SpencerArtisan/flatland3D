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
    val placementsByFrontToBack = world.placements.toSeq.sortBy(- _.z)

    Scene(rows
      .map(row => columns
        .map(column => Coord(column, row))
        .map { cellCoord =>
          placementsByFrontToBack.exists(_.occupiesSpaceAt(cellCoord))
        }))
  }

  def from(world: World, zOrder: Seq[Int]): Scene = {
    val rows = 0 until world.height
    val columns = 0 until world.width
    val placementsByFrontToBack = {
      if (zOrder.nonEmpty) zOrder.flatMap(z => world.placements.filter(_.z == z))
      else world.placements.toSeq.sortBy(- _.z)
    }

    Scene(rows
      .map(row => columns
        .map(column => Coord(column, row))
        .map { cellCoord =>
          placementsByFrontToBack.exists(_.occupiesSpaceAt(cellCoord))
        }))
  }

  def renderWith(world: World,
                 charFor: Placement => Char,
                 blankChar: Char = '.',
                 xScale: Int = 1,
                 zOrder: Seq[Int] = Nil): String = {
    val rows = 0 until world.height
    val columns = 0 until world.width
    val placementsByFrontToBack = {
      if (zOrder.nonEmpty) zOrder.flatMap(z => world.placements.filter(_.z == z))
      else world.placements.toSeq.sortBy(- _.z)
    }

    rows
      .map { row =>
        columns
          .map { column =>
            val cellCoord = Coord(column, row)
            val ch = placementsByFrontToBack
              .find(_.occupiesSpaceAt(cellCoord))
              .map(charFor)
              .getOrElse(blankChar)
            ch.toString * xScale
          }
          .mkString
      }
      .mkString("\n")
  }
}