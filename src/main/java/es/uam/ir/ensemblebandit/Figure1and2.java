/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit;

import es.uam.eps.ir.ranksys.core.Recommendation;
import static org.ranksys.formats.parsing.Parsers.lp;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.AbstractRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.ranksys.mf.Factorization;
import es.uam.eps.ir.ranksys.mf.als.HKVFactorizer;
import es.uam.eps.ir.ranksys.mf.rec.MFRecommender;
import es.uam.eps.ir.ranksys.nn.user.UserNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.TopKUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import es.uam.eps.ir.ranksys.nn.user.sim.VectorCosineUserSimilarity;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.fast.basic.RandomRecommender;
import es.uam.eps.ir.ranksys.rec.runner.Filters;
import es.uam.ir.ensemblebandit.arm.RecommenderArm;
import es.uam.ir.ensemblebandit.bandit.Bandit;
import es.uam.ir.ensemblebandit.bandit.EpsilonGreedy;
import es.uam.ir.ensemblebandit.bandit.RecommenderBandit;
import es.uam.ir.ensemblebandit.bandit.ThompsonSampling;
import es.uam.ir.ensemblebandit.datagenerator.AlterableSimpleFastPreferenceData;
import es.uam.ir.ensemblebandit.datagenerator.MapNegativeRatingsToZero;
import es.uam.ir.ensemblebandit.ensemble.Ensemble;
import es.uam.ir.ensemblebandit.filler.Filler;
import es.uam.ir.ensemblebandit.filler.RecommenderFill;
import es.uam.ir.ensemblebandit.ranksys.rec.fast.basic.RelevantPopularity;
import es.uam.ir.ensemblebandit.utils.GettUsersAndItems;
import es.uam.ir.ensemblebandit.utils.Split;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ranksys.formats.index.ItemsReader;
import org.ranksys.formats.index.UsersReader;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class Figure1and2 {
    public static String[] recNames = new String[]{
        "Random recommendation", 
        "Most popular", 
        "User-based kNN", 
        "Matrix factorization", 
        "Thompson sampling ensemble", 
        "Epsilon-greedy ensemble", 
        "Dynamic ensemble"
    };

    public static void main(String[] args) throws IOException {

        String dataPath = args[0];
        String resultsPath = "figure1.txt";
        double threshold = 1;
        int firstthreshold = 4;
        int nIter = 200;

        LogManager.getLogManager().reset();
        PrintStream out = new PrintStream(resultsPath);
        out.print("Epoch");
        for (String recName: recNames) {
            out.print("\t" + recName);
        }
        out.println();

        // Read files
        ByteArrayInputStream[] usersAndItemsInputStreams = GettUsersAndItems.run(dataPath);
        FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(usersAndItemsInputStreams[0], lp));
        FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(usersAndItemsInputStreams[1], lp));
        FastPreferenceData<Long, Long> data = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(dataPath, lp, lp), userIndex, itemIndex);
        data = MapNegativeRatingsToZero.run(data, firstthreshold);

        // Retings split
        SimpleFastPreferenceData<Long, Long>[] split = Split.randomUserSplit(data, 0.05);
        SimpleFastPreferenceData<Long, Long> trainData0 = split[0];
        SimpleFastPreferenceData<Long, Long> testData0 = split[1];

        Map<String, SimpleFastPreferenceData<Long, Long>> trainData = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> trainData0));
        Map<String, SimpleFastPreferenceData<Long, Long>> exclussionData = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> trainData0));
        Map<String, SimpleFastPreferenceData<Long, Long>> testData = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> testData0));
        long testRelevantRatings = testData0.getAllUsers().mapToLong(user -> testData0.getUserPreferences(user).filter(up -> up.v2 >= threshold).count()).sum();

        Bandit<RecommenderArm<Long, Long>> thompsonSamplingBandit = new ThompsonSampling<>(1000, 1);
        RecommenderArm<Long, Long> thompsonSamplingPopArm = new RecommenderArm<>("Most popular");
        RecommenderArm<Long, Long> thompsonSamplingMfArm = new RecommenderArm<>("Matrix factorization");
        RecommenderArm<Long, Long> thompsonSamplingKNNArm = new RecommenderArm<>("User-based kNN");
        thompsonSamplingBandit.add(thompsonSamplingPopArm);
        thompsonSamplingBandit.add(thompsonSamplingMfArm);
        thompsonSamplingBandit.add(thompsonSamplingKNNArm);
        RecommenderBandit thompsonSamplingRecommenderBandit = new RecommenderBandit<>(thompsonSamplingBandit);

        Bandit<RecommenderArm<Long, Long>> epsilonGreedyBandit = new EpsilonGreedy<>(0.1);
        RecommenderArm<Long, Long> epsilonGreedyPopArm = new RecommenderArm<>("Most popular");
        RecommenderArm<Long, Long> epsilonGreedyMfArm = new RecommenderArm<>("Matrix factorization");
        RecommenderArm<Long, Long> epsilonGreedyKNNArm = new RecommenderArm<>("User-based kNN");
        epsilonGreedyBandit.add(epsilonGreedyPopArm);
        epsilonGreedyBandit.add(epsilonGreedyMfArm);
        epsilonGreedyBandit.add(epsilonGreedyKNNArm);
        RecommenderBandit epsilonGreedyRecommenderBandit = new RecommenderBandit<>(epsilonGreedyBandit);

        Object2DoubleOpenHashMap<String> cumulatedReturns = new Object2DoubleOpenHashMap<>();
        cumulatedReturns.defaultReturnValue(0);
        // Epoch loop
        for (int epoch = 0; epoch <= nIter; epoch++) {
            System.out.println("Running epoch " + epoch);

            // Metrics
            Map<String, BinaryRelevanceModel<Long, Long>> binRel = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> new BinaryRelevanceModel<>(false, testData.get(recName), threshold)));
            Map<String, AbstractRecommendationMetric<Long, Long>> prec = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> new Precision<>(1, binRel.get(recName))));

            // Recommenders
            Map<String, Supplier<Recommender<Long, Long>>> recMap = new HashMap<>();

            recMap.put("Random recommendation", () -> new RandomRecommender<>(userIndex, itemIndex));
            recMap.put("Most popular", () -> new RelevantPopularity<>(trainData.get("Most popular"), threshold));

            recMap.put("User-based kNN", () -> {
                return getKNN(trainData.get("User-based kNN"), userIndex, itemIndex);
            });

            recMap.put("Matrix factorization", () -> {
                return getMF(trainData.get("Matrix factorization"), userIndex, itemIndex);
            });

            recMap.put("Thompson sampling ensemble", () -> {
                Map<String, Recommender<Long, Long>> map = getCombinedRecommenders("Thompson sampling ensemble", trainData, userIndex, itemIndex, threshold);

                thompsonSamplingPopArm.update(map.get("Most popular"), binRel.get("Thompson sampling ensemble"));
                thompsonSamplingMfArm.update(map.get("Matrix factorization"), binRel.get("Thompson sampling ensemble"));
                thompsonSamplingKNNArm.update(map.get("User-based kNN"), binRel.get("Thompson sampling ensemble"));

                return thompsonSamplingRecommenderBandit;
            });

            recMap.put("Epsilon-greedy ensemble", () -> {
                Map<String, Recommender<Long, Long>> map = getCombinedRecommenders("Epsilon-greedy ensemble", trainData, userIndex, itemIndex, threshold);

                epsilonGreedyPopArm.update(map.get("Most popular"), binRel.get("Epsilon-greedy ensemble"));
                epsilonGreedyMfArm.update(map.get("Matrix factorization"), binRel.get("Epsilon-greedy ensemble"));
                epsilonGreedyKNNArm.update(map.get("User-based kNN"), binRel.get("Epsilon-greedy ensemble"));

                return epsilonGreedyRecommenderBandit;
            });

            SimpleFastPreferenceData<Long, Long>[] splitEnsemble = Split.randomSplit(trainData.get("Dynamic ensemble"), 0.8);
            SimpleFastPreferenceData<Long, Long>[] splitExclusionEnsemble = Split.randomSplit(exclussionData.get("Dynamic ensemble"), 0.8);
            recMap.put("Dynamic ensemble", () -> {
                Map<String, Recommender<Long, Long>> map = getCombinedRecommenders(splitEnsemble[0], userIndex, itemIndex, threshold);
                Function<Long, Predicate<Long>> userFilter = Filters.and(Filters.notInTrain(splitEnsemble[0]), Filters.notInTrain(splitExclusionEnsemble[0]));

                Ensemble ensemble = new Ensemble();
                ensemble.addRecommender("Most popular", map.get("Most popular"));
                ensemble.addRecommender("Matrix factorization", map.get("Matrix factorization"));
                ensemble.addRecommender("User-based kNN", map.get("User-based kNN"));

                ensemble.validate(splitEnsemble[1], threshold, userFilter);
                String name = ensemble.getCurrentRecommenderName();
                ensemble.setCurrentRecommender(get(name, trainData.get("Dynamic ensemble"), userIndex, itemIndex, threshold));
                return ensemble;
            });

            // Run recommenders
            out.print(epoch);
            for (String recName: recNames) {
                Recommender<Long, Long> recommender = recMap.get(recName).get();
                try {
                    // Recommendation & evaluation
                    Set<Long> targetUsers = testData.get(recName).getUsersWithPreferences().collect(Collectors.toSet());
                    Function<Long, Predicate<Long>> userFilter = Filters.and(Filters.notInTrain(trainData.get(recName)), Filters.notInTrain(exclussionData.get(recName)));
                    int maxLength = 1;

                    ConcurrentMap<Long, Map<Long, Double>> recs = new ConcurrentHashMap<>();
                    ConcurrentMap<Long, Map<Long, Double>> recsTest = new ConcurrentHashMap<>();
                    double rewards = targetUsers.stream().parallel()
                            .map(user -> {
                                ConcurrentHashMap<Long, Double> rec = new ConcurrentHashMap<>();
                                recs.put(user, rec);

                                ConcurrentHashMap<Long, Double> recTest = new ConcurrentHashMap<>();
                                recsTest.put(user, recTest);

                                Map<Long, Double> prefTest = testData.get(recName).getUserPreferences(user).collect(Collectors.toMap(up -> up.v1, up -> up.v2));

                                Recommendation<Long, Long> recommendation = recommender.getRecommendation(user, maxLength, userFilter.apply(user));
                                recommendation.getItems().stream().parallel().forEach(ir -> {
                                    if (prefTest.containsKey(ir.v1)) {
                                        recTest.put(ir.v1, prefTest.get(ir.v1));
                                    } else {
                                        rec.put(ir.v1, 0.0);
                                    }
                                });

                                return recommendation;
                            })
                            .mapToDouble(rec -> prec.get(recName).evaluate(rec))
                            .sum();

                    // Print metric value
                    cumulatedReturns.addTo(recName, rewards);
                    out.print("\t" + cumulatedReturns.get(recName)/testRelevantRatings);

                    // Add ratings to train, remove from test
                    trainData.put(recName, AlterableSimpleFastPreferenceData.addRatings(trainData.get(recName), recsTest, userIndex, itemIndex));
                    exclussionData.put(recName, AlterableSimpleFastPreferenceData.addRatings(exclussionData.get(recName), recs, userIndex, itemIndex));
                    testData.put(recName, AlterableSimpleFastPreferenceData.removeRatings(testData.get(recName), recsTest, userIndex, itemIndex));
                } catch (IOException ex) {
                    Logger.getLogger(Figure1and2.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            out.println();

            thompsonSamplingRecommenderBandit.update();
            epsilonGreedyRecommenderBandit.update();
        }
        
        // Print traffic ratio curve of bandit
        epsilonGreedyBandit.printTrafficRatio("figure2-epsilon-greedy.txt");
        thompsonSamplingBandit.printTrafficRatio("figure2-thompson-sampling.txt");
        
        out.close();
    }

    public static Map<String, Recommender<Long, Long>> getCombinedRecommenders(
            String recName,
            Map<String, SimpleFastPreferenceData<Long, Long>> trainData,
            FastUserIndex<Long> userIndex,
            FastItemIndex<Long> itemIndex,
            double threshold) {
        return getCombinedRecommenders(trainData.get(recName), userIndex, itemIndex, threshold);
    }

    public static Map<String, Recommender<Long, Long>> getCombinedRecommenders(
            SimpleFastPreferenceData<Long, Long> trainData,
            FastUserIndex<Long> userIndex,
            FastItemIndex<Long> itemIndex,
            double threshold) {
        Recommender<Long, Long> rpop = new RelevantPopularity<>(trainData, (int) threshold);
        Recommender<Long, Long> mf = getMF(trainData, userIndex, itemIndex);
        Recommender<Long, Long> recUB = getKNN(trainData, userIndex, itemIndex);

        Map<String, Recommender<Long, Long>> recMap = new HashMap<>();
        recMap.put("Most popular", rpop);
        recMap.put("Matrix factorization", mf);
        recMap.put("User-based kNN", recUB);

        return recMap;
    }

    public static Recommender<Long, Long> getMF(SimpleFastPreferenceData<Long, Long> trainData,
            FastUserIndex<Long> userIndex,
            FastItemIndex<Long> itemIndex) {

        Filler<Long, Long> filler = new Filler<>(Filler.Mode.RND, itemIndex, userIndex, trainData);
        int numIter = 20;
        double lambda = 0.1;
        double alpha = 1;
        int mfk = 20;
        Factorization<Long, Long> factorization = new HKVFactorizer<Long, Long>(lambda, (double x) -> 1 + alpha * x, numIter).factorize(mfk, trainData);
        Recommender<Long, Long> mf = new MFRecommender<>(userIndex, itemIndex, factorization);
        Recommender<Long, Long> mffill = new RecommenderFill<>(mf, filler);
        
        return mffill;
    }

    public static Recommender<Long, Long> getKNN(SimpleFastPreferenceData<Long, Long> trainData,
            FastUserIndex<Long> userIndex,
            FastItemIndex<Long> itemIndex) {
        int k = userIndex.numUsers();
        Filler<Long, Long> filler = new Filler<>(Filler.Mode.RND, itemIndex, userIndex, trainData);
        UserSimilarity<Long> sim = new VectorCosineUserSimilarity<>(trainData, 0.5, true);
        UserNeighborhood<Long> neighborhoodd = new TopKUserNeighborhood<>(sim, k);
        Recommender<Long, Long> recUB = new UserNeighborhoodRecommender<>(trainData, neighborhoodd, 1);
        Recommender<Long, Long> recUBfill = new RecommenderFill<>(recUB, filler);
        return recUBfill;
    }

    public static Recommender<Long, Long> get(String name,
            SimpleFastPreferenceData<Long, Long> trainData,
            FastUserIndex<Long> userIndex,
            FastItemIndex<Long> itemIndex,
            double threshold) {
        switch (name) {
            case "Matrix factorization":
                return getMF(trainData, userIndex, itemIndex);
            case "User-based kNN":
                return getKNN(trainData, userIndex, itemIndex);
            case "Most popular":
            default:
                return new RelevantPopularity<>(trainData, (int) threshold);
        }
    }

}
