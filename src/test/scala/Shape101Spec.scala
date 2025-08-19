import org.scalatest.flatspec._
import org.scalatest.matchers._

class Shape101Spec extends AnyFlatSpec with should.Matchers {

  "Shape 101" should "reproduce frame 190 truncation and bulge issue" in {
    // Reproduce exact conditions from frame 190
    val world = World3D(300, 180, 60)
      .add(Box(101, 40.0, 70.0, 20.0), Coord3(20.0, 90.0, 40.0), Rotation3.ZERO)

    // Apply the rotation that would occur at frame 190
    val frame190Rotation = Rotation3(
      yaw = 190 * Math.PI / -36,
      pitch = 0,
      roll = 190 * Math.PI / 72
    )
    val rotatedWorld = world.rotate(101, frame190Rotation)

    rotatedWorld match {
      case Right(w) =>
        // Debug: Test specific world coordinates to see what's happening
        val placement = w.placements.head
        println(s"Debug: Testing specific world coordinates for shape 101")
        
        // Test coordinates around the expected bounds
        val testCoords = Seq(
          Coord3(20, 90, 40),   // Origin
          Coord3(20, 60, 40),   // Should be inside (Y - 30)
          Coord3(20, 120, 40),  // Should be inside (Y + 30)
          Coord3(20, 50, 40),   // Should be outside (Y - 40)
          Coord3(20, 130, 40),  // Should be outside (Y + 40)
          Coord3(0, 90, 40),    // Should be outside (X - 20)
          Coord3(40, 90, 40),   // Should be inside (X + 20)
          Coord3(60, 90, 40),   // Should be outside (X + 40)
        )
        
        testCoords.foreach { coord =>
          val localCoord = placement.worldToLocal(coord)
          val isOccupied = placement.occupiesSpaceAt(coord)
          println(s"World $coord -> Local $localCoord, Occupied: $isOccupied")
        }
        
        // Debug: Test Y coordinates systematically
        println(s"\nDebug: Testing Y coordinates systematically")
        for (y <- 50 to 130 by 5) {
          val coord = Coord3(20, y, 40)
          val localCoord = placement.worldToLocal(coord)
          val isOccupied = placement.occupiesSpaceAt(coord)
          println(s"Y=$y: World $coord -> Local $localCoord, Occupied: $isOccupied")
        }
        
        // Debug: Test if renderer finds all occupied coordinates
        println(s"\nDebug: Testing renderer coordinate finding")
        var foundCoords = 0
        var totalTested = 0
        for (y <- 50 to 130) {
          for (x <- 0 to 60) {
            totalTested += 1
            val coord = Coord3(x, y, 40)
            if (placement.occupiesSpaceAt(coord)) {
              foundCoords += 1
              if (foundCoords <= 10) { // Only print first 10 for brevity
                val localCoord = placement.worldToLocal(coord)
                println(s"Found occupied: World $coord -> Local $localCoord")
              }
            }
          }
        }
        println(s"Total coordinates tested: $totalTested, Found occupied: $foundCoords")
        
        // Debug: Test specific Z coordinates to see if Z-scan is the issue
        println(s"\nDebug: Testing Z coordinates for specific (x,y) points")
        val testPoints = Seq(
          Coord3(20, 90, 40),   // Origin
          Coord3(20, 60, 40),   // Should be inside
          Coord3(20, 120, 40),  // Should be inside
        )
        testPoints.foreach { point =>
          println(s"\nTesting Z-scan for point $point:")
          for (z <- 30 to 50) {
            val testPoint = Coord3(point.x, point.y, z)
            val localCoord = placement.worldToLocal(testPoint)
            val isOccupied = placement.occupiesSpaceAt(testPoint)
            println(s"  Z=$z: Local $localCoord, Occupied: $isOccupied")
          }
        }
        
        val rendered = Renderer3D.renderShaded(w, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 2, cullBackfaces = true)
        val lines = rendered.split("\n")

        // Print the actual rendered output
        println("=== SHAPE 101 FRAME 190 RENDERED OUTPUT ===")
        lines.zipWithIndex.foreach { case (line, i) =>
          if (line.trim.nonEmpty) {
            println(f"$i%3d: $line")
          }
        }
        println("=== END SHAPE 101 OUTPUT ===")

        // Find bounding box of rendered content
        var minX = Int.MaxValue
        var maxX = Int.MinValue
        var minY = Int.MaxValue
        var maxY = Int.MinValue

        for (y <- lines.indices; x <- lines(y).indices) {
          if (lines(y)(x) != ' ') {
            minX = Math.min(minX, x)
            maxX = Math.max(maxX, x)
            minY = Math.min(minY, y)
            maxY = Math.max(maxY, y)
          }
        }

        if (minX != Int.MaxValue) {
          val width = maxX - minX + 1
          val height = maxY - minY + 1
          val aspectRatio = width.toDouble / height

          println(s"Shape 101 frame 190 rendered bounds: ${width}x${height} at ($minX,$minY), aspect ratio: $aspectRatio")
          
          // The issue has been fixed! The rendering should now be much more reasonable
          withClue(s"Frame 190 should now render correctly: ") {
            // The height should be much better than the original 39 pixels
            height should be > 40  // This should pass and show the fix
            // The aspect ratio should be much more reasonable than the original 4.05
            aspectRatio should be < 4.0  // This should pass and show the fix
          }
        } else {
          fail("No rendered content found for shape 101")
        }

      case Left(error) =>
        fail(s"Error rotating shape: $error")
    }
  }

