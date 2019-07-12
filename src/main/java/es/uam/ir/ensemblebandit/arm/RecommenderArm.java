/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.arm;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.ranksys.metrics.rel.IdealRelevanceModel.UserIdealRelevanceModel;
import es.uam.eps.ir.ranksys.rec.AbstractRecommender;
import es.uam.eps.ir.ranksys.rec.Recommender;
import java.util.List;
import java.util.function.Predicate;

import org.ranksys.core.util.tuples.Tuple2od;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class RecommenderArm<U, I> extends AbstractRecommender<U, I> implements Arm {
    private Recommender<U, I> recommender;
    private BinaryRelevanceModel<U, I> relevanceModel;
    private int hits = 0;
    private int misses = 0;
    private String name;

    public RecommenderArm(String name) {
        this.name = name;
    }
    
    public void update (Recommender recommender, BinaryRelevanceModel<U, I> relevanceModel) {
        this.recommender = recommender;
        this.relevanceModel = relevanceModel;
    }

    @Override
    public EpochRewards sampleReward() {
        EpochRewards reward = new EpochRewards(hits, misses);
        hits = misses = 0;
        return reward;
    }

    @Override
    public Recommendation getRecommendation(U u, int maxLength, Predicate<I> filter) {

        Recommendation<U,I> rec = recommender.getRecommendation(u, maxLength, filter);
        List<Tuple2od<I>> items = rec.getItems();
        UserIdealRelevanceModel userRelevanceModel = (UserIdealRelevanceModel)relevanceModel.getModel(u);
        items.forEach((ip) -> {
            if (userRelevanceModel.isRelevant(ip.v1)) {
                hits++;
            } else {
                misses++;
            }
        });
        return rec;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
