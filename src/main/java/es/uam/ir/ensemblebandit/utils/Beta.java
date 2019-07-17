/*
* Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
* de Madrid, http://ir.ii.uam.es.
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/
package es.uam.ir.ensemblebandit.utils;

import java.util.Random;

/**
 *
 * @author Rocío Cañamares
 * @author Pablo Castells
 *
 */
public class Beta {
    int alpha, beta;

    public Beta(int alpha, int beta) {
        this.alpha = alpha;
        this.beta = beta;
    }
    
    public void hit() {
        alpha++;
    }

    public void miss() {
        beta++;
    }
    
    public void hit(int n) {
        alpha += n;
    }

    public void miss(int n) {
        beta += n;
    }
    
    public void updateAlpha (int n) {
        alpha = n;
    }

    public void updateBeta (int n) {
        beta = n;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getBeta() {
        return beta;
    }
    
    public double sample(Random rnd) {
        double a = gammaSample(rnd, alpha);
        return a / (a + gammaSample(rnd, beta));
    }
    
    public double gammaSample(Random rnd, int alpha) {
        if (alpha <= 0) return 0; 
        else if (alpha == 1) 
            return -Math.log(rnd.nextDouble());
        else if (alpha < 1) { 
            double c = 1.0 / alpha;
            double d = 1.0 / (1 - alpha);
            while (true) {
                double x = Math.pow(rnd.nextDouble(), c);
                double y = x + Math.pow(rnd.nextDouble(), d);
                if (y <= 1) return -Math.log(rnd.nextDouble()) * x/y;
            }
        } else {
            double b = alpha - 1;
            double c = 3 * alpha - 0.75;
            while (true) {
                double u = rnd.nextDouble();
                double v = rnd.nextDouble();
                double w = u * (1 - u);
                double y = Math.sqrt(c/w) * (u - 0.5);
                double x = b + y;
                if (x >= 0) {
                    double z = 64 * w * w * w * v * v;
                    if ((z <= (1 - 2 * y * y/x))
                            || (Math.log(z) <= 2 * (b * Math.log(x/b) - y)))
                        return x;
                }
            }
        }
    }
}
