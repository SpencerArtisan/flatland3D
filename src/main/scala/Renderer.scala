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
                   nearToFarZs: Seq[Int] = Nil): String = {
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
              val worldPoint = Coord(column, row, z)
              world.placements.find(_.occupiesSpaceAt(worldPoint)).map { placement =>
                val localPoint = placement.worldToLocal(worldPoint)
                val localNormal = placement.shape.surfaceNormalAt(localPoint)
                val localLight = transformLightToShapeSpace(light, placement.rotation)
                val brightness = calculateLambertianBrightness(localNormal, localLight, ambient)
                val shadingChar = brightnessToCharacter(brightness, chars)
                shadingChar
              }
            }.headOption

            val ch = hitChar.getOrElse(' ')
            ch.toString * xScale
          }
          .mkString
      }
      .mkString("\n")
  }

  private def transformLightToShapeSpace(worldLight: Coord, shapeRotation: Rotation): Coord =
    shapeRotation.inverse.applyTo(worldLight)

  private def calculateLambertianBrightness(surfaceNormal: Coord, lightDirection: Coord, ambientLevel: Double): Double = {
    val lightDotNormal = Math.max(0.0, surfaceNormal.dot(lightDirection))
    Math.min(1.0, Math.max(0.0, ambientLevel + (1.0 - ambientLevel) * lightDotNormal))
  }

  private def brightnessToCharacter(brightness: Double, characterGradient: String): Char = {
    val quantizationLevels = 4
    val quantizedBrightness = Math.round(brightness * (quantizationLevels - 1)).toDouble / (quantizationLevels - 1)
    val characterIndex = Math.min(characterGradient.length - 1, Math.max(0, (quantizedBrightness * (characterGradient.length - 1)).toInt))
    characterGradient.charAt(characterIndex)
  }

  def renderShadedForward(world: World,
                         lightDirection: Coord = Coord(0, 0, -1),
                         chars: String = ".,:-=+*#%@",
                         ambient: Double = 0.2,
                         xScale: Int = 1): String = {
    val light = lightDirection.normalize
    val frameBuffer = createFrameBuffer(world.width, world.height)
    val depthBuffer = createDepthBuffer(world.width, world.height)

    world.placements.foreach { placement =>
      renderPlacementForward(placement, light, chars, ambient, frameBuffer, depthBuffer)
    }

    frameBufferToString(frameBuffer, xScale)
  }

  private def createFrameBuffer(width: Int, height: Int): Array[Array[Char]] =
    Array.fill(height, width)(' ')

  private def createDepthBuffer(width: Int, height: Int): Array[Array[Double]] =
    Array.fill(height, width)(Double.MaxValue)

  private def renderPlacementForward(placement: Placement, 
                                   light: Coord, 
                                   chars: String, 
                                   ambient: Double,
                                   frameBuffer: Array[Array[Char]], 
                                   depthBuffer: Array[Array[Double]]): Unit = {
    val box = placement.shape.asInstanceOf[Box]
    enumerateCanonicalVoxels(box).foreach { canonicalVoxel =>
      val worldPoint = transformVoxelToWorld(canonicalVoxel, placement)
      val screenPixel = projectToScreen(worldPoint)
      
      if (isWithinScreenBounds(screenPixel, frameBuffer) && shouldRenderAtDepth(worldPoint.z, screenPixel, depthBuffer)) {
        val shadingChar = calculateVoxelShading(canonicalVoxel, box, light, placement, ambient, chars)
        renderPixelToBuffer(screenPixel, shadingChar, worldPoint.z, frameBuffer, depthBuffer)
      }
    }
  }

  private def enumerateCanonicalVoxels(box: Box): Seq[Coord] = {
    val halfWidth = (box.width / 2).toInt
    val halfHeight = (box.height / 2).toInt  
    val halfDepth = (box.depth / 2).toInt
    
    for {
      x <- -halfWidth to halfWidth
      y <- -halfHeight to halfHeight
      z <- -halfDepth to halfDepth
      voxel = Coord(x, y, z)
      if box.occupiesSpaceAt(voxel)
    } yield voxel
  }

  private def transformVoxelToWorld(canonicalVoxel: Coord, placement: Placement): Coord =
    placement.rotation.applyTo(canonicalVoxel) + placement.origin

  private def projectToScreen(worldPoint: Coord): (Int, Int) =
    (Math.round(worldPoint.x).toInt, Math.round(worldPoint.y).toInt)

  private def isWithinScreenBounds(screenPixel: (Int, Int), frameBuffer: Array[Array[Char]]): Boolean = {
    val (x, y) = screenPixel
    y >= 0 && y < frameBuffer.length && x >= 0 && x < frameBuffer(0).length
  }

  private def shouldRenderAtDepth(depth: Double, screenPixel: (Int, Int), depthBuffer: Array[Array[Double]]): Boolean = {
    val (x, y) = screenPixel
    depth < depthBuffer(y)(x)
  }

  private def calculateVoxelShading(canonicalVoxel: Coord, box: Box, worldLight: Coord, placement: Placement, ambient: Double, chars: String): Char = {
    val localNormal = box.surfaceNormalAt(canonicalVoxel)
    val localLight = transformLightToShapeSpace(worldLight, placement.rotation)
    val brightness = calculateLambertianBrightness(localNormal, localLight, ambient)
    brightnessToCharacter(brightness, chars)
  }

  private def renderPixelToBuffer(screenPixel: (Int, Int), 
                                char: Char, 
                                depth: Double,
                                frameBuffer: Array[Array[Char]], 
                                depthBuffer: Array[Array[Double]]): Unit = {
    val (x, y) = screenPixel
    frameBuffer(y)(x) = char
    depthBuffer(y)(x) = depth
  }

  private def frameBufferToString(frameBuffer: Array[Array[Char]], xScale: Int): String =
    frameBuffer.map(_.map(_.toString * xScale).mkString).mkString("\n")
}