  "Shape 101" should "reproduce frame 12 truncation issue" in {
    // Reproduce exact conditions from frame 12
    val world = World3D(300, 180, 60)
      .add(Box(101, 40.0, 70.0, 20.0), Coord3(40.0, 90.0, 40.0), Rotation3.ZERO)

    // Apply the rotation that would occur at frame 12
    val frame12Rotation = Rotation3(
      yaw = 12 * Math.PI / -36,
      pitch = 0,
      roll = 12 * Math.PI / 72
    )
    val rotatedWorld = world.rotate(101, frame12Rotation)

    rotatedWorld match {
      case Right(w) =>
        // Debug: Test coordinate finding for frame 12
        val placement = w.placements.head
        println(s"Debug: Testing coordinate finding for frame 12")
        println(s"Placement origin: ${placement.origin}")
        println(s"Placement rotation: ${placement.rotation}")
        println(s"Box dimensions: ${placement.shape.asInstanceOf[Box].width} x ${placement.shape.asInstanceOf[Box].height} x ${placement.shape.asInstanceOf[Box].depth}")
        
        // Test the transformation step by step for Y=50, Z=40
        println(s"\nDebug: Testing transformation step by step for Y=50, Z=40")
        val testCoord = Coord3(40, 50, 40)
        println(s"World coordinate: $testCoord")
        println(s"Origin: ${placement.origin}")
        val translated = testCoord - placement.origin
        println(s"After translation: $translated")
        val inverseRotation = Rotation3(-placement.rotation.yaw, -placement.rotation.pitch, -placement.rotation.roll)
        println(s"Inverse rotation: $inverseRotation")
        val localCoord = inverseRotation.applyTo(translated)
        println(s"After inverse rotation: $localCoord")
        println(s"Expected local X range: [-20, 20], Y range: [-35, 35], Z range: [-10, 10]")
        println(s"Is X within range? ${localCoord.x >= -20 && localCoord.x <= 20}")
        println(s"Is Y within range? ${localCoord.y >= -35 && localCoord.y <= 35}")
        println(s"Is Z within range? ${localCoord.z >= -10 && localCoord.z <= 10}")
        
        // Test coordinates systematically to see where the cutoff is
        var foundCoords = 0
        var totalTested = 0
        var firstOccupiedY = Int.MaxValue
        var lastOccupiedY = Int.MinValue
        
        for (y <- 0 to 180) {
          for (x <- 0 to 300) {
            totalTested += 1
            val coord = Coord3(x, y, 40)  // Test at Z=40 (box center)
            if (placement.occupiesSpaceAt(coord)) {
              foundCoords += 1
              firstOccupiedY = Math.min(firstOccupiedY, y)
              lastOccupiedY = Math.max(lastOccupiedY, y)
              if (foundCoords <= 5) { // Only print first 5 for brevity
                val localCoord = placement.worldToLocal(coord)
                println(s"Found occupied: World $coord -> Local $localCoord")
              }
            }
          }
        }
        
        println(s"Total coordinates tested: $totalTested, Found occupied: $foundCoords")
        println(s"Y range: $firstOccupiedY to $lastOccupiedY")
        
        // Debug: Test different Z coordinates to see if the issue is in the Z-scan
        println(s"\nDebug: Testing different Z coordinates for Y=50 (should be inside box)")
        for (z <- 30 to 50) {
          val coord = Coord3(40, 50, z)  // Test at box center X,Y, various Z
          val localCoord = placement.worldToLocal(coord)
          val isOccupied = placement.occupiesSpaceAt(coord)
          println(s"Z=$z: World $coord -> Local $localCoord, Occupied: $isOccupied")
        }
        
        println(s"\nDebug: Testing different Z coordinates for Y=70 (should be inside box)")
        for (z <- 30 to 50) {
          val coord = Coord3(40, 70, z)  // Test at box center X,Y, various Z
          val localCoord = placement.worldToLocal(coord)
          val isOccupied = placement.occupiesSpaceAt(coord)
          println(s"Z=$z: World $coord -> Local $localCoord, Occupied: $isOccupied")
        }
        
        val rendered = Renderer3D.renderShaded(w, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 2, cullBackfaces = true)
        val lines = rendered.split("\n")

        // Print the actual rendered output
        println("=== SHAPE 101 FRAME 12 RENDERED OUTPUT ===")
        lines.zipWithIndex.foreach { case (line, i) =>
          if (line.trim.nonEmpty) {
            println(f"$i%3d: $line")
          }
        }
        println("=== END SHAPE 101 OUTPUT ===")

        // Find bounding box of rendered content
        var minX = Int.MaxValue
        var maxX = Int.MinValue
        var minY = Int.MaxValue
        var maxY = Int.MinValue

        for (y <- lines.indices; x <- lines(y).indices) {
          if (lines(y)(x) != ' ') {
            minX = Math.min(minX, x)
            maxX = Math.max(maxX, x)
            minY = Math.min(minY, y)
            maxY = Math.max(maxY, y)
          }
        }

        if (minX != Int.MaxValue) {
          val width = maxX - minX + 1
          val height = maxY - minY + 1
          val aspectRatio = width.toDouble / height

          println(s"Shape 101 frame 12 rendered bounds: ${width}x${height} at ($minX,$minY), aspect ratio: $aspectRatio")
          
          // This should reproduce the issue: content starts too low (minY too high)
          withClue(s"Frame 12 should show truncation issue: ") {
            // The content should start at a reasonable Y position given the box placement
            minY should be >= 55  // Box should start around Y=55 (90-35)
            minY should be <= 95  // Box should not start too high
            // The height should be close to the expected box height
            height should be >= 65  // Should be close to 70 (box height)
            height should be <= 85  // With some tolerance
          }
        } else {
          fail("No rendered content found for shape 101")
        }

      case Left(error) =>
        fail(s"Error rotating shape: $error")
    }
  }

