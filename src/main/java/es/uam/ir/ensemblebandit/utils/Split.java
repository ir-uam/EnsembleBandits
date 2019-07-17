/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.utils;

import static org.ranksys.formats.parsing.Parsers.*;
import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class Split {
    static Random rnd = new Random();

    public static SimpleFastPreferenceData<Long, Long>[] randomSplit(FastPreferenceData<Long, Long> data, double rho) throws IOException {
        ByteArrayOutputStream trainOutputStream = new ByteArrayOutputStream();
        PrintStream trainPrint = new PrintStream(trainOutputStream);
        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        PrintStream testPrint = new PrintStream(testOutputStream);

        data.getUsersWithPreferences().forEachOrdered(user -> {
            data.getUserPreferences(user).forEachOrdered(up -> {
                double p = rnd.nextDouble();
                if (p < rho) {
                    trainPrint.println(user + "\t" + up.v1 + "\t" + up.v2);
                } else {
                    testPrint.println(user + "\t" + up.v1 + "\t" + up.v2);
                }
            });
        });

        trainPrint.close();
        testPrint.close();

        ByteArrayInputStream trainStream = new ByteArrayInputStream(trainOutputStream.toByteArray());
        SimpleFastPreferenceData trainData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(trainStream, lp, lp), data, data);
        
        ByteArrayInputStream testStream = new ByteArrayInputStream(testOutputStream.toByteArray());
        SimpleFastPreferenceData testData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(testStream, lp, lp), data, data);

        return new SimpleFastPreferenceData[]{trainData, testData};
    }

    public static SimpleFastPreferenceData<Long, Long>[] randomUserSplit(FastPreferenceData<Long, Long> data, double rho) throws IOException {
        ByteArrayOutputStream trainOutputStream = new ByteArrayOutputStream();
        PrintStream trainPrint = new PrintStream(trainOutputStream);
        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        PrintStream testPrint = new PrintStream(testOutputStream);

        data.getUsersWithPreferences().forEachOrdered(user -> {
            double p = rnd.nextDouble();
            data.getUserPreferences(user).forEachOrdered(up -> {
                if (p < rho) {
                    trainPrint.println(user + "\t" + up.v1 + "\t" + up.v2);
                } else {
                    testPrint.println(user + "\t" + up.v1 + "\t" + up.v2);
                }
            });
        });

        trainPrint.close();
        testPrint.close();

        ByteArrayInputStream trainStream = new ByteArrayInputStream(trainOutputStream.toByteArray());
        SimpleFastPreferenceData trainData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(trainStream, lp, lp), data, data);

        ByteArrayInputStream testStream = new ByteArrayInputStream(testOutputStream.toByteArray());
        SimpleFastPreferenceData testData = SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(testStream, lp, lp), data, data);

        return new SimpleFastPreferenceData[]{trainData, testData};
    }
}
