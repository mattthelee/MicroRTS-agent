/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.Ben;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

/**
 *
 * @author santi
 */

public class newAI extends AbstractionLayerAI {

    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType heavyType;

    // Strategy implemented by this class:
    // For the first 4 melee units: send it to attack to the nearest enemy unit
    // If over 4 melee units, send the rest to attack the base
    // If we have a base: train workers until we have 4 workers
    // If we have a barracks: train light and heavy in randomness
    // If we have a worker: do this if needed: build base, build barracks, harvest resources, attack if enemy close

    public newAI(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }


    public newAI(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable a_utt)
    {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
    }


    public AI clone() {
        return new newAI(utt, pf);

    }

    /*
        This is the main function of the AI. It is called at each game cycle with the most up to date game state and
        returns which actions the AI wants to execute in this cycle.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        This method returns the actions to be sent to each of the units in the gamestate controlled by the player,
        packaged as a PlayerAction.
     */
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");

        // Calculate how many bases units the enemy has
        int nmelee = 0;
        int nEnemyBase = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() != p.getID()) {
                nEnemyBase++;
            }
        }

        // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                baseBehavior(u, p, pgs);
        // behavior of barracks:
            } else if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
        // behavior of melee units:
            } else if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {

                boolean attackBase;
                nmelee++;
                //If 4 or more melee units, and enemy base still standing - use 4th and higher units to attack base
                if (nmelee >= 4 && nEnemyBase > 0) {
                    // If 4th or higher melee unit, attack the base
                    attackBase = true;
                } else {
                    attackBase = false;
                }
                meleeUnitBehavior(u, p, pgs, attackBase);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
                //System.out.println("X: " + u.getX() + " Y: " + u.getY());
            }
        }
        workersBehavior(workers, p, pgs);

        // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
        return translateActions(player, gs);
    }

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nSelfWorkers = 0;
        int nOppWorkers = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nSelfWorkers++;
            } else if (u2.getType() == workerType
                    && u2.getPlayer() != p.getID()){
                nOppWorkers++;
            }
        }
        //Train up to 4 Workers
        if ((nSelfWorkers < 4 || nSelfWorkers < nOppWorkers) && p.getResources() >= workerType.cost) {
            train(u, workerType);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nbarracks = 0;

        for (Unit u2 : pgs.getUnits()) {
            // Calculate how many barracks
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }

        double random = Math.random();

        // If only one barracks, only train Light units
        if (nbarracks <= 1) {
            if (p.getResources() >= lightType.cost) {
                train(u, lightType);
            }

        // If 2 barracks, randomly train Light or Heavy units
        } else if (nbarracks > 1 && random < 0.5) {
            if (p.getResources() >= lightType.cost) {
                train(u, lightType);
            }
        } else if (nbarracks > 1 && random >= 0.5) {
            if (p.getResources() >= heavyType.cost) {
                train(u, heavyType);
            }
        }
    }

    public void meleeUnitBehavior(Unit u, Player p, PhysicalGameState pgs, boolean attackBase) {

        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            // Just attack closest enemy if not scheduled to attack base
            if (!attackBase){
                if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestEnemy == null || d < closestDistance) {
                        closestEnemy = u2;
                        closestDistance = d;
                    }
                }
            // Attack the closest base if scheduled to attack base
            } else if (attackBase){
                if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID() && u2.getType() == baseType) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestEnemy == null || d < closestDistance) {
                        closestEnemy = u2;
                        closestDistance = d;
                    }
                }
            }
        }

        if (closestEnemy != null) {
            attack(u, closestEnemy);
        }
    }

    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
        int nbases = 0;
        int nbarracks = 0;

        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        freeWorkers.addAll(workers);

        if (workers.isEmpty()) {
            return;
        }

        for (Unit u2 : pgs.getUnits()) {
            // Calculate how many bases
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
            // Calculate how many barracks
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases < 1 && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += baseType.cost;
            }
        }

        int width = pgs.getWidth();
        int height = pgs.getHeight();

        if (nbarracks < 2 && !freeWorkers.isEmpty()) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                //Check if the unit in the bottom or top corner, then build barracks 2 away from their location
                ArrayList<Integer> positions = barracksPosition(pgs,u);
                buildIfNotAlreadyBuilding(u,barracksType,positions.get(0),positions.get(1),reservedPositions,p,pgs);
                /*
                if ((u.getX()+u.getY()) < width){
                    buildIfNotAlreadyBuilding(u,barracksType,u.getX()+2,u.getY()+1,reservedPositions,p,pgs);
                } else  buildIfNotAlreadyBuilding(u,barracksType,u.getX()-2,u.getY()-1,reservedPositions,p,pgs);
                */
                resourcesUsed += barracksType.cost;
            }
        }

        //Only harvest with 4 workers:

        int nHarvestWorkers = 4;
        Unit harvestWorker = null;

        List<Unit> harvestWorkers = new LinkedList<Unit>();
        if (freeWorkers.size()>nHarvestWorkers) {
            for (int i = 0 ; i < nHarvestWorkers; i++){
                harvestWorker = freeWorkers.remove(0);
                harvestWorkers.add(harvestWorker);
            }
        } else if (freeWorkers.size()>0) {
            for (int i = 0 ; i <= freeWorkers.size(); i++){
                harvestWorker = freeWorkers.remove(0);
                harvestWorkers.add(harvestWorker);
            }

        }



        // harvest with all the harvest workers:
        for (Unit u : harvestWorkers) {

            Unit closestEnemy = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestEnemy == null || d < closestDistance) {
                        closestEnemy = u2;
                        closestDistance = d;
                    }
                }
            }

            //Turn into attack units if an enemy comes close (within 5)

            if (closestEnemy != null && closestDistance < 3) {
//            System.out.println("LightRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
                attack(u, closestEnemy);
            } else { //Be a harvest unit else

                Unit closestBase = null;
                Unit closestResource = null;
                closestDistance = 0;
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getType().isResource) {
                        int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                        if (closestResource == null || d < closestDistance) {
                            closestResource = u2;
                            closestDistance = d;
                        }
                    }
                }
                closestDistance = 0;
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                        int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                        if (closestBase == null || d < closestDistance) {
                            closestBase = u2;
                            closestDistance = d;
                        }
                    }
                }
                if (closestResource != null && closestBase != null) {
                    AbstractAction aa = getAbstractAction(u);
                    if (aa instanceof Harvest) {
                        Harvest h_aa = (Harvest) aa;
                        if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase)
                            harvest(u, closestResource, closestBase);
                    } else {
                        harvest(u, closestResource, closestBase);
                    }
                }
                // If no more resources, turn into an attack unit
                if (closestResource == null) {
                    closestEnemy = null;
                    closestDistance = 0;
                    for (Unit u2 : pgs.getUnits()) {
                        if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                            int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                            if (closestEnemy == null || d < closestDistance) {
                                closestEnemy = u2;
                                closestDistance = d;
                            }
                        }
                    }
                    if (closestEnemy != null) {
                        attack(u, closestEnemy);
                    }
                }
            }
        }

        //Schedule the free workers - if more than 5, get some to attack the base
        if (workers.size() > 5){
            int i = 0;
            boolean attackBase;
            for(Unit u:freeWorkers) {

                if (i == 0 || i == 1){
                    attackBase = true;
                } else {
                    attackBase = false;
                }
                meleeUnitBehavior(u, p, pgs, attackBase);
                i++;
            }
        } else {
            boolean attackBase = false;
            for(Unit u:freeWorkers) meleeUnitBehavior(u, p, pgs, attackBase);
        }
    }

    public ArrayList barracksPosition(PhysicalGameState pgs, Unit u){
        int width = pgs.getWidth();
        int uX = u.getX();
        int buildX; int spaceX;

        int height = pgs.getHeight();
        int uY = u.getY();
        int buildY; int spaceY;

        //Determine if in top left or bottom right
        if ((uX+uY) < width){
            //top left
            spaceX = 1;
            spaceY = 1;
        } else {
            //bottom right
            spaceX = -1;
            spaceY = -1;
        }

        //Width
        if ((uX - spaceX > 0) && (uX + spaceX < width)){
            buildX = uX + spaceX;
        } else if ((uX + spaceX > 0) && (uX - spaceX < width)){
            buildX = uX - spaceX;
        } else {
            buildX = uX;
        }

        //Height
        if ((uY - spaceY > 0) && (uY + spaceY < height)){
            buildY = uY + spaceY;
        } else if ((uY + spaceY > 0) && (uY - spaceY < height)){
            buildY = uY - spaceY;
        } else {
            buildY = uY;
        }

        ArrayList<Integer> positions =  new ArrayList<>();
        positions.add(buildX);
        positions.add(buildY);
        return positions;
    }

    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }

}
