// Parser for Wavefront OBJ files
// Supports basic geometry (vertices and faces) for loading 3D models
object ObjParser {
  // Parse OBJ file content into vertices and triangles
  def parse(content: String): Either[String, TriangleMesh] = {
    try {
      val vertices = scala.collection.mutable.ArrayBuffer[Coord]()
      val faces = scala.collection.mutable.ArrayBuffer[Triangle]()
      
      content.linesIterator.zipWithIndex.foreach { case (line, lineNum) =>
        val trimmed = line.trim
        if (!trimmed.isEmpty && !trimmed.startsWith("#")) {
          trimmed.split("\\s+").toList match {
            // Vertex: v x y z
            case "v" :: x :: y :: z :: _ =>
              try {
                vertices += Coord(x.toDouble, y.toDouble, z.toDouble)
              } catch {
                case e: NumberFormatException =>
                  return Left(s"Invalid vertex coordinates on line ${lineNum + 1}: $line")
              }
            
            // Face: f v1 v2 v3 (or f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3)
            case "f" :: indices =>
              // Extract vertex indices (ignoring texture/normal indices)
              val vertexIndices = indices.map { index =>
                try {
                  // Handle both plain indices and v/vt/vn format
                  val vertexIndex = index.split("/")(0).toInt
                  // Convert 1-based OBJ indices to 0-based
                  if (vertexIndex > 0) vertexIndex - 1
                  else if (vertexIndex < 0) vertices.size + vertexIndex // Negative indices are relative to end
                  else return Left(s"Invalid vertex index 0 on line ${lineNum + 1}")
                } catch {
                  case _: NumberFormatException =>
                    return Left(s"Invalid face index on line ${lineNum + 1}: $index")
                  case _: ArrayIndexOutOfBoundsException =>
                    return Left(s"Invalid face format on line ${lineNum + 1}")
                }
              }
              
              // Validate indices
              if (vertexIndices.exists(i => i < 0 || i >= vertices.size)) {
                return Left(s"Vertex index out of range on line ${lineNum + 1}")
              }
              
              // Handle faces with more than 3 vertices by triangulating
              if (vertexIndices.size < 3) {
                return Left(s"Face has fewer than 3 vertices on line ${lineNum + 1}")
              } else if (vertexIndices.size == 3) {
                // Triangle face - use directly
                faces += Triangle(
                  vertices(vertexIndices(0)),
                  vertices(vertexIndices(1)),
                  vertices(vertexIndices(2))
                )
              } else {
                // N-gon face - triangulate using fan triangulation
                // This works for convex polygons but may not be ideal for concave ones
                for (i <- 1 until vertexIndices.size - 1) {
                  faces += Triangle(
                    vertices(vertexIndices(0)),
                    vertices(vertexIndices(i)),
                    vertices(vertexIndices(i + 1))
                  )
                }
              }
            
            // Ignore other OBJ elements (textures, normals, etc.)
            case _ =>
          }
        }
      }
      
      if (vertices.isEmpty) {
        Left("No vertices found in OBJ file")
      } else if (faces.isEmpty) {
        Left("No faces found in OBJ file")
      } else {
        Right(TriangleMesh(1, faces.toSeq)) // TODO: Allow ID to be specified
      }
      
    } catch {
      case e: Exception =>
        Left(s"Error parsing OBJ file: ${e.getMessage}")
    }
  }
  
  // Helper method to scale a mesh to fit within a given size
  def scaleMesh(mesh: TriangleMesh, targetSize: Double): TriangleMesh = {
    // Find current bounds
    val vertices = mesh.triangles.flatMap(t => Seq(t.v0, t.v1, t.v2))
    val minX = vertices.map(_.x).min
    val maxX = vertices.map(_.x).max
    val minY = vertices.map(_.y).min
    val maxY = vertices.map(_.y).max
    val minZ = vertices.map(_.z).min
    val maxZ = vertices.map(_.z).max
    
    // Calculate center and size
    val center = Coord(
      (minX + maxX) / 2,
      (minY + maxY) / 2,
      (minZ + maxZ) / 2
    )
    val size = math.max(
      math.max(maxX - minX, maxY - minY),
      maxZ - minZ
    )
    
    // Scale and center the mesh
    val scale = if (size > 0) targetSize / size else 1.0
    val scaledTriangles = mesh.triangles.map { t =>
      Triangle(
        (t.v0 - center) * scale,
        (t.v1 - center) * scale,
        (t.v2 - center) * scale
      )
    }
    
    TriangleMesh(mesh.id, scaledTriangles)
  }
}
