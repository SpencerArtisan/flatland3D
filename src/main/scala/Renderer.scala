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
                         xScale: Int = DEFAULT_X_SCALE,
                         viewport: Option[Viewport] = None): String = {
    val light = lightDirection.normalize
    
    // Determine rendering dimensions based on viewport or world bounds
    val (renderWidth, renderHeight) = viewport match {
      case Some(vp) => (vp.width, vp.height)
      case None => (world.width, world.height)
    }
    
    val frameBuffer = createFrameBuffer(renderWidth, renderHeight)
    val depthBuffer = createDepthBuffer(renderWidth, renderHeight)

    // Get placements to render (filtered by viewport if specified)
    val placementsToRender = viewport match {
      case Some(vp) => world.placementsInViewport(vp)
      case None => world.placements
    }

    placementsToRender.foreach { placement =>
      renderPlacementForward(placement, light, chars, ambient, frameBuffer, depthBuffer, viewport)
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
                                   depthBuffer: Array[Array[Double]],
                                   viewport: Option[Viewport] = None): Unit = {
    placement.shape match {
      case triangleMesh: TriangleMesh =>
        renderTriangleMeshForward(triangleMesh, placement, light, chars, ambient, frameBuffer, depthBuffer, viewport)
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

  private def frameBufferToString(frameBuffer: Array[Array[Char]], xScale: Int): String = {
    // First apply xScale to the frame buffer content
    val scaledContent = frameBuffer.map(_.map(_.toString * xScale).mkString).mkString("\n")
    
    // Then add the boundary frame around the scaled content
    addBoundaryFrameToString(scaledContent, frameBuffer(0).length * xScale, frameBuffer.length)
  }
  
  private def addBoundaryFrameToString(content: String, scaledWidth: Int, height: Int): String = {
    val lines = content.split("\n")
    
    // Box-drawing characters
    val topLeft = '┌'
    val topRight = '┐'
    val bottomLeft = '└'
    val bottomRight = '┘'
    val horizontal = '─'
    val vertical = '│'
    
    // Create top border
    val topBorder = topLeft + (horizontal.toString * scaledWidth) + topRight
    
    // Create bottom border  
    val bottomBorder = bottomLeft + (horizontal.toString * scaledWidth) + bottomRight
    
    // Create content lines with side borders
    val contentWithBorders = lines.map(line => vertical + line + vertical)
    
    // Combine all parts with a blank line at the top
    (Seq("") ++ Seq(topBorder) ++ contentWithBorders ++ Seq(bottomBorder)).mkString("\n")
  }



  private def renderTriangleMeshForward(triangleMesh: TriangleMesh,
                                      placement: Placement,
                                      light: Coord,
                                      chars: String,
                                      ambient: Double,
                                      frameBuffer: Array[Array[Char]],
                                      depthBuffer: Array[Array[Double]],
                                      viewport: Option[Viewport] = None): Unit = {
    // For triangle meshes, cast rays from screen pixels to find intersections
    for {
      y <- frameBuffer.indices
      x <- frameBuffer(y).indices
    } {
      val screenCoord = Coord(x, y, 0)
      val rayDirection = Coord(0, 0, 1) // Ray pointing into screen
      
      // Transform screen coordinates to world coordinates if using viewport
      val worldRayOrigin = viewport match {
        case Some(vp) => 
          // Transform viewport coordinates back to world coordinates
          val viewportCoord = Coord(x, y, 0)
          val worldCoord = Coord(
            vp.worldBounds.minX + x,
            vp.worldBounds.minY + y,
            vp.worldBounds.minZ
          )
          worldCoord
        case None => screenCoord
      }
      
      // Transform ray to shape's local coordinate system
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
