case class Placement(topLeft: Coord, rotatedRadians: Double, shape: Shape) {
  def occupiesSpaceAt(coord: Coord): Boolean = {
    val rotationCenter = this.topLeft + shape.center
    val coordRelativeToRotationCenter = coord - rotationCenter
    val rotatedCoordRelativeToRotationCenter = coordRelativeToRotationCenter.rotate(rotatedRadians)
    val rotatedCoord = rotatedCoordRelativeToRotationCenter + rotationCenter
    val relativeToPlacement = rotatedCoord - this.topLeft
    shape.occupiesSpaceAt(relativeToPlacement)
  }

  def rotate(radians: Double): Placement =
    new Placement(topLeft, rotatedRadians + radians, shape)
}
