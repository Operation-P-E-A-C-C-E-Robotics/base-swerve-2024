// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.function.DoubleFunction;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.FeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import frc.lib.swerve.SwerveDescription.CANIDs;
import frc.lib.swerve.SwerveDescription.Dimensions;
import frc.lib.swerve.SwerveDescription.EncoderOffsets;
import frc.lib.swerve.SwerveDescription.Gearing;
import frc.lib.swerve.SwerveDescription.Inversion;
import frc.lib.swerve.SwerveDescription.Physics;
import frc.lib.swerve.SwerveDescription.PidGains;
import frc.lib.util.JoystickCurves;

public final class Constants {
  public static final double period = 0.01;

  public static final class Cameras {
    public static final String frontLimelight = "limelight-front";
    public static final String rearLimelight = "limelight-rear";
    public static final int apriltagPipeline = 0;
    public static final int noteDectionPipeline = 1;
  }

  public static final class Shooter {
    public static final int upperFlywheelMotorId = 27; //will be 13 once swerve ids fixed
    public static final int lowerFlywheelMotorId = 29;
    public static final int triggerMotorId = 28;

    public static final int flywheelSwitchId = 0;
    public static final int triggerSwitchId = 1;

    public static final double flywheelMaxControllableVelocity = 0; //rotations per second
    public static final double flywheelGearRatio = 1;
    public static final double flywheelDiameter = Units.inchesToMeters(5);
    public static final double flywheelKv = 1;
    public static final double flywheelKa = 1;
    public static final double flywheelModelStDev = 3;
    public static final double flywheelEncoderStDev = 0.01;
    public static final double flywheelControlEffort = 12;
    public static final double flywheelErrorTolerance = 8;

    public static final double shotDetectionAccelerationThreshold = 0;
    public static final double shotDetectionTimeThreshold = 0;
    public static final double shotDetectionMinVelocity = 0;
    public static final double shotDetectionResetTime = 0;

    public static final TalonFXConfiguration flywheelConfigs = new TalonFXConfiguration();
    static {
      flywheelConfigs.CurrentLimits.StatorCurrentLimit = 20;
      flywheelConfigs.CurrentLimits.StatorCurrentLimitEnable = true;
      flywheelConfigs.MotorOutput.NeutralMode = NeutralModeValue.Coast; //needs to be coast because we can override this to brake but not the other way around

      flywheelConfigs.Feedback.SensorToMechanismRatio = flywheelGearRatio;
    }

    public static final double flywheelEfficiency = 1; // percentage of flywheel surface speed to exit velocity
    public static final double flywheelTolerance = 0; //how close to the target velocity the flywheel needs to be considered ready

    public static final double triggerMetersPerRotation = 0;
    public static final double triggerKp = 0;
  }

  public static final class FlywheelIntake {
    public static final int flywheelIntakeDeployMotorId = 18;
    public static final int flywheelIntakeRollerMotorId = 19;

    public static final double flywheelIntakeDeployKp = 0.02;
    public static final double flywheelIntakeDeployKi = 0;
    public static final double flywheelIntakeDeployKd = 0;

    public static final float flywheelIntakeDeployMinAngle = 0.0f;
    public static final float flywheelIntakeDeployMaxAngle = 360.0f;

    public static final double flywheelIntakeDeployGearing = 1;
    public static final double flywheelIntakeDeployTolerance = 0; //how close to the target position the deployer needs to be to be considered "deployed"

    public static final boolean flywheelIntakeDeployMotorInverted = false;
    public static final boolean flywheelIntakeRollerMotorInverted = false;
  }

  public static final class TriggerIntake {
    public static final int triggerIntakeRollerMotorId = 16;
    public static final int triggerIntakeDeployMotorId = 17;

    public static final double triggerIntakeDeployKp = 0.02;
    public static final double triggerIntakeDeployKi = 0;
    public static final double triggerIntakeDeployKd = 0;

    public static final double triggerIntakeDeployGearing = 1;
    public static final double triggerIntakeDeployTolerance = 0; //how close to the target position the deployer needs to be to be considered "deployed"

