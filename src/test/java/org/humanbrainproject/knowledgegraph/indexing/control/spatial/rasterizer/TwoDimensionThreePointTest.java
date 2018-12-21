package org.humanbrainproject.knowledgegraph.indexing.control.spatial.rasterizer;

import org.humanbrainproject.knowledgegraph.indexing.control.spatial.transformation.QuickNii;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

public class TwoDimensionThreePointTest {

    @Test
    public void draw() {
        TwoDimensionRasterizer.draw(Arrays.asList(new ThreeDVector(50,20,30),new ThreeDVector(-30,-40,30)));
    }


    @Test
    public void raster(){
        Collection<ThreeDVector> raster = new TwoDimensionRasterizer(new QuickNii(" 116.76450275662296,\n" +
                "           420.38180695602125,\n" +
                "           371.05990195986874,\n" +
                "           32.25523977201931,\n" +
                "           -483.5743708352436,\n" +
                "           -69.49201572740994,\n" +
                "           88.9241921312597,\n" +
                "           51.94987159616494,\n" +
                "           -320.22912581890387")).raster();
    }

    @Test
    public void raster2(){
        Collection<ThreeDVector> raster = new TwoDimensionRasterizer(new QuickNii("123.71850866693461,390.19717500016753,387.61315637828346," +
                "26.843998180123435,-465.5324333473836,-170.6163708811688," +
                "96.32585289892003,124.69908538465688,-324.99608542567577")).raster();



    }


    @Test
    public void raster3(){
        Collection<ThreeDVector> raster = new TwoDimensionRasterizer(new QuickNii("251.56799853050296,493.13337224155964,396.7562789015218,32.998644916653745,-542.7389726501993,17.065573368594528,42.80934797042113,-13.376575187247,-508.1954867544863")).raster();



    }
}