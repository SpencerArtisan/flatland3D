case class Placement(origin: Coord, rotation: Rotation, shape: Shape) {
  // Validation
  require(shape != null, "Shape cannot be null")
  
  def occupiesSpaceAt(coord: Coord): Boolean = {
    // Transform world coordinate to local coordinate system
    val localCoord = worldToLocal(coord)
    // Check if it's inside the shape in local coordinates
    shape.occupiesSpaceAt(localCoord)
  }

  def rotate(delta: Rotation): Placement =
    this.copy(rotation = Rotation(
      yaw = rotation.yaw + delta.yaw,
      pitch = rotation.pitch + delta.pitch,
      roll = rotation.roll + delta.roll
    ))

  def worldToLocal(coord: Coord): Coord = {
    // Transform from world space to local space:
    // 1. Translate by -(origin + shape.center) to get to box center
    // 2. Apply inverse rotation (rotate by negative angles)
    val boxCenter = origin + shape.center
    val translated = coord - boxCenter
    // Use the proper inverse rotation that applies the rotations in the correct order
    rotation.inverse.applyTo(translated)
  }
  
  // Check if this placement would extend beyond world boundaries
  def wouldExtendBeyondBounds(worldWidth: Int, worldHeight: Int, worldDepth: Int): Boolean = {
    // For infinite worlds (dimensions = 0), no boundary checking needed
    if (worldWidth == 0 && worldHeight == 0 && worldDepth == 0) {
      return false
    }
    
    require(worldWidth > 0 && worldHeight > 0 && worldDepth > 0, "World dimensions must be positive")
    
    shape match {
      case triangleMesh: TriangleMesh =>
        // Check all vertices of the triangle mesh
        val vertices = triangleMesh.triangles.flatMap { triangle =>
          Seq(triangle.v0, triangle.v1, triangle.v2)
        }.distinct
        
        vertices.exists { localVertex =>
          val worldVertex = rotation.applyTo(localVertex) + origin
          worldVertex.x < 0 || worldVertex.x >= worldWidth ||
          worldVertex.y < 0 || worldVertex.y >= worldHeight ||
          worldVertex.z < 0 || worldVertex.z >= worldDepth
        }
      case _ => 
        // For other shape types, assume they don't extend beyond bounds
        // This could be improved by adding bounds calculation to the Shape trait
        false
    }
  }
}
