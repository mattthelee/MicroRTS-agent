package exercise5;

import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.List;

public class BotExercise5 extends AIWithComputationBudget {
    private UnitTypeTable utt;
    private PathFinding pathFinding;
    private int calls;
    private int player;

    private Strategy myStrategy;

    /**
     * Constructs the controller with the specified time and iterations budget
     *
     * @param timeBudget       Time in milliseconds.
     * @param iterationsBudget Number of allowed iterations.
     * @param utt              Table that defines the unit types for this game.
     * @param pathFinding      Instance for performing path planning queries.
     */
    public BotExercise5(int timeBudget, int iterationsBudget, UnitTypeTable utt, PathFinding pathFinding) {
        super(timeBudget, iterationsBudget);
        this.utt = utt;
        this.pathFinding = pathFinding;
        calls = 0;

        myStrategy = new Strategy();
    }

    @Override
    public void reset() {
        calls = 0;
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        calls ++;
        this.player = player;
        return myStrategy.execute(player, gs, utt, pathFinding);
    }

    @Override
    public AI clone() {
        BotExercise5 myClone = new BotExercise5(TIME_BUDGET, ITERATIONS_BUDGET, utt, pathFinding);
        myClone.calls = calls;
        myClone.player = player;
        myClone.myStrategy = myStrategy.clone();
        return myClone;
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public void preGameAnalysis(GameState gs, long milliseconds, String readWriteFolder) throws Exception {
    }
}
