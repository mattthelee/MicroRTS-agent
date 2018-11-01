package tests;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.strategychooser.QMLeeSaundersHind;
import gui.MouseController;
import gui.PhysicalGameStateMouseJFrame;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 * @author santi
 */
public class PlayGameWithMouseTest {
    public static void main(String args[]) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);  // Set map

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 10000;  // Set maximum game length
        int TIME_BUDGET = 100;  // Set time budget
        boolean gameover = false;
                
        PhysicalGameStatePanel pgsp = new PhysicalGameStatePanel(gs);
        PhysicalGameStateMouseJFrame w = new PhysicalGameStateMouseJFrame("Game State Visualizer (Mouse)",
                                                                          640,640,pgsp);
//        PhysicalGameStateMouseJFrame w = new PhysicalGameStateMouseJFrame("Game State Visualizer (Mouse)",
//                                                                          400,400,pgsp);

        AI ai1 = new MouseController(w);  // This is a human player controller

        // Set opponent AI
//        AI ai2 = new PassiveAI();
//        AI ai2 = new RandomBiasedAI();
//        AI ai2 = new LightRush(utt, new AStarPathFinding());
        //AI ai2 = new ContinuingAI(new NaiveMCTS(TIME_BUDGET, -1, 100, 20, 0.33f, 0.0f, 0.75f, new RandomBiasedAI(), new SimpleEvaluationFunction(), true));
        AI ai2 = new QMLeeSaundersHind(100, utt, new AStarPathFinding(), new SimpleSqrtEvaluationFunction3(), 10);
        // Play game
        long nextTimeToUpdate = System.currentTimeMillis() + TIME_BUDGET;
        do {
            if (System.currentTimeMillis() >= nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);  // Get action from player 1
                PlayerAction pa2 = ai2.getAction(1, gs);  // Get action from player 2

                // Issue actions
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // Game ticks forward
                gameover = gs.cycle();
                w.repaint();
                nextTimeToUpdate += TIME_BUDGET;
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (!gameover && gs.getTime() < MAXCYCLES);

        // Tell the AIs the game is over
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        
        System.out.println("Game Over");
    }    
}
