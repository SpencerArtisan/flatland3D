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

  // Check if this is an infinite world (no boundaries)
  def isInfinite: Boolean = width == 0 && height == 0 && depth == 0

  // Get placements that are within a specific viewport
  def placementsInViewport(viewport: Viewport): Iterable[Placement] = {
    if (isInfinite) {
      // For infinite worlds, filter by viewport bounds
      shapes.values.filter(placement => {
        // Check if any part of the shape is within the viewport
        placement.shape match {
          case triangleMesh: TriangleMesh =>
            val vertices = triangleMesh.triangles.flatMap { triangle =>
              Seq(triangle.v0, triangle.v1, triangle.v2)
            }.distinct
            
            vertices.exists { localVertex =>
              val worldVertex = placement.rotation.applyTo(localVertex) + placement.origin
              viewport.containsWorldCoord(worldVertex)
            }
          case _ => false
        }
      })
    } else {
      // For bounded worlds, return all placements
      shapes.values
    }
  }
}

object World {
  // Create an infinite world with no boundaries
  val infinite: World = World(0, 0, 0)
}
