package frc.robot.statemachines;

import java.util.function.BooleanSupplier;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.lib.state.StateMachine;
import frc.robot.planners.AimPlanner;
import frc.robot.subsystems.Shooter;

public class ShooterStatemachine extends StateMachine<ShooterStatemachine.ShooterState> {
    private ShooterState state = ShooterState.RAMP_DOWN;

    private ShooterState lastAimingState = ShooterState.AUTO_AIM;

    private final Shooter shooter;
    private final AimPlanner aimPlanner;
    private final BooleanSupplier alignedToShoot;

    private boolean hasNote = false;

    private boolean manualTriggerMode = false;

    public ShooterStatemachine(Shooter shooter, AimPlanner aimPlanner, BooleanSupplier alignedToShoot){
        this.shooter = shooter;
        this.aimPlanner = aimPlanner;
        this.alignedToShoot = alignedToShoot;
    }

    /**
     * Handle the logic for changing states
     * e.g. intaking to indexing when the gamepiece is detected
     */
    private void updateState(){
        // switch (state) {
        //     case RAMP_DOWN:
        //         if(shooter.flywheelSwitchTripped() || shooter.triggerSwitchTripped()) state = ShooterState.INDEX;
        //     case INTAKE:
        //         if(shooter.flywheelSwitchTripped()) state = ShooterState.INDEX;
        //     case SHOOT:
        //         if(!alignedToShoot.getAsBoolean()) state = lastAimingState;
        //     case INDEX:
        //         if(!(shooter.triggerSwitchTripped() || shooter.flywheelSwitchTripped())) state = ShooterState.RAMP_DOWN;
        //     default:
        //         break;
        // }
        // if (
        //       (state == ShooterState.AUTO_AIM
        //     ||state == ShooterState.AIM_LAYUP
        //     ||state == ShooterState.AIM_PROTECTED)
        //     && alignedToShoot.getAsBoolean() 
        //     && shooter.flywheelAtTargetVelocity()
        // ) {
        //     lastAimingState = state;
        //     state = ShooterState.SHOOT;
        // }
    }

    /**
     * Request a state for the mechanism to attain
     * Won't allow for a state that would result in a collision or other dangerous situation
     * e.g. changing state before we have finished INDEXing
     * @param state
     */
    @Override
    public void requestState(ShooterState state){
        this.state = state;
    }

    /**
     * make the mechanism attain the desired state
     */
    @Override
    public void update(){
        updateState();
        SmartDashboard.putString("Shooter State", state.name());

        if(shooter.triggerSwitchTripped() || shooter.flywheelSwitchTripped()) hasNote = true;
        if(shooter.shotDetected()) hasNote = false;

        if(state == ShooterState.AUTO_AIM) {
            shooter.setFlywheelVelocity(aimPlanner.getTargetFlywheelVelocityRPS());
            shooter.setTrigerPercent(0);
            return;
        }

        if(state == ShooterState.SHOOT) {
            if(lastAimingState == ShooterState.AUTO_AIM) shooter.setFlywheelVelocity(aimPlanner.getTargetFlywheelVelocityRPS());
            else shooter.setFlywheelVelocity(lastAimingState.getFlywheelVelocity());
            shooter.setTrigerPercent(state.getTriggerPercent());
            return;
        }

        if(state == ShooterState.INDEX){
            if(shooter.flywheelSwitchTripped() && !shooter.triggerSwitchTripped()) shooter.setTrigerPercent(-state.getTriggerPercent());
            else if (shooter.triggerSwitchTripped() && !shooter.flywheelSwitchTripped()) shooter.setTrigerPercent(state.getTriggerPercent());
            else shooter.setTrigerPercent(0.0);

            shooter.brakeFlywheel();
            return;
        }

        if (state == ShooterState.COAST) {
            shooter.coastFlywheel();
            shooter.setTrigerPercent(0.0);
            return;
        }

        if (state == ShooterState.RAMP_DOWN) {
            shooter.brakeFlywheel();
            shooter.setTrigerPercent(0.0);
            return;
        }

        shooter.setFlywheelVelocity(state.getFlywheelVelocity());
        shooter.setTrigerPercent(state.getTriggerPercent());
    }

    @Override
    public ShooterState getState(){
        return state;
    }

    @Override
    public boolean transitioning(){
        return !shooter.flywheelAtTargetVelocity();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    public boolean hasNote(){
        return hasNote;
    }

    public enum ShooterState{
        RAMP_DOWN(0.0,0.0),
        COAST (0.0, 0.0),
        INTAKE(-10.0,1.0), //NOTE: this should fold flat if the flywheel-side intake is out
        INDEX(0.0,0.2),
        HANDOFF(20.0,1.0), //to diverter
        AIM_LAYUP(40.0,0.0),
        AIM_PROTECTED(200.0,0.0),
        AUTO_AIM(0.0,0.0),
        SHOOT(0.0,1.0);

        private Double flywheelVelocity, triggerPercent;

        public Double getFlywheelVelocity(){
            return flywheelVelocity;
        }

        public Double getTriggerPercent(){
            return triggerPercent;
        }

        private ShooterState (Double flywheelVelocity, Double triggerPercentage){
            this.flywheelVelocity = flywheelVelocity;
            this.triggerPercent = triggerPercentage;
        }

        private ShooterState() {
            flywheelVelocity = Double.NaN;
            triggerPercent = Double.NaN;
        }
    }
}
