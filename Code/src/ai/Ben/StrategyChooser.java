package ai.Ben;

import ai.abstraction.*;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.abstraction.WorkerRush;
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

    private UnitTypeTable utt = new UnitTypeTable();
    private PathFinding pf = new BFSPathFinding();

    private int MAXSIMULATIONTIME;
    private int INERTIACYCLES;
    private int GAMECOUNT = 0;

    private AI newAI; private AI LightRush; private AI WorkerRush; private AI HeavyRush; private AI RangedRush; private AI MattRush;
    private AI lastStrategy = null;
    private PlayerActionTableEntry actions = new PlayerActionTableEntry();


    private List<AI> strategies = new ArrayList<>();
    private List<AI> enemyStrategies = new ArrayList<>();

    private SimpleSqrtEvaluationFunction evaluateFunction = new SimpleSqrtEvaluationFunction();
    //private ComplexEvaluationFunction evaluateFunction = new ComplexEvaluationFunction();

    private List<GameState> simulationGameStates = new ArrayList<>();
    Integer[] strategyCounts = new Integer[4];
    //TODO - Make the strategyCounts dynamic - bring the strategies intot the constructor

    int predictedEnemyStrategy;

    public StrategyChooser( int lookahead, PathFinding a_pf, AI newai, AI workerRush,
                           AI lightRush , AI heavyRush, AI rangedRush, AI mattRush, int inertiaCycles) {
        super(a_pf);
        MAXSIMULATIONTIME = lookahead;
        newAI = newai;
        WorkerRush = workerRush;
        LightRush = lightRush;
        HeavyRush = heavyRush;
        RangedRush = rangedRush;
        MattRush = mattRush;
        INERTIACYCLES = inertiaCycles;

        strategies.add(newAI);
        strategies.add(WorkerRush);
        strategies.add(MattRush);
        strategies.add(LightRush);
        //strategies.add(HeavyRush);
        //strategies.add(RangedRush);
        //strategies.add(RandomAI);

        //List<AI> enemyStrategies = new ArrayList<>();

        enemyStrategies.add(WorkerRush);
        enemyStrategies.add(LightRush);
        enemyStrategies.add(HeavyRush);
        enemyStrategies.add(RangedRush);


    }


    public void reset() {
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

            //GameState gs2 = gs.clone();


            // evaluateStrategies returns a PlayerAction to be returned by the getAction
            return evaluateStrategies(player, gs, strategies, enemyStrategies);

        } else {
            return new PlayerAction();
        }

    }




    public PlayerAction evaluateStrategies(int player, GameState gs, List<AI> strategies, List<AI> enemyStrategies) throws Exception {

        // Could combine with getAction method??

        AI topStrategy = new WorkerRush(utt,pf);

        // First game run
        if ( GAMECOUNT == 0 ){

            topStrategy = firstRunStrategyAnalysis(player, gs, topStrategy);
            System.out.println("First run using: " + topStrategy );

            // Initialise the strategyCounts to 0
            for (int i=0 ; i<strategyCounts.length ; i++){
                strategyCounts[i] = 0;
            }

        } else if ( GAMECOUNT % INERTIACYCLES == 0 ){
            // Come to end of inertia run
            // Find the top strategies by simulating all the strategies in 'strategies' list
            System.out.println( "\n" + "Simulation --- " );

            topStrategy = findTopStrategy(player, strategies, topStrategy);

            AI ai2 = enemyStrategies.get(predictedEnemyStrategy);

            for (int i=0 ; i<strategyCounts.length ; i++){
                System.out.println(strategies.get(i) + " v " + ai2 + ": Search depth of " + strategyCounts[i]);
            }

            System.out.println("Chosen strategy: " + topStrategy);



        } else {
            // Middle of an inertia run, so keep last strategy
            topStrategy = lastStrategy;

            System.out.println( "Inertia strategy game tick " + (GAMECOUNT % INERTIACYCLES) + " using " + topStrategy.toString() );

            if (GAMECOUNT % INERTIACYCLES == 1 ){
                // First of the inertia run - so create the simulations

                // Initialise the simulationGameStates back to empty
                simulationGameStates = new ArrayList<>();

                // Initialise the strategyCounts to 0
                for (int i=0 ; i<strategyCounts.length ; i++){
                    strategyCounts[i] = 0;
                }

                startGameStateSimulation(player, gs, strategies, enemyStrategies);
            } else {
                // Middle of inertia run - so continue simulations
                continueGameStateSimulation(player , strategies, enemyStrategies);
            }
        }

        actions.pa = topStrategy.getAction(player,gs);
        lastStrategy = topStrategy;

        //System.out.println("Using: " + topStrategy.toString() + " with score of: " + highscore + "\n");

        GAMECOUNT ++;

        return actions.pa;
    }

    public AI firstRunStrategyAnalysis(int player, GameState gs3, AI topStrategy) {

        PhysicalGameState pgs = gs3.getPhysicalGameState();
        int mapHeight = pgs.getHeight();
        int nbases = 0;

        // Calculate how many bases to use to decide the map size
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
            topStrategy = WorkerRush;

        } else if   (mapHeight == 16 && nbases == 2) {
            //Map = "maps/16x16/TwoBasesBarracks16x16.xml"
            System.out.println("Map = maps/16x16/TwoBasesBarracks16x16.xml");
            topStrategy = LightRush;

        } else if (mapHeight == 24){
            //Map = "maps/24x24/basesWorkers24x24H.xml"
            System.out.println("Map = maps/24x24/basesWorkers24x24H.xml");
            topStrategy = MattRush;
        } else {
            topStrategy = WorkerRush;
        }

        return topStrategy;
    }

  
    public AI findTopStrategy(int player, List<AI> strategies, AI topStrategy) {


        float highscore = Integer.MIN_VALUE;

        for (int i = 0;i < strategies.size();i++){
            AI aiStrategy = strategies.get(i);
            // Find the simulated GameState relating to this strategy
            GameState sGS = simulationGameStates.get(i);

            float score = evaluateFunction.evaluate(player,1-player, sGS);

            System.out.println("Finished considering playing as: " + aiStrategy.toString() + " : " + score);

            if (score > highscore){
                highscore = score;
                topStrategy = aiStrategy;
            }
        }

        System.out.println("Using: " + topStrategy.toString() + " with score of: " + highscore + "\n");
        return topStrategy;
    }


    public void startGameStateSimulation(int player, GameState gs, List<AI> strategies, List<AI> enemyStrategies) throws Exception{

        Integer[] votes = predictEnemyStrategy(player,gs);

        int simulations = 0;
        for (int j = 0; j<4 ; j++){
            if (votes[j] > 0){
                simulations++;
            }
        }

        int timeAllowed = (MAXSIMULATIONTIME-10)/(simulations*strategies.size());


        for (int i = 0;i < strategies.size();i++){
            AI aiStrategy = strategies.get(i);


            for (int j = 0 ; j < 4 ; j++){
                if (votes[j] > 0){
                    predictedEnemyStrategy = j;
                    //simulationCounts.add(0);
                    GameState tGS = simulate(gs,gs.getTime() + timeAllowed, player, aiStrategy, enemyStrategies.get(j),i);
                    simulationGameStates.add(tGS);
                }
            }
        }
    }

    public void continueGameStateSimulation(int player, List<AI> strategies, List<AI> enemyStrategies ) throws Exception {


        int timeAllowed = (MAXSIMULATIONTIME-10)/3;


        List<GameState> newSimulationGameStates = new ArrayList<>();

        AI enemyStrategy = enemyStrategies.get(predictedEnemyStrategy);

        for (int i = 0; i < strategies.size();i++){
            // Get the part simulated GameState
            GameState tGS = simulationGameStates.get(i);
            AI aiStrategy = strategies.get(i);


            newSimulationGameStates.add(simulate(tGS,tGS.getTime() + timeAllowed, player, aiStrategy, enemyStrategy,i));

        }

        simulationGameStates = newSimulationGameStates;
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

        //votes[0] is Worker, votes[1] is Light, votes[2] is Heavy, votes[3] is Ranged
        Integer[] votes = new Integer[4];

        votes[0] = 1 + (nEnemyWorkers*4);
        votes[1] = 1 + (nEnemyLight*5);
        votes[2] = (nEnemyHeavy*5) ;
        votes[3] = (nEnemyRanged*5);

        int maxTop = 0;
        for (int i = 0; i < votes.length; i++) {
            maxTop = votes[i] > votes[maxTop] ? i : maxTop;
        }

        /*

        int maxSecond = 0;
        for (int i = 0; i < votes.length; i++) {
            if (i != maxTop) {
                maxSecond = votes[i] > votes[maxSecond] ? i : maxSecond;
            }
        }
        System.out.println("Second: " + votes[maxSecond] + " at index " + maxSecond);

        */

        for (int i = 0; i < votes.length; i++){
            if (i != maxTop ){
                votes[i] = 0;
            }
        }
        return votes;

    }

    public GameState simulate(GameState gs, int time, int player, AI ai1, AI ai2, int simulationIndex) throws Exception {

        boolean gameover = false;

        GameState gs2 = gs.clone();
        //Count the number of simulation issues
        int count = 0;
        do{
            if (gs2.isComplete()) {
                gameover = gs2.cycle();
            } else {

                //Run the simulation of our strategy against a provided enemy strategy

                gs2.issue(ai1.getAction(player, gs2));
                gs2.issue(ai2.getAction(1 - player, gs2));

                count ++;

            }
        } while(!gameover && gs2.getTime() < time);

        strategyCounts[simulationIndex] += count;

        //System.out.println("Simulate count for " + simulationIndex + ": " + strategyCounts[simulationIndex]);

        return gs2;
    }




    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        //parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        //parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        //parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        //parameters.add(new ParameterSpecification("MaxActions",long.class,100));
        //parameters.add(new ParameterSpecification("playoutAI",AI.class, newAI));
        //parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction()));

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