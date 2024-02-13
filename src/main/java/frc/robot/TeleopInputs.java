package frc.robot;

import edu.wpi.first.math.geometry.Pose2d;
import frc.lib.util.AllianceFlipUtil;
import frc.robot.TeleopStatemachine.TeleopState;
import frc.robot.statemachines.SwerveStatemachine.SwerveState;
import frc.robot.subsystems.Swerve;

/**
 * This class is used to define the inputs for the teleop state machine.
 * Joystick inputs get mapped here, and also automated inputs (for instance, triggering
 * a state when the robot is in a certain position or when a certain sensor is triggered).
 * Note that the actual inputs are defined in the OI class to make it easier to modify,
 * but the actual logic for the state machine is defined here.
 */
public class TeleopInputs {
    private TeleopMode mode = TeleopMode.SPEAKER;
    private IntakingMode intakingMode = IntakingMode.NONE;
    private ClimbMode climbMode = ClimbMode.ALIGN;
    private boolean aiming = false;

    private final double AUTO_AIM_X = 5; // distance from left wall to start aiming.
    private final double AMP_HANDOFF_X = 5; // distance from left wall to start handoff.
    private final double AMP_ALIGN_X = 3; // distance from left wall to start aligning.
    private final double AMP_ALIGN_Y = 3; // distance from bottom wall to start aligning.

    private final double INTAKE_TRANSITION_VELOCITY_THRESHOLD = 0.5; // m/s

    private TeleopInputs() {
        // This is a singleton class, so the constructor is private to prevent
        // instantiation from outside the class.
    }


    /**
     * Get what state the swerve should be in, 
     * based on the joystick inputs and any automated inputs.
     * @return
     */
    public SwerveState getWantedSwerveState() {
        if(OI.Overrides.forceAim.getAsBoolean() || aiming) {
            return SwerveState.AIM;
        }

        if(intakingMode == IntakingMode.BACK) {
            return SwerveState.ALIGN_INTAKING;
        }

        if(OI.Swerve.isRobotCentric.getAsBoolean()) {
            return SwerveState.ROBOT_CENTRIC;
        }

        if(OI.Swerve.isLockIn.getAsBoolean()) {
            return SwerveState.LOCK_IN;
        }

        return OI.Swerve.isOpenLoop.getAsBoolean() ? SwerveState.OPEN_LOOP_TELEOP : SwerveState.CLOSED_LOOP_TELEOP;
    }

    /**
     * Get what state the robot should be in,
     * based on the joystick inputs and automated inputs.
     * @return
     */
    public TeleopState getWantedTeleopState() {
        var blueAlliancePose = AllianceFlipUtil.apply(Swerve.getInstance().getPose()); //robot pose for automation

        // change the mode based on the operator inputs
        if(OI.Modes.wantsAmpMode.getAsBoolean()) mode = TeleopMode.AMP;
        if(OI.Modes.wantsClimbMode.getAsBoolean()) mode = TeleopMode.CLIMB;
        if(OI.Modes.wantsSpeakerMode.getAsBoolean()) mode = TeleopMode.SPEAKER;

        // reset the climb mode if we're not climbing
        if(mode != TeleopMode.CLIMB) climbMode = ClimbMode.ALIGN;

        // operator overrides - these take precedence over everything else
        if(OI.Inputs.wantsStow.getAsBoolean())  return TeleopState.STOW;
        if(OI.Overrides.forceHandoff.getAsBoolean()) return TeleopState.HANDOFF;
        if(OI.Overrides.forceAmp.getAsBoolean()) return TeleopState.ALIGN_AMP;
        if(OI.Overrides.forceAim.getAsBoolean()) return TeleopState.AUTO_AIM;

        //handle the drivers' intaking requests, these take precedence over modes & automation
        intakingMode = wantedIntakeMode();
        if(intakingMode != IntakingMode.NONE) {
            return intakingMode == IntakingMode.FRONT ? TeleopState.INTAKE_FRONT : TeleopState.INTAKE_BACK;
        }

        //handle the driver's request to "place" (a button that does different things based on the mode)
        if(OI.Inputs.wantsPlace.getAsBoolean()) {
            switch (mode) {
                case AMP:
                    return TeleopState.PLACE_AMP;
                case CLIMB:
                    if(climbMode == ClimbMode.RETRACT) return TeleopState.PLACE_TRAP;
                case SPEAKER:
                    return TeleopState.SHOOT;
                default:
                    break;
            }
        }


        //handle mode-specific automation
        switch (mode) {
            case AMP:
                aiming = false;
                if(wantsHandoff(blueAlliancePose)) {
                    return TeleopState.HANDOFF;
                }
                if(wantsAlignAmp(blueAlliancePose)) {
                    return TeleopState.ALIGN_AMP;
                }
                return TeleopState.REST;
            case CLIMB:
                aiming = false;
                climbMode = wantedClimbMode();
                if(climbMode == ClimbMode.ALIGN) return TeleopState.ALIGN_CLIMB;
                if(climbMode == ClimbMode.EXTEND) return TeleopState.CLIMB_EXTEND;
                if(climbMode == ClimbMode.RETRACT) return TeleopState.CLIMB_RETRACT;
                if(climbMode == ClimbMode.BALANCE) return TeleopState.CLIMB_BALANCE;
                return TeleopState.ALIGN_CLIMB;
            case SPEAKER:
                aiming = wantsAim(blueAlliancePose); // stored for use in swerve state
                if(OI.Inputs.wantsShoot.getAsBoolean()) return TeleopState.SHOOT;
                if(aiming) {
                    return TeleopState.AUTO_AIM;
                }
                return TeleopState.REST;
            default:
                break;
        }


        return TeleopState.REST;
    }
    
