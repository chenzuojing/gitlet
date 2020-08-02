package bearmaps.server.handler.impl;

import bearmaps.AugmentedStreetMapGraph;
import bearmaps.server.handler.APIRouteHandler;
import spark.Request;
import spark.Response;
import bearmaps.utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bearmaps.utils.Constants.*;

/**
 * Handles requests from the web browser for map images. These images
 * will be rastered into one large image to be displayed to the user.
 * @author rahul, Josh Hug, _________
 */
public class RasterAPIHandler extends APIRouteHandler<Map<String, Double>, Map<String, Object>> {

    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside RasterAPIHandler.processRequest(). <br>
     * ullat : upper left corner latitude, <br> ullon : upper left corner longitude, <br>
     * lrlat : lower right corner latitude,<br> lrlon : lower right corner longitude <br>
     * w : user viewport window width in pixels,<br> h : user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
            "lrlon", "w", "h"};

    /**
     * The result of rastering must be a map containing all of the
     * fields listed in the comments for RasterAPIHandler.processRequest.
     **/
    private static final String[] REQUIRED_RASTER_RESULT_PARAMS = {"render_grid", "raster_ul_lon",
            "raster_ul_lat", "raster_lr_lon", "raster_lr_lat", "depth", "query_success"};


    @Override
    protected Map<String, Double> parseRequestParams(Request request) {
        return getRequestParams(request, REQUIRED_RASTER_REQUEST_PARAMS);
    }

    double ldpp(double lonDist, double width) {
        return lonDist / width;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param requestParams Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @param response : Not used by this function. You may ignore.
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image;
     *                    can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */

    @Override
    public Map<String, Object> processRequest(Map<String, Double> requestParams, Response response) {
        System.out.println(requestParams);
        Map<String, Object> results = new HashMap<>();
        double queryBoxUpperLeftLon = requestParams.get("ullon");
        double queryBoxUpperLeftLat = requestParams.get("ullat");
        double queryBoxLowerRightLon = requestParams.get("lrlon");
        double queryBoxLowerRightLat = requestParams.get("lrlat");
        double queryBoxWidth = requestParams.get("w");
        double queryBoxHeight = requestParams.get("h");

        if (queryBoxLowerRightLon < ROOT_ULLON
                || queryBoxLowerRightLat > ROOT_ULLAT
                || queryBoxUpperLeftLon > ROOT_LRLON
                || queryBoxUpperLeftLat < ROOT_LRLAT
                || queryBoxLowerRightLon < queryBoxUpperLeftLon
                || queryBoxUpperLeftLat < queryBoxLowerRightLat) {
            return queryFail();
        }

        double requestLdpp = ldpp(queryBoxLowerRightLon - queryBoxUpperLeftLon,
                queryBoxWidth);
        System.out.println(requestLdpp);

        int rasterDepth = 7;
        double lonDist = ROOT_LRLON - ROOT_ULLON;
        for (int i = 0; i < 8; i++) {
            if (ldpp(lonDist, TILE_SIZE) <= requestLdpp) {
                rasterDepth = i;
                break;
            }
            System.out.println("too big: " + ldpp(lonDist, TILE_SIZE));
            lonDist = lonDist / 2;
        }
        int tileSpan = (int) Math.pow(2, rasterDepth);
        double tileLon = (ROOT_LRLON - ROOT_ULLON) / tileSpan;
        double tileLat = (ROOT_ULLAT - ROOT_LRLAT) / tileSpan;
        System.out.println("just right: " + ldpp(lonDist, TILE_SIZE) + " depth: " + rasterDepth);
        System.out.println("full map lon: " + (ROOT_LRLON - ROOT_ULLON) + "full map lat: " + (ROOT_ULLAT - ROOT_LRLAT));
        System.out.println("tileLon: " + tileLon + " tileLat: " + tileLat);

        int tileXIndexStart = 0;
        double rasterUpperLeftLon = ROOT_ULLON;
        int tileYIndexStart = 0;
        double rasterUpperLeftLat = ROOT_ULLAT;
        int tileXIndexEnd = tileSpan - 1;
        double rasterLowerRightLon = ROOT_LRLON;
        int tileYIndexEnd = tileSpan - 1;
        double rasterLowerRightLat = ROOT_LRLAT;
        if (queryBoxUpperLeftLon > ROOT_ULLON) {
            tileXIndexStart = (int) Math.floor((queryBoxUpperLeftLon - ROOT_ULLON) / tileLon);
            rasterUpperLeftLon = tileXIndexStart * tileLon + ROOT_ULLON;
        }
        if (queryBoxUpperLeftLat > ROOT_LRLAT) {
            tileYIndexStart = (int) Math.floor((ROOT_ULLAT - queryBoxUpperLeftLat) / tileLat);
            rasterUpperLeftLat = -(tileYIndexStart * tileLat) + ROOT_ULLAT;
        }
        if (queryBoxLowerRightLon < ROOT_LRLON) {
            tileXIndexEnd = (int) Math.floor((queryBoxLowerRightLon - ROOT_ULLON) / tileLon);
            rasterLowerRightLon = (tileXIndexEnd + 1) * tileLon + ROOT_ULLON;
        }
        if (queryBoxLowerRightLat < ROOT_ULLAT) {
            tileYIndexEnd = (int) Math.floor((ROOT_ULLAT - queryBoxLowerRightLat) / tileLat);
            rasterLowerRightLat = -(tileYIndexEnd + 1) * tileLat + ROOT_ULLAT;
        }

        int numTilesWidth = tileXIndexEnd - tileXIndexStart + 1;
        int numTilesHeight = tileYIndexEnd - tileYIndexStart + 1;
        String[][] renderGrid = new String[numTilesHeight][numTilesWidth];
        for (int x = tileXIndexStart; x <= tileXIndexEnd; x++) {
            for (int y = tileYIndexStart; y <= tileYIndexEnd; y++) {
                String image = "d" + rasterDepth + "_x" + x + "_y" + y + ".png";
                renderGrid[y - tileYIndexStart][x - tileXIndexStart] = image;
            }
        }
        results.put("render_grid", renderGrid);
        results.put("raster_ul_lon", rasterUpperLeftLon); // -122.244873047
        results.put("raster_ul_lat", rasterUpperLeftLat);
        results.put("raster_lr_lon", rasterLowerRightLon);
        results.put("raster_lr_lat", rasterLowerRightLat);
        results.put("depth", rasterDepth);
        results.put("query_success", true);

//        System.out.println("============================");
//
//        double longitudinalDistancePerTile = requestLdpp * Constants.TILE_SIZE;
////        System.out.println("longitudinalDistancePerTile: " + longitudinalDistancePerTile);
//
//        double latitudinalDistancePerPixel =
//                (queryBoxUpperLeftLat - queryBoxLowerRightLat) / queryBoxHeight;
////        System.out.println("** latitudinalDistancePerPixel: " + latitudinalDistancePerPixel);
//        double latitudinalDistancePerTile = latitudinalDistancePerPixel * Constants.TILE_SIZE;
//
////        System.out.println("** latitudinalDistancePerTile: " + latitudinalDistancePerTile);
//
//        double resultUpperLeftLon =
//                (queryBoxUpperLeftLon - Constants.ROOT_ULLON) / longitudinalDistancePerTile;
//        double resultLowerRightLon =
//                (queryBoxLowerRightLon - queryBoxUpperLeftLon) / longitudinalDistancePerTile;
//        double resultUpperLeftLat =
//                (Constants.ROOT_ULLAT - queryBoxUpperLeftLat) / longitudinalDistancePerTile;
//        double resultLowerRightLat =
//                (queryBoxUpperLeftLat - queryBoxLowerRightLat) / longitudinalDistancePerTile;
//
//        System.out.println("resultUpperLeftLon: " + resultUpperLeftLon);
//        System.out.println("resultLowerRightLon: " + resultLowerRightLon);
////        System.out.println("resultUpperLeftLat: " + resultUpperLeftLat);
////        System.out.println("resultLowerRightLat: " + resultLowerRightLat);
//
//        int startingLonTile = 1 + (int) Math.ceil(resultUpperLeftLon);
//        int endingLonTile = startingLonTile + 1 + (int) Math.ceil(resultLowerRightLon);
//        int startingLatTile = 1 + (int) resultUpperLeftLat;
//        int endingLatTile = startingLatTile + 1 + (int) startingLatTile;
//
//        System.out.println("startingTile: " + startingLonTile);
//        System.out.println("endingTile: " + endingLonTile);
//        System.out.println("startingLatTile: " + startingLatTile);
//        System.out.println("endingLatTile: " + endingLatTile);
//
//        int depth = endingLonTile - startingLonTile;
//        int matrixLen = depth + 1;
//
//        System.out.println("depth: " + depth);
//
//        String[][] filesToDisplay = new String[matrixLen][matrixLen];
//        for (int y = 0; y < matrixLen; y++) {
//            for (int x = 0; x < matrixLen; x++) {
//                int xVal = startingLonTile + x;
//                int yVal = startingLatTile + y;
//                String image = "d" + depth + "_x" + xVal + "_y" + yVal + ".png";
//                filesToDisplay[y][x] = image;
//            }
//        }
//
//        double rasterUpperLeftLon =
//                Constants.ROOT_ULLON + (longitudinalDistancePerTile * startingLonTile);
//
//        results.put("render_grid", filesToDisplay);
//        results.put("raster_ul_lon", rasterUpperLeftLon); // -122.244873047
//        results.put("raster_ul_lat", queryBoxUpperLeftLat);
//        results.put("raster_lr_lon", queryBoxLowerRightLon);
//        results.put("raster_lr_lat", queryBoxLowerRightLat);
//        results.put("depth", depth);
//        results.put("query_success", true);



        return results;
    }

    @Override
    protected Object buildJsonResponse(Map<String, Object> result) {
        boolean rasterSuccess = validateRasteredImgParams(result);

        if (rasterSuccess) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            writeImagesToOutputStream(result, os);
            String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
            result.put("b64_encoded_image_data", encodedImage);
        }
        return super.buildJsonResponse(result);
    }

    private Map<String, Object> queryFail() {
        Map<String, Object> results = new HashMap<>();
        results.put("render_grid", null);
        results.put("raster_ul_lon", 0.0);
        results.put("raster_ul_lat", 0.0);
        results.put("raster_lr_lon", 0.0);
        results.put("raster_lr_lat", 0.0);
        results.put("depth", 0.0);
        results.put("query_success", false);
        return results;
    }

    /**
     * Validates that Rasterer has returned a result that can be rendered.
     * @param rip : Parameters provided by the rasterer
     */
    private boolean validateRasteredImgParams(Map<String, Object> rip) {
        for (String p : REQUIRED_RASTER_RESULT_PARAMS) {
            if (!rip.containsKey(p)) {
                System.out.println("Your rastering result is missing the " + p + " field.");
                return false;
            }
        }
        if (rip.containsKey("query_success")) {
            boolean success = (boolean) rip.get("query_success");
            if (!success) {
                System.out.println("query_success was reported as a failure");
                return false;
            }
        }
        return true;
    }

    /**
     * Writes the images corresponding to rasteredImgParams to the output stream.
     * In Spring 2016, students had to do this on their own, but in 2017,
     * we made this into provided code since it was just a bit too low level.
     */
    private  void writeImagesToOutputStream(Map<String, Object> rasteredImageParams,
                                                  ByteArrayOutputStream os) {
        String[][] renderGrid = (String[][]) rasteredImageParams.get("render_grid");
        int numVertTiles = renderGrid.length;
        int numHorizTiles = renderGrid[0].length;

        BufferedImage img = new BufferedImage(numHorizTiles * Constants.TILE_SIZE,
                numVertTiles * Constants.TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics graphic = img.getGraphics();
        int x = 0, y = 0;

        for (int r = 0; r < numVertTiles; r += 1) {
            for (int c = 0; c < numHorizTiles; c += 1) {
                graphic.drawImage(getImage(Constants.IMG_ROOT + renderGrid[r][c]), x, y, null);
                x += Constants.TILE_SIZE;
                if (x >= img.getWidth()) {
                    x = 0;
                    y += Constants.TILE_SIZE;
                }
            }
        }

        /* If there is a route, draw it. */
        double ullon = (double) rasteredImageParams.get("raster_ul_lon"); //tiles.get(0).ulp;
        double ullat = (double) rasteredImageParams.get("raster_ul_lat"); //tiles.get(0).ulp;
        double lrlon = (double) rasteredImageParams.get("raster_lr_lon"); //tiles.get(0).ulp;
        double lrlat = (double) rasteredImageParams.get("raster_lr_lat"); //tiles.get(0).ulp;

        final double wdpp = (lrlon - ullon) / img.getWidth();
        final double hdpp = (ullat - lrlat) / img.getHeight();
        AugmentedStreetMapGraph graph = SEMANTIC_STREET_GRAPH;
        List<Long> route = ROUTE_LIST;

        if (route != null && !route.isEmpty()) {
            Graphics2D g2d = (Graphics2D) graphic;
            g2d.setColor(Constants.ROUTE_STROKE_COLOR);
            g2d.setStroke(new BasicStroke(Constants.ROUTE_STROKE_WIDTH_PX,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            route.stream().reduce((v, w) -> {
                g2d.drawLine((int) ((graph.lon(v) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(v)) * (1 / hdpp)),
                        (int) ((graph.lon(w) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(w)) * (1 / hdpp)));
                return w;
            });
        }

        rasteredImageParams.put("raster_width", img.getWidth());
        rasteredImageParams.put("raster_height", img.getHeight());

        try {
            ImageIO.write(img, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private BufferedImage getImage(String imgPath) {
        BufferedImage tileImg = null;
        if (tileImg == null) {
            try {
                File in = new File(imgPath);
                tileImg = ImageIO.read(in);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        return tileImg;
    }
}
