package ai.Ben;

import ai.abstraction.*;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction;
import rts.*;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.util.*;


/**
 *
 * @author Matt Lee, Johnny Hind, Ben Saunders
 */

public class StrategyChooser extends AbstractionLayerAI {

    //
    private int MAXSIMULATIONTIME;
    private int INERTIACYCLES;
    private int GAMECOUNT;
    UnitTypeTable UTT;

    // Initialise all the AI strategies used in the class
    private AI newAI; private AI LightRush; private AI WorkerRush; private AI HeavyRush; private AI RangedRush; private AI MattRush;
    // Initialise the list of AIStrategies played and enemyStrategies simulated against
    private List<AI> AIStrategies = new ArrayList<>();
    private List<AI> enemyStrategies = new ArrayList<>();
    // Initialise the lastStrategy AI variable, which tracks the last used AI strategy for inertia
    private AI lastStrategy = null;
    // Initialise the predictedEnemyStrategy integer, which tracks the enemy strategy that has been predicted for that simulation inertia run
    private int predictedEnemyStrategy;
    // Initialise the actions PlayerActionTableEntry, which tracks the action returned by getAction and evaluateStrategies method
    private PlayerActionTableEntry actions = new PlayerActionTableEntry();


    //TODO - Check why we don't need utt or pf
    //private UnitTypeTable utt = new UnitTypeTable();
    //Initialise the PathFinding algorithm pf that is used by each
    //private PathFinding pf = new BFSPathFinding();

    // Initialise the evaluationFunction which provides an evaluation on a given game state, to be used when scoring a simulating game state
    private EvaluationFunction evaluateFunction = new SimpleSqrtEvaluationFunction();
    //private ComplexEvaluationFunction evaluateFunction = new ComplexEvaluationFunction();

    // Initialise the simulationGameStates List, which tracks all the simulated game states when running an inertia game tick
    private List<GameState> simulationGameStates = new ArrayList<>();
    // Initialise the simulationCounts Integer List, which tracks the depth of search of each simulated game state
    private Integer[] simulationCounts;



    // Strategy Chooser Constructor
    public StrategyChooser(UnitTypeTable utt , PathFinding pf){
        this(100, utt,  pf, 10);
    }

    public StrategyChooser(int timeBudget, UnitTypeTable utt , PathFinding pf, EvaluationFunction evalFunc, int inertiaCycles){
        this(timeBudget, utt,  pf, inertiaCycles);
        this.setEvaluationFunction(evalFunc);
    }

    public StrategyChooser(UnitTypeTable utt){
        this(100,utt,new BFSPathFinding(),10);
    }

    public StrategyChooser( int timeBudget, UnitTypeTable utt, PathFinding a_pf, int inertiaCycles) {
        super(a_pf);
        MAXSIMULATIONTIME = timeBudget;
        newAI = new newAI(utt,a_pf);
        WorkerRush = new WorkerRush2(utt,a_pf);
        LightRush = new LightRush(utt,a_pf);
        HeavyRush = new HeavyRush(utt,a_pf);
        RangedRush = new RangedRush2(utt,a_pf);
        MattRush = new mattRushAi(utt,a_pf);
        INERTIACYCLES = inertiaCycles;
        GAMECOUNT = 0;
        UTT = utt;

        // Initialise the AIStrategies to all the AI strategies we wish to evaluate and simulate with
        AIStrategies.add(MattRush);
        AIStrategies.add(newAI);
        AIStrategies.add(WorkerRush);
        AIStrategies.add(RangedRush);

        //AIStrategies.add(LightRush);

        // Initialise the enemyStrategies to all the enemy strategies we wish to simulate against
        enemyStrategies.add(WorkerRush);
        enemyStrategies.add(LightRush);
        enemyStrategies.add(HeavyRush);
        enemyStrategies.add(RangedRush);

        // Initialise simulationCounts to an Integer List the size of the AIStrategies
        simulationCounts = new Integer[AIStrategies.size()];
    }

    public void reset() {
        //evaluateFunction = new SimpleSqrtEvaluationFunction();
        //evaluateFunction = new ComplexEvaluationFunction();
    }

    public AI clone() {
        return new StrategyChooser(MAXSIMULATIONTIME,UTT, pf, INERTIACYCLES);
    }

    public class PlayerActionTableEntry {
        PlayerAction pa;
    }