    public static final float triggerIntakeDeployMinAngle = 0.0f;
    public static final float triggerIntakeDeployMaxAngle = 360.0f;

    public static final boolean triggerIntakeDeployMotorInverted = false;
    public static final boolean triggerIntakeRollerMotorInverted = false;
  }

  public static final class Pivot {
    public static final int pivotMasterID = 22;
    public static final int pivotSlaveID = 24;
    public static final int pivotCANCoderID = 25;

    public static final double restingAngle = 0; //rotations

    public static final double pivotMinAngle = 0;
    public static final double pivotMaxAngle = 1000;

    public static final double pivotGearRatio = 100; // pivot rotations per motor rotation
    public static final double pivotTolerance = 0; //how close to the target position the pivot needs to be considered ready

    public static final double gravityFeedforwardkG = 0;

    public static final TalonFXConfiguration pivotConfigs = new TalonFXConfiguration();
    static {
      pivotConfigs.CurrentLimits.StatorCurrentLimit = 20;
      pivotConfigs.CurrentLimits.StatorCurrentLimitEnable = true;
      pivotConfigs.MotorOutput.NeutralMode = NeutralModeValue.Brake;

      pivotConfigs.Slot0.kP = 100;
      pivotConfigs.Slot0.kI = 0;
      pivotConfigs.Slot0.kD = 0;
      pivotConfigs.Slot0.kS = 0;
      pivotConfigs.Slot0.kV = 1;
      pivotConfigs.Slot0.kA = 1;

      pivotConfigs.MotionMagic.MotionMagicExpo_kA = 1;
      pivotConfigs.MotionMagic.MotionMagicExpo_kV = 1;
      pivotConfigs.MotionMagic.MotionMagicCruiseVelocity = 0;

      pivotConfigs.Feedback.FeedbackRemoteSensorID = pivotCANCoderID;
      pivotConfigs.Feedback.FeedbackSensorSource = FeedbackSensorSourceValue.FusedCANcoder;
      pivotConfigs.Feedback.FeedbackRotorOffset = 0;
      pivotConfigs.Feedback.RotorToSensorRatio = pivotGearRatio;
    }
  }

  public static final class Diverter {
    public static final int diverterRollerMotorId = 50;
    public static final int diverterDeployMotorId = 26;

    public static final double diverterDeployGearRatio = 1;
    public static final double diverterDeployTolerance = 0; //how close to the target position the deployer needs to be to be considered "deployed"

    public static final double maxDiverterExtension = 0; //in meters

    public static final TalonFXConfiguration diverterDeployConfigs = new TalonFXConfiguration();
    static {
      diverterDeployConfigs.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
      diverterDeployConfigs.CurrentLimits.StatorCurrentLimit = 20;
      diverterDeployConfigs.CurrentLimits.StatorCurrentLimitEnable = true;
      diverterDeployConfigs.MotorOutput.NeutralMode = NeutralModeValue.Brake;
      
      diverterDeployConfigs.Slot0.kP = 0;
      diverterDeployConfigs.Slot0.kI = 0;
      diverterDeployConfigs.Slot0.kD = 0;
      diverterDeployConfigs.Slot0.kS = 0;
      diverterDeployConfigs.Slot0.kV = 0;
      diverterDeployConfigs.Slot0.kA = 0;
      
      diverterDeployConfigs.MotionMagic.MotionMagicExpo_kA = 0;
      diverterDeployConfigs.MotionMagic.MotionMagicExpo_kV = 0;
      diverterDeployConfigs.MotionMagic.MotionMagicCruiseVelocity = 0;
    }

    public static final TalonFXConfiguration diverterRollerConfigs = new TalonFXConfiguration();
    static {
      diverterRollerConfigs.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
      diverterRollerConfigs.CurrentLimits.StatorCurrentLimit = 20;
      diverterRollerConfigs.CurrentLimits.StatorCurrentLimitEnable = true;
      diverterRollerConfigs.MotorOutput.NeutralMode = NeutralModeValue.Coast;

      diverterRollerConfigs.Slot0.kP = 0; //for holding position
    }
  }

