package ai.Ben;

import ai.RandomBiasedAI;
import ai.abstraction.*;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.abstraction.WorkerRush;
import ai.abstraction.RangedRush;
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
 * @author santi
 */
public class StrategyChooser extends AbstractionLayerAI {

    UnitTypeTable utt = new UnitTypeTable();
    PathFinding pf = new BFSPathFinding();

    long MAXACTIONS;
    int MAXSIMULATIONTIME;


    List<AI> strategies = new ArrayList<>();
    SimpleSqrtEvaluationFunction evaluateFunction = new SimpleSqrtEvaluationFunction();

    public StrategyChooser( int lookahead, PathFinding a_pf,AI newai, AI workerRush,
                           AI lightRush , AI heavyRush, AI rangedRush) {
        super(a_pf);
        MAXACTIONS = -1;
        MAXSIMULATIONTIME = lookahead;
        newAI = newai;
        WorkerRush = workerRush;
        LightRush = lightRush;
        HeavyRush = heavyRush;
        RangedRush = rangedRush;
    }


    public void reset() {
        strategies = new ArrayList<>();
        evaluateFunction = new SimpleSqrtEvaluationFunction();
    }

    public AI clone() {
        return new StrategyChooser(MAXSIMULATIONTIME,pf,
                newAI,WorkerRush,LightRush,HeavyRush,RangedRush);
    }

    public class PlayerActionTableEntry {
        PlayerAction pa;
    }

    AI newAI; AI LightRush; AI WorkerRush; AI HeavyRush; AI RangedRush;

    PlayerActionTableEntry actions = new PlayerActionTableEntry();
    AI lastStrategy = null;


    //Main method that receives the action
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        if (gs.canExecuteAnyAction(player)) {

            GameState gs2 = gs.clone();

            List<AI> strategies = new ArrayList<>();

            strategies.add(newAI);
            strategies.add(WorkerRush);
            strategies.add(LightRush);
            strategies.add(HeavyRush);
            //strategies.add(RangedRush);
            //strategies.add(RandomAI);

            return evaluateStrategies(player, gs2, strategies);

        } else {
            return new PlayerAction();
        }

    }


    public PlayerAction evaluateStrategies(int player, GameState gs2, List<AI> strategies) throws Exception {
        float highscore = Float.NEGATIVE_INFINITY;
        AI topStrategy = null;

        GameState gs3 = gs2.clone();
        Integer[] votes = predictEnemyStrategy(player,gs3);
        //The scores of the current strategy against a particular enemy strategy
        float workerRushScore; float lightRushScore; float heavyRushScore; float rangedRushScore; float randomScore;

        int simulations = 0;
        for (int j = 0; j<4 ; j++){
            if (votes[j] > 0){
                simulations++;
            }
        }
        int totalRuns = (simulations*strategies.size());
        int timeAllowed = MAXSIMULATIONTIME/totalRuns;


        for (int i = 0;i < strategies.size();i++){
            AI aiStrategy = strategies.get(i);

            /// Keep in the for loop

            if (votes[0] > 0){
                System.out.println("Considering WorkerRush");
                workerRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, WorkerRush));
            } else { workerRushScore = 0;}

            if (votes[1] > 0){
                System.out.println("Considering LightRush");
                lightRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, LightRush));
            } else { lightRushScore = 0;}

            if (votes[2] > 0){
                System.out.println("Considering HeavyRush");
                 heavyRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, HeavyRush));
            } else { heavyRushScore = 0;}

            if (votes[3] > 0){
                System.out.println("Considering RangedRush");
                 rangedRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, RangedRush));
            } else { rangedRushScore = 0;}


            float score = (votes[0]*workerRushScore) + (votes[1]*lightRushScore) + (votes[2]*heavyRushScore) + (votes[3]*rangedRushScore);

            //System.out.println(aiStrategy.toString() + " : " + score);

            if (score > highscore){
                highscore = score;
                topStrategy = aiStrategy;
            }
        }
        //System.out.println(topStrategy);

        PlayerAction action = topStrategy.getAction(player,gs2);
        actions.pa = action;

        lastStrategy = topStrategy;
        //System.out.println("Total Runs: " +totalRuns);
        //System.out.println("----------------------" + lastStrategy.toString());

        return actions.pa;
    }

    public Integer[] predictEnemyStrategy(int player, GameState gs3){
        //int workerRushVote = 1, lightRushVote = 1, heavyRushVote = 1, rangedRushVote = 1;
        PhysicalGameState pgs = gs3.getPhysicalGameState();
        //Player p = gs3.getPlayer(player);

        //Calculate how many units types the enemy has, to determine the votes
        int nEnemyWorkers = 0;
        int nEnemyLight = 0;
        int nEnemyHeavy = 0;
        int nEnemyRanged = 0;
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
        votes[0] = 1+ (nEnemyWorkers*5);
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

        int count = 0;
        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                //Run the simulation of our strategy against a pre-set enemy strat
                gs.issue(ai1.getAction(0, gs));
                gs.issue(ai2.getAction(1, gs));
                count ++;
                //Could add in different strategies of the opponent
                //Find their strategy, then simulate as if this strat
            }
        }while(!gameover && gs.getTime()<time);

        //System.out.println(ai1 + " v " + ai2 + ": " + count);
        return gs;
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


    public void setEvaluationFunction(SimpleSqrtEvaluationFunction a_ef) {
        evaluateFunction = a_ef;
    }
}
