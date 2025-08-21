# Flatland3D Technical Reference

## Mathematical Foundations

### 3D Coordinate System
- **Right-handed coordinate system**: X right, Y up, Z forward
- **Origin**: Top-left corner of the world
- **Units**: Arbitrary units (typically integers for world boundaries, doubles for calculations)

### Vector Mathematics

#### Coordinate Operations
```scala
case class Coord(x: Double, y: Double, z: Double)
```

**Vector Operations**:
- **Addition**: `a + b = Coord(a.x + b.x, a.y + b.y, a.z + b.z)`
- **Subtraction**: `a - b = Coord(a.x - b.x, a.y - b.y, a.z - b.z)`
- **Scalar Multiplication**: `a * s = Coord(a.x * s, a.y * s, a.z * s)`
- **Dot Product**: `a · b = a.x * b.x + a.y * b.y + a.z * b.z`
- **Cross Product**: `a × b = Coord(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)`

**Utility Functions**:
- **Magnitude**: `|v| = √(v.x² + v.y² + v.z²)`
- **Normalization**: `v̂ = v / |v|`
- **Distance**: `d(a,b) = |b - a|`
- **Midpoint**: `mid(a,b) = (a + b) / 2`
- **Linear Interpolation**: `lerp(a,b,t) = a + (b - a) * t` where `t ∈ [0,1]`

### Rotation System

#### Euler Angles
- **Yaw**: Rotation around Z-axis (left/right)
- **Pitch**: Rotation around Y-axis (up/down)  
- **Roll**: Rotation around X-axis (forward/backward)

#### Rotation Order
**Implementation**: Roll (X) → Pitch (Y) → Yaw (Z)
**Matrix Form**: `R = Rz(ψ) × Ry(θ) × Rx(φ)`

**Individual Rotation Matrices**:
```
Roll (X):     Pitch (Y):    Yaw (Z):
[1  0    0 ] [cos θ 0 sin θ] [cos ψ -sin ψ 0]
[0 cos φ -sin φ] [0   1   0   ] [sin ψ  cos ψ 0]
[0 sin φ  cos φ] [-sin θ 0 cos θ] [0     0    1]
```

**Performance Optimization**: Trigonometric values are pre-computed in the constructor:
```scala
private val sinYaw = Math.sin(yaw)
private val cosYaw = Math.cos(yaw)
// ... etc
```

## Rendering Pipeline

### 1. World Setup
```scala
val world = World(width, height, depth)
  .add(shape, origin, rotation)
```

### 2. Frame Generation
```scala
// Lazy evaluation for memory efficiency
val frames = LazyList.from(0).map { frameIndex =>
  val rotation = calculateRotation(frameIndex)
  val worldState = applyRotation(world, rotation)
  renderFrame(worldState)
}
```

### 3. Rendering Process

#### Z-Buffer Algorithm
- **Scan Order**: Far-to-near (Z decreasing)
- **Purpose**: Proper occlusion handling
- **Implementation**: First hit wins (nearest object)

#### Pixel Processing
```scala
for (row <- 0 until world.height) {
  for (column <- 0 until world.width) {
    val zScan = (world.depth - 1) to 0 by -1
    val ch = zScan.find { z =>
      val coord = Coord(column, row, z)
      world.placements.exists(_.occupiesSpaceAt(coord))
    }.map { z =>
      // Found occupied space, determine character
      val coord = Coord(column, row, z)
      val placement = world.placements.find(_.occupiesSpaceAt(coord)).get
      charFor(placement)
    }.getOrElse(blankChar)
  }
}
```

### 4. Lighting Calculation

#### Lambertian Shading Model
```scala
brightness = ambient + (1 - ambient) × max(0, normal · lightDirection)
```

**Components**:
- **Ambient Light**: Base illumination level (default: 0.2)
- **Diffuse Light**: Surface normal dot product with light direction
- **Clamping**: Ensures brightness ∈ [0, 1]

#### Surface Normal Calculation
**Triangle Meshes**: Use actual triangle normal
**Other Shapes**: Gradient-based estimation:
```scala
def surfaceNormalAt(local: Coord): Coord = {
  val eps = NORMAL_EPSILON
  val gx = occupancyGradient(local, Coord(eps, 0, 0))
  val gy = occupancyGradient(local, Coord(0, eps, 0))
  val gz = occupancyGradient(local, Coord(0, 0, eps))
  val grad = Coord(gx, gy, gz)
  if (grad.magnitude == 0) FALLBACK_NORMAL else grad.normalize
}
```

#### Light Transformation
```scala
// Transform world light direction to shape's local space
private def transformLightToShapeSpace(worldLight: Coord, shapeRotation: Rotation): Coord =
  shapeRotation.inverse.applyTo(worldLight)
```

## Shape System

