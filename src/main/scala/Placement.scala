case class Placement(origin: Coord, rotation: Rotation, shape: Shape) {
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
    val inverseRotation = Rotation(-rotation.yaw, -rotation.pitch, -rotation.roll)
    inverseRotation.applyTo(translated)
  }
  
  // Check if this placement would extend beyond world boundaries
  def wouldExtendBeyondBounds(worldWidth: Int, worldHeight: Int, worldDepth: Int): Boolean = {
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
      case _ => false // Unknown shape type
    }
  }
}
