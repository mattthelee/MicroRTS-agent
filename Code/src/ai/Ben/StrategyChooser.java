package ai.Ben;

import ai.RandomBiasedAI;
import ai.abstraction.*;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.abstraction.WorkerRush;
import ai.abstraction.RangedRush;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.ComplexEvaluationFunction;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction;
import rts.*;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.util.*;


/**
 *
 * @author santi
 */
public class StrategyChooser extends AbstractionLayerAI {

    UnitTypeTable utt = new UnitTypeTable();
    PathFinding pf = new BFSPathFinding();

    //long MAXACTIONS;
    int MAXSIMULATIONTIME;
    int INERTIACYCLES;
    int GAMECOUNT = 0;

    AI newAI; AI LightRush; AI WorkerRush; AI HeavyRush; AI RangedRush; AI MattRush;

    PlayerActionTableEntry actions = new PlayerActionTableEntry();
    AI lastStrategy = null;

    List<AI> strategies = new ArrayList<>();
    SimpleSqrtEvaluationFunction evaluateFunction = new SimpleSqrtEvaluationFunction();
    //ComplexEvaluationFunction evaluateFunction = new ComplexEvaluationFunction();
    public StrategyChooser( int lookahead, PathFinding a_pf,AI newai, AI workerRush,
                           AI lightRush , AI heavyRush, AI rangedRush, AI mattRush, int inertiaCycles) {
        super(a_pf);
        //MAXACTIONS = -1;
        MAXSIMULATIONTIME = lookahead;
        newAI = newai;
        MattRush = mattRush;
        WorkerRush = workerRush;
        LightRush = lightRush;
        HeavyRush = heavyRush;
        RangedRush = rangedRush;
        MattRush = mattRush;
        INERTIACYCLES = inertiaCycles;
    }


    public void reset() {
        strategies = new ArrayList<>();
        evaluateFunction = new SimpleSqrtEvaluationFunction();
        //evaluateFunction = new ComplexEvaluationFunction();
    }

    public AI clone() {
        return new StrategyChooser(MAXSIMULATIONTIME,pf,

                newAI,WorkerRush,LightRush,HeavyRush,RangedRush, MattRush, INERTIACYCLES);
    }

    public class PlayerActionTableEntry {
        PlayerAction pa;
    }



