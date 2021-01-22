import javafx.util.Pair;
import lpsolve.LpSolveException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Estrid on 5/12/2017.
 */
class Recommender {
    private Path dir;
    private Map<String,LinkedList<Sequence>> userSequences;
    private Map<String, LinkedList<Sequence>> allUserSequences;
    private OrienteeringEnvironment environment;
    private String[] stats;
    private int maxIter;
    private int numDim;
    private double lr;
    private double regWeight;
    private double contextWeight;

    Recommender(Path dir, Map<String, LinkedList<Sequence>> userSequences, Map<String, LinkedList<Sequence>> allUserSequences, OrienteeringEnvironment environment, Properties props) {
        this.dir = dir;
        this.userSequences = userSequences;
        this.allUserSequences = allUserSequences;
        this.environment = environment;
        this.stats = props.getProperty("metrics").split(":");
        this.maxIter = Integer.parseInt(props.getProperty("numEpoch"));
        this.numDim = Integer.parseInt(props.getProperty("numDim"));
        this.lr = Double.parseDouble(props.getProperty("lr"));
        this.regWeight = Double.parseDouble(props.getProperty("regWeight"));
        this.contextWeight = Double.parseDouble(props.getProperty("contextWeight"));

    }

    void recommend() throws IOException, LpSolveException {
        int count = 0;
        Result result = new Result();
        int totalSequences = 0;
        for(Map.Entry<String, LinkedList<Sequence>> list:userSequences.entrySet()){
            totalSequences += list.getValue().size();

        }

        for (Map.Entry<String, LinkedList<Sequence>> entry : userSequences.entrySet()) {
            String testUser = entry.getKey();
            for (Sequence testSequence : entry.getValue()) {
                count++;
                Visit[] testVisits = testSequence.visits;
                System.out.println("Progress: " + count + "/" + totalSequences);

                System.out.println("=======Sequence Test========");
                System.out.println("Test sequence: No." + testSequence.number + ". " + testSequence.toString(environment));

                double timeBudget = Math.ceil(this.environment.getTimeCost(testSequence));

                System.out.println("Time budget: " + timeBudget);
                environment.setConstraints(testVisits, timeBudget);

                Pair<Sequence, Double> solutionAndRuntime = computeSolution(testSequence, testUser);
                Sequence solution = solutionAndRuntime.getKey();
                Double runtime = solutionAndRuntime.getValue();

                if (solution != null) {
                    Map<String, Float> resultStats = solution.getResultStats(testSequence, stats);
                    resultStats.put("runtime", runtime.floatValue());
                    result.result.add(resultStats);
                }

            }
        }

        /*
         * Print result
         */

        System.out.println("=================Result=================");
        System.out.println("Total tested sequences: " + count);
        System.out.println(result);
    }

    private Pair<Sequence,Double> computeSolution(Sequence testSequence, String testUser) throws LpSolveException, IOException {
        Sequence solution =new Sequence();
        Double time = 0.0;
        Pair<Sequence, Double> result = new Pair<>(solution, time);
        result = recommend(allUserSequences, environment, testSequence, testUser, dir);

        if(result.getKey()!=null) {
            System.out.println(" solution: " + result.getKey().toString());
        }
        return result;
    }

    private Pair<Sequence, Double> recommend(Map<String, LinkedList<Sequence>> allUserSequences, OrienteeringEnvironment environment, Sequence testSequence, String testUser, Path dir) throws IOException, LpSolveException{

        Pair<Boolean, double[]> intEstimator = estimate(allUserSequences, environment, testSequence, testUser, dir);

        if (!intEstimator.getKey()) {
            return new Pair<>(null,0.);
        }
        double[] userInt = intEstimator.getValue();
        environment.setInterestScore(userInt);

        double time = System.currentTimeMillis();
        Sequence solution = solveContextualILP(environment);

        time = System.currentTimeMillis() - time;
        return new Pair<>(solution,time);
    }

    private Pair<Boolean, double[]> estimate(Map<String, LinkedList<Sequence>> allUserSequences, OrienteeringEnvironment environment, Sequence testSequence, String testUser, Path dir) throws IOException{
        boolean hasHistory = true;
        double[] userInt = new EmbeddingLearner(dir, testSequence.number, this.maxIter, allUserSequences.size(), environment.POIGraph.length, this.numDim,  this.lr, this.regWeight, this.contextWeight).predictWithContextual(testUser, environment);
        if(powerOfVector(userInt) == 0.0){
            hasHistory = false;
        }
        return new Pair<>(hasHistory, userInt);
    }

    private double powerOfVector(double[] userInt) {
        double pow = 0.0;
        for (double v : userInt) {
            pow = pow + v * v;
        }
        return pow;
    }

    private Sequence solveContextualILP(OrienteeringEnvironment environment) throws LpSolveException {
        long timeout = 60*3;
        ContextualILPSolver solver = new ContextualILPSolver(environment.numberOfPOIs, environment, timeout);
        return solver.solve();
    }

    private class Result{
        LinkedList<Map<String, Float>> result;
        Result() {
            this.result = new LinkedList<>();
        }

        double getAvg(String stat){
            double precision = 0;
            for(Map<String, Float> mappedStats: result){
                precision = precision + mappedStats.get(stat);
            }
            return precision/result.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("accuracy:");
            sb.append("\n");
            for (String stat : stats) {
                sb.append(stat);
                sb.append(": ");
                sb.append(getAvg(stat));
                sb.append("\n");
            }
            sb.append("runtime: ");
            sb.append(getAvg("runtime"));
            sb.append("\n");
            return sb.toString();
        }
    }
}