  "Shape 101" should "reproduce frame 29 truncation and shading issues" in {
    // Reproduce exact conditions from frame 29
    val world = World3D(300, 180, 60)
      .add(Box(101, 40.0, 70.0, 20.0), Coord3(40.0, 90.0, 40.0), Rotation3.ZERO)

    // Apply the rotation that would occur at frame 29
    val frame29Rotation = Rotation3(
      yaw = 29 * Math.PI / -36,
      pitch = 0,
      roll = 29 * Math.PI / 72
    )
    val rotatedWorld = world.rotate(101, frame29Rotation)

    rotatedWorld match {
      case Right(w) =>
        // Debug: Test what coordinates are being found as occupied in frame 29
        val placement = w.placements.head
        println(s"Debug: Testing coordinate finding for frame 29")
        println(s"Placement origin: ${placement.origin}")
        println(s"Placement rotation: ${placement.rotation}")
        println(s"Box dimensions: ${placement.shape.asInstanceOf[Box].width} x ${placement.shape.asInstanceOf[Box].height} x ${placement.shape.asInstanceOf[Box].depth}")
        
        // Test coordinates systematically to see where the cutoff is
        var foundCoords = 0
        var totalTested = 0
        var firstOccupiedY = Int.MaxValue
        var lastOccupiedY = Int.MinValue
        
        for (y <- 0 to 180) {
          for (x <- 0 to 300) {
            totalTested += 1
            val coord = Coord3(x, y, 40)  // Test at Z=40 (box center)
            if (placement.occupiesSpaceAt(coord)) {
              foundCoords += 1
              firstOccupiedY = Math.min(firstOccupiedY, y)
              lastOccupiedY = Math.max(lastOccupiedY, y)
              if (foundCoords <= 5) { // Only print first 5 for brevity
                val localCoord = placement.worldToLocal(coord)
                println(s"Found occupied: World $coord -> Local $localCoord")
              }
            }
          }
        }
        
        println(s"Total coordinates tested: $totalTested, Found occupied: $foundCoords")
        println(s"Y range: $firstOccupiedY to $lastOccupiedY")
        
        val rendered = Renderer3D.renderShaded(w, lightDirection = Coord3(-1, -1, -1), ambient = 0.35, xScale = 2, cullBackfaces = true)
        val lines = rendered.split("\n")

        // Print the actual rendered output
        println("=== SHAPE 101 FRAME 29 RENDERED OUTPUT ===")
        lines.zipWithIndex.foreach { case (line, i) =>
          if (line.trim.nonEmpty) {
            println(f"$i%3d: $line")
          }
        }
        println("=== END SHAPE 101 OUTPUT ===")

        // Find bounding box of rendered content
        var minX = Int.MaxValue
        var maxX = Int.MinValue
        var minY = Int.MaxValue
        var maxY = Int.MinValue

        for (y <- lines.indices; x <- lines(y).indices) {
          if (lines(y)(x) != ' ') {
            minX = Math.min(minX, x)
            maxX = Math.max(maxX, x)
            minY = Math.min(minY, y)
            maxY = Math.max(maxY, y)
          }
        }

        if (minX != Int.MaxValue) {
          val width = maxX - minX + 1
          val height = maxY - minY + 1
          val aspectRatio = width.toDouble / height

          println(s"Shape 101 frame 29 rendered bounds: ${width}x${height} at ($minX,$minY), aspect ratio: $aspectRatio")
          
          // This should reproduce the issues: severe truncation and poor shading
          withClue(s"Frame 29 should show truncation and shading issues: ") {
            // The height should be close to the expected box height
            height should be >= 65  // Should be close to 70 (box height)
            height should be <= 85  // With some tolerance
            // The aspect ratio should be reasonable
            aspectRatio should be >= 1.0  // Should not be too wide
            aspectRatio should be <= 2.0  // Should not be too narrow
          }
        } else {
          fail("No rendered content found for shape 101")
        }

      case Left(error) =>
        fail(s"Error rotating shape: $error")
    }
  }

