/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.filler;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.rec.AbstractRecommender;
import es.uam.eps.ir.ranksys.rec.Recommender;
import java.util.function.Predicate;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class RecommenderFill<U,I> extends AbstractRecommender<U, I>  {
    private final Recommender<U, I> recommender;
    private final Filler<U,I> filler;

    public RecommenderFill(Recommender<U, I> recommender, Filler<U, I> filler) {
        this.recommender = recommender;
        this.filler = filler;
    }

    @Override
    public Recommendation<U, I> getRecommendation(U u, int maxLength, Predicate<I> filter) {
        Recommendation<U,I> recommendation = recommender.getRecommendation(u, maxLength, filter);
        
        return new Recommendation<>(u, filler.fill(recommendation.getItems(), maxLength, filter, u));
    }
    
}