### Triangle Geometry

#### Triangle Properties
```scala
case class Triangle(v0: Coord, v1: Coord, v2: Coord)
```

**Calculated Properties**:
- **Normal**: `normal = (v1 - v0) × (v2 - v0) / |(v1 - v0) × (v2 - v0)|`
- **Centroid**: `centroid = (v0 + v1 + v2) / 3`

#### Ray-Triangle Intersection
**Algorithm**: Möller-Trumbore
**Complexity**: O(1) per triangle

```scala
def intersect(rayOrigin: Coord, rayDirection: Coord): Option[Double] = {
  val edge1 = v1 - v0
  val edge2 = v2 - v0
  val h = rayDirection.cross(edge2)
  val a = edge1.dot(h)
  
  if (Math.abs(a) < PARALLEL_THRESHOLD) return None
  
  val f = 1.0 / a
  val s = rayOrigin - v0
  val u = f * s.dot(h)
  
  if (u < 0.0 || u > 1.0) return None
  
  val q = s.cross(edge1)
  val v = f * rayDirection.dot(q)
  
  if (v < 0.0 || u + v > 1.0) return None
  
  val t = f * edge2.dot(q)
  if (t > INTERSECTION_THRESHOLD) Some(t) else None
}
```

### Triangle Mesh

#### Space Occupancy Testing
**Algorithm**: Multiple ray casting with majority voting
**Purpose**: Handle edge cases and numerical precision issues

```scala
def occupiesSpaceAt(coord: Coord): Boolean = {
  val rayDirections = Seq(
    Coord(1, 0, 0),      // +X
    Coord(0, 1, 0),      // +Y
    Coord(0, 0, 1),      // +Z
    Coord(1, 1, 0).normalize  // Diagonal XY
  )
  
  val results = rayDirections.map { rayDirection =>
    val intersectionCount = triangles.count(_.intersect(coord, rayDirection).isDefined)
    intersectionCount % 2 == 1  // Odd = inside, Even = outside
  }
  
  results.count(_ == true) > results.length / 2  // Majority vote
}
```

#### Surface Normal Calculation
```scala
override def surfaceNormalAt(local: Coord): Coord = {
  val closestTriangle = triangles.minBy(triangle => {
    val toTriangle = triangle.centroid - local
    toTriangle.magnitude
  })
  closestTriangle.normal
}
```

## Transformation Pipeline

### World-to-Local Coordinate Transformation

#### 1. Translation
```scala
val boxCenter = origin + shape.center
val translated = coord - boxCenter
```

#### 2. Rotation
```scala
val inverseRotation = Rotation(-rotation.yaw, -rotation.pitch, -rotation.roll)
val localCoord = inverseRotation.applyTo(translated)
```

### Boundary Checking

#### Vertex Transformation
```scala
def wouldExtendBeyondBounds(worldWidth: Int, worldHeight: Int, worldDepth: Int): Boolean = {
  shape match {
    case triangleMesh: TriangleMesh =>
      val vertices = triangleMesh.triangles.flatMap { triangle =>
        Seq(triangle.v0, triangle.v1, triangle.v2)
      }.distinct
      
      vertices.exists { localVertex =>
        val worldVertex = rotation.applyTo(localVertex) + origin
        worldVertex.x < 0 || worldVertex.x >= worldWidth ||
        worldVertex.y < 0 || worldVertex.y >= worldHeight ||
        worldVertex.z < 0 || worldVertex.z >= worldDepth
      }
    case _ => false
  }
}
```

## Performance Characteristics

### Time Complexity

#### Rendering
- **Per Pixel**: O(shapes × triangles per shape)
- **Total**: O(width × height × depth × shapes × triangles)
- **Optimization**: Early termination on first hit

#### Shape Operations
- **Space Occupancy**: O(triangles × ray directions)
- **Surface Normal**: O(triangles) for closest triangle
- **Boundary Check**: O(vertices) for transformation

### Space Complexity
- **World**: O(width × height × depth)
- **Shapes**: O(triangles)
- **Animation**: O(frames) with lazy evaluation

### Memory Usage Patterns
- **Immutable Data**: No in-place modifications
- **Lazy Evaluation**: Frames generated on-demand
- **Object Reuse**: Minimal allocations during animation

## Configuration Constants

### Rendering Parameters
```scala
private val DEFAULT_EPSILON = 1e-10        // Numerical precision
private val DEFAULT_AMBIENT = 0.2          // Ambient light level
private val DEFAULT_X_SCALE = 1            // Horizontal scaling
private val QUANTIZATION_LEVELS = 8        // Shading granularity
```

### Shape Parameters
```scala
private val NORMAL_EPSILON = 0.5           // Normal calculation precision
private val PARALLEL_THRESHOLD = 1e-10     // Ray-triangle parallel threshold
private val INTERSECTION_THRESHOLD = 1e-10 // Ray-triangle intersection threshold
```