    /*
        getAction is the main method of the AI. It is called at each game cycle with the most up to date game state and
        returns which actions the AI wants to execute in this cycle.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        This method returns the actions to be sent to each of the units in the GameState controlled by the player,
        packaged as a PlayerAction.
         */
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        // Called when actions are available to be played in the current game state
        if (gs.canExecuteAnyAction(player)) {

            // Initialise the topStrategy variable, which tracks the last used strategy
            // Finds the best strategy to implement in this game cycle, and performs getAction() to provide action
            AI topStrategy;

            // If GAMECOUNT is at zero, this is the first game cycle
            // Find the best strategy to play on this given map
            if ( GAMECOUNT == 0 ){

                // Call firstRunStrategyAnalysis, which returns the appropriate topStrategy for the first game cycle on this map
                topStrategy = firstRunStrategyAnalysis(player, gs);
                // Logging to keep track of the first game cycle and the strategy used
                System.out.println("First game cycle using: " + topStrategy );

                // Set all the simulationCounts to 0, to start depth counting from zero
                for (int i=0 ; i < simulationCounts.length ; i++ ){
                    simulationCounts[i] = 0;
                }

            // If GAMECOUNT is a multiple of INERTIACYCLES, this is the end of an inertia run
            // Evaluate all simulated game states and choose the top strategy to play with
            } else if ( GAMECOUNT % INERTIACYCLES == 0 ){

                // Logging to say when the Simulation cycle has started
                System.out.println( "\n" + "Simulation --- " );

                // Find the top strategies by simulating all the strategies in 'AIStrategies' list
                topStrategy = findTopStrategy(player, AIStrategies);

                AI ai2 = enemyStrategies.get(predictedEnemyStrategy);

                for (int i=0 ; i<simulationCounts.length ; i++){
                    System.out.println(AIStrategies.get(i) + " v " + ai2 + ": Search depth of " + simulationCounts[i]);
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

                    // Set all the simulationCounts to 0, to start depth counting from zero
                    for (int i=0 ; i < simulationCounts.length ; i++ ){
                        simulationCounts[i] = 0;
                    }

                    startGameStateSimulation(player, gs, AIStrategies, enemyStrategies);
                } else {
                    // Middle of inertia run - so continue simulations
                    continueGameStateSimulation(player , AIStrategies, enemyStrategies);
                }
            }

            actions.pa = topStrategy.getAction(player,gs);
            lastStrategy = topStrategy;

            GAMECOUNT ++;

            return actions.pa;

            //return evaluateStrategies(player, gs, AIStrategies, enemyStrategies);
        } else {
            // If no actions can be played, continue with the current actions as a PlayerAction()
            return new PlayerAction();
        }
    }

    private AI firstRunStrategyAnalysis(int player, GameState gs) {

        AI topStrategy;
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int mapHeight = pgs.getHeight();
        int nbases = 0;

        // Calculate how many bases to use to decide the map size
        for (Unit u : pgs.getUnits()) {
            if (u.getType().name.equals("Base")
                    && u.getPlayer() == player) {
                nbases++;
            }
        }

        if (mapHeight == 8){
            //Map = "maps/NoWhereToRun9x8.xml"
            System.out.println("Map = maps/NoWhereToRun9x8.xml");
            topStrategy = RangedRush;

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


    private AI findTopStrategy(int player, List<AI> AIStrategies) {

        AI topStrategy = null;
        float highscore = Integer.MIN_VALUE;

        for (int i = 0;i < AIStrategies.size();i++){
            AI aiStrategy = AIStrategies.get(i);
            // Find the simulated GameState relating to this strategy
            GameState sGS = simulationGameStates.get(i);

            float score = evaluateFunction.evaluate(player,1-player, sGS);

            System.out.println("Finished considering playing as: " + aiStrategy + " : " + score);

            if (score > highscore || i == 0){
                highscore = score;
                topStrategy = aiStrategy;
            }
        }

        System.out.println("Using: " + topStrategy + " with score of: " + highscore + "\n");
        return topStrategy;
    }


    private void startGameStateSimulation(int player, GameState gs, List<AI> AIStrategies, List<AI> enemyStrategies) throws Exception{

        Integer[] votes = predictEnemyStrategy(player,gs);

        int simulations = 0;
        for (int j = 0; j<4 ; j++){
            if (votes[j] > 0){
                simulations++;
            }
        }

        int timeAllowed = (MAXSIMULATIONTIME-10)/(simulations*AIStrategies.size());


        for (int i = 0;i < AIStrategies.size();i++){
            AI aiStrategy = AIStrategies.get(i);


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

    private void continueGameStateSimulation(int player, List<AI> AIStrategies, List<AI> enemyStrategies ) throws Exception {


        int timeAllowed = (MAXSIMULATIONTIME-10)/3;


        List<GameState> newSimulationGameStates = new ArrayList<>();

        AI enemyStrategy = enemyStrategies.get(predictedEnemyStrategy);

        for (int i = 0; i < AIStrategies.size();i++){
            // Get the part simulated GameState
            GameState tGS = simulationGameStates.get(i);
            AI aiStrategy = AIStrategies.get(i);


            newSimulationGameStates.add(simulate(tGS,tGS.getTime() + timeAllowed, player, aiStrategy, enemyStrategy,i));

        }

        simulationGameStates = newSimulationGameStates;
    }

    private Integer[] predictEnemyStrategy(int player, GameState gs){
        PhysicalGameState pgs = gs.getPhysicalGameState();

        //Calculate how many units types the enemy has, to determine the votes
        int nEnemyWorkers = 0; int nEnemyLight = 0; int nEnemyHeavy = 0; int nEnemyRanged = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) {
                switch (u.getType().name){
                    case "Worker": nEnemyWorkers ++;
                    break;
                    case "Light": nEnemyLight++;
                    break;
                    case "Heavy": nEnemyHeavy++;
                    break;
                    case "Ranged": nEnemyRanged++;
                    break;
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

        simulationCounts[simulationIndex] += count;

        //System.out.println("Simulate count for " + simulationIndex + ": " + strategyCounts[simulationIndex]);

        return gs2;
    }

    public void setInertiaCycles(int INERTIACYCLES) {
        this.INERTIACYCLES = INERTIACYCLES;
    }

    public void setEvaluationFunction(EvaluationFunction evaluateFunction) {
        this.evaluateFunction = evaluateFunction;
    }

    public String toString(){
        return getClass().getSimpleName() + "(" + pf +"," +  evaluateFunction + "," + INERTIACYCLES + ")";
    }

    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        //Inertia Cycles
        parameters.add(new ParameterSpecification("InertiaCycles",int.class,INERTIACYCLES));
        //Time Budget
        parameters.add(new ParameterSpecification("TimeBudget",int.class,MAXSIMULATIONTIME));
        //Evaluation Function
        parameters.add(new ParameterSpecification("EvaluationFunction",EvaluationFunction.class,evaluateFunction));

        return parameters;
    }

}