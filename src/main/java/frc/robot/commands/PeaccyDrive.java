package frc.robot.commands;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Command;
import frc.lib.telemetry.SwerveTelemetry;
import frc.lib.util.Curves;
import frc.robot.Constants;
import frc.robot.subsystems.DriveTrain;

public class PeaccyDrive extends Command {
    private final DoubleSupplier xVelocitySup, yVelocitySup, angularVelocitySup, autoHeadingAngleSup;
    private final BooleanSupplier isAutoAngleSup, isFieldRelativeSup, isOpenLoopSup, isLockInSup;
    private final DriveTrain driveTrain;

    private final SwerveRequest.FieldCentric fieldCentricRequest = new SwerveRequest.FieldCentric();
    private final SwerveRequest.RobotCentric robotCentricRequest = new SwerveRequest.RobotCentric();
    private final SwerveRequest.FieldCentricFacingAngle autoHeadingRequest = new SwerveRequest.FieldCentricFacingAngle();
    private final SwerveRequest.SwerveDriveBrake lockInRequest = new SwerveRequest.SwerveDriveBrake();

    private final double linearSpeedDeadband = 0.1,
                         angularVelocityDeadband = 0.13;
    private final SlewRateLimiter linearSpeedLimiter = new SlewRateLimiter(Constants.Swerve.teleopLinearSpeedLimit);
    private final SlewRateLimiter linearAngleLimiter = new SlewRateLimiter(Constants.Swerve.teleopLinearAngleLimit);
    private final SlewRateLimiter angularVelocityLimiter = new SlewRateLimiter(Constants.Swerve.teleopAngularRateLimit);
    private BooleanSupplier isZeroOdometrySup;


    /**
     * PeaccyDrive is a swerve drive command designed to handle all the different
     * modes of driving that we want to use.
     *
     * It has advanced input smoothing and deadbanding,
     * field centric and robot centric modes,
     * and auto angle (automatic heading adjustment) modes.
     * @param xVelocitySup the requested x velocity
     * @param yVelocitySup the requested y velocity
     * @param angularVelocitySup the requested angular velocity
     * @param autoHeadingAngleSup the requested auto heading angle
     * @param isAutoAngleSup whether or not to use automatic heading adjustment
     * @param isFieldRelativeSup whether or not to use field centric mode
     * @param isOpenLoopSup whether or not to use open loop controls
     * @param isLockInSup whether or not to X-lock the wheels when stopped
     * @param isZeroOdometry whether or not to zero the odometry
     * @param driveTrain the swerve subsystem
     */
    public PeaccyDrive(DoubleSupplier xVelocitySup,
                      DoubleSupplier yVelocitySup,
                      DoubleSupplier angularVelocitySup,
                      DoubleSupplier autoHeadingAngleSup,
                      BooleanSupplier isAutoAngleSup,
                      BooleanSupplier isFieldRelativeSup,
                      BooleanSupplier isOpenLoopSup,
                      BooleanSupplier isLockInSup,
                      BooleanSupplier isZeroOdometry,
                      DriveTrain driveTrain) {
        this.xVelocitySup = xVelocitySup;
        this.yVelocitySup = yVelocitySup;
        this.angularVelocitySup = angularVelocitySup;
        this.autoHeadingAngleSup = autoHeadingAngleSup;
        this.isAutoAngleSup = isAutoAngleSup;
        this.isFieldRelativeSup = isFieldRelativeSup;
        this.isOpenLoopSup = isOpenLoopSup;
        this.isLockInSup = isLockInSup;
        this.isZeroOdometrySup = isZeroOdometry;
        this.driveTrain = driveTrain;

        autoHeadingRequest.HeadingController.setP(Constants.Swerve.autoHeadingKP);
        autoHeadingRequest.HeadingController.setI(Constants.Swerve.autoHeadingKI);
        autoHeadingRequest.HeadingController.setD(Constants.Swerve.autoHeadingKD);

        addRequirements(driveTrain);
    }

