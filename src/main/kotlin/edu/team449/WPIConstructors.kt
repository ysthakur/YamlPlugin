package edu.team449

/**
 * A map where the keys are the (qualified) names of WPI classes
 * and the values are the names of the parameters to those classes'
 * constructors.
 * Parameter names ending in question marks are optional.
 */
val wpiCtors = mapOf(
  "edu.wpi.first.wpilibj2.command.SequentialCommandGroup" to mapOf(
    "requiredSubsystems?" to "edu.wpi.first.wpilibj2.command.Subsystem",
    "commands" to "List<edu.wpi.first.wpilibj2.command.Command>"
  ),
  "edu.wpi.first.wpilibj2.command.ParallelCommandGroup" to mapOf(
    "requiredSubsystems?" to "edu.wpi.first.wpilibj2.command.Subsystem",
    "commands" to "List<edu.wpi.first.wpilibj2.command.Command>"
  ),
  "edu.wpi.first.wpilibj.geometry.Rotation2d" to mapOf(
    "radians" to null
  ),
  "edu.wpi.first.wpilibj.DoubleSolenoid" to mapOf(
    "module?" to null,
    "forward" to null,
    "reverse" to null
  )
)