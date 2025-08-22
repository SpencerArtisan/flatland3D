case class Rotation(yaw: Double, pitch: Double, roll: Double) {
  // Pre-compute trigonometric values for performance
  private val sinYaw = Math.sin(yaw)
  private val cosYaw = Math.cos(yaw)
  private val sinPitch = Math.sin(pitch)
  private val cosPitch = Math.cos(pitch)
  private val sinRoll = Math.sin(roll)
  private val cosRoll = Math.cos(roll)
  
  // Rotation matrix elements (computed once for efficiency)
  private val m00 = cosPitch * cosYaw
  private val m01 = sinRoll * sinPitch * cosYaw - cosRoll * sinYaw
  private val m02 = cosRoll * sinPitch * cosYaw + sinRoll * sinYaw
  private val m10 = cosPitch * sinYaw
  private val m11 = sinRoll * sinPitch * sinYaw + cosRoll * cosYaw
  private val m12 = cosRoll * sinPitch * sinYaw - sinRoll * cosYaw
  private val m20 = -sinPitch
  private val m21 = sinRoll * cosPitch
  private val m22 = cosRoll * cosPitch

  // Apply yaw (Z), then pitch (Y), then roll (X): Rx * Ry * Rz
  def applyTo(v: Coord): Coord = {
    // Yaw around Z first
    val rzX = v.x * cosYaw - v.y * sinYaw
    val rzY = v.x * sinYaw + v.y * cosYaw
    val rz = Coord(rzX, rzY, v.z)

    // Then pitch around Y
    val ryX = rz.x * cosPitch + rz.z * sinPitch
    val ryZ = -rz.x * sinPitch + rz.z * cosPitch
    val ry = Coord(ryX, rz.y, ryZ)

    // Finally roll around X
    val rxY = ry.y * cosRoll - ry.z * sinRoll
    val rxZ = ry.y * sinRoll + ry.z * cosRoll
    Coord(ry.x, rxY, rxZ)
  }

  // For a proper inverse rotation, we need to:
  // 1. Negate the angles
  // 2. Apply them in reverse order (Rz' * Ry' * Rx' instead of Rx * Ry * Rz)
  def inverse: Rotation = {
    // The inverse of a rotation matrix is its transpose
    // For Euler angles, this means negating the angles and applying them in reverse order
    val inverted = Rotation(-yaw, -pitch, -roll)
    
    // Pre-compute the inverse transformation matrix elements
    val sinInvYaw = Math.sin(-yaw)
    val cosInvYaw = Math.cos(-yaw)
    val sinInvPitch = Math.sin(-pitch)
    val cosInvPitch = Math.cos(-pitch)
    val sinInvRoll = Math.sin(-roll)
    val cosInvRoll = Math.cos(-roll)
    
    // Create a new rotation that applies the transformations in reverse order
    new Rotation(inverted.yaw, inverted.pitch, inverted.roll) {
      override def applyTo(v: Coord): Coord = {
        // Apply inverse roll (X) first
        val rxY = v.y * cosInvRoll - v.z * sinInvRoll
        val rxZ = v.y * sinInvRoll + v.z * cosInvRoll
        val rx = Coord(v.x, rxY, rxZ)
        
        // Then inverse pitch (Y)
        val ryX = rx.x * cosInvPitch + rx.z * sinInvPitch
        val ryZ = -rx.x * sinInvPitch + rx.z * cosInvPitch
        val ry = Coord(ryX, rx.y, ryZ)
        
        // Finally inverse yaw (Z)
        val rzX = ry.x * cosInvYaw - ry.y * sinInvYaw
        val rzY = ry.x * sinInvYaw + ry.y * cosInvYaw
        Coord(rzX, rzY, ry.z)
      }
    }
  }

  // Transform a normal vector using the inverse transpose of the rotation matrix
  def transformNormal(normal: Coord): Coord = {
    // For a pure rotation matrix, the inverse transpose is the same as the original matrix
    // This is because rotation matrices are orthogonal (their inverse is their transpose)
    // So we can use the pre-computed matrix elements directly
    val nx = m00 * normal.x + m10 * normal.y + m20 * normal.z
    val ny = m01 * normal.x + m11 * normal.y + m21 * normal.z
    val nz = m02 * normal.x + m12 * normal.y + m22 * normal.z
    Coord(nx, ny, nz).normalize
  }
}

object Rotation {
  val ZERO: Rotation = Rotation(0, 0, 0)
}
