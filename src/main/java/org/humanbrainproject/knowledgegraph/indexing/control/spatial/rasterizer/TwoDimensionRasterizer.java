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

package org.humanbrainproject.knowledgegraph.indexing.control.spatial.rasterizer;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation.ThreeDTransformation;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple rasterizer for two dimensional elements based on a {@link ThreeDTransformation} logic.
 */
@ToBeTested(easy = true)
public class TwoDimensionRasterizer {

    private final ThreeDTransformation transformation;

    private static Logger logger = LoggerFactory.getLogger(TwoDimensionRasterizer.class);

    private int width = 100;
    private int height = 100;

    public TwoDimensionRasterizer(ThreeDTransformation transformation) {
        this.transformation = transformation;

    }

    public Collection<ThreeDVector> raster() {
        List<ThreeDVector> result = new ArrayList<>();
        for(int row = 0; row<height; row++){
            for(int col =0; col<width; col++){
                double y = (double)row/(double)height;
                double x = (double)col/(double)width;
                result.add(transformation.getPoint(x, y));
            }
        }
        return result;
    }

    /**
     * debug-only html export of a rendered display of the generated dots.
     */
    public static void draw(Collection<ThreeDVector> dots) {
        double maxX = dots.stream().max(Comparator.comparingDouble(ThreeDVector::getX)).get().getX();
        double maxY = dots.stream().max(Comparator.comparingDouble(ThreeDVector::getY)).get().getY();
        double maxZ = dots.stream().max(Comparator.comparingDouble(ThreeDVector::getZ)).get().getZ();

        double max = Math.ceil(Math.max(Math.max(maxX, maxY), maxZ));

        String dotString = String.join(",", dots.stream().map(d -> "["+d.normalize((int)(-max/2.0), (int)Math.ceil(max/2.0))+"]").collect(Collectors.toSet()));
        StringBuilder sb = new StringBuilder();
        sb.append(" <!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/three.js/r72/three.min.js\"></script>\n" +
                "  <script src=\"https://threejs.org/examples/js/controls/TrackballControls.js\"></script>" +
                "  <meta charset=\"utf-8\">\n" +
                "  <title>JS Bin</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div id=\"cont\"></div>\n" +
                "<script>var scene = new THREE.Scene(); \n");
        sb.append(
                "var camera = new THREE.PerspectiveCamera(75, window.innerWidth/window.innerHeight, 0.1, 1000); \n" );
        sb.append(
                "var renderer = new THREE.WebGLRenderer({ alpha: true }); \n" +
                "\n" +
                "renderer.setSize(window.innerWidth, window.innerHeight); \n" +
                "renderer.setClearColor(0xffffff, 0); // bg color\n" +
                "document.body.appendChild(renderer.domElement); // displays canvas\n" +
                "\n" +
                "camera.position.z = 150; // move away to see coord center\n" +
                "camera.position.y = 0;\n" +
                "\n" +
                "controls = new THREE.TrackballControls(camera, renderer.domElement);\n" +
                "\n" +
                "// CUBE\n");
        sb.append("var geometry = new THREE.CubeGeometry(100,100,100);\n");
        sb.append(
                "var cubeMaterials = [ \n" +
                "    new THREE.MeshBasicMaterial({color:0xff0000, transparent:true, opacity:0.2, side: THREE.DoubleSide}),\n" +
                "    new THREE.MeshBasicMaterial({color:0x00ff00, transparent:true, opacity:0.2, side: THREE.DoubleSide}), \n" +
                "    new THREE.MeshBasicMaterial({color:0x0000ff, transparent:true, opacity:0.2, side: THREE.DoubleSide}),\n" +
                "    new THREE.MeshBasicMaterial({color:0xffff00, transparent:true, opacity:0.2, side: THREE.DoubleSide}), \n" +
                "    new THREE.MeshBasicMaterial({color:0xff00ff, transparent:true, opacity:0.2, side: THREE.DoubleSide}), \n" +
                "    new THREE.MeshBasicMaterial({color:0x00ffff, transparent:true, opacity:0.2, side: THREE.DoubleSide}), \n" +
                "]; \n" +
                "// Create a MeshFaceMaterial, which allows the cube to have different materials on each face \n" +
                "var cubeMaterial = new THREE.MeshFaceMaterial(cubeMaterials); \n" +
                "var cube = new THREE.Mesh(geometry, cubeMaterial);\n" +
                "scene.add( cube );\n" +
                "\n" +
                "\n" +
                "\n" +
                "\tvar geom = new THREE.Geometry();\n");

        sb.append("var dots = [");
        sb.append(dotString);
        sb.append("];\n");
        sb.append("for(d of dots){" +
                "var dotGeometry = new THREE.Geometry();\n" +
                "dotGeometry.vertices.push(new THREE.Vector3( d[0], d[1], d[2]));\n" +
                "var dotMaterial = new THREE.PointsMaterial( { size: 5, sizeAttenuation: false, color:0xff0000 } );\n" +
                "var dot = new THREE.Points( dotGeometry, dotMaterial );\n" +
                "scene.add( dot );" +
                "}");

        sb.append("\n" +
                "\n" +
                "var render = function () { \n" +
                "    requestAnimationFrame(render); \n" +
                "    controls.update();\n" +
                "    renderer.render(scene, camera); \n" +
                "};\n" +
                "\n" +
                "render();</script>" +
                "</body>\n" +
                "</html> ");
        try(FileWriter fileWriter = new FileWriter("/tmp/draw.html")){
            fileWriter.write(sb.toString());
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }

        logger.info("Rendered version available at file:///tmp/draw.html");

    }

}
