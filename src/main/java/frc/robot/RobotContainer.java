package frc.robot;

import frc.robot.subsystems.Swerve;
import edu.wpi.first.wpilibj.Joystick;
import frc.robot.RobotStatemachine.RobotState;
import frc.robot.planners.AimPlanner;
import frc.robot.planners.IntakeMotionPlanner;
import frc.robot.planners.StageAvoidancePlanner;
import frc.robot.statemachines.ClimberStatemachine;
import frc.robot.statemachines.DiverterStatemachine;
import frc.robot.statemachines.FlywheelIntakeStatemachine;
import frc.robot.statemachines.PivotStatemachine;
import frc.robot.statemachines.ShooterStatemachine;
import frc.robot.statemachines.SwerveStatemachine;
import frc.robot.statemachines.TriggerIntakeStatemachine;
import frc.robot.subsystems.Climber;
import frc.robot.subsystems.Diverter;
import frc.robot.subsystems.FlywheelIntake;
import frc.robot.subsystems.Pivot;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.TriggerIntake;

public class RobotContainer {
    private static RobotContainer instance = new RobotContainer();

    /* SUBSYSTEMS */
    private final Swerve swerve = new Swerve();
    private final FlywheelIntake flywheelIntake = new FlywheelIntake();
    private final TriggerIntake triggerIntake = new TriggerIntake();
    private final Pivot pivot = new Pivot();
    private final Shooter shooter = new Shooter();
    private final Diverter diverter = new Diverter();
    private final Climber climber = new Climber();

    /* PLANNERS */
    private final IntakeMotionPlanner intakeMotionPlanner = new IntakeMotionPlanner(
        pivot::getPivotPosition,
        flywheelIntake::getDeploymentAngle
    );

    private final AimPlanner aimPlanner = new AimPlanner(
        swerve::getPose,
        swerve::getChassisSpeeds,
        false
    );

    private final StageAvoidancePlanner stageAvoidancePlanner = new StageAvoidancePlanner(
        swerve::getPose
    );

    /* STATE MACHINES */
    private final SwerveStatemachine swerveStatemachine = new SwerveStatemachine(swerve, aimPlanner);
    private final FlywheelIntakeStatemachine flywheelIntakeStatemachine = new FlywheelIntakeStatemachine(flywheelIntake, intakeMotionPlanner, shooter::hasNote);
    private final TriggerIntakeStatemachine triggerIntakeStatemachine = new TriggerIntakeStatemachine(triggerIntake, intakeMotionPlanner, shooter::hasNote);
    private final PivotStatemachine pivotStatemachine = new PivotStatemachine(pivot, aimPlanner, stageAvoidancePlanner, intakeMotionPlanner);
    private final ShooterStatemachine shooterStatemachine = new ShooterStatemachine(shooter, aimPlanner, () -> false); //TODO
    private final DiverterStatemachine diverterStatemachine = new DiverterStatemachine(diverter, stageAvoidancePlanner);
    private final ClimberStatemachine climberStatemachine = new ClimberStatemachine(climber, () -> swerve.getGyroAngle().getX());

    /* Joysticks */
    private final Joystick driverJoystick = new Joystick(0);
    // private final Joystick operatorJoystick = new Joystick(1);

    private final RobotStatemachine robotStatemachine = new RobotStatemachine(
        swerveStatemachine,
        flywheelIntakeStatemachine,
        triggerIntakeStatemachine,
        shooterStatemachine,
        pivotStatemachine,
        diverterStatemachine,
        climberStatemachine
    );

    public static RobotContainer getInstance() {
        return instance;
    }

    public RobotStatemachine getRobotStatemachine() {
        return robotStatemachine;
    }

    public void run() {
        if(edu.wpi.first.wpilibj.RobotState.isTeleop()) updateTeleopControls();
        robotStatemachine.update();
    }

    public void requestState(RobotState state) {
        robotStatemachine.requestState(state);
    }

    private void updateTeleopControls () {
        //EXAMPLE BUTTON:
        // if(driverJoystick.getRawButtonPressed(0)) {
        //     robotStatemachine.requestSwerveState(SwerveState.ROBOT_CENTRIC);
        // }
    }
}