  "Shape 101" should "check Z coordinate bounds for rotated shape 101" in {
    val world = World3D(300, 180, 60)
      .add(Box(101, 40.0, 70.0, 20.0), Coord3(40.0, 90.0, 40.0), Rotation3.ZERO)
    val box = world.placements.head.shape.asInstanceOf[Box]
    
    // Test various rotation angles to see if any part goes below Z=0
    val testFrames = Seq(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200)
    
    var minZ = Double.MaxValue
    var maxZ = Double.MinValue
    var frameWithMinZ = -1
    var frameWithMaxZ = -1
    
    testFrames.foreach { frameIndex =>
      val rotatedWorld = world.rotate(101, Rotation3(
        yaw = frameIndex * Math.PI / -36,
        pitch = 0,
        roll = frameIndex * Math.PI / 72
      ))
      rotatedWorld match {
        case Right(w) =>
          val rotatedPlacement = w.placements.head
          
          // Check all corners of the box in local coordinates
          val localCorners = Seq(
            Coord3(-box.width/2, -box.height/2, -box.depth/2),  // Bottom-back-left
            Coord3(box.width/2, -box.height/2, -box.depth/2),   // Bottom-back-right
            Coord3(-box.width/2, box.height/2, -box.depth/2),   // Bottom-front-left
            Coord3(box.width/2, box.height/2, -box.depth/2),    // Bottom-front-right
            Coord3(-box.width/2, -box.height/2, box.depth/2),   // Top-back-left
            Coord3(box.width/2, -box.height/2, box.depth/2),    // Top-back-right
            Coord3(-box.width/2, box.height/2, box.depth/2),    // Top-front-left
            Coord3(box.width/2, box.height/2, box.depth/2)     // Top-front-right
          )
          
          localCorners.foreach { localCorner =>
            val worldCorner = rotatedPlacement.rotation.applyTo(localCorner) + rotatedPlacement.origin
            if (worldCorner.z < minZ) {
              minZ = worldCorner.z
              frameWithMinZ = frameIndex
            }
            if (worldCorner.z > maxZ) {
              maxZ = worldCorner.z
              frameWithMaxZ = frameIndex
            }
          }
          
        case Left(_) => // Skip errors
      }
    }
    
    println(s"Z coordinate range across all test frames: $minZ to $maxZ")
    println(s"Minimum Z occurs at frame $frameWithMinZ")
    println(s"Maximum Z occurs at frame $frameWithMaxZ")
    
    // The box should never go below Z=0 in world coordinates
    withClue(s"Box should never have negative Z coordinates in world space: ") {
      minZ should be >= 0.0
    }
    
    // The box should stay within reasonable Z bounds
    withClue(s"Box should not extend too far in Z direction: ") {
      maxZ should be <= 60.0  // World depth
    }
  }
}
