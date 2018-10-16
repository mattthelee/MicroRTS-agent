package tests;

import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.List;

public class OriginalBot extends AIWithComputationBudget {

    public OriginalBot(int timeBudget, int iterationBudget, UnitTypeTable utt, PathFinding pf) {
        super(timeBudget, iterationBudget);

        actionNumber = 0;
        player = 0;

        this.utt = utt;
        this.pf = pf;

    }

    private UnitTypeTable utt;
    private PathFinding pf;

    int actionNumber;
    int player;

    int timeBudget; //Isn't this what the 'Super' does??
    int iterationBudget; //Isn't this what the 'Super' does??

    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType heavyType;



    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();

        actionNumber++;
        //Player playerID = gs.getPlayer(player);
        Player p = gs.getPlayer(player);

        //System.out.println("PlayerID: " + playerID + "|| Action's so far: " + actionNumber);


        int nPlayerUnits = 0;
        int nEnemyUnits = 0;
        int nResources = 0;

        int resourceAvailable = p.getResources();

        if ( gs.canExecuteAnyAction(p.getID())){
            //Can execute actions

            for (Unit u : pgs.getUnits()){
                if (u.getPlayer() == p.getID()){
                    nPlayerUnits++;
                    if (u.getType().name == "Worker" && gs.getActionAssignment(u) == null){
                        List actionsList = u.getUnitActions(gs);


                        //System.out.println(actionsList);
                        //System.out.println("X: " + u.getX() + " - Y: " + u.getY() + " - ID: " + u.getID());

                    }
                } else if (u.getPlayer() != p.getID()){
                    nEnemyUnits++;
                } else if (u.getType().isResource){
                    nResources++;
                }
            //System.out.println("Player units: "+ nPlayerUnits + ". Enemy units: " + nEnemyUnits);
            }


        } else {
            //Can't execute actions
        }

        PlayerAction pA = new PlayerAction();
        return  pA;

    }

    public AI clone() {
        OriginalBot OriginalBotObject = new OriginalBot(timeBudget, iterationBudget, utt, pf);

        OriginalBotObject.actionNumber = actionNumber;
        OriginalBotObject.player = player;

        return OriginalBotObject;

    }

    public void reset() {
        actionNumber = 0;
    }

    public void reset(UnitTypeTable utt) {

        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList();
        return parameters;
    }

    public void preGameAnalysis() {

    }

}




