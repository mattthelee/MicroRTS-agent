package tests;

import ai.Ben.WorkerRush2;
import ai.Ben.mattRushAi;
import ai.abstraction.*;
import ai.Ben.newAI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.*;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleEvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.machinelearning.bayes.BayesianModel;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.uct.UCT;
import ai.minimax.ABCD.ABCD;
import ai.minimax.MiniMaxResult;
import ai.minimax.RTMiniMax.IDRTMinimax;
import ai.montecarlo.MonteCarlo;
import ai.montecarlo.NewMonteCarlo;
import ai.Ben.StrategyChooser;
import ai.puppet.PuppetSearchAB;
import ai.puppet.PuppetSearchMCTS;
import ai.scv.SCV;
import ai.socket.SocketAI;
import exercise5.BotExercise5;
import gui.PhysicalGameStatePanel;
import javax.swing.JFrame;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/**
 * @author santi
 */

public class GameVisualSimulationTest {
    public static void main(String args[]) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        PathFinding pf = new BFSPathFinding();

        // Assignment Maps:
        PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);// Set map
        //PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/TwoBasesBarracks16x16.xml", utt);// Set map
        //PhysicalGameState pgs = PhysicalGameState.load("maps/24x24/basesWorkers24x24H.xml", utt);
        //PhysicalGameState pgs = PhysicalGameState.load("maps/NoWhereToRun9x8.xml", utt);

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 2000;  // Maximum length of the game
        int TIME_BUDGET = 100;  // Time budget for AIs
        boolean gameover = false;



        // Set AIs playing the game


        //AI ai1 = new OriginalBot(timeBudget, iterationBudget, utt, pf);
        //AI ai1 = new BotExercise5(timeBudget, iterationBudget, utt, pf);

        int lookahead = 100;
        int playouts_per_cycle = -1;
        int inertiaCycles = 10;



        AI ai1 = new StrategyChooser(TIME_BUDGET, utt, pf, inertiaCycles);

        //AI ai2 = new mattRushAi(utt);

        //AI ai2 = new NewMonteCarlo(timeBudget, playouts_per_cycle, 150, new newAI(utt,pf), a_ef);
        //AI ai2 = new newAI(utt, pf);
        //AI ai2 = new newAI(utt, pf);

        //AI ai2 = new ABCD(utt);

        //AI ai1 = new NewMonteCarlo(utt, pf);

        //AI ai2 = new PuppetSearchMCTS(utt);

        //AI ai2 = new IDRTMinimax(utt);

        //AI ai2 = new SCV(utt);
        //AI ai2 = new WorkerRush(utt, pf);
        //AI ai1 = new LightRush(utt, pf);

        //AI ai2 = new RangedRush(utt, pf);
        //AI ai2 = new HeavyRush( utt, pf);
        //AI ai2 = new NaiveMCTS(timeBudget, -1, 100, 20, 0.33f, 0.0f, 0.75f,
          //      new newAI(utt,pf), new SimpleEvaluationFunction(), true);

        //AI ai2 = new EconomyMilitaryRush(utt, pf);
        //AI ai2 = new EconomyRushBurster(utt, pf);
        //AI ai2 = new WorkerDefense(utt, pf);

        AI ai2 = new RandomBiasedAI();


        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,true,
                                                        PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,
//                                                        PhysicalGameStatePanel.COLORSCHEME_WHITE);

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
                nextTimeToUpdate+=TIME_BUDGET;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
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
