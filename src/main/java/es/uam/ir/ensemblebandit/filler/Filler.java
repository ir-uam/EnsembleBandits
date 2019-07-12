/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.filler;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.fast.basic.RandomRecommender;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class Filler<U, I> {
    public enum Mode {
        RND, POP, ID, NONE
    };
    private final Mode mode;
    private List<Integer> planB;
    private final FastPreferenceData<U, I> data;
    private double threshold;
    private RandomRecommender<U, I> randomRecommender;

    public Filler(Mode mode, FastItemIndex<I> iIndex, FastUserIndex<U> uIndex, FastPreferenceData<U, I> data, int length, double threshold) {
        this(mode, iIndex, uIndex, data, threshold, 0, user -> item -> true);
    }

    public Filler(Mode mode, FastItemIndex<I> iIndex, FastUserIndex<U> uIndex, FastPreferenceData<U, I> data) {
        this(mode, iIndex, uIndex, data, 1, 0, user -> item -> true);
    }

    public Filler(Mode mode, FastItemIndex<I> iIndex, FastUserIndex<U> uIndex, FastPreferenceData<U, I> data, double threshold, int length, Function<U, IntPredicate> filter) {
        this.data = data;
        this.mode = mode;
        this.threshold = threshold;
        this.randomRecommender = new RandomRecommender<>(uIndex, iIndex);

        if (mode.equals(Mode.NONE)) {
            this.planB = new ArrayList<>();
            return;
        }

        if (mode.equals(Mode.RND)) {
            return;
        }

        this.planB = iIndex.getAllIidx()
                .boxed()
                .sorted((iidx1, iidx2) -> {
                    int cmp = Double.compare(weight(iidx2), weight(iidx1));
                    if (cmp == 0) {
                        return Integer.compare(iidx2, iidx1);
                    } else {
                        return cmp;
                    }
                })
                .collect(Collectors.toList());
    }

    private double weight(int iidx) {
        switch (mode) {
            case POP:
                return data.getIidxPreferences(iidx).filter(ip -> ip.v2 >= threshold).count();
            case ID:
            default:
                return iidx;
        }
    }

    private double weight(I item) {
        switch (mode) {
            case POP:
                return data.numUsers(item);
            case ID:
            default:
                return data.item2iidx(item);
        }
    }

    public List<Tuple2id> fill(List<Tuple2id> items, int length, IntPredicate filter, U user) {
        List<Tuple2id> newItems = new ArrayList<>(items);

        if (newItems.size() < length) {
            Set<Integer> iidxSet = new HashSet<>();
            items.forEach(item -> iidxSet.add(item.v1));
            
            if (this.mode.equals(Mode.RND)) {
                planB = randomRecommender.getRecommendation(data.user2uidx(user), length-newItems.size(), item -> !iidxSet.contains(item) && filter.test(item))
                        .getIidxs()
                        .parallelStream()
                        .map(id -> id.v1)
                        .collect(Collectors.toList());
            }
            
            if (length == 0) {
                length = planB.size();
            }

            List<Integer> allItems = planB.stream()
                    .filter(iidx -> !iidxSet.contains(iidx) && filter.test(iidx))
                    .collect(Collectors.toList());

            for (int i = 0; i < allItems.size() && newItems.size() < length; i++) {
                int iidx = allItems.get(i);
                newItems.add(new Tuple2id(iidx, weight(iidx)));
            }
        }
        return newItems;
    }

    public List<Tuple2od<I>> fill(List<Tuple2od<I>> items, int length, Predicate<I> filter, U user) {
        List<Tuple2od<I>> newItems = new ArrayList<>(items);
        Set<I> itemSet = new HashSet<>();
        items.forEach(item -> itemSet.add(item.v1));

        if (length == 0) {
            length = planB.size();
        }

        if (this.mode.equals(Mode.RND)) {
            planB = randomRecommender.getRecommendation(user, length-newItems.size(), item -> !itemSet.contains(item) && filter.test(item))
                        .getItems()
                        .parallelStream()
                        .map(id -> data.item2iidx(id.v1))
                        .collect(Collectors.toList());
        }

        if (newItems.size() < length) {
            List<I> allItems = planB.stream()
                    .map(iidx -> data.iidx2item(iidx))
                    .filter(item -> !itemSet.contains(item) && filter.test(item))
                    .collect(Collectors.toList());

            for (int i = 0; i < allItems.size() && newItems.size() < length; i++) {
                I item = allItems.get(i);
                newItems.add(new Tuple2od(item, weight(item)));
            }
        }
        return newItems;
    }
}
