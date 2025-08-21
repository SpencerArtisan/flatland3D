// Utility functions for creating common triangle-based shapes
object TriangleShapes {
  // Configuration constants
  private val CUBE_TRIANGLES_PER_FACE = 2
  private val CUBE_TOTAL_TRIANGLES = 12
  private val TETRAHEDRON_FACES = 4
  private val PYRAMID_BASE_TRIANGLES = 2
  private val PYRAMID_SIDE_TRIANGLES = 4
  private val PYRAMID_TOTAL_TRIANGLES = PYRAMID_BASE_TRIANGLES + PYRAMID_SIDE_TRIANGLES
  
  // Create a cube using triangles (2 triangles per face, 12 triangles total)
  def cube(id: Int, size: Double): TriangleMesh = {
    require(size > 0, "Cube size must be positive")
    val half = size / 2
    
    // Define 8 vertices of the cube
    val vertices = Seq(
      Coord(-half, -half, -half), // 0: left-bottom-back
      Coord( half, -half, -half), // 1: right-bottom-back
      Coord( half,  half, -half), // 2: right-top-back
      Coord(-half,  half, -half), // 3: left-top-back
      Coord(-half, -half,  half), // 4: left-bottom-front
      Coord( half, -half,  half), // 5: right-bottom-front
      Coord( half,  half,  half), // 6: right-top-front
      Coord(-half,  half,  half)  // 7: left-top-front
    )
    
    // Define triangles for each face (counter-clockwise from outside)
    val triangles = Seq(
      // Back face (z = -half)
      Triangle(vertices(0), vertices(2), vertices(1)),
      Triangle(vertices(0), vertices(3), vertices(2)),
      
      // Front face (z = +half)
      Triangle(vertices(4), vertices(5), vertices(6)),
      Triangle(vertices(4), vertices(6), vertices(7)),
      
      // Left face (x = -half)
      Triangle(vertices(0), vertices(4), vertices(7)),
      Triangle(vertices(0), vertices(7), vertices(3)),
      
      // Right face (x = +half)
      Triangle(vertices(1), vertices(2), vertices(6)),
      Triangle(vertices(1), vertices(6), vertices(5)),
      
      // Bottom face (y = -half)
      Triangle(vertices(0), vertices(1), vertices(5)),
      Triangle(vertices(0), vertices(5), vertices(4)),
      
      // Top face (y = +half)
      Triangle(vertices(3), vertices(7), vertices(6)),
      Triangle(vertices(3), vertices(6), vertices(2))
    )
    
    TriangleMesh(id, triangles)
  }
  
  // Create a simple tetrahedron (4 triangular faces)
  def tetrahedron(id: Int, size: Double): TriangleMesh = {
    require(size > 0, "Tetrahedron size must be positive")
    val vertices = Seq(
      Coord(0, size, 0),           // Top vertex
      Coord(-size, -size, -size),  // Base vertex 1
      Coord(size, -size, -size),   // Base vertex 2
      Coord(0, -size, size)        // Base vertex 3
    )
    
    val triangles = Seq(
      Triangle(vertices(0), vertices(2), vertices(1)), // Front face (counter-clockwise from outside)
      Triangle(vertices(0), vertices(3), vertices(2)), // Right face (counter-clockwise from outside)
      Triangle(vertices(0), vertices(1), vertices(3)), // Left face (counter-clockwise from outside)
      Triangle(vertices(1), vertices(2), vertices(3))  // Base face (counter-clockwise from outside)
    )
    
    TriangleMesh(id, triangles)
  }
  