    //Main method that receives the action
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        if (gs.canExecuteAnyAction(player)) {

            GameState gs2 = gs.clone();

            List<AI> strategies = new ArrayList<>();

            strategies.add(newAI);
            strategies.add(WorkerRush);
            strategies.add(MattRush);
            //strategies.add(LightRush);
            //strategies.add(HeavyRush);
            //strategies.add(RangedRush);
            //strategies.add(RandomAI);

            // evaluateStrategies returns a PlayerAction to be returned by the getAction
            return evaluateStrategies(player, gs2, strategies);

        } else {
            return new PlayerAction();
        }

    }


    public PlayerAction evaluateStrategies(int player, GameState gs2, List<AI> strategies) throws Exception {

        // Could combine with getAction method??

        AI topStrategy = new WorkerRush(utt,pf);
        //GameState gs3 = gs2.clone();

        // First game run
        if ( GAMECOUNT == 0 ){
            System.out.println("First run" );
            topStrategy = firstRunStrategyAnalysis(player, gs2, topStrategy);

        } else if ( GAMECOUNT % INERTIACYCLES == 0 ){
            // Come to end of inertia run
            // Find the top strategies by simulating all the strategies in 'strategies' list
            System.out.println( "\n" + "Simulation -- " );
            topStrategy = simulateAndFindTopStrategy(player, gs2, strategies, topStrategy);

        } else {
            // Middle of an inertia run, so keep last strategy
            topStrategy = lastStrategy;
            //TODO Keep simulation to find the best strategy when inertia is finished
            //TODO set global variable for
            System.out.println("Inertia strategy using " + topStrategy.toString() );
        }

        //PlayerAction action =
        actions.pa = topStrategy.getAction(player,gs2);
        lastStrategy = topStrategy;

        //System.out.println("Total Runs: " + totalRuns);
        //System.out.println("Using: " + topStrategy.toString() + " with score of: " + highscore + "\n");

        GAMECOUNT ++;

        return actions.pa;
    }


    public AI firstRunStrategyAnalysis(int player, GameState gs3, AI topStrategy) throws Exception {

        //TODO What about the hidden map??
        //TODO Amend the relevant initial strategies for each map

        PhysicalGameState pgs = gs3.getPhysicalGameState();
        int mapHeight = pgs.getHeight();
        int nbases = 0;

        // Calculate how many bases
        for (Unit u : pgs.getUnits()) {
            if (u.getType().name == "Base"
                    && u.getPlayer() == player) {
                nbases++;
            }
        }

        if (mapHeight == 8){
            //Map = "maps/NoWhereToRun9x8.xml"
            System.out.println("Map = maps/NoWhereToRun9x8.xml");
            topStrategy = WorkerRush;

        } else if (mapHeight == 16 && nbases == 1) {
            //Map = "maps/16x16/basesWorkers16x16.xml"
            System.out.println("Map = maps/16x16/basesWorkers16x16.xml");
            topStrategy = MattRush;

        } else if   (mapHeight == 16 && nbases == 2) {
            //Map = "maps/16x16/TwoBasesBarracks16x16.xml"
            System.out.println("Map = maps/16x16/TwoBasesBarracks16x16.xml");
            topStrategy = MattRush;

        } else if (mapHeight == 24){
            //Map = "maps/24x24/basesWorkers24x24H.xml"
            System.out.println("Map = maps/24x24/basesWorkers24x24H.xml");
            topStrategy = MattRush;
        } else {
            topStrategy = WorkerRush;
        }

        System.out.println("Using: " + topStrategy.toString() );
        return topStrategy;
    }

    public AI simulateAndFindTopStrategy(int player, GameState gs, List<AI> strategies, AI topStrategy) throws Exception{

        Integer[] votes = predictEnemyStrategy(player,gs);

        float highscore = Integer.MIN_VALUE;

        int simulations = 0;
        for (int j = 0; j<4 ; j++){
            if (votes[j] > 0){
                simulations++;
            }
        }
        int totalRuns = (simulations*strategies.size());
        int timeAllowed = MAXSIMULATIONTIME/totalRuns;

        float workerRushScore; float lightRushScore; float heavyRushScore; float rangedRushScore;

        for (int i = 0;i < strategies.size();i++){
            AI aiStrategy = strategies.get(i);


            if (votes[0] > 0){
                //The scores of the current strategy against a particular enemy strategy
                workerRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs,gs.getTime() + timeAllowed, aiStrategy, WorkerRush));
                System.out.println("Simulating against WorkerRush. EvalScore: " + workerRushScore );
            } else { workerRushScore = 0;}

            if (votes[1] > 0){
                lightRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs,gs.getTime() + timeAllowed, aiStrategy, LightRush));
                System.out.println("Simulating against LightRush. EvalScore: " + lightRushScore );
            } else { lightRushScore = 0;}

            if (votes[2] > 0){
                heavyRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs,gs.getTime() + timeAllowed, aiStrategy, HeavyRush));
                System.out.println("Simulating against HeavyRush. EvalScore: " + heavyRushScore );
            } else { heavyRushScore = 0;}

            if (votes[3] > 0){
                rangedRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs,gs.getTime() + timeAllowed, aiStrategy, RangedRush));
                System.out.println("Simulating against RangedRush. EvalScore: " + rangedRushScore );
            } else { rangedRushScore = 0;}

            float score = (votes[0]*workerRushScore) + (votes[1]*lightRushScore) + (votes[2]*heavyRushScore) + (votes[3]*rangedRushScore);

            System.out.println("Finished considering playing as: " + aiStrategy.toString() + " : " + score);

            if (score > highscore){
                highscore = score;
                topStrategy = aiStrategy;
            }
        }
        //System.out.println(topStrategy);


        System.out.println("Using: " + topStrategy.toString() + " with score of: " + highscore + "\n");
        return topStrategy;
    }


    public Integer[] predictEnemyStrategy(int player, GameState gs){
        PhysicalGameState pgs = gs.getPhysicalGameState();

        //Calculate how many units types the enemy has, to determine the votes
        int nEnemyWorkers = 0; int nEnemyLight = 0; int nEnemyHeavy = 0; int nEnemyRanged = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) {
                if (u.getType().name == "Worker"){
                    nEnemyWorkers ++;
                } else if (u.getType().name == "Light"){
                    nEnemyLight++;
                } else if (u.getType().name == "Heavy"){
                    nEnemyHeavy++;
                } else if (u.getType().name == "Ranged"){
                    nEnemyRanged++;
                }
            }
        }

        //votes[0] is Worker, votes[1] is Light, votes[2] is Heavy, votes[3] = Ranged
        Integer[] votes = new Integer[4];

        //Integer[] array = new Integer[4];
        votes[0] = 1 + (nEnemyWorkers*4);
        votes[1] = (nEnemyLight*5);
        votes[2] = (nEnemyHeavy*5) ;
        votes[3] = (nEnemyRanged*5);

        int maxTop = 0;
        for (int i = 0; i < votes.length; i++) {
            maxTop = votes[i] > votes[maxTop] ? i : maxTop;
        }
        //System.out.println("Max: " + votes[maxTop] + " at index " + maxTop);

        int maxSecond = 0;
        for (int i = 0; i < votes.length; i++) {
            if (i != maxTop) {
                maxSecond = votes[i] > votes[maxSecond] ? i : maxSecond;
            }
        }
        //System.out.println("Second: " + votes[maxSecond] + " at index " + maxSecond);

        for (int i = 0; i < votes.length; i++){
            if (i != maxTop && i != maxSecond){
                votes[i] = 0;
            }
        }
        return votes;

    }

    public GameState simulate(GameState gs, int time, AI ai1, AI ai2) throws Exception {
        boolean gameover = false;

        GameState gs2 = gs.clone();

        //Count the number of simulation issues
        int count = 0;
        do{
            if (gs2.isComplete()) {
                gameover = gs2.cycle();
            } else {
                //Run the simulation of our strategy against a pre-set enemy strategy
                gs2.issue(ai1.getAction(0, gs));
                gs2.issue(ai2.getAction(1, gs));
                count ++;
                //Find their strategy, then simulate as if this strategy
            }
        }while(!gameover && gs2.getTime()<time);

        System.out.println(ai1 + " v " + ai2 + ": " + count);
        return gs2;
    }


    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("MaxActions",long.class,100));
        parameters.add(new ParameterSpecification("playoutAI",AI.class, newAI));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction()));

        return parameters;
    }


    public int getPlayoutLookahead() {
        return MAXSIMULATIONTIME;
    }

    public void setPlayoutLookahead(int a_pola) {
        MAXSIMULATIONTIME = a_pola;
    }

    public EvaluationFunction getEvaluationFunction() {
        return evaluateFunction;
    }


}
