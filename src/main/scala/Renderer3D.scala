
object Renderer3D {
  // Orthographic projection along -Z: for each (x,y), scan z from near to far and stop at first hit
  def projectOrthographic(world: World3D,
                          nearToFarZs: Seq[Int] = Nil): Scene = {
    val rows = 0 until world.height
    val columns = 0 until world.width
    val zScan: Seq[Int] = if (nearToFarZs.nonEmpty) nearToFarZs else (world.depth - 1) to 0 by -1

    val occupied = rows.map { row =>
      columns.map { column =>
        zScan.exists { z =>
          val p = Coord3(column, row, z)
          world.placements.exists(_.occupiesSpaceAt(p))
        }
      }
    }
    Scene(occupied)
  }

  def renderWith(world: World3D,
                 charFor: Placement3D => Char,
                 blankChar: Char = '.',
                 xScale: Int = 1,
                 nearToFarZs: Seq[Int] = Nil): String = {
    val rows = 0 until world.height
    val columns = 0 until world.width
    val zScan: Seq[Int] = if (nearToFarZs.nonEmpty) nearToFarZs else (world.depth - 1) to 0 by -1

    rows
      .map { row =>
        columns
          .map { column =>
            val ch = zScan
              .flatMap { z =>
                val p = Coord3(column, row, z)
                world.placements.find(_.occupiesSpaceAt(p)).map(charFor)
              }
              .headOption
              .getOrElse(blankChar)
            ch.toString * xScale
          }
          .mkString
      }
      .mkString("\n")
  }

  // Very simple Lambert-like shading for boxes: approximate per-voxel normal by comparing to shape center.
  def renderShaded(world: World3D,
                   lightDirection: Coord3 = Coord3(0, 0, -1),
                   chars: String = " .:-=+*#%@",
                   xScale: Int = 1,
                   nearToFarZs: Seq[Int] = Nil): String = {
    val rows = 0 until world.height
    val columns = 0 until world.width
    val zScan: Seq[Int] = if (nearToFarZs.nonEmpty) nearToFarZs else (world.depth - 1) to 0 by -1
    val light = lightDirection.normalize

    def shadeAt(p: Coord3): Option[Char] = {
      world.placements.find(_.occupiesSpaceAt(p)).map { placement =>
        val centerWorld = placement.origin + placement.shape.center
        val normalApprox = (p - centerWorld).normalize
        val ndotl = Math.max(0.0, normalApprox.dot(light))
        val idx = Math.min(chars.length - 1, Math.max(0, (ndotl * (chars.length - 1)).toInt))
        chars.charAt(idx)
      }
    }

    rows
      .map { row =>
        columns
          .map { column =>
            val ch = zScan
              .flatMap { z => shadeAt(Coord3(column, row, z)) }
              .headOption
              .getOrElse(' ')
            ch.toString * xScale
          }
          .mkString
      }
      .mkString("\n")
  }
}


