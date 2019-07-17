/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.bandit;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.rec.AbstractRecommender;
import es.uam.ir.ensemblebandit.arm.RecommenderArm;
import es.uam.ir.ensemblebandit.arm.EpochRewards;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class RecommenderBandit<U,I> extends AbstractRecommender<U, I> {
    Bandit<RecommenderArm<U,I>> bandit;

    public RecommenderBandit(Bandit<RecommenderArm<U,I>> bandit) {
        this.bandit = bandit;
    }
    
    public void update () {
        bandit.update();
    }
    
    public void addArm (RecommenderArm<U,I> arm) {
        bandit.add(arm);
    }
    
    public List<Map<RecommenderArm<U,I>, EpochRewards>> getEpochs() {
        return bandit.getEpochs();
    }

    @Override
    public Recommendation<U, I> getRecommendation(U u, int maxLength, Predicate<I> filter) {
        RecommenderArm<U,I> arm = bandit.selectArm();
        return arm.getRecommendation(u, maxLength, filter);
    }
}
