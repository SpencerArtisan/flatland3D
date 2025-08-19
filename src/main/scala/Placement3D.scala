
case class Placement3D(origin: Coord3, rotation: Rotation3, shape: Shape3) {
  def occupiesSpaceAt(coord: Coord3): Boolean = {
    val rotationCenter = origin + shape.center
    val coordRelativeToRotationCenter = coord - rotationCenter
    val rotatedCoordRelativeToRotationCenter = rotation.applyTo(coordRelativeToRotationCenter)
    val rotatedCoord = rotatedCoordRelativeToRotationCenter + rotationCenter
    val relativeToPlacement = rotatedCoord - origin
    shape.occupiesSpaceAt(relativeToPlacement)
  }

  def rotate(delta: Rotation3): Placement3D =
    this.copy(rotation = Rotation3(
      yaw = rotation.yaw + delta.yaw,
      pitch = rotation.pitch + delta.pitch,
      roll = rotation.roll + delta.roll
    ))

  def worldToLocal(coord: Coord3): Coord3 = {
    val rotationCenter = origin + shape.center
    val coordRelativeToRotationCenter = coord - rotationCenter
    val rotatedCoordRelativeToRotationCenter = rotation.applyTo(coordRelativeToRotationCenter)
    val rotatedCoord = rotatedCoordRelativeToRotationCenter + rotationCenter
    rotatedCoord - origin
  }
}