  public static final class Climber {
    public static final int climberLeftMotorId = 21;
    public static final int climberRightMotorId = 23;

    public static final double climberGearRatio = 1;

    public static final double climberTolerance = 0; //-Stupid Questions With Parker-: could i also name this climberError? or is error reserved for something else

    public static final TalonFXConfiguration climberConfigs = new TalonFXConfiguration();
    static {
      climberConfigs.CurrentLimits.StatorCurrentLimit = 20;
      climberConfigs.CurrentLimits.StatorCurrentLimitEnable = true;

      climberConfigs.Slot0.kP = 0;
      climberConfigs.Slot0.kI = 0;
      climberConfigs.Slot0.kD = 0;
      climberConfigs.Slot0.kS = 0;
      climberConfigs.Slot0.kV = 0;
      climberConfigs.Slot0.kA = 0;

      climberConfigs.MotionMagic.MotionMagicExpo_kA = 0;
      climberConfigs.MotionMagic.MotionMagicExpo_kV = 0;
      climberConfigs.MotionMagic.MotionMagicCruiseVelocity = 0;
    }

    public static final InvertedValue climberLeftMotorIsInverted = InvertedValue.CounterClockwise_Positive;
    public static final InvertedValue climberRightMotorIsInverted = InvertedValue.CounterClockwise_Positive;
    // todo delete this comment when sure the booleans are what we want
  }
  public static final class Swerve {
    /* TELEOP */
    //speeds in m/s (probably)
    public static final double teleopLinearMultiplier = 7.0;
    public static final double teleopAngularMultiplier = 7.0;

    //acceleration limits
    public static final double teleopLinearSpeedLimit = 5.0;
	  public static final double teleopLowBatteryLinearSpeedLimit = 2; //more acceleration limit when battery is low
    public static final double teleopLinearAngleLimit = 2.0;
    public static final double teleopAngularRateLimit = 3.0;

    //deadband
    public static final double teleopLinearSpeedDeadband = 0.15;
    public static final double teleopAngularVelocityDeadband = 0.2;
    public static final double teleopDeadbandDebounceTime = 0.1;

    public static final DoubleFunction <Double> teleopLinearSpeedCurve = (double linearSpeed) -> JoystickCurves.herraFCurve(linearSpeed, 6, 4.5); //a nice gentle curve which is Peaccy's (me!!) favorite :)
    public static final DoubleFunction <Double> teleopAngularVelocityCurve = (double angularVelocity) -> JoystickCurves.powerCurve(angularVelocity, 2); //TODO decide if the driver (me) wants a curve on this or not.

    //number of loops to keep track of position correction for (so multiply by 20ms to get the duration the correction is considering)
    //todo too slow?
    public static final int teleopPositionCorrectionIters = 0; 




    /* CTRE SWERVE CONSTANTS */
    public static final Dimensions dimensions = new Dimensions(Units.inchesToMeters(24.75), Units.inchesToMeters(24.75));

    /*change from dev drivebase
     * front left -> rear right
     * front right to rear left
     * rear left to front right
     * rear right to front left
     */

    //to: whoever did this; why did you label the modules differetly on the robot vs the tuner????? -love, peaccy
    public static final CANIDs frontLeftIDs =   new CANIDs(4,   5,    6); //module 0
    public static final CANIDs rearLeftIDs =   new CANIDs(7,   8,    9); //module 1
    public static final CANIDs rearRightIDs =   new CANIDs(10,   11,   12); //module 2
    public static final CANIDs frontRightIDs =    new CANIDs(13,  14,   15); //module 3

    public static final Gearing gearing = new Gearing(DriveGearRatios.SDSMK4i_L2, ((150.0 / 7.0) / 1.0), (3.85/2), 0);
    // public static final EncoderOffsets offsets = new EncoderOffsets(-0.488770, -0.225342, -0.224609, -0.906738); //todo these offsets are very wrong.
    public static final EncoderOffsets offsets = new EncoderOffsets(
      -0.409424, //Front Left, module 0
      0.277344, //Front Right, module 3
      -0.412109,  // Rear Left, module 1
      -0.307861); //Rear Right, module 2

