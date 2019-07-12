/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.datagenerator;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import static org.ranksys.formats.parsing.Parsers.lp;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;

/**
 *
 * @author Pablo Castells
 * @author Rocío Cañamares
 *
 */
public class MapNegativeRatingsToZero {
    /**
     * 
     * @param data
     * @param threshold
     * @return
     * @throws IOException 
     */
    public static FastPreferenceData<Long, Long> run(FastPreferenceData<Long, Long> data, double threshold) throws IOException {

        ByteArrayOutputStream newDataOutputStream = new ByteArrayOutputStream();
        PrintStream newData = new PrintStream(newDataOutputStream);

        data.getAllUsers().forEachOrdered(user -> {
            data.getUserPreferences(user).forEachOrdered(up -> newData.println(user + "\t" + up.v1 + "\t" + Math.max(up.v2 - threshold + 1, 0)));
        });

        ByteArrayInputStream newDataInputStream = new ByteArrayInputStream(newDataOutputStream.toByteArray());

        return SimpleFastPreferenceData.load(SimpleRatingPreferencesReader.get().read(newDataInputStream, lp, lp), data, data);
    }

}
