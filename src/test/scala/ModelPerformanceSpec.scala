import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ModelPerformanceSpec extends AnyFlatSpec with Matchers {
  // Helper to format memory in MB
  private def formatMemory(bytes: Long): String = f"${bytes.toDouble / 1024 / 1024}%.1f MB"
  
  // Helper to measure memory usage
  private def getMemoryUsage: (Long, Long, Long) = {
    val runtime = Runtime.getRuntime
    val total = runtime.totalMemory()
    val free = runtime.freeMemory()
    val used = total - free
    (total, used, free)
  }
  
  "Car model" should "load and transform with performance metrics" in {
    // Force garbage collection before starting
    System.gc()
    Thread.sleep(100) // Give GC time to run
    
    // Record initial memory state
    val (initialTotal, initialUsed, initialFree) = getMemoryUsage
    println("\nInitial Memory State:")
    println(s"  Total: ${formatMemory(initialTotal)}")
    println(s"  Used: ${formatMemory(initialUsed)}")
    println(s"  Free: ${formatMemory(initialFree)}")
    
    // Load and parse model
    val startLoad = System.nanoTime()
    val modelContent = scala.io.Source.fromResource("models/car.obj").mkString
    val loadTime = (System.nanoTime() - startLoad) / 1000000 // ms
    println(s"\nFile Load Time: ${loadTime}ms")
    println(s"File Size: ${modelContent.length / 1024.0}KB")
    
    // Parse model
    val parseResult = ObjParser.parse(modelContent)
    parseResult.isRight shouldBe true
    
    // Get and output parse metrics
    val loadMetrics = ObjParser.getLastMetrics
    loadMetrics.isDefined shouldBe true
    println("\nParse Metrics:")
    println(loadMetrics.get.toString)
    
    // Memory after parsing
    val (parseTotal, parseUsed, parseFree) = getMemoryUsage
    println("\nMemory After Parsing:")
    println(s"  Total: ${formatMemory(parseTotal)}")
    println(s"  Used: ${formatMemory(parseUsed)} (${formatMemory(parseUsed - initialUsed)} increase)")
    println(s"  Free: ${formatMemory(parseFree)}")
    
    // Measure transformation performance
    val mesh = parseResult.right.get
    
    // Single scale operation
    val startScale = System.nanoTime()
    val scaledMesh = ObjParser.scaleMesh(mesh.copy(id = 1), 10.0)
    val scaleTime = (System.nanoTime() - startScale) / 1000000 // ms
    println(s"\nScale Operation (10x): ${scaleTime}ms")
    
    // Single world transform and render
    val world = World.infinite
    val startTransform = System.nanoTime()
    val worldWithMesh = world.add(scaledMesh, Coord(0, 0, 0), Rotation(Math.PI/4, Math.PI/4, 0))
    val transformTime = (System.nanoTime() - startTransform) / 1000000 // ms
    println(s"\nWorld Transform Time: ${transformTime}ms")
    
    // Detailed render timing
    println("\nRender Performance Breakdown:")
    
    // Setup timing
    val startSetup = System.nanoTime()
    val viewport = Viewport.centeredAt(Coord(0, 0, 0))
    val setupTime = (System.nanoTime() - startSetup) / 1000000
    println(s"  Setup Time: ${setupTime}ms")
    
    // Viewport filtering timing
    val startFilter = System.nanoTime()
    val placementsInView = worldWithMesh.placementsInViewport(viewport)
    val filterTime = (System.nanoTime() - startFilter) / 1000000
    println(s"  Viewport Filtering: ${filterTime}ms")
    println(s"  Placements in view: ${placementsInView.size}")
    
    // Render timing with triangle count
    val startRender = System.nanoTime()
    val rendered = Renderer.renderShadedForward(
      worldWithMesh,
      lightDirection = Coord(-1, -1, -1),
      ambient = 0.35,
      xScale = 2,
      viewport = Some(viewport)
    )
    val renderTime = (System.nanoTime() - startRender) / 1000000
    println(s"  Total Render Time: ${renderTime}ms")
    
    // Get detailed render metrics
    val renderMetrics = Renderer.getLastRenderMetrics
    println("\nDetailed Render Metrics:")
    println(renderMetrics.toString)
    
    // Calculate additional stats
    val triangleCount = placementsInView.map(_.shape.asInstanceOf[TriangleMesh].triangles.size).sum
    println("\nDerived Performance Metrics:")
    println(s"  Total Triangles: $triangleCount")
    println(s"  Output Resolution: ${viewport.width}x${viewport.height} (${viewport.width * viewport.height} pixels)")
    println(s"  Triangle Tests/Pixel: ${renderMetrics.totalIntersectionTests.toDouble / renderMetrics.totalPixels}")
    println(s"  Microseconds/Triangle Test: ${(renderMetrics.intersectionTestTimeMs * 1000.0) / renderMetrics.totalIntersectionTests}")
    println(s"  Microseconds/Ray Transform: ${(renderMetrics.rayTransformTimeMs * 1000.0) / renderMetrics.totalPixels}")
    println(s"  Microseconds/Shading Op: ${(renderMetrics.shadingTimeMs * 1000.0) / renderMetrics.successfulIntersections}")
    
    val totalTime = setupTime + filterTime + renderTime
    println(s"\nTotal Processing Time: ${totalTime}ms")
    
    // Final memory state
    val (finalTotal, finalUsed, finalFree) = getMemoryUsage
    println("\nFinal Memory State:")
    println(s"  Total: ${formatMemory(finalTotal)}")
    println(s"  Used: ${formatMemory(finalUsed)} (${formatMemory(finalUsed - initialUsed)} total increase)")
    println(s"  Free: ${formatMemory(finalFree)}")
    
    // Basic assertions
    loadMetrics.get.parseTimeMs should be < 1000L // Parse should take less than 1 second
    loadMetrics.get.vertexCount should be > 0
    loadMetrics.get.triangleCount should be > 0
  }
}
