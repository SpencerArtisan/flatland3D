
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
        // Binary search along -Z (view direction) to approximate the surface point between outside and inside voxels
        val viewDir = Coord3(0, 0, -1) // orthographic along -Z
        val step = 1.0
        var tOutside = 0.0
        var tInside = 0.0
        var found = false
        // Find a boundary bracket: move forward until outside, then back 1 step inside
        var t = 0.0
        while (!found && t > -3.0) { // look up to 3 voxels towards the camera
          val q = Coord3(p.x + viewDir.x * t, p.y + viewDir.y * t, p.z + viewDir.z * t)
          if (!placement.occupiesSpaceAt(q)) {
            tOutside = t
            tInside = t + step
            found = true
          }
          t -= step
        }
        val surfacePoint = if (found) {
          var a = tOutside
          var b = tInside
          var i = 0
          while (i < 5) { // 5 iterations are enough for sub-voxel precision
            val m = (a + b) / 2
            val qm = Coord3(p.x + viewDir.x * m, p.y + viewDir.y * m, p.z + viewDir.z * m)
            if (placement.occupiesSpaceAt(qm)) b = m else a = m
            i += 1
          }
          Coord3(p.x + viewDir.x * b, p.y + viewDir.y * b, p.z + viewDir.z * b)
        } else p

        // Compute local-space normal via shape API for flat shading when available
        val local = placement.worldToLocal(surfacePoint)
        val localNormal = placement.shape.surfaceNormalAt(local)
        val worldNormal = placement.rotation.applyTo(localNormal).normalize

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


