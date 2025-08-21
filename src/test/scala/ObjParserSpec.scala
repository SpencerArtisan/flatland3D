import org.scalatest.flatspec._
import org.scalatest.matchers._

class ObjParserSpec extends AnyFlatSpec with should.Matchers {
  // Simple cube OBJ data
  val cubeObj = """
    |# Simple cube with 8 vertices and 12 triangles
    |v -1.0 -1.0 -1.0  # 1: left-bottom-back
    |v  1.0 -1.0 -1.0  # 2: right-bottom-back
    |v  1.0  1.0 -1.0  # 3: right-top-back
    |v -1.0  1.0 -1.0  # 4: left-top-back
    |v -1.0 -1.0  1.0  # 5: left-bottom-front
    |v  1.0 -1.0  1.0  # 6: right-bottom-front
    |v  1.0  1.0  1.0  # 7: right-top-front
    |v -1.0  1.0  1.0  # 8: left-top-front
    |
    |# Back face
    |f 1 3 2
    |f 1 4 3
    |
    |# Front face
    |f 5 6 7
    |f 5 7 8
    |
    |# Left face
    |f 1 5 8
    |f 1 8 4
    |
    |# Right face
    |f 2 3 7
    |f 2 7 6
    |
    |# Bottom face
    |f 1 2 6
    |f 1 6 5
    |
    |# Top face
    |f 4 8 7
    |f 4 7 3
    |""".stripMargin

  "ObjParser" should "parse a simple cube correctly" in {
    val result = ObjParser.parse(cubeObj)
    result.isRight should be (true)
    
    val mesh = result.getOrElse(fail("Failed to parse cube"))
    mesh.triangles.size should be (12) // 6 faces * 2 triangles per face
    
    // Verify some vertices are in the correct positions
    val vertices = mesh.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2)).distinct
    vertices.size should be (8) // Cube has 8 unique vertices
    
    // Check that vertices form a cube
    val xs = vertices.map(_.x)
    val ys = vertices.map(_.y)
    val zs = vertices.map(_.z)
    
    xs.min should be (-1.0 +- 0.001)
    xs.max should be (1.0 +- 0.001)
    ys.min should be (-1.0 +- 0.001)
    ys.max should be (1.0 +- 0.001)
    zs.min should be (-1.0 +- 0.001)
    zs.max should be (1.0 +- 0.001)
  }

  it should "handle vertex/texture/normal face indices" in {
    val objWithTextures = """
      |v -1.0 -1.0 -1.0
      |v 1.0 -1.0 -1.0
      |v 1.0 1.0 -1.0
      |vt 0.0 0.0
      |vt 1.0 0.0
      |vt 1.0 1.0
      |vn 0.0 0.0 -1.0
      |f 1/1/1 2/2/1 3/3/1
      |""".stripMargin
    
    val result = ObjParser.parse(objWithTextures)
    result.isRight should be (true)
    
    val mesh = result.getOrElse(fail("Failed to parse"))
    mesh.triangles.size should be (1)
    mesh.triangles.head.v0 should be (Coord(-1.0, -1.0, -1.0))
    mesh.triangles.head.v1 should be (Coord(1.0, -1.0, -1.0))
    mesh.triangles.head.v2 should be (Coord(1.0, 1.0, -1.0))
  }

  it should "handle faces with more than 3 vertices" in {
    val objWithQuad = """
      |v -1.0 -1.0 0.0
      |v 1.0 -1.0 0.0
      |v 1.0 1.0 0.0
      |v -1.0 1.0 0.0
      |f 1 2 3 4
      |""".stripMargin
    
    val result = ObjParser.parse(objWithQuad)
    result.isRight should be (true)
    
    val mesh = result.getOrElse(fail("Failed to parse"))
    mesh.triangles.size should be (2) // Quad should be split into 2 triangles
  }

  it should "handle negative vertex indices" in {
    val objWithNegativeIndices = """
      |v -1.0 -1.0 0.0
      |v 1.0 -1.0 0.0
      |v 1.0 1.0 0.0
      |f -3 -2 -1
      |""".stripMargin
    
    val result = ObjParser.parse(objWithNegativeIndices)
    result.isRight should be (true)
    
    val mesh = result.getOrElse(fail("Failed to parse"))
    mesh.triangles.size should be (1)
  }

  it should "reject invalid vertex coordinates" in {
    val objWithInvalidVertex = """
      |v -1.0 abc 0.0
      |v 1.0 -1.0 0.0
      |v 1.0 1.0 0.0
      |f 1 2 3
      |""".stripMargin
    
    val result = ObjParser.parse(objWithInvalidVertex)
    result.isLeft should be (true)
    result.left.getOrElse("") should include ("Invalid vertex coordinates")
  }

  it should "reject invalid face indices" in {
    val objWithInvalidFace = """
      |v -1.0 -1.0 0.0
      |v 1.0 -1.0 0.0
      |v 1.0 1.0 0.0
      |f 1 2 xyz
      |""".stripMargin
    
    val result = ObjParser.parse(objWithInvalidFace)
    result.isLeft should be (true)
    result.left.getOrElse("") should include ("Invalid face index")
  }

  it should "reject out of range vertex indices" in {
    val objWithBadIndex = """
      |v -1.0 -1.0 0.0
      |v 1.0 -1.0 0.0
      |v 1.0 1.0 0.0
      |f 1 2 4
      |""".stripMargin
    
    val result = ObjParser.parse(objWithBadIndex)
    result.isLeft should be (true)
    result.left.getOrElse("") should include ("Vertex index out of range")
  }

  it should "reject faces with too few vertices" in {
    val objWithTwoVertexFace = """
      |v -1.0 -1.0 0.0
      |v 1.0 -1.0 0.0
      |v 1.0 1.0 0.0
      |f 1 2
      |""".stripMargin
    
    val result = ObjParser.parse(objWithTwoVertexFace)
    result.isLeft should be (true)
    result.left.getOrElse("") should include ("fewer than 3 vertices")
  }

  it should "scale mesh to fit target size" in {
    val result = ObjParser.parse(cubeObj)
    result.isRight should be (true)
    
    val mesh = result.getOrElse(fail("Failed to parse cube"))
    val scaledMesh = ObjParser.scaleMesh(mesh, 2.0)
    
    // Check that scaled mesh fits within target size
    val vertices = scaledMesh.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2))
    val xs = vertices.map(_.x)
    val ys = vertices.map(_.y)
    val zs = vertices.map(_.z)
    
    val width = xs.max - xs.min
    val height = ys.max - ys.min
    val depth = zs.max - zs.min
    
    width should be (2.0 +- 0.001)
    height should be (2.0 +- 0.001)
    depth should be (2.0 +- 0.001)
  }
}