### Animation Parameters
```scala
private val FRAME_DELAY_MS = 66            // ~15 FPS
private val YAW_ROTATION_RATE = Math.PI / -36   // -5°/frame
private val ROLL_ROTATION_RATE = Math.PI / 72   // 2.5°/frame
```

## Error Handling

### Validation
```scala
// World dimensions
require(width >= 0, "World width must be non-negative")
require(height >= 0, "World height must be non-negative")
require(depth >= 0, "World depth must be non-negative")

// Shape parameters
require(size > 0, "Cube size must be positive")
require(shape != null, "Shape cannot be null")

// Interpolation
require(t >= 0.0 && t <= 1.0, "Interpolation factor must be between 0 and 1")
```

### Error Types
```scala
case class NoSuchShape(shapeId: Int) extends Exception
```

### Error Handling Patterns
```scala
def rotate(shapeId: Int, delta: Rotation): Either[NoSuchShape, World] = {
  shapes.get(shapeId)
    .map(placement => {
      // Success case
      val newPlacement = placement.rotate(delta)
      if (newPlacement.wouldExtendBeyondBounds(width, height, depth)) {
        this  // Return unchanged world
      } else {
        this.copy(shapes = shapes + (shapeId -> newPlacement))
      }
    })
    .toRight(NoSuchShape(shapeId))  // Error case
}
```

## Testing Strategy

### Test Categories

#### 1. Unit Tests
- **Mathematical Operations**: Vector math, rotations, transformations
- **Shape Behavior**: Occupancy testing, surface normals
- **World Management**: Shape placement, rotation, boundaries

#### 2. Integration Tests
- **Rendering Pipeline**: End-to-end rendering workflow
- **Animation System**: Frame generation and display
- **Boundary Conditions**: Edge cases and limits

#### 3. Regression Tests
- **Known Issues**: Frame 190 truncation, specific rotation angles
- **Performance**: Large world handling, complex shapes

### Test Data Patterns
```scala
// Boundary testing
val testCoords = Seq(
  Coord(0, 0, 0),           // Origin
  Coord(width-1, height-1, depth-1),  // Max bounds
  Coord(-1, 0, 0),          // Below bounds
  Coord(width, 0, 0)         // Above bounds
)

// Rotation edge cases
val testRotations = Seq(
  Rotation(0, 0, 0),        // No rotation
  Rotation(Math.PI/2, 0, 0), // 90° yaw
  Rotation(Math.PI, 0, 0),   // 180° yaw
  Rotation(Math.PI*2, 0, 0)  // 360° yaw
)
```

## Debugging Tools

### Frame Diagnostics
```scala
val details = Seq(
  f"Frame: $frameIndex%3d",
  f"Yaw:   ${yawDegrees}%6.1f°",
  f"Pitch: ${pitchDegrees}%6.1f°", 
  f"Roll:  ${rollDegrees}%6.1f°"
).mkString("  ")
```

### Rendering Analysis
```scala
// Find bounding box of rendered content
var minX = Int.MaxValue
var maxX = Int.MinValue
var minY = Int.MaxValue
var maxY = Int.MinValue

for (y <- lines.indices; x <- lines(y).indices) {
  if (lines(y)(x) != ' ') {
    minX = Math.min(minX, x)
    maxX = Math.max(maxX, x)
    minY = Math.min(minY, y)
    maxY = Math.max(maxY, y)
  }
}
```

### Performance Monitoring
```scala
// Frame timing
val startTime = System.currentTimeMillis()
val rendered = Renderer.renderShadedForward(world, ...)
val renderTime = System.currentTimeMillis() - startTime
```

## Optimization Techniques

### 1. Pre-computation
- Trigonometric values in `Rotation` class
- Shape properties (normals, centroids) as lazy values

### 2. Lazy Evaluation
- Frame generation using `LazyList`
- Surface normal calculation on-demand

### 3. Early Termination
- Z-scan stops on first hit
- Boundary checks prevent unnecessary transformations

### 4. Memory Efficiency
- `StringBuilder` for rendering output
- Immutable data structures reduce copying

## Scalability Considerations

### World Size Limits
- **Memory**: O(width × height × depth)
- **Rendering**: O(width × height × depth × shapes)
- **Practical Limit**: ~100³ for interactive performance

### Shape Complexity
- **Triangle Count**: Affects occupancy testing and normal calculation
- **Performance**: Linear scaling with triangle count
- **Recommendation**: < 1000 triangles for real-time rendering

### Animation Performance
- **Frame Rate**: Limited by rendering complexity
- **Memory**: Lazy evaluation prevents memory buildup
- **Optimization**: Reduce world size or shape complexity

