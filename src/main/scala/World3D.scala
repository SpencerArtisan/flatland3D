
case class World3D(width: Int, height: Int, depth: Int, private val shapes: Map[Int, Placement3D] = Map()) {

  def reset: World3D = World3D(width, height, depth)

  def add(shape: Shape3, origin: Coord3, rotation: Rotation3 = Rotation3.ZERO): World3D =
    this.copy(shapes = shapes + (shape.id -> Placement3D(origin, rotation, shape)))

  def rotate(shapeId: Int, delta: Rotation3): Either[NoSuchShape, World3D] =
    shapes.get(shapeId)
      .map(placement => this.copy(shapes = shapes + (shapeId -> placement.rotate(delta))))
      .toRight(NoSuchShape(shapeId))

  def placements: Iterable[Placement3D] = shapes.values
}