    @Override
    public void execute() {
        double xVelocity = xVelocitySup.getAsDouble();
        double yVelocity = yVelocitySup.getAsDouble();
        double angularVelocity = angularVelocitySup.getAsDouble();
        double autoHeadingAngle = autoHeadingAngleSup.getAsDouble();

        boolean isAutoHeading = isAutoAngleSup.getAsBoolean();
        boolean isFieldRelative = isFieldRelativeSup.getAsBoolean();
        boolean isOpenLoop = isOpenLoopSup.getAsBoolean();
        boolean isLockIn = isLockInSup.getAsBoolean();
        boolean isZeroOdometry = isZeroOdometrySup.getAsBoolean();

        if(isZeroOdometry) driveTrain.resetOdometry();

        // handle smoothing and deadbanding
        Translation2d linearVelocity = new Translation2d(xVelocity, yVelocity);
        linearVelocity = smoothAndDeadband(linearVelocity).times(Constants.Swerve.teleopLinearMultiplier);
        angularVelocity = smoothAndDeadband(angularVelocity) * Constants.Swerve.teleopAngularMultiplier;

        // log data
        SwerveTelemetry.updateSwerveCommand(
            linearVelocity.getX(), 
            linearVelocity.getY(), 
            angularVelocity, 
            autoHeadingAngle, 
            isAutoHeading, 
            isFieldRelative, 
            isOpenLoop, 
            isLockIn, 
            isZeroOdometry
        );



        //handle lock in
        if (isLockIn) {
            if (linearVelocity.equals(new Translation2d(0,0)) && angularVelocity == 0) {
                driveTrain.drive(lockInRequest.withIsOpenLoop(isOpenLoop));
                return;
            }
        }

        //handle auto angle
        if (isAutoHeading) {
            //convert angle to be -180 to 180
            autoHeadingAngle = Math.IEEEremainder(autoHeadingAngle, 360);
            if (autoHeadingAngle > 180) autoHeadingAngle -= 360;
            if (autoHeadingAngle < -180) autoHeadingAngle += 360;

            autoHeadingRequest.withIsOpenLoop(isOpenLoop)
                            .withVelocityX(linearVelocity.getX())
                            .withVelocityY(linearVelocity.getY())
                            .withTargetDirection(Rotation2d.fromDegrees(autoHeadingAngle))
                            .withRotationalDeadband(0.1); // todo do we need rotational deadband?

            driveTrain.drive(autoHeadingRequest);
            return;
        }

        //handle field relative
        if (isFieldRelative) {
            fieldCentricRequest.withIsOpenLoop(isOpenLoop)
                                .withVelocityX(linearVelocity.getX())
                                .withVelocityY(linearVelocity.getY())
                                .withRotationalRate(angularVelocity);

            driveTrain.drive(fieldCentricRequest);
            return;
        }

        //handle robot relative
        robotCentricRequest.withIsOpenLoop(isOpenLoop)
                            .withVelocityX(linearVelocity.getX())
                            .withVelocityY(linearVelocity.getY())
                            .withRotationalRate(angularVelocity);

        driveTrain.drive(robotCentricRequest);
    }

    private Translation2d smoothAndDeadband (Translation2d linearVelocity) {
        //handle deadband and reset the rate limiter if we're in the deadband
        double rawLinearSpeed = handleDeadbandFixSlope(linearSpeedDeadband,linearVelocity.getNorm());
        if(Math.abs(rawLinearSpeed) < linearSpeedDeadband) linearSpeedLimiter.reset(0);
        rawLinearSpeed = Curves.herraFCurve(rawLinearSpeed, 6, 4.5);

        //smooth the linear speed
        double linearSpeed = linearSpeedLimiter.calculate(rawLinearSpeed);

        //smooth the linear angle
        double rawLinearAngle = linearVelocity.getAngle().getRadians();
        double linearAngle = linearAngleLimiter.calculate(rawLinearAngle);

        // override the smoothing if it lags too far behind the raw value
        // (mainly after stopping and changing direction)
        if (Math.abs(linearAngle - rawLinearAngle) > Math.PI/4) {
            linearAngleLimiter.reset(rawLinearAngle);
            linearAngle = rawLinearAngle;
        }

        return new Translation2d(linearSpeed, new Rotation2d(linearAngle));
    }

    private double smoothAndDeadband (double angularVelocity) {
        //apply deadband to angular velocity
        angularVelocity = handleDeadbandFixSlope(angularVelocityDeadband, angularVelocity);

        //simple square curve on the angular velocity to make it more responsive
        angularVelocity = Math.copySign(Math.pow(angularVelocity, 2), angularVelocity);

        //limit the angular velocity
        angularVelocity = angularVelocityLimiter.calculate(angularVelocity);

        return angularVelocity;
    }

    /**
     * This function handles deadband by increases the slope of the output after
     * the deadband, so the output is 0 at the end of the deadband but still 100% for 100% input. 
     * This preserves fine control while still eliminating unnecessary current draw and preventing joystick drift.
     * @param deadband the deadband to use
     * @param value the value to apply the deadband to
     * @return the value with the deadband applied (like magic)
     */
    private double handleDeadbandFixSlope (double deadband, double value) {
        if (Math.abs(value) < deadband) return 0;
        return (value - (deadband * Math.signum(value)))/(1 - deadband);
    }
}
