
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
}


