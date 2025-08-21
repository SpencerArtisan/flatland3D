import org.scalatest.flatspec._
import org.scalatest.matchers._

class TriangleSpec extends AnyFlatSpec with should.Matchers {

  "Triangle" should "calculate normal correctly" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(1, 0, 0), 
      Coord(0, 1, 0)
    )
    
    // Normal should point in +Z direction for counter-clockwise vertices
    triangle.normal should be (Coord(0, 0, 1))
  }

  "Triangle" should "calculate centroid correctly" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(3, 0, 0),
      Coord(0, 3, 0)
    )
    
    triangle.centroid should be (Coord(1, 1, 0))
  }

  "Triangle" should "detect ray intersection" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(1, 0, 0),
      Coord(0, 1, 0)
    )
    
    // Ray from below triangle pointing up should intersect
    val rayOrigin = Coord(0.25, 0.25, -1)
    val rayDirection = Coord(0, 0, 1)
    
    val intersection = triangle.intersectRay(rayOrigin, rayDirection)
    intersection should be (defined)
    intersection.get should be (1.0 +- 0.001)
  }

  "Triangle" should "reject ray that misses" in {
    val triangle = Triangle(
      Coord(0, 0, 0),
      Coord(1, 0, 0),
      Coord(0, 1, 0)
    )
    
    // Ray that misses the triangle
    val rayOrigin = Coord(2, 2, -1)  // Outside triangle bounds
    val rayDirection = Coord(0, 0, 1)
    
    triangle.intersectRay(rayOrigin, rayDirection) should be (None)
  }

  "TriangleCube" should "be constructed with correct triangle count" in {
    val cube = TriangleShapes.cube(1, 2.0) // 2x2x2 cube centered at origin
    
    // Cube should have 12 triangles (2 per face, 6 faces)
    cube.triangles.length should be (12)
    
    // All triangles should have valid normals
    cube.triangles.foreach { triangle =>
      triangle.normal.magnitude should be (1.0 +- 0.001)
    }
  }
  
  "TriangleCube" should "provide surface normals" in {
    val cube = TriangleShapes.cube(1, 2.0)
    
    // Test normals at different face centers (cube is 2.0 units wide, so faces are at ±1.0)
    val rightFaceNormal = cube.surfaceNormalAt(Coord(1.01, 0, 0))  // Just outside right face at x=+1.0
    val topFaceNormal = cube.surfaceNormalAt(Coord(0, 1.01, 0))    // Just outside top face at y=+1.0
    val frontFaceNormal = cube.surfaceNormalAt(Coord(0, 0, 1.01))  // Just outside front face at z=+1.0
    
    // Normals should point outward from cube faces
    rightFaceNormal.x should be > 0.0
    topFaceNormal.y should be > 0.0  
    frontFaceNormal.z should be > 0.0
  }

  // Spaceship mesh tests
  "Spaceship mesh" should "be a valid 3D model" in {
    val ship = TriangleShapes.cobra(1, 8.0)
    
    // Should have enough triangles to form a recognizable shape
    ship.triangles.length should be > 10
    
    // All triangles should have valid normals
    ship.triangles.foreach { triangle =>
      triangle.normal.magnitude should be (1.0 +- 0.001)
    }
    
    // Calculate bounding box
    val allVertices = ship.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2))
    val minX = allVertices.map(_.x).min
    val maxX = allVertices.map(_.x).max
    val minY = allVertices.map(_.y).min
    val maxY = allVertices.map(_.y).max
    val minZ = allVertices.map(_.z).min
    val maxZ = allVertices.map(_.z).max
    
    // Should have reasonable proportions (not completely flat in any dimension)
    val width = maxX - minX
    val height = maxY - minY
    val length = maxZ - minZ
    
    width should be > 0.0
    height should be > 0.0
    length should be > 0.0
    
    // Should respect the input size parameter
    val maxDimension = List(width, height, length).max
    maxDimension should be <= 8.0  // Input size
  }
  
  it should "have consistent surface normals" in {
    val ship = TriangleShapes.cobra(1, 8.0)
    
    // Sample a few points on the surface
    val normals = List(
      ship.surfaceNormalAt(Coord(0, 0, 4)),   // Front
      ship.surfaceNormalAt(Coord(0, 0, -4)),  // Rear
      ship.surfaceNormalAt(Coord(2, 0, 0)),   // Side
      ship.surfaceNormalAt(Coord(0, 1, 0))    // Top
    )
    
    // All normals should be unit vectors
    normals.foreach { normal =>
      normal.magnitude should be (1.0 +- 0.001)
    }
  }
}