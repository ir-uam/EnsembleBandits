/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.bandit;

import es.uam.ir.ensemblebandit.arm.Arm;
import es.uam.ir.ensemblebandit.arm.EpochRewards;
import es.uam.ir.ensemblebandit.utils.Beta;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class ThompsonSampling<A extends Arm> extends Bandit<A> {
    Map<A, Beta> beta;
    int alpha0;
    int beta0;

    public ThompsonSampling(int alpha0) {
        this(alpha0, 0);
    }

    public ThompsonSampling(int alpha0, int beta0) {
        this.alpha0 = alpha0;
        this.beta0 = beta0;
        beta = new HashMap<>();
    }

    @Override
    public void add(A arm) {
        super.add(arm);
        beta.put(arm, new Beta(alpha0, beta0));
    }

    @Override
    public A selectArm() {
        A bestArm = randomArm();
        double max = 0;
        for (A arm : arms) {
            double mean = beta.get(arm).sample(rnd);
            if (mean > max) {
                max = mean;
                bestArm = arm;
            }
        }
        return bestArm;
    }

    @Override
    public EpochRewards update(A arm) {
        EpochRewards reward = arm.sampleReward();
        beta.get(arm).hit(reward.getHits());
        beta.get(arm).miss(reward.getMisses());
        return reward;
    }
}
