
case class Placement3D(origin: Coord3, rotation: Rotation3, shape: Shape3) {
  def occupiesSpaceAt(coord: Coord3): Boolean = {
    // Transform world coordinate to local coordinate system
    val localCoord = worldToLocal(coord)
    // Check if it's inside the shape in local coordinates
    shape.occupiesSpaceAt(localCoord)
  }

  def rotate(delta: Rotation3): Placement3D =
    this.copy(rotation = Rotation3(
      yaw = rotation.yaw + delta.yaw,
      pitch = rotation.pitch + delta.pitch,
      roll = rotation.roll + delta.roll
    ))

  def worldToLocal(coord: Coord3): Coord3 = {
    // Transform from world space to local space:
    // 1. Translate by -(origin + shape.center) to get to box center
    // 2. Apply inverse rotation (rotate by negative angles)
    val boxCenter = origin + shape.center
    val translated = coord - boxCenter
    val inverseRotation = Rotation3(-rotation.yaw, -rotation.pitch, -rotation.roll)
    inverseRotation.applyTo(translated)
  }
  
  // Check if this placement would extend beyond world boundaries
  def wouldExtendBeyondBounds(worldWidth: Int, worldHeight: Int, worldDepth: Int): Boolean = {
    val box = shape.asInstanceOf[Box]
    val halfWidth = box.width / 2
    val halfHeight = box.height / 2
    val halfDepth = box.depth / 2
    
    // Check all 8 corners of the box after rotation
    val corners = Seq(
      Coord3(-halfWidth, -halfHeight, -halfDepth),
      Coord3(halfWidth, -halfHeight, -halfDepth),
      Coord3(-halfWidth, halfHeight, -halfDepth),
      Coord3(halfWidth, halfHeight, -halfDepth),
      Coord3(-halfWidth, -halfHeight, halfDepth),
      Coord3(halfWidth, -halfHeight, halfDepth),
      Coord3(-halfWidth, halfHeight, halfDepth),
      Coord3(halfWidth, halfHeight, halfDepth)
    )
    
    corners.exists { localCorner =>
      val worldCorner = rotation.applyTo(localCorner) + origin
      worldCorner.x < 0 || worldCorner.x >= worldWidth ||
      worldCorner.y < 0 || worldCorner.y >= worldHeight ||
      worldCorner.z < 0 || worldCorner.z >= worldDepth
    }
  }
}


