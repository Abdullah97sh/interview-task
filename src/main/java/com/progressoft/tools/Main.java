package com.progressoft.tools;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Normalizer nor = new NormalizerImpl();

        Path p1 = Paths.get(args[0]);
        Path p2 = Paths.get(args[1]);

        if (args[3].equals("min-max")) {
            nor.minMaxScaling(p1, p2, args[2]);
        }

        if (args[3].equals("z-score")) {
            nor.zscore(p1, p2, args[2]);
        }

    }
}