    // public static final Inversion inversion = new Inversion(false, true, true, false); //herra 4.5, 6
    public static final Inversion inversion = new Inversion(false, true, false, true);

    //inertia only used for simulation
    public static final Physics physics = new Physics(0.05,0.01, Robot.isReal() ? 50 : 800, 10);
    public static final double steerMotorCurrentLimit = Robot.isReal() ? 40 : 120; //amps
    
    public static final PidGains driveGains = new PidGains(0.35, 0, 0, 0.11, 0.3); 
    public static final PidGains angleGains = new PidGains(90, 0, 0.001, 0, 0);

    public static final int pigeonCANId = 3;
    public static final boolean invertSteerMotors = Robot.isReal(); //cant invert in simulation which is dumb.

    /* HEADING CONTROLLER CONSTANTS */
    public static final double autoHeadingKP = 500;
    public static final double autoHeadingKI = 0.0; //DOES NOTHING LOL
    public static final double autoHeadingKD = 0.0; //ALSO DOES NOTHING LOL
    public static final double autoHeadingKV = 0.7;
    public static final double autoHeadingKA = 0.02;
    public static final double autoHeadingMaxVelocity = 3; //deg/s (i think)
    public static final double autoHeadingMaxAcceleration = 10; //deg/s^2
    public static final double lockHeadingMaxVelocity = 6;
    public static final double lockHeadingMaxAcceleration = 20;
    public static final boolean useSoftHoldHeading = false;
    public static final double softHeadingCurrentLimit = 30;

    
    /* PATH FOLLOWING CONSTANTS */
    public static final double pathfollowingMaxVelocity = 3,
                              pathfollowingMaxAcceleration = 3,
                              pathfollowingMaxAngularVelocity = 360,
                              pathfollowingMaxAngularAcceleration = 360;

    public static final double measuredMaxVelocity = 3,
                              measuredMaxAcceleration = 3,
                              measuredMaxAngularVelocity = 360,
                              measuredMaxAngularAcceleration = 360;

    public static final PathConstraints autoMaxSpeed = new PathConstraints(
      pathfollowingMaxVelocity,
      pathfollowingMaxAcceleration,
      pathfollowingMaxAngularVelocity,
      pathfollowingMaxAngularAcceleration
    );

    public static final HolonomicPathFollowerConfig pathFollowerConfig = new HolonomicPathFollowerConfig(
      new PIDConstants(10, 0, 0), 
      new PIDConstants(5, 0, 0), 
      pathfollowingMaxVelocity, 
      dimensions.frontLeft.getNorm(), //drive radius
      new ReplanningConfig(true, true),
      period
    );
  }

  public static final class ControlSystem {
    public static final int PDPCanId = 2;
    public static final int PCHCanId = 1;

    public static final ModuleType PDPModuleType = ModuleType.kRev;
  }

  public static final class Field{
    public static final Translation2d targetTranslation = new Translation2d(0, 0);
  }

  //stolen from 364 :D
  public class DriveGearRatios{
    /** SDS MK3 - 8.16 : 1 */
    public static final double SDSMK3_Standard = (8.16 / 1.0);
    /** SDS MK3 - 6.86 : 1 */
    public static final double SDSMK3_Fast = (6.86 / 1.0);

    /** SDS MK4 - 8.14 : 1 */
    public static final double SDSMK4_L1 = (8.14 / 1.0);
    /** SDS MK4 - 6.75 : 1 */
    public static final double SDSMK4_L2 = (6.75 / 1.0);
    /** SDS MK4 - 6.12 : 1 */
    public static final double SDSMK4_L3 = (6.12 / 1.0);
    /** SDS MK4 - 5.14 : 1 */
    public static final double SDSMK4_L4 = (5.14 / 1.0);
    
    /** SDS MK4i - 8.14 : 1 */
    public static final double SDSMK4i_L1 = (8.14 / 1.0);
    /** SDS MK4i - 6.75 : 1 */
    public static final double SDSMK4i_L2 = (6.75 / 1.0);
    /** SDS MK4i - 6.12 : 1 */
    public static final double SDSMK4i_L3 = (6.12 / 1.0);
}
}
