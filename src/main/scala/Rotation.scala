case class Rotation(yaw: Double, pitch: Double, roll: Double) {
  private val sinYaw = Math.sin(yaw)
  private val cosYaw = Math.cos(yaw)
  private val sinPitch = Math.sin(pitch)
  private val cosPitch = Math.cos(pitch)
  private val sinRoll = Math.sin(roll)
  private val cosRoll = Math.cos(roll)

  // Apply roll (X), pitch (Y), then yaw (Z): Rz * Ry * Rx
  def applyTo(v: Coord): Coord = {
    // Roll around X
    val rxY = v.y * cosRoll - v.z * sinRoll
    val rxZ = v.y * sinRoll + v.z * cosRoll
    val rx = Coord(v.x, rxY, rxZ)

    // Pitch around Y
    val ryX = rx.x * cosPitch + rx.z * sinPitch
    val ryZ = -rx.x * sinPitch + rx.z * cosPitch
    val ry = Coord(ryX, rx.y, ryZ)

    // Yaw around Z
    val rzX = ry.x * cosYaw - ry.y * sinYaw
    val rzY = ry.x * sinYaw + ry.y * cosYaw
    Coord(rzX, rzY, ry.z)
  }
}

object Rotation {
  val ZERO: Rotation = Rotation(0, 0, 0)
}
