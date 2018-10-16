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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;




/**
 *
 * @author santi
 */
public class StrategyChooser extends AbstractionLayerAI {
    public static final int DEBUG = 0;
    EvaluationFunction ef = new SimpleSqrtEvaluationFunction();;
    UnitTypeTable utt = new UnitTypeTable();
    PathFinding pf = new BFSPathFinding();



    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;

    long MAXACTIONS;
    int MAXSIMULATIONTIME;


    //New definitions

    List<AI> strategies = new ArrayList<>();
    SimpleSqrtEvaluationFunction evaluateFunction = new SimpleSqrtEvaluationFunction();




    public StrategyChooser( int lookahead,
                           PathFinding a_pf,AI newai, AI workerRush,
                           AI lightRush , AI heavyRush, AI rangedRush) {
        super(a_pf);
        MAXACTIONS = -1;
        MAXSIMULATIONTIME = lookahead;
        //ef = a_ef;
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
        float accum_evaluation = 0;
        int visit_count = 0;
    }

    //List<PlayerActionTableEntry> actions = null;

    //AI aiToUse = null;
    AI newAI = new newAI(utt, pf);
    AI LightRush = new LightRush(utt,pf);
    AI WorkerRush = new WorkerRush(utt,pf);
    AI HeavyRush = new HeavyRush(utt, pf);
    AI RangedRush = new RangedRush(utt,pf);
    AI RandomAI = new RandomBiasedAI();



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

            //AI LightRush2 = new LightRush(utt,pf);
            //return newAI2.getAction(player,gs);

        } else {
            return new PlayerAction();
        }

    }


    public PlayerAction evaluateStrategies(int player, GameState gs2, List<AI> strategies) throws Exception {
        float highscore = Float.NEGATIVE_INFINITY;
        AI topStrategy = null;

        for (int i = 0;i < strategies.size();i++){
            AI aiStrategy = strategies.get(i);
            GameState gs3 = gs2.clone();

            //TODO - Bring this out of the for loop - As its the same
            int[] votes = predictEnemyStrategy(player,gs3);
            //TODO - Can remove total vote as it doesn't need to be weighted
            int totalVote = votes[0] + votes[1] + votes[2] + votes[3] + votes[4];
            //The scores of the current strategy against a particular enemy strategy
            float workerRushScore; float lightRushScore; float heavyRushScore; float rangedRushScore; float randomScore;

            int simulations = 0;
            for (int j = 0; j<5 ; j++){
                if (votes[j] > 0){
                    simulations++;
                }
            }
            int timeAllowed = MAXSIMULATIONTIME/(simulations*strategies.size());

            /// Keep in the for loop

            if (votes[0] > 0){
                workerRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, WorkerRush));
            } else { workerRushScore = 0;}

            if (votes[1] > 0){
                lightRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, LightRush));
            } else { lightRushScore = 0;}

            if (votes[2] > 0){
                 heavyRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, HeavyRush));
            } else { heavyRushScore = 0;}

            if (votes[3] > 0){
                 rangedRushScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, RangedRush));
            } else { rangedRushScore = 0;}

            if (votes[4] > 0){
                 randomScore = evaluateFunction.evaluate(player,1-player, simulate(gs3,gs3.getTime() + timeAllowed, aiStrategy, RandomAI));
            } else { randomScore = 0;}

            float score = ((votes[0]*workerRushScore) + (votes[1]*lightRushScore) + (votes[2]*heavyRushScore) + (votes[3]*rangedRushScore) + votes[4]*randomScore)/totalVote;

            //System.out.println(aiStrategy.toString() + " : " + score);

            if (score > highscore){
                highscore = score;
                topStrategy = aiStrategy;
                //TODO - Bring out that find action out of the for loop
                PlayerAction action = topStrategy.getAction(player,gs2);
                actions.pa = action;
            }
        }
        //System.out.println(topStrategy);


        lastStrategy = topStrategy;
        System.out.println("----------------------" + lastStrategy.toString());

        return actions.pa;
    }

    public int[] predictEnemyStrategy(int player, GameState gs3){
        //int workerRushVote = 1, lightRushVote = 1, heavyRushVote = 1, rangedRushVote = 1;
        PhysicalGameState pgs = gs3.getPhysicalGameState();
        //Player p = gs3.getPlayer(player);

        //Calculate how many units types the enemy has, to determine the votes
        int nEnemyWorkers = 0;
        int nEnemyLight = 0;
        int nEnemyHeavy = 0;
        int nEnemyRanged = 0;
        for (Unit u : pgs.getUnits()) {

            //System.out.println(u.getType().name);

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

        int votes[] = new int[5];

        //System.out.println("Workers: " + nEnemyWorkers + " Light: " + nEnemyLight + " Heavy: " + nEnemyHeavy + " Ranged: " + nEnemyRanged);

        //TODO - Just have the votes to be units times by a certain weighting
        //TODO - non-linear
        //TODO - Default to WorkerRush

        //Just have votes

        if (nEnemyWorkers > 4){
            //System.out.println("Enemy is WorkerRush");
            //workerRushVote
            votes[0] = 10;
        }
        if (nEnemyLight > 1){
            //System.out.println("Enemy is LightRush");
            //lightRushVote
            votes[1] = 10;
            //Amount of Light units weighted by certain score
        }
        if (nEnemyHeavy > 1){
            //System.out.println("Enemy is HeavyRush");
            //heavyRushVote
            votes[2] = 10;
        }
        if (nEnemyRanged > 1){
            //System.out.println("Enemy is RangedRush");
            //rangedRushVote
            votes[3] = 10;
        }
        //RandomScore always needs to be initialised at a low value, to allow something to be analysed
        votes[4] = 1;

        /*
        System.out.println(votes[0]);
        System.out.println(votes[1]);
        System.out.println(votes[2]);
        System.out.println(votes[3]);
        System.out.println(votes[4]);
        */

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

        System.out.println(ai1 + " v " + ai2 + ": " + count);
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





    public AI getplayoutAI() {
        return newAI;
    }


    public void setplayoutAI(AI a_dp) {
        newAI = a_dp;
    }


    public EvaluationFunction getEvaluationFunction() {
        return ef;
    }


    public void setEvaluationFunction(EvaluationFunction a_ef) {
        ef = a_ef;
    }
}