    public TeleopMode getMode() {
        return mode;
    }

    private boolean wantsAlignAmp(Pose2d blueAlliancePose) {
        if (blueAlliancePose.getX() < AMP_ALIGN_X && blueAlliancePose.getY() < AMP_ALIGN_Y) return true;
        return false;
    }

    private boolean wantsHandoff(Pose2d blueAlliancePose) {
        if (blueAlliancePose.getX() < AMP_HANDOFF_X) return true;
        return false;
    }

    private boolean wantsAim(Pose2d blueAlliancePose) {
        if (blueAlliancePose.getX() < AUTO_AIM_X) return true;
        return false;
    }

    private IntakingMode wantedIntakeMode() {
        // operator overrides
        if(OI.Overrides.forceIntakeFront.getAsBoolean()) return IntakingMode.FRONT;
        if(OI.Overrides.forceIntakeBack.getAsBoolean()) return IntakingMode.BACK;

        // if the driver doesn't want to intake, return NONE
        // otherwise, default to the back intake
        if(!OI.Inputs.wantsIntake.getAsBoolean()) return IntakingMode.NONE;
        else if(intakingMode == IntakingMode.NONE)intakingMode = IntakingMode.BACK;

        //automated transitions between front and back intakes
        var xVelocity = Swerve.getInstance().getChassisSpeeds().vxMetersPerSecond;
        if(xVelocity < -INTAKE_TRANSITION_VELOCITY_THRESHOLD) return IntakingMode.BACK;
        else if(xVelocity > INTAKE_TRANSITION_VELOCITY_THRESHOLD) return IntakingMode.FRONT;

        return intakingMode;
    }

    private ClimbMode wantedClimbMode() {
        if(!OI.Inputs.wantsAlign.getAsBoolean()) return ClimbMode.ALIGN;
        if(OI.Inputs.wantsBalance.getAsBoolean()) return ClimbMode.BALANCE;
        if(OI.Inputs.wantsClimbExtend.getAsBoolean()) return ClimbMode.EXTEND;
        if(OI.Inputs.wantsClimbRetract.getAsBoolean()) return ClimbMode.RETRACT;
        return climbMode;
    }

    public enum TeleopMode {
        SPEAKER, AMP, CLIMB
    }

    public enum IntakingMode {
        FRONT, BACK, NONE
    }

    public enum ClimbMode {
        EXTEND, RETRACT, BALANCE, ALIGN
    }

    private static TeleopInputs instance = new TeleopInputs();
    public static TeleopInputs getInstance() {
        return instance;
    }
}