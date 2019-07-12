/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.main;

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
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.fast.basic.RandomRecommender;
import es.uam.eps.ir.ranksys.rec.runner.Filters;
import es.uam.ir.ensemblebandit.datagenerator.AlterableSimpleFastPreferenceData;
import es.uam.ir.ensemblebandit.datagenerator.MapNegativeRatingsToZero;
import es.uam.ir.ensemblebandit.ensemble.Ensemble;
import es.uam.ir.ensemblebandit.ranksys.rec.fast.basic.RelevantPopularity;
import es.uam.ir.ensemblebandit.utils.Split;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
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
public class Figure3 {
    public static String[] recNames = new String[]{
        "Random recommendation", 
        "Most popular", 
        "User-based kNN", 
        "Matrix factorization", 
        "Dynamic ensemble"
    };

    public static void main(String[] args) throws IOException {
        String path = "datasets/ml1m/";
        String userPath = path + "users.txt";
        String itemPath = path + "items.txt";
        String dataPath = path + "data.txt";
        String resultsPath = "figure3.txt";
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
        FastUserIndex<Long> userIndex = SimpleFastUserIndex.load(UsersReader.read(userPath, lp));
        FastItemIndex<Long> itemIndex = SimpleFastItemIndex.load(ItemsReader.read(itemPath, lp));
        FastPreferenceData<Long, Long> data = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(dataPath, lp, lp), userIndex, itemIndex);
        data = MapNegativeRatingsToZero.run(data, firstthreshold);

        // Ratings split
        SimpleFastPreferenceData<Long, Long>[] split = Split.randomUserSplit(data, 0.01);
        SimpleFastPreferenceData<Long, Long> trainData0 = split[0];
        SimpleFastPreferenceData<Long, Long> testData0 = split[1];

        Map<String, SimpleFastPreferenceData<Long, Long>> trainData = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> trainData0));
        Map<String, SimpleFastPreferenceData<Long, Long>> exclussionData = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> trainData0));
        Map<String, SimpleFastPreferenceData<Long, Long>> testData = Stream.of(recNames).collect(Collectors.toMap(recName -> recName, recName -> testData0));

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
                return Figure1and2.getKNN(trainData.get("User-based kNN"), userIndex, itemIndex);
            });

            recMap.put("Matrix factorization", () -> {
                return Figure1and2.getMF(trainData.get("Matrix factorization"), userIndex, itemIndex);
            });

            SimpleFastPreferenceData<Long, Long>[] splitEnsemble = Split.randomSplit(trainData.get("Dynamic ensemble"), 0.8);
            SimpleFastPreferenceData<Long, Long>[] splitExclusionEnsemble = Split.randomSplit(exclussionData.get("Dynamic ensemble"), 0.8);
            recMap.put("Dynamic ensemble", () -> {
                Map<String, Recommender<Long, Long>> map = Figure1and2.getCombinedRecommenders(splitEnsemble[0], userIndex, itemIndex, threshold);
                Function<Long, Predicate<Long>> userFilter = Filters.and(Filters.notInTrain(splitEnsemble[0]), Filters.notInTrain(splitExclusionEnsemble[0]));

                Ensemble ensemble = new Ensemble();
                ensemble.addRecommender("Most popular", map.get("Most popular"));
                ensemble.addRecommender("Matrix factorization", map.get("Matrix factorization"));
                ensemble.addRecommender("User-based kNN", map.get("User-based kNN"));

                ensemble.validate(splitEnsemble[1], threshold, userFilter);
                String name = ensemble.getCurrentRecommenderName();
                ensemble.setCurrentRecommender(Figure1and2.get(name, trainData.get("Dynamic ensemble"), userIndex, itemIndex, threshold));
                return ensemble;
            });

            // Run recommenders
            out.print(epoch);
            for (String recName: recNames) {
                Recommender<Long, Long> recommender = recMap.get(recName).get();
                try {
                    //Recommendation & evaluation
                    Set<Long> targetUsers = testData.get(recName).getUsersWithPreferences().collect(Collectors.toSet());
                    Function<Long, Predicate<Long>> userFilter = Filters.and(Filters.notInTrain(trainData.get(recName)), Filters.notInTrain(exclussionData.get(recName)));
                    int maxLength = 1;

                    ConcurrentMap<Long, Map<Long, Double>> recs = new ConcurrentHashMap<>();
                    ConcurrentMap<Long, Map<Long, Double>> recsTest = new ConcurrentHashMap<>();
                    double prec1 = targetUsers.stream().parallel()
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
                    int testRelevantRatings = targetUsers.stream().mapToInt(user -> binRel.get(recName).getModel(user).getRelevantItems().size()).sum();
                    cumulatedReturns.addTo(recName, prec1);
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
        }
        out.close();
    }
}
