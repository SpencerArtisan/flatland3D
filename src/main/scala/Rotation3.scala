
case class Rotation3(yaw: Double, pitch: Double, roll: Double) {
  private val sinYaw = Math.sin(yaw)
  private val cosYaw = Math.cos(yaw)
  private val sinPitch = Math.sin(pitch)
  private val cosPitch = Math.cos(pitch)
  private val sinRoll = Math.sin(roll)
  private val cosRoll = Math.cos(roll)

  // Apply roll (X), pitch (Y), then yaw (Z): Rz * Ry * Rx
  def applyTo(v: Coord3): Coord3 = {
    // Roll around X
    val rxY = v.y * cosRoll - v.z * sinRoll
    val rxZ = v.y * sinRoll + v.z * cosRoll
    val rx = Coord3(v.x, rxY, rxZ)

    // Pitch around Y
    val ryX = rx.x * cosPitch + rx.z * sinPitch
    val ryZ = -rx.x * sinPitch + rx.z * cosPitch
    val ry = Coord3(ryX, rx.y, ryZ)

    // Yaw around Z
    val rzX = ry.x * cosYaw - ry.y * sinYaw
    val rzY = ry.x * sinYaw + ry.y * cosYaw
    Coord3(rzX, rzY, ry.z)
  }
}

object Rotation3 {
  val ZERO: Rotation3 = Rotation3(0, 0, 0)
}


