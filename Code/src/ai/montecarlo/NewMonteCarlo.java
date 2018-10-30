package ai.montecarlo;


import ai.strategychooser.newAI;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.InterruptibleAI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 *
 * @author santi
 */
public class NewMonteCarlo extends AIWithComputationBudget implements InterruptibleAI {
    public static final int DEBUG = 0;
    EvaluationFunction ef = null;
    //AI newAI = null;


    public class PlayerActionTableEntry {
        PlayerAction pa;
        float accum_evaluation = 0;
        int visit_count = 0;
    }


    PathFinding pf = new BFSPathFinding();
    UnitTypeTable utt = new UnitTypeTable();

    Random r = new Random();
    AI newAI = new newAI(utt, pf);
    long max_actions_so_far = 0;

    PlayerActionGenerator moveGenerator = null;
    boolean allMovesGenerated = false;
    List<PlayerActionTableEntry> actions = null;
    GameState gs_to_start_from = null;
    int run = 0;
    int playerForThisComputation;

    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;

    long MAXACTIONS = 100;
    int MAXSIMULATIONTIME = 1024;


    public NewMonteCarlo(UnitTypeTable utt, PathFinding pf) {
        this(100, -1, 100,
                new newAI(utt, pf),
                new SimpleSqrtEvaluationFunction3());
    }


    public NewMonteCarlo(int available_time, int playouts_per_cycle, int lookahead, AI policy, EvaluationFunction a_ef) {
        super(available_time, playouts_per_cycle);
        MAXACTIONS = -1;
        MAXSIMULATIONTIME = lookahead;
        newAI = policy;
        ef = a_ef;
    }

    public NewMonteCarlo(int available_time, int playouts_per_cycle, int lookahead, long maxactions, AI policy, EvaluationFunction a_ef) {
        super(available_time, playouts_per_cycle);
        MAXACTIONS = maxactions;
        MAXSIMULATIONTIME = lookahead;
        newAI = policy;
        ef = a_ef;
    }


    public void printStats() {
        if (total_cycles_executed>0 && total_actions_issued>0) {
            System.out.println("Average runs per cycle: " + ((double)total_runs)/total_cycles_executed);
            System.out.println("Average runs per action: " + ((double)total_runs)/total_actions_issued);
        }
    }

    public void reset() {
        moveGenerator = null;
        actions = null;
        gs_to_start_from = null;
        run = 0;
    }

    public AI clone() {
        return new NewMonteCarlo(TIME_BUDGET, ITERATIONS_BUDGET, MAXSIMULATIONTIME, MAXACTIONS, newAI, ef);
    }




    //Main method that receives the action
    public final PlayerAction getAction(int player, GameState gs) throws Exception
    {
        if (gs.canExecuteAnyAction(player)) {
            startNewComputation(player,gs.clone());
            computeDuringOneGameFrame();
            return getBestActionSoFar();
        } else {
            return newAI.getAction(player, gs);
        }
    }



    public void startNewComputation(int a_player, GameState gs) throws Exception {
        if (DEBUG>=2) System.out.println("Starting a new search. ..");
        if (DEBUG>=2) System.out.println(gs);
        //System.out.println("Start new computation");
        playerForThisComputation = a_player;
        gs_to_start_from = gs;
        moveGenerator = new PlayerActionGenerator(gs,playerForThisComputation);
        moveGenerator.randomizeOrder();

        allMovesGenerated = false;
        actions = null;
        run = 0;

    }


    public void resetSearch() {
        if (DEBUG>=2) System.out.println("Resetting search...");
        gs_to_start_from = null;
        moveGenerator = null;
        actions = null;
        run = 0;
    }


