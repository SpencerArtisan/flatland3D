object Renderer {
  // Configuration constants
  private val DEFAULT_EPSILON = 1e-10
  private val DEFAULT_AMBIENT = 0.2
  private val DEFAULT_X_SCALE = 1
  private val DEFAULT_BLANK_CHAR = '.'
  private val DEFAULT_SHADING_CHARS = ".,:-=+*#%@"
  private val QUANTIZATION_LEVELS = 8

  def renderWith(world: World,
                 charFor: Placement => Char,
                 blankChar: Char = DEFAULT_BLANK_CHAR,
                 xScale: Int = DEFAULT_X_SCALE,
                 nearToFarZs: Seq[Int] = Nil): String = {
    // Handle empty world case
    if (world.width == 0 || world.height == 0 || world.depth == 0) {
      return ""
    }
    
    val rows = 0 until world.height
    val columns = 0 until world.width
    // Default Z-scan: far-to-near for proper occlusion (first hit = nearest object)
    val zScan: Seq[Int] = if (nearToFarZs.nonEmpty) nearToFarZs else (world.depth - 1) to 0 by -1

    // Use StringBuilder for better memory efficiency
    val result = new StringBuilder
    
    for (row <- 0 until world.height) {
      if (row > 0) result.append('\n')
      
      for (column <- 0 until world.width) {
        // Use find instead of manual loop for better readability and performance
        val ch = zScan.find { z =>
          val p = Coord(column, row, z)
          world.placements.exists(_.occupiesSpaceAt(p))
        }.map { z =>
          val p = Coord(column, row, z)
          charFor(world.placements.find(_.occupiesSpaceAt(p)).get)
        }.getOrElse(blankChar)
        
        result.append(ch.toString * xScale)
      }
    }
    
    result.toString
  }

  // Very simple Lambert-like shading for boxes: approximate per-voxel normal by comparing to shape center.
  def renderShaded(world: World,
                   lightDirection: Coord = Coord(0, 0, -1),
                   chars: String = DEFAULT_SHADING_CHARS,
                   ambient: Double = DEFAULT_AMBIENT,
                   xScale: Int = DEFAULT_X_SCALE,
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

    // Use StringBuilder for better memory efficiency
    val result = new StringBuilder
    
    for (row <- 0 until world.height) {
      if (row > 0) result.append('\n')
      
      for (column <- 0 until world.width) {
        // Enhanced Z-scan: check multiple Z values per pixel for better accuracy at extreme rotations
        val hitChar: Option[Char] = zScan.flatMap { z =>
          val worldPoint = Coord(column, row, z)
          world.placements.find(_.occupiesSpaceAt(worldPoint)).map { placement =>
            val localPoint = placement.worldToLocal(worldPoint)
            val localNormal = placement.shape.surfaceNormalAt(localPoint)
            val localLight = transformLightToShapeSpace(light, placement.rotation)
            val brightness = calculateLambertianBrightness(localNormal, localLight, ambient)
            brightnessToCharacter(brightness, chars)
          }
        }.headOption

        val ch = hitChar.getOrElse(' ')
        result.append(ch.toString * xScale)
      }
    }
    
    result.toString
  }

  private def transformLightToShapeSpace(worldLight: Coord, shapeRotation: Rotation): Coord =
    shapeRotation.inverse.applyTo(worldLight)

  private def calculateLambertianBrightness(surfaceNormal: Coord, lightDirection: Coord, ambientLevel: Double): Double = {
    val lightDotNormal = Math.max(0.0, surfaceNormal.dot(lightDirection))
    Math.min(1.0, Math.max(0.0, ambientLevel + (1.0 - ambientLevel) * lightDotNormal))
  }

  private def brightnessToCharacter(brightness: Double, characterGradient: String): Char = {
    val quantizedBrightness = Math.round(brightness * (QUANTIZATION_LEVELS - 1)).toDouble / (QUANTIZATION_LEVELS - 1)
    val characterIndex = Math.min(characterGradient.length - 1, Math.max(0, (quantizedBrightness * (characterGradient.length - 1)).toInt))
    characterGradient.charAt(characterIndex)
  }

  def renderShadedForward(world: World,
                         lightDirection: Coord = Coord(0, 0, -1),
                         chars: String = DEFAULT_SHADING_CHARS,
                         ambient: Double = DEFAULT_AMBIENT,
                         xScale: Int = DEFAULT_X_SCALE): String = {
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
        // Log unsupported shape type instead of throwing exception
        // For now, skip rendering unsupported shapes
        Console.err.println(s"Warning: Unsupported shape type: ${placement.shape.getClass.getSimpleName}")
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
    // For triangle meshes, cast rays from screen pixels to find intersections
    for {
      y <- frameBuffer.indices
      x <- frameBuffer(y).indices
    } {
      val screenCoord = Coord(x, y, 0)
      val rayDirection = Coord(0, 0, 1) // Ray pointing into screen
      
      // Transform ray to shape's local coordinate system
      val worldRayOrigin = screenCoord
      val localRayOrigin = placement.rotation.inverse.applyTo(worldRayOrigin - placement.origin)
      val localRayDirection = placement.rotation.inverse.applyTo(rayDirection)
      
      // Find closest intersection with triangle mesh
      triangleMesh.intersectRay(localRayOrigin, localRayDirection) match {
        case Some((distance, triangle)) =>
          val worldZ = screenCoord.z + distance
          if (worldZ < depthBuffer(y)(x)) {
            val surfaceNormal = triangle.normal
            val transformedLight = transformLightToShapeSpace(light, placement.rotation)
            val brightness = calculateLambertianBrightness(surfaceNormal, transformedLight, ambient)
            val shadingChar = brightnessToCharacter(brightness, chars)
            
            frameBuffer(y)(x) = shadingChar
            depthBuffer(y)(x) = worldZ
          }
        case None => // No intersection, leave pixel unchanged
      }
    }
  }
}
