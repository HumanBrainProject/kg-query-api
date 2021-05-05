/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.Tested;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Tested
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    /**
     * re-scales the existing 3d-vector to an axis with the given minimal and maximal extension. This is used to "squeeze" the graph into a common box - regardless of its resolution. This is useful for visualization only.
     */
    public String normalize(int min, int max) {
        int axisWidth = max - min;
        double p = ((double) axisWidth) / 100.0;
        double xPerc = (((double) min) + p * x);
        double yPerc = (((double) min) + p * y);
        double zPerc = (((double) min) + p * z);
        return new ThreeDVector(xPerc, yPerc, zPerc).toString();
    }

    public static List<ThreeDVector> parse(String vectorString) {
        List<ThreeDVector> result = new ArrayList<>();
        String normalized = vectorString.replaceAll("[^0-9,.-]", "");
        String[] split = normalized.split(",");
        int triple = 0;
        while (split.length >= (triple + 1) * 3) {
            int startIndex = triple * 3;
            result.add(new ThreeDVector(
                    Double.parseDouble(split[startIndex]),
                    Double.parseDouble(split[startIndex + 1]),
                    Double.parseDouble(split[startIndex + 2])
            ));
            triple++;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreeDVector that = (ThreeDVector) o;
        return Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.z, z) == 0;
    }

    @Override
    public int hashCode() {

        return Objects.hash(x, y, z);
    }
}
