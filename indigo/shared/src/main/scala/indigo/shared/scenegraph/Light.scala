package indigo.shared.scenegraph

import indigo.shared.datatypes.Point
import indigo.shared.datatypes.Radians
import indigo.shared.datatypes.RGB

sealed trait Light {
  val height: Int
  val color: RGB
  val power: Double
}

final class PointLight(
    val position: Point,
    val height: Int,
    val color: RGB,
    val power: Double,
    val attenuation: Int
) extends Light {
  def moveTo(newPosition: Point): PointLight =
    new PointLight(newPosition, height, color, power, attenuation)

  def moveBy(amount: Point): PointLight =
    new PointLight(position + amount, height, color, power, attenuation)

  def withHeight(newHeight: Int): PointLight =
    new PointLight(position, newHeight, color, power, attenuation)

  def withColor(newColor: RGB): PointLight =
    new PointLight(position, height, newColor, power, attenuation)

  def withPower(newPower: Double): PointLight =
    new PointLight(position, height, color, newPower, attenuation)

  def withAttenuation(distance: Int): PointLight =
    new PointLight(position, height, color, power, distance)
}
object PointLight {
  def apply(position: Point, height: Int, color: RGB, power: Double, attenuation: Int): PointLight =
    new PointLight(position, height, color, power, attenuation)

  val default: PointLight =
    apply(Point.zero, 100, RGB.White, 1.5d, 100)
}

final class SpotLight(
    val position: Point,
    val height: Int,
    val color: RGB,
    val power: Double,
    val attenuation: Int,
    val angle: Radians,
    val rotation: Radians,
    val near: Int,
    val far: Int
) extends Light {
  def moveTo(newPosition: Point): SpotLight =
    new SpotLight(newPosition, height, color, power, attenuation, angle, rotation, near, far)

  def moveBy(amount: Point): SpotLight =
    new SpotLight(position + amount, height, color, power, attenuation, angle, rotation, near, far)

  def withHeight(newHeight: Int): SpotLight =
    new SpotLight(position, newHeight, color, power, attenuation, angle, rotation, near, far)

  def withNear(distance: Int): SpotLight =
    new SpotLight(position, height, color, power, attenuation, angle, rotation, distance, far)

  def withFar(distance: Int): SpotLight =
    new SpotLight(position, height, color, power, attenuation, angle, rotation, near, distance)

  def withColor(newColor: RGB): SpotLight =
    new SpotLight(position, height, newColor, power, attenuation, angle, rotation, near, far)

  def withPower(newPower: Double): SpotLight =
    new SpotLight(position, height, color, newPower, attenuation, angle, rotation, near, far)

  def withAttenuation(distance: Int): SpotLight =
    new SpotLight(position, height, color, power, distance, angle, rotation, near, far)

  def withAngle(newAngle: Radians): SpotLight =
    new SpotLight(position, height, color, power, attenuation, newAngle, rotation, near, far)

  def rotateTo(newRotation: Radians): SpotLight =
    new SpotLight(position, height, color, power, attenuation, angle, newRotation, near, far)

  def rotateBy(amount: Radians): SpotLight =
    new SpotLight(position, height, color, power, attenuation, angle, rotation + amount, near, far)
}
object SpotLight {
  def apply(position: Point, height: Int, color: RGB, power: Double, attenuation: Int, angle: Radians, rotation: Radians, near: Int, far: Int): SpotLight =
    new SpotLight(position, height, color, power, attenuation, angle, rotation, near, far)

  val default: SpotLight =
    apply(Point.zero, 100, RGB.White, 1.5, 100, Radians.fromDegrees(45), Radians.zero, 10, 300)
}

final class DirectionLight(
    val height: Int,
    val color: RGB,
    val power: Double,
    val rotation: Radians
) extends Light {
  def withHeight(newHeight: Int): DirectionLight =
    new DirectionLight(newHeight, color, power, rotation)

  def withColor(newColor: RGB): DirectionLight =
    new DirectionLight(height, newColor, power, rotation)

  def withPower(newPower: Double): DirectionLight =
    new DirectionLight(height, color, newPower, rotation)

  def rotateTo(newRotation: Radians): DirectionLight =
    new DirectionLight(height, color, power, newRotation)

  def rotateBy(amount: Radians): DirectionLight =
    new DirectionLight(height, color, power, rotation + amount)
}
object DirectionLight {
  def apply(height: Int, color: RGB, power: Double, rotation: Radians): DirectionLight =
    new DirectionLight(height, color, power, rotation)

  val default: DirectionLight =
    apply(100, RGB.White, 1.0, Radians.zero)
}
