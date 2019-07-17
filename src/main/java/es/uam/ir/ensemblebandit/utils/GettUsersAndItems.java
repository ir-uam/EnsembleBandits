/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 */
public class GettUsersAndItems {
    public static ByteArrayInputStream[] run (String dataPath) throws IOException {
        Set<String> users = new HashSet<>();
        Set<String> items = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataPath)))) {
            reader.lines().forEach(l -> {
                String[] tokens = l.split("\t");
                users.add(tokens[0]);
                items.add(tokens[1]);
            });
        }

        ByteArrayOutputStream userOutputStream = new ByteArrayOutputStream();
        try (PrintStream userData = new PrintStream(userOutputStream)) {
            for (String u : users) {
                userData.println(u);
            }
        }

        ByteArrayOutputStream itemOutputStream = new ByteArrayOutputStream();
        try (PrintStream itemData = new PrintStream(itemOutputStream)) {
            for (String i : items) {
                itemData.println(i);
            }
        }
        
        return new ByteArrayInputStream[] {new ByteArrayInputStream(userOutputStream.toByteArray()), new ByteArrayInputStream(itemOutputStream.toByteArray())};
    }
}
