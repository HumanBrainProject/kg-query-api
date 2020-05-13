/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.ExampleValues;

@Tested
public class BoundingBox {

    private final ThreeDVector from;
    private final ThreeDVector to;
    private final String referenceSpace;

    public BoundingBox(float xFrom, float yFrom, float zFrom, float xTo, float yTo, float zTo, String referenceSpace){
        this(new ThreeDVector(xFrom, yFrom, zFrom), new ThreeDVector(xTo, yTo, zTo), referenceSpace);
    }

    public static BoundingBox parseBoundingBox(String completeBoundingBox) {
        if(completeBoundingBox==null){
            return null;
        }
        String[] boxSplit = completeBoundingBox.split(":");
        if(boxSplit.length!=2){
            throw new IllegalArgumentException("Invalid bounding box! Please define the reference space and the geometry in the form \""+ ExampleValues.MBB_EXAMPLE+"\"!");
        }
        String referenceSpace = boxSplit[0].trim();
        String boundingBox = boxSplit[1].trim();
        String normalized = boundingBox.replaceAll("[^0-9,.]", "");
        String[] split = normalized.split(",");
        if(split.length!=6){
            throw new IllegalArgumentException("Invalid length of values in bounding box");
        }
        return new BoundingBox(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]), referenceSpace);
    }




    public BoundingBox(ThreeDVector from, ThreeDVector to, String referenceSpace) {
        this.from = from;
        this.to = to;
        this.referenceSpace = referenceSpace;
    }

    public ThreeDVector getFrom() {
        return from;
    }

    public ThreeDVector getTo() {
        return to;
    }

    public String getReferenceSpace() {
        return referenceSpace;
    }
}
