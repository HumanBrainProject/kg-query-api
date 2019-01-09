package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;

import java.util.ArrayList;
import java.util.List;

@ToBeTested(easy = true)
public class ThreeDVector {

    private final double x;
    private final double y;
    private final double z;

    public ThreeDVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("%.16f,%.16f,%.16f", x, y, z);
    }


    public String integers() {
        return String.format("%d,%d,%d", (int)x, (int)y, (int)z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String normalize(int min, int max){
        int axisWidth = max-min;
        double p = 100.0/((double)axisWidth);
        double xPerc = (p*(((double)max)-x));
        double yPerc = (p*(((double)max)-y));
        double zPerc = (p*(((double)max)-z));
        return String.format("%.16f,%.16f,%.16f", xPerc, yPerc, zPerc);
    }

    public static List<ThreeDVector> parse(String vectorString){
        List<ThreeDVector> result = new ArrayList<>();
        String normalized = vectorString.replaceAll("[^0-9,.-]", "");
        String[] split = normalized.split(",");
        int triple = 0;
        while(split.length>=(triple+1)*3){
            int startIndex = triple*3;
            result.add(new ThreeDVector(
                    Double.parseDouble(split[startIndex]),
                    Double.parseDouble(split[startIndex+1]),
                    Double.parseDouble(split[startIndex+2])
            ));
            triple++;
        }
        return result;
    }

    public double[] getAsArray(){
        return new double[]{x,y,z};
    }


}
