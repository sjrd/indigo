package indigoexamples.model

import indigo.shared.temporal.Signal
import indigoexts.geometry.Vertex
import indigo.shared.dice.Dice
import indigo.shared.time.Millis
import indigo.shared.collections.NonEmptyList
import indigo.shared.datatypes.Radians
import indigo.shared.datatypes.RGBA

final case class Flare(flightTime: Millis, movementSignal: Signal[Vertex], tint: RGBA) extends Projectile

object Flare {

  def generateFlare(dice: Dice, startPosition: Vertex, initialAngle: Radians, tint: RGBA): Flare = {
    val flightTime = Projectiles.pickFlightTime(dice, Millis(300L), Millis(500L))

    val signalFunction: Dice => Signal[Vertex] =
      pickEndPoint(initialAngle) andThen
        createArcControlVertices(startPosition) andThen
        Projectiles.createArcSignal(flightTime)

    Flare(flightTime, signalFunction(dice), tint)
  }

  def createArcControlVertices(startPosition: Vertex): Vertex => NonEmptyList[Vertex] =
    target => NonEmptyList(startPosition, startPosition + target)

  def pickEndPoint(initialAngle: Radians): Dice => Vertex =
    dice => {
      val radius: Double =
        dice.rollDouble * 0.3d

      Vertex(
        Math.sin(wobble(dice, initialAngle.value - 0.2d, initialAngle.value + 0.2d)),
        Math.cos(wobble(dice, initialAngle.value - 0.2d, initialAngle.value + 0.2d))
      ) * Vertex(radius * 3.0d, radius)
    }

  def wobble(dice: Dice, low: Double, high: Double): Double =
    low + ((high - low) * dice.rollDouble)

}
