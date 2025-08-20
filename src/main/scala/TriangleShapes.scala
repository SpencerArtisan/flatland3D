// Utility functions for creating common triangle-based shapes
object TriangleShapes {
  
  // Create a cube using triangles (2 triangles per face, 12 triangles total)
  def cube(id: Int, size: Double): TriangleMesh = {
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
    val vertices = Seq(
      Coord(0, size, 0),           // Top vertex
      Coord(-size, -size, -size),  // Base vertex 1
      Coord(size, -size, -size),   // Base vertex 2
      Coord(0, -size, size)        // Base vertex 3
    )
    
    val triangles = Seq(
      Triangle(vertices(0), vertices(1), vertices(2)), // Front face
      Triangle(vertices(0), vertices(2), vertices(3)), // Right face
      Triangle(vertices(0), vertices(3), vertices(1)), // Left face
      Triangle(vertices(1), vertices(3), vertices(2))  // Base face
    )
    
    TriangleMesh(id, triangles)
  }
  
  // Create a pyramid with square base (5 faces: 1 square base + 4 triangular sides)
  def pyramid(id: Int, baseSize: Double, height: Double): TriangleMesh = {
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
}