/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.ranksys.rec.fast.basic;

import es.uam.eps.ir.ranksys.fast.FastRecommendation;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.fast.AbstractFastRecommender;
import static java.util.Comparator.comparingDouble;
import java.util.List;
import java.util.function.IntPredicate;
import static java.util.stream.Collectors.toList;
import static org.ranksys.core.util.tuples.Tuples.tuple;
import org.ranksys.core.util.tuples.Tuple2id;
import static java.lang.Math.min;

/**
 * Popularity-based recommender. Non-personalized recommender that returns the
 * items with more relevant ratings, according to the preference data provided.
 *
 * @author Pablo Castells
 * @author Rocío Cañamares
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public class RelevantPopularity<U, I> extends AbstractFastRecommender<U, I> {

    private final List<Tuple2id> popList;

    /**
     * Constructor.
     *
     * @param data preference data
     * @param threshold ratings with a value larger than or equal to this
     * threshold are considered relevant
     */
    public RelevantPopularity(FastPreferenceData<U, I> data, double threshold) {
        super(data, data);

        popList = data.getIidxWithPreferences()
                .mapToObj(iidx -> tuple(iidx, data.getIidxPreferences(iidx).filter(ip -> ip.v2 >= threshold).count()))
                .sorted(comparingDouble(Tuple2id::v2).reversed())
                .collect(toList());
    }

    @Override
    public FastRecommendation getRecommendation(int uidx, int maxLength, IntPredicate filter) {

        List<Tuple2id> items = popList.stream()
                .filter(is -> filter.test(is.v1))
                .limit(min(maxLength, popList.size()))
                .collect(toList());

        return new FastRecommendation(uidx, items);
    }
}