    public void computeDuringOneGameFrame() throws Exception {
        //moveGenerator holds the actions available
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        int nruns = 0;
        long cutOffTime = (TIME_BUDGET>0 ? System.currentTimeMillis() + TIME_BUDGET:0);
        if (TIME_BUDGET<=0) cutOffTime = 0;

        if (actions==null) {
            actions = new ArrayList<>();
            if (MAXACTIONS>0 && moveGenerator.getSize()>2*MAXACTIONS) {
                for(int i = 0;i<MAXACTIONS;i++) {
                    //pate seems to hold the actions
                    NewMonteCarlo.PlayerActionTableEntry pate = new NewMonteCarlo.PlayerActionTableEntry();
                    pate.pa = moveGenerator.getNextAction(cutOffTime);
                    actions.add(pate);
                }
                max_actions_so_far = Math.max(moveGenerator.getSize(),max_actions_so_far);
                if (DEBUG>=1) System.out.println("MontCarloAI (random action sampling) for player " + playerForThisComputation + " chooses between " + moveGenerator.getSize() + " actions [maximum so far " + max_actions_so_far + "] (cycle " + gs_to_start_from.getTime() + ")");
            } else {
                PlayerAction pa;
                long count = 0;
                do{
                    pa = moveGenerator.getNextAction(cutOffTime);
                    if (pa!=null) {
                        NewMonteCarlo.PlayerActionTableEntry pate = new NewMonteCarlo.PlayerActionTableEntry();
                        pate.pa = pa;
                        actions.add(pate);
                        count++;
                        if (MAXACTIONS>0 && count>=2*MAXACTIONS) break; // this is needed since some times, moveGenerator.size() overflows
                    }
                }while(pa!=null);
                max_actions_so_far = Math.max(actions.size(),max_actions_so_far);
                if (DEBUG>=1) System.out.println("MontCarloAI (complete generation plus random reduction) for player " + playerForThisComputation + " chooses between " + actions.size() + " actions [maximum so far " + max_actions_so_far + "] (cycle " + gs_to_start_from.getTime() + ")");
                while(MAXACTIONS>0 && actions.size()>MAXACTIONS) actions.remove(r.nextInt(actions.size()));
            }
        }

        while(true) {
            if (TIME_BUDGET>0 && (System.currentTimeMillis() - start)>=TIME_BUDGET) break;
            if (ITERATIONS_BUDGET>0 && nruns>=ITERATIONS_BUDGET) break;
            monteCarloRun(playerForThisComputation, gs_to_start_from);
            nruns++;
        }

        total_cycles_executed++;
    }


    public void monteCarloRun(int player, GameState gs) throws Exception {
        //System.out.println("Monte Clrlo Run");
        int idx = run%actions.size();
//        System.out.println(idx);
        PlayerActionTableEntry pate = actions.get(idx);

        GameState gs2 = gs.cloneIssue(pate.pa);
        GameState gs3 = gs2.clone();
        simulate(gs3,gs3.getTime() + MAXSIMULATIONTIME);
        int time = gs3.getTime() - gs2.getTime();

        pate.accum_evaluation += ef.evaluate(player, 1-player, gs3)*Math.pow(0.99,time/10.0);
        pate.visit_count++;
        run++;
        total_runs++;
    }


    public PlayerAction getBestActionSoFar() {
        // find the best:
        PlayerActionTableEntry best = null;
        for(PlayerActionTableEntry pate:actions) {
            if (best==null || (pate.accum_evaluation/pate.visit_count)>(best.accum_evaluation/best.visit_count)) {
                best = pate;
            }
        }
        if (best==null) {
            NewMonteCarlo.PlayerActionTableEntry pate = new NewMonteCarlo.PlayerActionTableEntry();
            pate.pa = moveGenerator.getRandom();
            System.err.println("MonteCarlo.getBestActionSoFar: best action was null!!! action.size() = " + actions.size());
        }

        if (DEBUG>=1) {
            System.out.println("Executed " + run + " runs");
            System.out.println("Selected action: " + best + " visited " + best.visit_count + " with average evaluation " + (best.accum_evaluation/best.visit_count));
        }

        total_actions_issued++;

        return best.pa;
    }


    public void simulate(GameState gs, int time) throws Exception {
        boolean gameover = false;

        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                gs.issue(newAI.getAction(0, gs));
                gs.issue(newAI.getAction(1, gs));
                //Could add in different strategies of the opponent
                //Find their strategy, then simulate as if this strat
            }
        }while(!gameover && gs.getTime()<time);
    }


    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + "," + ITERATIONS_BUDGET + "," +  MAXSIMULATIONTIME + "," + MAXACTIONS + ", " + newAI + ", " + ef + ")";
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
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));

        return parameters;
    }


    public int getPlayoutLookahead() {
        return MAXSIMULATIONTIME;
    }


    public void setPlayoutLookahead(int a_pola) {
        MAXSIMULATIONTIME = a_pola;
    }


    public long getMaxActions() {
        return MAXACTIONS;
    }


    public void setMaxActions(long a_ma) {
        MAXACTIONS = a_ma;
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
