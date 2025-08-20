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
    placement.shape match {
      case triangleMesh: TriangleMesh =>
        renderTriangleMeshForward(triangleMesh, placement, light, chars, ambient, frameBuffer, depthBuffer)
      case _ =>
        throw new IllegalArgumentException(s"Unsupported shape type: ${placement.shape.getClass.getSimpleName}")
    }
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

  private def renderTriangleMeshForward(triangleMesh: TriangleMesh,
                                      placement: Placement,
                                      light: Coord,
                                      chars: String,
                                      ambient: Double,
                                      frameBuffer: Array[Array[Char]],
                                      depthBuffer: Array[Array[Double]]): Unit = {
    // Similar to voxel rendering: enumerate surface points and project to screen
    enumerateTriangleSurfacePoints(triangleMesh).foreach { localPoint =>
      val worldPoint = placement.rotation.applyTo(localPoint) + placement.origin
      val screenPixel = (Math.round(worldPoint.x).toInt, Math.round(worldPoint.y).toInt)
      
      val (x, y) = screenPixel
      if (y >= 0 && y < frameBuffer.length && x >= 0 && x < frameBuffer(y).length && worldPoint.z < depthBuffer(y)(x)) {
        val surfaceNormal = triangleMesh.surfaceNormalAt(localPoint)
        val transformedLight = transformLightToShapeSpace(light, placement.rotation)
        val brightness = calculateLambertianBrightness(surfaceNormal, transformedLight, ambient)
        val shadingChar = brightnessToCharacter(brightness, chars)
        
        frameBuffer(y)(x) = shadingChar
        depthBuffer(y)(x) = worldPoint.z
      }
    }
  }
  
  private def enumerateTriangleSurfacePoints(triangleMesh: TriangleMesh): Seq[Coord] = {
    // For triangle meshes, sample both surface points AND include all vertices
    // This ensures we capture the full geometric extent
    val vertices = triangleMesh.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2)).distinct
    val surfacePoints = triangleMesh.triangles.flatMap { triangle =>
      sampleTrianglePoints(triangle, density = 1.0) // Reduce density since we have vertex coverage
    }
    
    vertices ++ surfacePoints
  }
  
  private def sampleTrianglePoints(triangle: Triangle, density: Double): Seq[Coord] = {
    // Simple edge-based sampling to ensure good coverage without excessive points
    val samplesPerEdge = Math.max(2, (Math.sqrt(triangle.area) * density).toInt)
    
    val points = for {
      i <- 0 to samplesPerEdge
      j <- 0 to (samplesPerEdge - i)
      if i + j <= samplesPerEdge // Stay within triangle
    } yield {
      val u = if (samplesPerEdge > 0) i.toDouble / samplesPerEdge else 0.0
      val v = if (samplesPerEdge > 0) j.toDouble / samplesPerEdge else 0.0
      val w = 1.0 - u - v
      
      if (w >= 0) {
        triangle.v0 * u + triangle.v1 * v + triangle.v2 * w
      } else {
        // Clamp to triangle boundary
        val total = u + v
        val normalizedU = if (total > 0) u / total else 0.5
        val normalizedV = if (total > 0) v / total else 0.5
        triangle.v0 * normalizedU + triangle.v1 * normalizedV
      }
    }
    
    points.toSeq
  }
}
