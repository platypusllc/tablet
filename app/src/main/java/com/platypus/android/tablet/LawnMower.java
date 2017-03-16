package com.platypus.android.tablet;

import com.mapbox.mapboxsdk.geometry.LatLng;
import java.util.ArrayList;

/**
 * Created by shenty on 5/6/16.
 */
public class LawnMower {
    public static final double LON_D_PER_M = 1.0 / 90000.0;
    public static final double LAT_D_PER_M = 1.0 / 110000.0;

//    private Object[] getLawnmowerPath(ArrayList<LatLng> area, double stepSize) {
//        // Compute the bounding box
//        double minLat = 360;
//        double maxLat = -360;
//        double minLon = 360;
//        double maxLon = -360;
//        Double curLat = null;
//        for (LatLng latLon : area) {  //get list of points
//            if (latLon.getLatitude() > maxLat) { //if latlong.getLatitude()...
//                maxLat = latLon.getLatitude();
//            } else if (latLon.getLatitude() < minLat) {
//                minLat = latLon.getLatitude();
//            }
//            if (latLon.getLongitude() > maxLon) {
//                maxLon = latLon.getLongitude();
//            } else if (latLon.getLongitude() < minLon) {
//                minLon = latLon.getLongitude();
//            }
//        }
//        curLat = minLat;
//
//        double totalLength = 0.0;
//        Double leftLon = null, rightLon = null; //locations
//        ArrayList<LatLng> path = new ArrayList<LatLng>(); //can just
//        //be latlng
//        while (curLat <= maxLat) {
//            // Left to right
//            leftLon = getMinLonAt(area, minLon, maxLon, curLat);
//            rightLon = getMaxLonAt(area, minLon, maxLon, curLat);
//            if (leftLon != null && rightLon != null) {
//                path.add(new LatLng(curLat, leftLon));
//                path.add(new LatLng(curLat, rightLon));
//                totalLength += Math.abs((rightLon - leftLon) * LON_D_PER_M);
//            } else {
//            }
//            // Right to left
//            curLat = curLat+stepSize;
//            if (curLat <= maxLat) {
//                totalLength += stepSize;
//                rightLon = getMaxLonAt(area, minLon, maxLon, curLat);
//                leftLon = getMinLonAt(area, minLon, maxLon, curLat);
//                if (leftLon != null && rightLon != null) {
//                    path.add(new LatLng(curLat, rightLon));
//                    path.add(new LatLng(curLat, leftLon));
//                    totalLength += Math.abs((rightLon - leftLon) * LON_D_PER_M);
//                } else {
//                }
//            }
//            curLat = curLat + stepSize;
//            if (curLat <= maxLat) {
//                totalLength += stepSize;
//            }
//        }
//
//        return new Object[]{path, totalLength};
//    }
//    private static Double getMinLonAt(ArrayList<LatLng area, double minLon, double maxLon, double lat) {
//        final double lonDiff = 1.0 / 90000.0 * 10.0;
//        LatLng latLon = new LatLng(lat, minLon);
//        while (!isLocationInside(latLon, (ArrayList<LatLng>) area.getOuterBoundary()) && minLon <= maxLon) {
//            minLon = minLon + lonDiff;
//            latLon = new LatLng(lat, minLon);
//            if (minLon > maxLon) {
//                // Overshot (this part of the area is tiny), so ignore it by returning null
//                return null;
//            }
//        }
//        return minLon;
//    }
//
//    private static Double getMaxLonAt(ArrayList<LatLng> area, double minLon, double maxLon, double lat) {
//        final double lonDiff = 1.0 / 90000.0 * 10.0;
//        LatLng latLon = new LatLng(lat, maxLon);
//        while (!isLocationInside(latLon, (ArrayList<LatLng>) area.getOuterBoundary())) {
//            maxLon = maxLon-lonDiff;
//            latLon = new LatLng(lat, maxLon);
//            if (maxLon < minLon) {
//                // Overshot (this part of the area is tiny), so ignore it by returning null
//                return null;
//            }
//        }
//        return maxLon;
//    }
//    public static boolean isLocationInside(LatLng point, ArrayList<? extends LatLng> positions) {
//        boolean result = false;
//        LatLng p1 = positions.get(0);
//        for (int i = 1; i < positions.size(); i++) {
//            LatLng p2 = positions.get(i);
//
//            if (((p2.getLatitude() <= point.getLatitude()
//                    && point.getLatitude() < p1.getLatitude())
//                    || (p1.getLatitude() <= point.getLatitude()
//                    && point.getLatitude() < p2.getLatitude()))
//                    && (point.getLongitude() < (p1.getLongitude() - p2.getLongitude())
//                    * (point.getLatitude() - p2.getLatitude())
//                    / (p1.getLatitude() - p2.getLatitude()) + p2.getLongitude())) {
//                result = !result;
//            }
//            p1 = p2;
//        }
//        return result;
//    }

}
