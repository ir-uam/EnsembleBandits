/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.arm;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class EpochRewards {
    private final int hits;
    private final int misses;

    public EpochRewards(int hits, int misses) {
        this.hits = hits;
        this.misses = misses;
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }
}
