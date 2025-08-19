
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
                   nearToFarZs: Seq[Int] = Nil,
                   cullBackfaces: Boolean = true): String = {
    val rows = 0 until world.height
    val columns = 0 until world.width
    val light = lightDirection.normalize
    val viewDirWorld = Coord3(0, 0, -1)

    rows
      .map { row =>
        columns
          .map { column =>
            val zFront = world.depth - 1
            val w0 = Coord3(column, row, zFront)
            val w1 = Coord3(column, row, zFront - 1)

            // Find nearest front-facing intersection across placements
            val hitChar: Option[Char] = world.placements.foldLeft(Option.empty[(Double, Char)]) { (best, placement) =>
              val l0 = placement.worldToLocal(w0)
              val l1 = placement.worldToLocal(w1)
              val dir = Coord3(l1.x - l0.x, l1.y - l0.y, l1.z - l0.z)
              val rayLen = Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z)
              val rayDirLocal = if (rayLen == 0) Coord3(0, 0, -1) else Coord3(dir.x / rayLen, dir.y / rayLen, dir.z / rayLen)
              val invDir = Coord3(
                if (dir.x != 0) 1.0 / dir.x else Double.PositiveInfinity,
                if (dir.y != 0) 1.0 / dir.y else Double.PositiveInfinity,
                if (dir.z != 0) 1.0 / dir.z else Double.PositiveInfinity
              )

              val minX = 0.0; val maxX = placement.shape match { case b: Box => b.width; case _ => Double.PositiveInfinity }
              val minY = 0.0; val maxY = placement.shape match { case b: Box => b.height; case _ => Double.PositiveInfinity }
              val minZ = 0.0; val maxZ = placement.shape match { case b: Box => b.depth; case _ => Double.PositiveInfinity }

              val tx1 = (minX - l0.x) * invDir.x; val tx2 = (maxX - l0.x) * invDir.x
              val tminX = Math.min(tx1, tx2); val tmaxX = Math.max(tx1, tx2)
              val ty1 = (minY - l0.y) * invDir.y; val ty2 = (maxY - l0.y) * invDir.y
              val tminY = Math.min(ty1, ty2); val tmaxY = Math.max(ty1, ty2)
              val tz1 = (minZ - l0.z) * invDir.z; val tz2 = (maxZ - l0.z) * invDir.z
              val tminZ = Math.min(tz1, tz2); val tmaxZ = Math.max(tz1, tz2)

              val tEnter = Math.max(tminX, Math.max(tminY, tminZ))
              val tExit = Math.min(tmaxX, Math.min(tmaxY, tmaxZ))

              if (tExit >= tEnter && tExit >= 0) {
                val eps = 1e-6
                val localNormal =
                  if (Math.abs(tEnter - tminX) < eps) Coord3(if (tx1 > tx2) 1 else -1, 0, 0)
                  else if (Math.abs(tEnter - tminY) < eps) Coord3(0, if (ty1 > ty2) 1 else -1, 0)
                  else Coord3(0, 0, if (tz1 > tz2) 1 else -1)

                val worldNormal = placement.rotation.applyTo(localNormal).normalize
                val dotNV = - (localNormal.dot(rayDirLocal))
                val grazingEps = 1e-3
                val isBackface = dotNV < grazingEps
                if (!cullBackfaces || !isBackface) {
                  val ndotl = Math.max(0.0, worldNormal.dot(light))
                  val brightnessLinear = Math.min(1.0, Math.max(0.0, ambient + (1.0 - ambient) * ndotl))
                  val levels = 4
                  val brightness = Math.round(brightnessLinear * (levels - 1)).toDouble / (levels - 1)
                  val idx = Math.min(chars.length - 1, Math.max(0, (brightness * (chars.length - 1)).toInt))
                  val ch = chars.charAt(idx)
                  val depthBias = 1e-6
                  best match {
                    case None => Some((tEnter, ch))
                    case Some((tBest, _)) if tEnter + depthBias < tBest => Some((tEnter, ch))
                    case other => other
                  }
                } else best
              } else best
            }.map(_._2)

            val ch = hitChar.getOrElse(' ')
            ch.toString * xScale
          }
          .mkString
      }
      .mkString("\n")
  }
}


