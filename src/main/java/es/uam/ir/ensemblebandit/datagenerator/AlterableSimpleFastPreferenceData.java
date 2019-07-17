/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.datagenerator;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 */
public class AlterableSimpleFastPreferenceData<U, I> extends SimpleFastPreferenceData<U, I>{

    protected AlterableSimpleFastPreferenceData(int numPreferences, List<List<IdxPref>> uidxList, List<List<IdxPref>> iidxList, FastUserIndex<U> uIndex, FastItemIndex<I> iIndex) {
        super(numPreferences, uidxList, iidxList, uIndex, iIndex);
    }

    public static <U, I> SimpleFastPreferenceData<U, I> removeRatings(
            SimpleFastPreferenceData<U, I> old_data,
            Map<U, Map<I, Double>> ratings, FastUserIndex<U> uIndex, FastItemIndex<I> iIndex) throws IOException {
        AtomicInteger numPreferences = new AtomicInteger();

        //CLONING
        List<List<IdxPref>> uidxList = new ArrayList<>();
        for (int uidx = 0; uidx < old_data.numUsers(); uidx++) {
            List<IdxPref> old_uList = old_data.getUidxPreferences(uidx).collect(Collectors.toList());
            if (old_uList != null) {
                old_uList = new ArrayList<>(old_uList);
            }
            uidxList.add(old_uList);
        }

        List<List<IdxPref>> iidxList = new ArrayList<>();
        for (int iidx = 0; iidx < old_data.numItems(); iidx++) {
            List<IdxPref> old_iList = old_data.getIidxPreferences(iidx).collect(Collectors.toList());
            if (old_iList != null) {
                old_iList = new ArrayList<>(old_iList);
            }
            iidxList.add(old_iList);
        }

        //REMOVING
        ratings.forEach((user, items) -> {
            int uidx = old_data.user2uidx(user);
            List<IdxPref> uList = uidxList.get(uidx);
            if (uList == null) return;
            for (I item : items.keySet()) {
                int iidx = old_data.item2iidx(item);
                List<IdxPref> iList = iidxList.get(iidx);
                if (iList == null) return;
                
                //Remove from u
                uList = uList.stream().filter(up -> up.v1 != iidx).collect(Collectors.toList());
                if (uList.isEmpty()) uList = null;
                uidxList.set(uidx, uList);

                //Remove from i
                iList = iList.stream().filter(ip -> ip.v1 != uidx).collect(Collectors.toList());
                if (iList.isEmpty()) iList = null;
                iidxList.set(iidx, iList);

                numPreferences.incrementAndGet();
            }
        });

        return new AlterableSimpleFastPreferenceData<>(numPreferences.intValue(), uidxList, iidxList, uIndex, iIndex);
    }


    public static <U, I> SimpleFastPreferenceData<U, I> addRatings(
            SimpleFastPreferenceData<U, I> old_data,
            Map<U, Map<I, Double>> ratings,FastUserIndex<U> uIndex, FastItemIndex<I> iIndex) throws IOException {
        AtomicInteger numPreferences = new AtomicInteger();

        //CLONING
        List<List<IdxPref>> uidxList = new ArrayList<>();
        for (int uidx = 0; uidx < old_data.numUsers(); uidx++) {
            List<IdxPref> old_uList = old_data.getUidxPreferences(uidx).collect(Collectors.toList());
            if (old_uList != null) {
                old_uList = new ArrayList<>(old_uList);
            }
            uidxList.add(old_uList);
        }

        List<List<IdxPref>> iidxList = new ArrayList<>();
        for (int iidx = 0; iidx < old_data.numItems(); iidx++) {
            List<IdxPref> old_iList = old_data.getIidxPreferences(iidx).collect(Collectors.toList());
            if (old_iList != null) {
                old_iList = new ArrayList<>(old_iList);
            }
            iidxList.add(old_iList);
        }

        //ADDING
        ratings.forEach((user, items) -> {
            int uidx = old_data.user2uidx(user);
            List<IdxPref> uList = uidxList.get(uidx);
            if (uList == null) {
                uList = new ArrayList<>();
                uidxList.set(uidx, uList);
            }
            for (I item : items.keySet()) {
                int iidx = old_data.item2iidx(item);
                double value = items.get(item);
                List<IdxPref> iList = iidxList.get(iidx);
                if (iList == null) {
                    iList = new ArrayList<>();
                    iidxList.set(iidx, iList);
                }

                //Add to u
                uList.add(new IdxPref(iidx, value));

                //Add to i
                iList.add(new IdxPref(uidx, value));

                numPreferences.incrementAndGet();
            }
        });

        return new AlterableSimpleFastPreferenceData<>(numPreferences.intValue(), uidxList, iidxList, uIndex, iIndex);
    }

    
}