  // Create a pyramid with square base (5 faces: 1 square base + 4 triangular sides)
  def pyramid(id: Int, baseSize: Double, height: Double): TriangleMesh = {
    require(baseSize > 0, "Pyramid base size must be positive")
    require(height > 0, "Pyramid height must be positive")
    val half = baseSize / 2
    
    val vertices = Seq(
      Coord(0, height / 2, 0),     // Top vertex (apex)
      Coord(-half, -height / 2, -half), // Base vertex 1
      Coord(half, -height / 2, -half),  // Base vertex 2
      Coord(half, -height / 2, half),   // Base vertex 3
      Coord(-half, -height / 2, half)   // Base vertex 4
    )
    
    val triangles = Seq(
      // Square base (2 triangles)
      Triangle(vertices(1), vertices(2), vertices(3)),
      Triangle(vertices(1), vertices(3), vertices(4)),
      
      // Triangular sides
      Triangle(vertices(0), vertices(2), vertices(1)), // Side 1
      Triangle(vertices(0), vertices(3), vertices(2)), // Side 2
      Triangle(vertices(0), vertices(4), vertices(3)), // Side 3
      Triangle(vertices(0), vertices(1), vertices(4))  // Side 4
    )
    
    TriangleMesh(id, triangles)
  }

  // Create Elite Cobra Mk III spaceship (for Easter egg)
  // Based on authentic 1984 Elite wireframe design
  def cobra(id: Int, size: Double): TriangleMesh = {
    require(size > 0, "Cobra size must be positive")
    val scale = size / 8.0  // Normalize to size 8 reference
    
    // Authentic Elite Cobra Mk III vertices - based on 1984 wireframe design
    // Coordinate system: +X=right, +Y=up, +Z=forward (toward nose)
    val vertices = Seq(
      // Blunt angular nose cone (characteristic Elite shape)
      Coord(0, 0, 4.5 * scale),                        // 0: Nose tip
      Coord(-1.0 * scale, 0, 3.8 * scale),            // 1: Nose left edge
      Coord(1.0 * scale, 0, 3.8 * scale),             // 2: Nose right edge
      Coord(0, -0.8 * scale, 3.8 * scale),            // 3: Nose bottom edge
      Coord(0, 0.8 * scale, 3.8 * scale),             // 4: Nose top edge
      
      // Tapering diamond cross-section main body
      Coord(-1.4 * scale, 0, 2.0 * scale),            // 5: Front left body edge
      Coord(1.4 * scale, 0, 2.0 * scale),             // 6: Front right body edge
      Coord(0, -1.0 * scale, 2.0 * scale),            // 7: Front bottom body edge
      Coord(0, 1.0 * scale, 2.0 * scale),             // 8: Front top body edge
      Coord(-1.2 * scale, 0, 0.5 * scale),            // 9: Mid-left body
      Coord(1.2 * scale, 0, 0.5 * scale),             // 10: Mid-right body
      Coord(0, -0.8 * scale, 0.5 * scale),            // 11: Mid-bottom body
      Coord(0, 0.8 * scale, 0.5 * scale),             // 12: Mid-top body
      
      // Gracefully swept-back wings
      Coord(-3.2 * scale, 0.2 * scale, -1.0 * scale), // 13: Left wing tip
      Coord(3.2 * scale, 0.2 * scale, -1.0 * scale), // 14: Right wing tip
      Coord(-2.2 * scale, -0.4 * scale, -0.5 * scale), // 15: Left wing bottom
      Coord(2.2 * scale, -0.4 * scale, -0.5 * scale), // 16: Right wing bottom
      Coord(-2.2 * scale, 0.6 * scale, -0.5 * scale), // 17: Left wing top
      Coord(2.2 * scale, 0.6 * scale, -0.5 * scale), // 18: Right wing top
      
      // Distinct engine pods
      Coord(-1.8 * scale, -0.6 * scale, -3.2 * scale), // 19: Left engine rear
      Coord(1.8 * scale, -0.6 * scale, -3.2 * scale), // 20: Right engine rear
      Coord(-1.8 * scale, 0.2 * scale, -2.6 * scale), // 21: Left engine mid
      Coord(1.8 * scale, 0.2 * scale, -2.6 * scale), // 22: Right engine mid
      
      // Additional vertices for engine pod detail
      Coord(-1.8 * scale, -0.2 * scale, -2.8 * scale), // 23: Left engine rear top
      Coord(1.8 * scale, -0.2 * scale, -2.8 * scale), // 24: Right engine rear top
      Coord(-1.8 * scale, -0.4 * scale, -2.5 * scale), // 25: Left engine mid bottom
      Coord(1.8 * scale, -0.4 * scale, -2.5 * scale)  // 26: Right engine mid bottom
    )
    
    // Define triangles for authentic Elite Cobra wireframe appearance
    val triangles = Seq(
      // Blunt angular nose cone (4 triangular faces)
      Triangle(vertices(0), vertices(1), vertices(4)),  // Left-top nose face
      Triangle(vertices(0), vertices(4), vertices(2)),  // Right-top nose face  
      Triangle(vertices(0), vertices(2), vertices(3)),  // Right-bottom nose face
      Triangle(vertices(0), vertices(3), vertices(1)),  // Left-bottom nose face
      
      // Tapering diamond body - front section
      Triangle(vertices(1), vertices(5), vertices(8)),  // Left-top front
      Triangle(vertices(2), vertices(8), vertices(6)),  // Right-top front
      Triangle(vertices(2), vertices(6), vertices(7)),  // Right-bottom front
      Triangle(vertices(1), vertices(7), vertices(5)),  // Left-bottom front
      Triangle(vertices(3), vertices(7), vertices(1)),  // Bottom-left front
      Triangle(vertices(3), vertices(2), vertices(7)),  // Bottom-right front
      Triangle(vertices(4), vertices(1), vertices(8)),  // Top-left front
      Triangle(vertices(4), vertices(8), vertices(2)),  // Top-right front
      
      // Tapering diamond body - middle section
      Triangle(vertices(5), vertices(9), vertices(12)), // Left-top mid
      Triangle(vertices(6), vertices(12), vertices(10)), // Right-top mid
      Triangle(vertices(6), vertices(10), vertices(11)), // Right-bottom mid
      Triangle(vertices(5), vertices(11), vertices(9)), // Left-bottom mid
      Triangle(vertices(7), vertices(11), vertices(5)), // Bottom-left mid
      Triangle(vertices(7), vertices(6), vertices(11)), // Bottom-right mid
      Triangle(vertices(8), vertices(5), vertices(12)), // Top-left mid
      Triangle(vertices(8), vertices(12), vertices(6)), // Top-right mid
      
      // Gracefully swept wings
      Triangle(vertices(9), vertices(15), vertices(17)), // Left wing root
      Triangle(vertices(9), vertices(17), vertices(12)), // Left wing top
      Triangle(vertices(15), vertices(13), vertices(17)), // Left wing outer
      Triangle(vertices(10), vertices(12), vertices(18)), // Right wing root
      Triangle(vertices(10), vertices(18), vertices(16)), // Right wing top
      Triangle(vertices(16), vertices(18), vertices(14)), // Right wing outer
      
      // Distinct engine pods - left
      Triangle(vertices(15), vertices(21), vertices(25)), // Left engine top front
      Triangle(vertices(25), vertices(19), vertices(15)), // Left engine bottom front
      Triangle(vertices(21), vertices(23), vertices(19)), // Left engine top rear
      Triangle(vertices(19), vertices(25), vertices(21)), // Left engine side
      
      // Distinct engine pods - right
      Triangle(vertices(16), vertices(26), vertices(22)), // Right engine top front
      Triangle(vertices(20), vertices(16), vertices(26)), // Right engine bottom front
      Triangle(vertices(24), vertices(22), vertices(20)), // Right engine top rear
      Triangle(vertices(26), vertices(22), vertices(20)), // Right engine side
      
      // Engine pod connections
      Triangle(vertices(21), vertices(22), vertices(25)), // Engine connection top
      Triangle(vertices(25), vertices(26), vertices(19)), // Engine connection bottom
      Triangle(vertices(19), vertices(26), vertices(20))  // Engine connection rear
    )
    
    TriangleMesh(id, triangles)
  }
}