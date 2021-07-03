package com.progressoft.tools;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class NormalizerImpl implements Normalizer {

    @Override
    public ScoringSummary zscore(Path csvPath, Path destPath, String colToStandardize) {

        return calcZScore(csvPath, destPath, colToStandardize);
    }

    private ScoringSummary calcZScore(Path csvPath, Path destPath, String colToStandardize) {
        List<String> lines = new ArrayList<>();
        List<BigDecimal> targetList = new ArrayList<>();

        readFile(csvPath, lines);

        int columnIndex = getColumnIndex(lines, colToStandardize);
        fillTargetList(lines, targetList, columnIndex);

        // get tha max and min value in arrayList
        BigDecimal maxNum = Collections.max(targetList);
        BigDecimal minNum = Collections.min(targetList);

        BigDecimal calcMean = calculateMean(targetList);

        BigDecimal variance = calculateVariance(targetList, calcMean);

        BigDecimal SD = calculateSD(variance);


        List<BigDecimal> zScoreResult = new ArrayList<>();
        BigDecimal zEachValue;
        BigDecimal xSubMean;
        for (BigDecimal x : targetList) {
            xSubMean = x.subtract(calcMean);
            zEachValue = xSubMean.divide(SD, 2, RoundingMode.HALF_EVEN);

            zEachValue = zEachValue.setScale(2, RoundingMode.HALF_EVEN);
            zScoreResult.add(zEachValue);
        }

        BigDecimal median = calculateMedian(targetList);

        writeOnFile(destPath, colToStandardize, lines, columnIndex, zScoreResult, "_z");


        return new ScoringSummaryImpl(calcMean, SD, variance, median, minNum.setScale(2, RoundingMode.HALF_EVEN), maxNum.setScale(2, RoundingMode.HALF_EVEN));
    }

    private void fillTargetList(List<String> lines, List<BigDecimal> targetList, int columnIndex) {
        lines.stream().skip(1).forEach(l -> {
            String[] splitLine = l.split(",");
            String colValue = splitLine[columnIndex];
            BigDecimal bigValue = new BigDecimal(colValue);
            targetList.add(bigValue);
        });
    }


    @Override
    public ScoringSummary minMaxScaling(Path csvPath, Path destPath, String colToNormalize) {

        return calculateMinMax(csvPath, destPath, colToNormalize);
    }

    private ScoringSummary calculateMinMax(Path csvPath, Path destPath, String colToNormalize) {
        List<String> lines = new ArrayList<>();
        ArrayList<BigDecimal> targetList = new ArrayList<>();

        readFile(csvPath, lines);

        int columnIndex = getColumnIndex(lines, colToNormalize);
        fillTargetList(lines, targetList, columnIndex);

        // get tha max and min value in arrayList
        BigDecimal maxNum = Collections.max(targetList);
        BigDecimal minNum = Collections.min(targetList);


        // calculate minMax for each value and add it to List
        List<BigDecimal> minMaxList = new ArrayList<>();
        for (BigDecimal x : targetList) {
            BigDecimal xSub = x.subtract(minNum);
            BigDecimal mSubM = maxNum.subtract(minNum);

            BigDecimal mm = xSub.divide(mSubM, 2, RoundingMode.HALF_EVEN);
            minMaxList.add(mm);
        }


        BigDecimal calcMean = calculateMean(targetList);

        BigDecimal variance = calculateVariance(targetList, calcMean);

        BigDecimal SD = calculateSD(variance);

        BigDecimal median = calculateMedian(targetList);

        writeOnFile(destPath, colToNormalize, lines, columnIndex, minMaxList, "_mm");

        return new ScoringSummaryImpl(calcMean, SD, variance, median, minNum.setScale(2, RoundingMode.HALF_EVEN), maxNum.setScale(2, RoundingMode.HALF_EVEN));


    }

    private void readFile(Path csvPath, List<String> lines) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(String.valueOf(csvPath)));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("source file not found");
        }
    }

    private void writeOnFile(Path destPath, String colToNormalize, List<String> lines, int columnIndex, List<BigDecimal> normalizationMethodList, String suffix) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(String.valueOf(destPath)));
            StringBuilder sb = new StringBuilder();

            buildRow(columnIndex, sb, lines.get(0), colToNormalize + suffix);
            int count1 = 0;

            for (String x : lines.subList(1, lines.size())) {
                buildRow(columnIndex, sb, x, String.valueOf(normalizationMethodList.get(count1)));
                count1++;
            }
            bw.write(String.valueOf(sb));
            bw.close();

        } catch (IOException e) {
            throw new IllegalArgumentException("the destination is not a file");
        }
    }

    private BigDecimal calculateMean(List<BigDecimal> targetList) {
        BigDecimal calcMean;
        BigDecimal sumTargetLis = new BigDecimal(0);
        for (BigDecimal x : targetList) {
            sumTargetLis = sumTargetLis.add(x);
        }
        calcMean = sumTargetLis.divide(BigDecimal.valueOf(targetList.size()), RoundingMode.HALF_EVEN).setScale(2, RoundingMode.HALF_EVEN);

        return calcMean;
    }


    private BigDecimal calculateSD(BigDecimal variance) {

        BigDecimal SD = BigDecimal.valueOf(Math.sqrt((variance.intValue())));
        SD = SD.setScale(2, RoundingMode.HALF_EVEN);

        return SD;
    }


    private BigDecimal calculateVariance(List<BigDecimal> targetList, BigDecimal calcMean) {
        BigDecimal variance;
        BigDecimal vari;
        BigDecimal sumVari = new BigDecimal(0);

        for (BigDecimal x : targetList) {

            vari = (x.subtract(calcMean).multiply(x.subtract(calcMean)));
            sumVari = sumVari.add(vari);
        }
        variance = sumVari.divide(BigDecimal.valueOf(targetList.size()), 0, RoundingMode.HALF_EVEN);
        variance = variance.setScale(2, RoundingMode.HALF_EVEN);

        return variance;
    }

    private void buildRow(int columnIndex, StringBuilder sb, String oldHeaders, String mewHeader) {
        String[] arrayOfLines = oldHeaders.split(",");
        List<String> columns = Arrays.stream(arrayOfLines).collect(Collectors.toList());
        columns.add(columnIndex + 1, mewHeader);
        columns.forEach(c -> sb.append(c).append(","));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("\n");
    }

    private BigDecimal calculateMedian(List<BigDecimal> targetList) {
        BigDecimal median;
        targetList.sort(Comparator.naturalOrder());
        median = targetList.get((targetList.size() + 1) / 2 - 1).setScale(2, RoundingMode.HALF_EVEN);

        return median;
    }

    private int getColumnIndex(List<String> lines, String colToNormalize) {
        String head = lines.get(0); //headers
        String[] splitHead = head.split(",");
        int count = 0;
        for (String hh : splitHead) {

            if (hh.equals(colToNormalize)) {
                break;
            }
            count++;
        }
        if (count == splitHead.length || count > splitHead.length)
            throw new IllegalArgumentException("column " + colToNormalize + " not found");

        return count;
    }
}