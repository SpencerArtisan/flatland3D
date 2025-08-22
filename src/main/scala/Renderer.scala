object Renderer {
  // Performance metrics
  case class RenderMetrics(
    rayTransformTimeMs: Long = 0,
    intersectionTestTimeMs: Long = 0,
    shadingTimeMs: Long = 0,
    totalPixels: Int = 0,
    totalIntersectionTests: Long = 0,
    successfulIntersections: Int = 0
  ) {
    def totalTimeMs: Long = rayTransformTimeMs + intersectionTestTimeMs + shadingTimeMs
    
    override def toString: String = {
      val avgIntersectionsPerPixel = if (totalPixels > 0) totalIntersectionTests.toDouble / totalPixels else 0
      val hitRate = if (totalIntersectionTests > 0) successfulIntersections.toDouble / totalIntersectionTests * 100 else 0
      
      s"""Render Metrics:
         |  Ray Transform Time: ${rayTransformTimeMs}ms
         |  Intersection Tests: ${intersectionTestTimeMs}ms
         |  Shading Time: ${shadingTimeMs}ms
         |  Total Time: ${totalTimeMs}ms
         |  Total Pixels: $totalPixels
         |  Total Intersection Tests: $totalIntersectionTests
         |  Successful Intersections: $successfulIntersections
         |  Avg Intersections/Pixel: $avgIntersectionsPerPixel
         |  Hit Rate: ${f"$hitRate%.2f"}%
         |""".stripMargin
    }
  }
  
  private var currentMetrics = RenderMetrics()
  def getLastRenderMetrics: RenderMetrics = currentMetrics
  
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

  private def transformLightToShapeSpace(worldLight: Coord, shapeRotation: Rotation): Coord = {
    // When we rotate the cube to look at a face, we want that face to be lit the same way
    // as the front face was. This means we need to rotate the light by the inverse rotation
    // that we applied to the cube.
    //
    // For example:
    // - If we rotate the cube 90° right to look at its right face
    // - We need to rotate the light 90° left to maintain the same relative angle
    shapeRotation.inverse.applyTo(worldLight)
  }

  private def calculateLambertianBrightness(surfaceNormal: Coord, lightDirection: Coord, ambientLevel: Double): Double = {
    val normalizedNormal = surfaceNormal.normalize
    val normalizedLight = lightDirection.normalize
    // For consistent shading, we want the absolute value of the dot product
    // This ensures that faces pointing in opposite directions get the same shading
    val lightDotNormal = Math.abs(normalizedNormal.dot(normalizedLight))
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
    import scala.collection.parallel.CollectionConverters._
    
    val totalPixels = frameBuffer.length * frameBuffer(0).length
    
    // Thread-safe counters using atomic variables
    val rayTransformTime = new java.util.concurrent.atomic.AtomicLong(0)
    val intersectionTestTime = new java.util.concurrent.atomic.AtomicLong(0)
    val shadingTime = new java.util.concurrent.atomic.AtomicLong(0)
    val intersectionTests = new java.util.concurrent.atomic.AtomicLong(0)
    val successfulIntersections = new java.util.concurrent.atomic.AtomicInteger(0)
    
    // Process rows in parallel
    frameBuffer.indices.par.foreach { y =>
      // Process each pixel in the row
      frameBuffer(y).indices.foreach { x =>
        val screenCoord = Coord(x, y, 0)
        val rayDirection = Coord(0, 0, 1) // Ray pointing into screen
      
        // Transform timing start
        val transformStart = System.nanoTime()
        
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
        
        rayTransformTime.addAndGet(System.nanoTime() - transformStart)
        
        // Intersection timing start
        val intersectStart = System.nanoTime()
        
        // Find closest intersection with triangle mesh
        intersectionTests.addAndGet(triangleMesh.triangles.size) // Count total tests
        val intersection = triangleMesh.intersectRay(localRayOrigin, localRayDirection)
        
        intersectionTestTime.addAndGet(System.nanoTime() - intersectStart)
        
        intersection match {
          case Some((distance, triangle)) =>
            successfulIntersections.incrementAndGet()
            val worldZ = screenCoord.z + distance
            if (worldZ < depthBuffer(y)(x)) {
              // Shading timing start
              val shadingStart = System.nanoTime()
              
              // Transform the normal into world space using proper normal transformation
              val worldNormal = placement.rotation.transformNormal(triangle.normal)
              val brightness = calculateLambertianBrightness(worldNormal, light, ambient)
              val shadingChar = brightnessToCharacter(brightness, chars)
              
              frameBuffer(y)(x) = shadingChar
              depthBuffer(y)(x) = worldZ
              
              shadingTime.addAndGet(System.nanoTime() - shadingStart)
            }
          case None => // No intersection, leave pixel unchanged
        }
      }
    }
    
    // Update metrics (convert nano to milliseconds)
    currentMetrics = RenderMetrics(
      rayTransformTimeMs = rayTransformTime.get() / 1000000,
      intersectionTestTimeMs = intersectionTestTime.get() / 1000000,
      shadingTimeMs = shadingTime.get() / 1000000,
      totalPixels = totalPixels,
      totalIntersectionTests = intersectionTests.get(),
      successfulIntersections = successfulIntersections.get()
    )
  }


}
