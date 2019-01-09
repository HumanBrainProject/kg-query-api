package org.humanbrainproject.knowledgegraph.indexing.control.spatial.rasterizer;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation.ThreeDTransformation;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;

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

        System.out.println("Rendered version available at file:///tmp/draw.html");

    }

}
