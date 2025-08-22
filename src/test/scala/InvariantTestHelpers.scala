import org.scalatest.matchers.should.Matchers

/**
 * Helper utilities for invariant testing in Flatland3D.
 * Provides reusable components for testing mathematical and logical invariants
 * across different system components.
 */
object InvariantTestHelpers extends Matchers {
  
  /**
   * Standard orthogonal viewing rotations for cube face testing.
   * Each rotation positions the cube so one face is viewed straight-on.
   */
  val OrthogonalViews: List[(Rotation, String)] = List(
    (Rotation.ZERO, "front"),                                    // Front view (default)
    (Rotation(yaw = Math.PI, pitch = 0, roll = 0), "back"),     // Back view  
    (Rotation(yaw = Math.PI/2, pitch = 0, roll = 0), "right"),  // Right view
    (Rotation(yaw = -Math.PI/2, pitch = 0, roll = 0), "left"),  // Left view
    (Rotation(yaw = 0, pitch = Math.PI/2, roll = 0), "top"),    // Top view
    (Rotation(yaw = 0, pitch = -Math.PI/2, roll = 0), "bottom") // Bottom view
  )
  
  /**
   * Creates a viewport centered on the origin that can contain a shape of the given size.
   */
  def createCenteredViewport(shapeSize: Double, margin: Double = 4.0): Viewport = {
    val viewportSize = (shapeSize * margin).toInt
    Viewport(
      Coord.ZERO, // Center the viewport at the origin where our shapes are placed
      viewportSize, 
      viewportSize,
      viewportSize
    )
  }
  
  /**
   * Renders a world from multiple orthogonal views and returns the results.
   */
  def renderOrthogonalViews(
    world: World, 
    shape: TriangleMesh, 
    shapeId: Int,
    origin: Coord = Coord.ZERO,
    lightDirection: Coord = Coord(-1, -1, -1).normalize,
    ambient: Double = 0.35
  ): List[(String, String)] = {
    
    val viewport = createCenteredViewport(10.0) // Assume reasonable default size
    
    OrthogonalViews.map { case (rotation, viewName) =>
      val rotatedWorld = world.add(shape, origin, rotation)
      val rendered = Renderer.renderShadedForward(
        rotatedWorld,
        lightDirection = lightDirection,
        ambient = ambient,
        xScale = 1,
        viewport = Some(viewport)
      )
      (viewName, rendered)
    }
  }
  
  /**
   * Extracts the actual face content from a rendered viewport, removing borders and whitespace.
   * Returns a clean square of characters representing the rendered face.
   */
  def extractFaceContent(rendered: String): String = {
    val allLines = rendered.split("\n")
    
    // Find the face lines - they contain non-whitespace characters but aren't borders
    val faceLines = allLines
      .filter(line => line.exists(c => !c.isWhitespace))  // Find non-empty lines
      .filter(line => !line.contains("┌") && !line.contains("└"))  // Skip frame borders
      .map(line => line.replaceAll("[│]", "").trim)  // Remove vertical borders and trim
      .filter(_.nonEmpty)  // Skip any empty lines after cleaning
    
    // Take a square region from the center (assuming face is roughly square)
    val faceSize = if (faceLines.nonEmpty) Math.min(5, faceLines.head.length) else 0
    val cleanedLines = faceLines.map(line => 
      if (line.length >= faceSize) line.takeRight(faceSize) else line
    )
    
    cleanedLines.mkString("\n")
  }
  
  /**
   * Asserts that all rendered views have identical face patterns.
   * This is a key invariant for orthogonal cube face rendering.
   */
  def assertIdenticalFaceShading(renderings: List[(String, String)]): Unit = {
    require(renderings.nonEmpty, "Must provide at least one rendering")
    
    val facesWithContent = renderings.map { case (viewName, rendered) =>
      val faceContent = extractFaceContent(rendered)
      withClue(s"$viewName view should not be empty: ") {
        faceContent should not be empty
      }
      (viewName, faceContent)
    }
    
    // Extract reference shading from the first view
    val (refViewName, refFaceContent) = facesWithContent.head
    val refLines = refFaceContent.split("\n")
    val refShading = if (refLines.nonEmpty && refLines.head.nonEmpty) refLines.head.head else ' '
    
    // Verify all views match the reference shading
    facesWithContent.foreach { case (viewName, faceContent) =>
      val lines = faceContent.split("\n")
      
      withClue(s"$viewName view should have same dimensions as $refViewName: ") {
        lines.length should be(refLines.length)
        if (lines.nonEmpty && refLines.nonEmpty) {
          lines.head.length should be(refLines.head.length)
        }
      }
      
      withClue(s"$viewName view should have identical shading to $refViewName ('$refShading'): ") {
        lines.foreach { line =>
          line.foreach { char =>
            char should be(refShading)
          }
        }
      }
    }
  }
  
