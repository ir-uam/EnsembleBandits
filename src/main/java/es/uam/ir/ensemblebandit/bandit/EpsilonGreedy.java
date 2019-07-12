/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.bandit;

import es.uam.ir.ensemblebandit.arm.Arm;
import es.uam.ir.ensemblebandit.arm.EpochRewards;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class EpsilonGreedy<A extends Arm> extends Bandit<A> {
    double epsilon;
    Map<A, Double> means;
    Map<A, Integer> num;

    public EpsilonGreedy(double epsilon) {
        this.epsilon = epsilon;
        means = new HashMap<>();
        num = new HashMap<>();
    }

    @Override
    public void add(A arm) {
        super.add(arm);
        means.put(arm, 0.0);
        num.put(arm, 0);
    }

    @Override
    public A selectArm() {
        A bestArm = randomArm();
        if (rnd.nextDouble() < epsilon) {
            return bestArm;
        } else {
            double max = 0;
            for (A arm : arms) {
                if (means.get(arm) > max) {
                    max = means.get(arm);
                    bestArm = arm;
                }
            }
            return bestArm;
        }
    }

    @Override
    public EpochRewards update(A arm) {
        EpochRewards rewards = arm.sampleReward();
        int n = num.get(arm);
        num.put(arm,num.get(arm) + rewards.getHits() + rewards.getMisses());
        means.put(arm, (means.get(arm) * n + rewards.getHits()) / num.get(arm));
        return rewards;
    }
}
