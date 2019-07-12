/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.ensemble;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.metrics.AbstractRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.ranksys.rec.AbstractRecommender;
import es.uam.eps.ir.ranksys.rec.Recommender;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class Ensemble<U, I> extends AbstractRecommender<U,I>{
    public static Random rnd = new Random();
    Map<String, Recommender<U, I>> recommenders;
    Recommender<U, I> currentRecommender;
    String currentRecommenderName;

    public Ensemble() {
        recommenders = new HashMap<>();
    }

    public void addRecommender(String name, Recommender<U, I> recommender) {
        recommenders.put (name, recommender);
    }

    public String getCurrentRecommenderName() {
        return currentRecommenderName;
    }

    public void setCurrentRecommender(Recommender<U, I> currentRecommender) {
        this.currentRecommender = currentRecommender;
    }

    public void validate(FastPreferenceData<U, I> validationData, double threshold, Function<U, Predicate<I>> userFilter) {
        rndAssignCurrentRecommender();
        
        double maxValue = 0;
        int maxLength = 1;
        BinaryRelevanceModel<U,I> binRel = new BinaryRelevanceModel<>(false, validationData, threshold);
        AbstractRecommendationMetric<U, I> metric = new Precision<>(1, binRel);
        Set<U> targetUsers = validationData.getUsersWithPreferences().collect(Collectors.toSet());
        for (String name : recommenders.keySet()) {
            Recommender<U, I> recommender = recommenders.get(name);
            double value = targetUsers.stream().parallel()
                    .map(user -> recommender.getRecommendation(user, maxLength, userFilter.apply(user)))
                    .mapToDouble(rec -> metric.evaluate(rec))
                    .sum();
            if (value > maxValue) {
                maxValue = value;
                currentRecommenderName = name;
                currentRecommender = recommender;
            }
        }
    }

    @Override
    public Recommendation<U, I> getRecommendation(U u, int maxLength, Predicate<I> filter) {
        return currentRecommender.getRecommendation(u, maxLength, filter);
    }
    
    private void rndAssignCurrentRecommender() {
        int n = rnd.nextInt(recommenders.size());
        currentRecommenderName = recommenders.keySet().toArray(new String[]{})[n];
        currentRecommender = recommenders.get(currentRecommenderName);
    }
}