  /**
   * Calculates the rotation needed to view a face straight-on (face normal pointing toward -Z).
   * This is used for shapes with non-axis-aligned faces like tetrahedrons.
   */
  def calculateFaceViewingRotation(faceNormal: Coord): Rotation = {
    val targetDirection = Coord(0, 0, -1) // We want to view the face straight-on (normal toward viewer)
    
    // If the normal is already pointing toward -Z, no rotation needed
    if ((faceNormal - targetDirection).magnitude < 1e-6) {
      return Rotation.ZERO
    }
    
    // If the normal is pointing toward +Z, rotate 180° around Y
    if ((faceNormal - Coord(0, 0, 1)).magnitude < 1e-6) {
      return Rotation(yaw = Math.PI, pitch = 0, roll = 0)
    }
    
    // For general case, find rotation that aligns the normal with -Z
    // This is a simplified approach - for a full implementation, we'd use quaternions or matrix math
    val cross = faceNormal.cross(targetDirection)
    val dot = faceNormal.dot(targetDirection)
    
    // Calculate rotation angle
    val angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)))
    
    // Use cross product to determine rotation axis and construct Euler angles
    // This is an approximation - more complex shapes may need more sophisticated calculation
    val yaw = Math.atan2(cross.x, -cross.z)
    val pitch = Math.atan2(cross.y, Math.sqrt(cross.x * cross.x + cross.z * cross.z))
    
    Rotation(yaw = yaw, pitch = pitch, roll = 0)
  }
  
  /**
   * Creates viewing rotations for all faces of a triangle mesh by analyzing face normals.
   * Returns a list of (rotation, faceName) pairs for viewing each face straight-on.
   */
  def createFaceViewingRotations(triangleMesh: TriangleMesh): List[(Rotation, String)] = {
    // Group triangles by their normals (faces with same normal belong to same face)
    val faceGroups = triangleMesh.triangles.groupBy(_.normal)
    
    faceGroups.toList.zipWithIndex.map { case ((normal, triangles), index) =>
      val rotation = calculateFaceViewingRotation(normal)
      val faceName = s"face${index + 1}"
      (rotation, faceName)
    }
  }
  
  /**
   * Renders a triangle mesh from all its face-on views and returns the results.
   * This works for any shape by analyzing the face normals.
   * 
   * For a true invariant test, the light direction is transformed relative to each
   * face's orientation so that each face experiences equivalent lighting conditions.
   */
  def renderAllFaceViews(
    world: World,
    shape: TriangleMesh, 
    shapeId: Int,
    origin: Coord = Coord.ZERO,
    lightDirection: Coord = Coord(-1, -1, -1).normalize,
    ambient: Double = 0.35
  ): List[(String, String)] = {
    
    val viewport = createCenteredViewport(10.0) // Assume reasonable default size
    val faceRotations = createFaceViewingRotations(shape)
    
    faceRotations.map { case (rotation, faceName) =>
      val rotatedWorld = world.add(shape, origin, rotation)
      
      // Transform the light direction to maintain equivalent lighting conditions
      // Apply the inverse rotation to the light so each face sees light from the same relative angle
      val transformedLight = rotation.inverse.applyTo(lightDirection)
      
      val rendered = Renderer.renderShadedForward(
        rotatedWorld,
        lightDirection = transformedLight,
        ambient = ambient,
        xScale = 1,
        viewport = Some(viewport)
      )
      (faceName, rendered)
    }
  }
  
  /**
   * Verifies that a mathematical operation maintains an invariant property.
   * Useful for testing properties like "rotate 360° returns to original state".
   */
  def verifyInvariant[T](
    initial: T, 
    operation: T => T, 
    iterations: Int = 1,
    invariantName: String = "operation"
  )(invariantCheck: (T, T) => Unit): Unit = {
    
    val result = (1 to iterations).foldLeft(initial) { (current, _) =>
      operation(current)  
    }
    
    withClue(s"Invariant '$invariantName' should hold after $iterations iterations: ") {
      invariantCheck(initial, result)
    }
  }
}
