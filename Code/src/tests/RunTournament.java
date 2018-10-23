package tests;

import ai.Ben.StrategyChooser;
import ai.Ben.WorkerRush2;
import ai.Ben.mattRushAi;
import ai.RandomBiasedAI;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightDefense;
import ai.abstraction.LightRush;
import ai.Ben.newAI;
import ai.abstraction.RangedRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.evaluation.SimpleEvaluationFunction;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.uct.UCT;
import rts.units.UnitTypeTable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static tournaments.RoundRobinTournament.runTournament;

public class RunTournament {
    public static void main(String args[]) throws Exception {

        // Set tournament settings
        int rounds = 2;                                // Number of rounds in the tournament
        int timeBudget = 100;                          // Time budget allowed per action (default 100ms)
        int maxGameLength = 2000;                      // Maximum game length (default 2000 ticks)
        boolean fullObservability = true;              // Full or partial observability (default true)
        boolean selfMatches = false;                   // If self-play should be used (default false)
        boolean timeOutCheck = true;                   // If the game should count as a loss if a bot times out (default true)
        boolean preAnalysis = true;                    // If bots are allowed to analyse the game before starting (default true)
        int preAnalysisBudgetFirstTimeInAMap = 1000;   // Time budget for pre-analysis if playing first time on a new map (default 1s)
        int preAnalysisBudgetRestOfTimes = 1000;       // Time budget for pre-analysis for all other cases (default 1s)
        boolean runGC = false;                         // If Java Garbage Collector should be called before each player action (default false)
        int iterationBudget = -1;                      // Iteration budget, set to -1 for infinite (default: -1)
        int playOnlyWithThisAI = -1;                   //  AI index in list of AIs, if one AI should be included in all matches played (default -1)

        int inertiaCycles = 10;

        // Create list of AIs participating in tournament
        List<AI> AIs = new ArrayList<>();

        UnitTypeTable utt = new UnitTypeTable();
        PathFinding pf = new BFSPathFinding();

        // Add AIs to list
        //AIs.add(new UCT(timeBudget, -1, 100, 20, new RandomBiasedAI(),
        //        new SimpleEvaluationFunction()));
        //AIs.add(new NaiveMCTS(timeBudget, -1, 100, 20, 0.33f, 0.0f, 0.75f,
        //        new RandomBiasedAI(), new SimpleEvaluationFunction(), true));
        //AIs.add(new LightDefense(utt, pf));
        //AIs.add(new newAI(utt,pf));
        //AIs.add( new RandomBiasedAI());
        AIs.add( new RandomBiasedAI());
        AIs.add(new StrategyChooser(timeBudget, pf, new newAI(utt,pf), new WorkerRush2(utt,pf), new LightRush(utt,pf), new HeavyRush(utt,pf), new RangedRush(utt,pf), new mattRushAi(utt), inertiaCycles));



        // Create list of maps for tournament
        List<String> maps = new ArrayList<>();
        maps.add("maps/8x8/basesWorkers8x8.xml");

        // Initialize result writing
        String folderForReadWriteFolders = "readwrite";

//        String traceOutputFolder = "traces";
        String traceOutputFolder = null;  // Ignore traces

//        Writer out = new BufferedWriter(new FileWriter(new File("results.txt")));  // Print to file
        Writer out = new PrintWriter(System.out);  // Print to console

//        Writer progress = new BufferedWriter(new FileWriter(new File("progress.txt")));  // Write progress to file
        Writer progress = new PrintWriter(System.out);  // Write progress to console
//        Writer progress = null;  // Ignore progress

        // Run tournament
        runTournament(AIs,playOnlyWithThisAI, maps, rounds, maxGameLength, timeBudget, iterationBudget,
                preAnalysisBudgetFirstTimeInAMap, preAnalysisBudgetRestOfTimes, fullObservability, selfMatches,
                timeOutCheck, runGC, preAnalysis, new UnitTypeTable(), traceOutputFolder, out,
                progress, folderForReadWriteFolders);
    }
}
