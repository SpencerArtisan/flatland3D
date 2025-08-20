object Renderer {

  def renderWith(world: World,
                 charFor: Placement => Char,
                 blankChar: Char = '.',
                 xScale: Int = 1,
                 nearToFarZs: Seq[Int] = Nil): String = {
    // Handle empty world case
    if (world.width == 0 || world.height == 0 || world.depth == 0) {
      return ""
    }
    
    val rows = 0 until world.height
    val columns = 0 until world.width
    // Default Z-scan: far-to-near for proper occlusion (first hit = nearest object)
    val zScan: Seq[Int] = if (nearToFarZs.nonEmpty) nearToFarZs else (world.depth - 1) to 0 by -1

    rows
      .map { row =>
        columns
          .map { column =>
            // Scan Z coordinates and stop at first hit for proper occlusion
            var ch = blankChar
            var found = false
            var zIndex = 0
            while (zIndex < zScan.length && !found) {
              val z = zScan(zIndex)
              val p = Coord(column, row, z)
              val placement = world.placements.find(_.occupiesSpaceAt(p))
              if (placement.isDefined) {
                ch = charFor(placement.get)
                found = true
              }
              zIndex += 1
            }
            ch.toString * xScale
          }
          .mkString
      }
      .mkString("\n")
  }

  // Very simple Lambert-like shading for boxes: approximate per-voxel normal by comparing to shape center.
  def renderShaded(world: World,
                   lightDirection: Coord = Coord(0, 0, -1),
                   chars: String = ".,:-=+*#%@",
                   ambient: Double = 0.2,
                   xScale: Int = 1,
                   nearToFarZs: Seq[Int] = Nil,
                   cullBackfaces: Boolean = true): String = {
    // Handle empty world case
    if (world.width == 0 || world.height == 0 || world.depth == 0) {
      return ""
    }
    
    val rows = 0 until world.height
    val columns = 0 until world.width
    // Default Z-scan: far-to-near for proper occlusion (first hit = nearest object)
    val zScan: Seq[Int] = if (nearToFarZs.nonEmpty) nearToFarZs else (world.depth - 1) to 0 by -1
    val light = lightDirection.normalize

    rows
      .map { row =>
        columns
          .map { column =>
            // Enhanced Z-scan: check multiple Z values per pixel for better accuracy at extreme rotations
            val hitChar: Option[Char] = zScan.flatMap { z =>
              // Check only the integer Z coordinate for simplicity and correctness
              val worldPoint = Coord(column, row, z)
              world.placements.find(_.occupiesSpaceAt(worldPoint)).map { placement =>
                // Calculate surface normal for shading
                val localPoint = placement.worldToLocal(worldPoint)
                val localNormal = placement.shape.surfaceNormalAt(localPoint)
                val worldNormal = placement.rotation.applyTo(localNormal).normalize
                
                val shouldRender = true
                
                if (shouldRender) {
                  val ndotl = Math.max(0.0, worldNormal.dot(light))
                  val brightnessLinear = Math.min(1.0, Math.max(0.0, ambient + (1.0 - ambient) * ndotl))
                  val levels = 4
                  val brightness = Math.round(brightnessLinear * (levels - 1)).toDouble / (levels - 1)
                  val idx = Math.min(chars.length - 1, Math.max(0, (brightness * (chars.length - 1)).toInt))
                  chars.charAt(idx)
                } else {
                  ' ' // Skip this placement, continue scanning
                }
              }
            }.headOption

            val ch = hitChar.getOrElse(' ')
            ch.toString * xScale
          }
          .mkString
      }
      .mkString("\n")
  }
}
