
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
                   chars: String = ".,:-=+*#%@",
                   ambient: Double = 0.2,
                   xScale: Int = 1,
                   nearToFarZs: Seq[Int] = Nil): String = {
    val rows = 0 until world.height
    val columns = 0 until world.width
    val zScan: Seq[Int] = if (nearToFarZs.nonEmpty) nearToFarZs else (world.depth - 1) to 0 by -1
    val light = lightDirection.normalize

    def shadeAt(p: Coord3): Option[Char] = {
      world.placements.find(_.occupiesSpaceAt(p)).map { placement =>
        // Determine face normal for boxes for consistent per-face shading
        val worldNormal: Coord3 = placement.shape match {
          case box: Box =>
            // Compute local coordinates of world point relative to placement
            val rotationCenter = placement.origin + placement.shape.center
            val coordRelativeToRotationCenter = p - rotationCenter
            val rotatedCoordRelativeToRotationCenter = placement.rotation.applyTo(coordRelativeToRotationCenter)
            val rotatedCoord = rotatedCoordRelativeToRotationCenter + rotationCenter
            val local = rotatedCoord - placement.origin

            val dx0 = local.x
            val dx1 = box.width - local.x
            val dy0 = local.y
            val dy1 = box.height - local.y
            val dz0 = local.z
            val dz1 = box.depth - local.z

            val minDist = Seq(dx0, dx1, dy0, dy1, dz0, dz1).min
            val localNormal =
              if (minDist == dx0) Coord3(-1, 0, 0)
              else if (minDist == dx1) Coord3(1, 0, 0)
              else if (minDist == dy0) Coord3(0, -1, 0)
              else if (minDist == dy1) Coord3(0, 1, 0)
              else if (minDist == dz0) Coord3(0, 0, -1)
              else Coord3(0, 0, 1)
            placement.rotation.applyTo(localNormal).normalize
          case _ =>
            // Fallback: radial approximation
            val centerWorld = placement.origin + placement.shape.center
            (p - centerWorld).normalize
        }

        val ndotl = Math.max(0.0, worldNormal.dot(light))
        val brightness = Math.min(1.0, Math.max(0.0, ambient + (1.0 - ambient) * ndotl))
        val idx = Math.min(chars.length - 1, Math.max(0, (brightness * (chars.length - 1)).toInt))
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


