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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public abstract class Bandit<A extends Arm> {
    protected List<A> arms;
    protected Random rnd;
    protected List<Map<A, EpochRewards>> rewards;

    public Bandit() {
        rnd = new Random();
        arms = new ArrayList<>();
        rewards = new ArrayList<>();;
    }

    public abstract A selectArm();

    public void add(A arm) {
        arms.add(arm);
    }

    public A randomArm() {
        return arms.get(rnd.nextInt(arms.size()));
    }

    public abstract EpochRewards update(A arm);

    public void update() {
        Map<A, EpochRewards> map = arms.stream().collect(Collectors.toMap(arm -> arm, arm -> update(arm)));
        rewards.add(map);
    }

    public List<Map<A, EpochRewards>> getEpochs() {
        return rewards;
    }

    public void printTrafficRatio(String fileName) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(fileName)) {

            //Header
            out.print("Epoch");
            arms.forEach((arm) -> out.print("\t" + arm));
            out.println();

            //Body
            for (int epoch = 0; epoch < rewards.size(); epoch++) {
                out.print(epoch);
                for (A arm : arms) {
                    EpochRewards reward = rewards.get(epoch).get(arm);
                    out.print("\t" + (reward.getHits() + reward.getMisses()));
                }
                out.println();
            }
        }
    }
}
