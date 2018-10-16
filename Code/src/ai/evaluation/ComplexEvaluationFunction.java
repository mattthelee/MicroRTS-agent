/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.evaluation;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author santi
 * 
 * This function uses the same base evaluation as SimpleSqrtEvaluationFunction and SimpleSqrtEvaluationFunction2, but returns the (proportion*2)-1 of the total score on the board that belongs to one player.
 * The advantage of this function is that evaluation is bounded between -1 and 1.
 */
public class ComplexEvaluationFunction extends EvaluationFunction {
    public static float RESOURCE = 20;
    public static float RESOURCE_IN_WORKER = 10;
    public static float UNIT_BONUS_MULTIPLIER = 40.0f;


    public float evaluate(int maxplayer, int minplayer, GameState gs){
        this.evaluate(maxplayer,minplayer,gs,1.0);
    }
    public float evaluate(int maxplayer, int minplayer, GameState gs, float aggressionWeight) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float s1 = gs.getPlayer(maxplayer).getResources()*RESOURCE;
        float s2 = gs.getPlayer(minplayer).getResources()*RESOURCE;
        List<Unit> maxUnits = new ArrayList<Unit>();
        List<Unit> minUnits = new ArrayList<Unit>();
        Unit maxBase = null;
        Unit minBase = null;

        for(Unit u:pgs.getUnits()){
            if (u.getPlayer() == maxplayer){
                maxUnits.add(u);
                if (u.getType().name == "Base" {
                    maxBase = u;
                }
            } else if (u.getPlayer() == minplayer){
                if (u.getType().name == "Base" {
                    minBase = u;
                }
            }
        }
        for(Unit maxUnit : maxUnits){
            
        }

        if (s1 + s2 == 0) return 0.5f;
        return  (2*s1 / (s1 + (aggressionWeight * s2)))-1;
    }

    public float evaluate(int maxplayer, int minplayer, GameState gs) {
        float s1 = base_score(maxplayer,gs);
        float s2 = base_score(minplayer,gs);
        if (s1 + s2 == 0) return 0.5f;
        return  (2*s1 / (s1 + s2))-1;
    }
    
    public float base_score(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float score = gs.getPlayer(player).getResources()*RESOURCE;
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer() !=player) {
                ;
            }
        }
        boolean anyunit = false;
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer()==player) {
                anyunit = true;
                score += u.getResources() * RESOURCE_IN_WORKER;
                score += UNIT_BONUS_MULTIPLIER * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints() );
            }
        }
        if (!anyunit) return 0;
        return score;
    }

    public float dist_to_enemy(int Player, Unit u, GameState gs){
        for (Unit unit : pgs.get)
    }
    
    public float upperBound(GameState gs) {
        return 1.0f;
    }
}
