case class World(width: Int, height: Int, depth: Int, private val shapes: Map[Int, Placement] = Map()) {
  // Validation
  require(width >= 0, "World width must be non-negative")
  require(height >= 0, "World height must be non-negative")
  require(depth >= 0, "World depth must be non-negative")

  def reset: World = World(width, height, depth)

  def add(shape: Shape, origin: Coord, rotation: Rotation = Rotation.ZERO): World =
    this.copy(shapes = shapes + (shape.id -> Placement(origin, rotation, shape)))

  def rotate(shapeId: Int, delta: Rotation): Either[NoSuchShape, World] =
    shapes.get(shapeId)
      .map(placement => {
        val newPlacement = placement.rotate(delta)
        // Check if the rotation would cause the shape to extend beyond world boundaries
        if (newPlacement.wouldExtendBeyondBounds(width, height, depth)) {
          // Return the original placement unchanged to prevent boundary violations
          this
        } else {
          this.copy(shapes = shapes + (shapeId -> newPlacement))
        }
      })
      .toRight(NoSuchShape(shapeId))

  def placements: Iterable[Placement] = shapes.values
}
