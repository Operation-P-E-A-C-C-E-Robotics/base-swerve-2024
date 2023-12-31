package frc.robot.subsystems;

import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;
import com.ctre.phoenix6.mechanisms.swerve.SwerveDrivetrain.SwerveDriveState;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.GoalEndState;
import com.pathplanner.lib.path.PathPlannerPath;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.lib.sensors.LimelightHelper;
import frc.lib.swerve.PeaccefulSwerve;
import frc.lib.swerve.SwerveDescription;
import frc.lib.telemetry.SwerveTelemetry;
import frc.robot.Constants;

import static frc.robot.Constants.Swerve.*;

import java.util.List;

public class DriveTrain extends SubsystemBase {
    protected final PeaccefulSwerve swerve;

    private final SwerveRequest.ApplyChassisSpeeds autonomousRequest = new SwerveRequest.ApplyChassisSpeeds()
                                                                                        .withIsOpenLoop(false);

    // private LimelightHelper limelight;

    public DriveTrain(LimelightHelper limelight) {
        swerve = SwerveDescription.generateDrivetrain(
            dimensions, 
            frontLeftIDs, 
            frontRighIDs, 
            rearLeftIDs, 
            rearRightIDs, 
            gearing, 
            offsets, 
            inversion, 
            physics, 
            driveGains, 
            angleGains, 
            pigeonCANId, 
            invertSteerMotors
        );

        swerve.setSteerCurrentLimit(steerMotorCurrentLimit);

        //pathplanner config
        AutoBuilder.configureHolonomic(
            this::getPose, 
            this::resetOdometry, 
            this::getChassisSpeeds, 
            this::drive, 
            Constants.Swerve.pathFollowerConfig, 
            this
        );

        //log swerve state data as fast as it comes in
        swerve.registerTelemetry((SwerveDriveState state) -> {
            SwerveTelemetry.updateSwerveState(state, getChassisSpeeds());
        });

        System.out.println("DriveTrain Initialized");

        // this.limelight = limelight;
    }

    /**
     * make it go.
     * @param request the request to apply to the drivetrain.
     */
    public void drive(SwerveRequest request) {
        swerve.setControl(request);
    }

    /**
     * make it go in auto.
     * @param speeds the chassis speeds to apply to the drivetrain.
     */
    public void drive(ChassisSpeeds speeds) {
        drive(autonomousRequest.withSpeeds(speeds));
    }

    /**
     * the missile knows where it is at all times. it knows this because it knows where it isn't.
     * @return the pose of the robot.
     */
    public Pose2d getPose () {
        if(swerve.odometryIsValid()) return swerve.getState().Pose;
        return new Pose2d();
    }

    /**
     * this missile even knows how fast it's traveling. it knows this because it knows how fast it isn't traveling.
     * @return the chassis speeds of the robot.
     */
    public ChassisSpeeds getChassisSpeeds() {
        return swerve.getChassisSpeeds();
    }
    
    /**
     * sometimes, the missile forgets where it is, and it's not even where it's been.
     */
    public void resetOdometry() {
        swerve.tareEverything();
    }

    /**
     * sometimes, we need to tell the missile where it is, and it's not even where it's been.
     * By subtracting where it's been from where it is, or where it's going from where it was, we get
     * where it should be.
     * @param pose the pose to set the robot to.
     */
    public void resetOdometry(Pose2d pose) {
        swerve.seedFieldRelative(pose);
    }

    /**
     * drive in a straight line to a target pose.
     * @param target the goal pose
     * @return the command to follow the path
     */
    public Command driveToPose(Pose2d target){
        //the target rotation is the angle of the curve, and we want to go in a straight line, so it
        //needs to be the angle between the robot and the target
        Rotation2d targetRotation = target.minus(getPose()).getRotation();

        //bezier curve from the current pose to the target pose
        List<Translation2d> waypoints = PathPlannerPath.bezierFromPoses(
            getPose(), 
            new Pose2d(target.getTranslation(), targetRotation)
        );

        PathPlannerPath path = new PathPlannerPath(
            waypoints,
            Constants.Swerve.autoMaxSpeed,
            new GoalEndState(0.0, target.getRotation())
        );

        return AutoBuilder.followPathWithEvents(path);
    }

    public double getTotalDriveCurrent(){
        return swerve.getTotalDriveCurrent();
    }

    @Override
    public void periodic() {
        // limelight.updateCTRESwerveOdometry(swerve, getPose(), getChassisSpeeds()); causes errer for some reason
    }

    @Override
    public void simulationPeriodic() {
        swerve.updateSimState(Constants.period, 12);
    }

    
}

