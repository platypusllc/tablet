//*TODO  CHANGE THIS ADD MAPBOX STUFF BACK
package com.platypus.android.tablet;

/**
 * Created by shenty on 2/13/16.
 */
import android.graphics.Color;

import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;

public class PolyArea
{
    private LatLng centroid;
    ArrayList<LatLng> vertices;
    ArrayList<LatLng> originalVerts;
    public ArrayList<LatLng> quickHull(ArrayList<LatLng> points)
    {
        originalVerts = new ArrayList<LatLng>(points);
        ArrayList<LatLng> convexHull = new ArrayList<LatLng>();
        if (points.size() < 3) {
            return points;
        }

        int minLatLng = -1, maxLatLng = -1;
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        for (int i = 0; i < points.size(); i++)
        {
            if (points.get(i).getLatitude() < minX)
            {
                minX = points.get(i).getLatitude();
                minLatLng = i;
            }
            if (points.get(i).getLatitude() > maxX)
            {
                maxX = points.get(i).getLatitude();
                maxLatLng = i;
            }
        }

        LatLng A = points.get(minLatLng);
        LatLng B = points.get(maxLatLng);
        convexHull.add(A);
        convexHull.add(B);
        points.remove(A);
        points.remove(B);
        originalVerts.remove(A);
        originalVerts.remove(B);

        ArrayList<LatLng> leftSet = new ArrayList<LatLng>();
        ArrayList<LatLng> rightSet = new ArrayList<LatLng>();

        for (int i = 0; i < points.size(); i++)
        {
            LatLng p = points.get(i);
            if (pointLocation(A, B, p) == -1)
                leftSet.add(p);
            else if (pointLocation(A, B, p) == 1)
                rightSet.add(p);
        }
        hullSet(A, B, rightSet, convexHull);
        hullSet(B, A, leftSet, convexHull);
        vertices = new ArrayList<LatLng>(convexHull);

        return convexHull;
    }

    public double distance(LatLng A, LatLng B, LatLng C)
    {
        double ABx = B.getLongitude() - A.getLongitude();
        double ABy = B.getLatitude() - A.getLatitude();
        double num = ABx * (A.getLatitude() - C.getLatitude()) - ABy * (A.getLongitude() - C.getLongitude());
        if (num < 0)
            num = -num;
        return num;
    }

    public void hullSet(LatLng A, LatLng B, ArrayList<LatLng> set,
                        ArrayList<LatLng> hull)
    {
        int insertPosition = hull.indexOf(B);
        if (set.size() == 0)
            return;
        if (set.size() == 1)
        {
            LatLng p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        double dist = Double.MIN_VALUE;
        int furthestLatLng = -1;
        for (int i = 0; i < set.size(); i++)
        {
            LatLng p = set.get(i);
            double distance = distance(A, B, p);
            if (distance > dist)
            {
                dist = distance;
                furthestLatLng = i;
            }
        }
        LatLng P = set.get(furthestLatLng);
        set.remove(furthestLatLng);
        hull.add(insertPosition, P);
        // Determine who's to the left of AP

        ArrayList<LatLng> leftSetAP = new ArrayList<LatLng>();
        for (int i = 0; i < set.size(); i++)
        {
            LatLng M = set.get(i);
            if (pointLocation(A, P, M) == 1)
            {
                leftSetAP.add(M);
            }
        }

        // Determine who's to the left of PB

        ArrayList<LatLng> leftSetPB = new ArrayList<LatLng>();
        for (int i = 0; i < set.size(); i++)
        {
            LatLng M = set.get(i);
            if (pointLocation(P, B, M) == 1)
            {
                leftSetPB.add(M);
            }
        }
        hullSet(A, P, leftSetAP, hull);
        hullSet(P, B, leftSetPB, hull);
    }

    public double pointLocation(LatLng A, LatLng B, LatLng P)
    {
        double cp1 = (B.getLongitude() - A.getLongitude()) * (P.getLatitude() - A.getLatitude()) - (B.getLatitude() - A.getLatitude()) * (P.getLongitude() - A.getLongitude());
        if (cp1 > 0)
            return 1;
        else if (cp1 == 0)
            return 0;
        else
            return -1;
    }



    public LatLng computeCentroid(ArrayList<LatLng> vertices)
    {
        double tempLat = 0;
        double tempLon = 0;

        for (LatLng i : vertices)
        {
            tempLat += i.getLatitude();
            tempLon += i.getLongitude();
        }
        return new LatLng(tempLat/vertices.size(),tempLon/vertices.size());
    }


    public ArrayList<ArrayList<LatLng>> createSmallerPolygons(ArrayList<LatLng> vertices) {

        centroid = computeCentroid(vertices);
        ArrayList<LatLng> pointToCenter = new ArrayList<LatLng>();
        for (LatLng i : vertices)
        {
            pointToCenter.add(new LatLng(i.getLatitude()-centroid.getLatitude(),i.getLongitude()-centroid.getLongitude()));
        }
        ArrayList<ArrayList<LatLng>> spirals = new ArrayList<ArrayList<LatLng>>();
        for (double i = 1; i >= 0; i-=.1)
        {
            ArrayList<LatLng> points = new ArrayList<LatLng>();
            for (LatLng p : pointToCenter)
            {
                points.add(new LatLng(centroid.getLatitude()+p.getLatitude()*i,centroid.getLongitude()+p.getLongitude()*i));
            }
            spirals.add(points);
        }
        return spirals;
    }


    public static void main(String args[])
    {
        System.out.println("Quick Hull Test");
        //Scanner sc = new Scanner(System.in);
        //      System.out.println("Enter the number of points");
        //double N = sc.nextInt();

        ArrayList<LatLng> points = new ArrayList<LatLng>();
//        System.out.println("Enter the coordinates of each points: <x> <y>");
        // for (int i = 0; i < N; i++)
        // {
        //     double x = sc.nextInt();
        //     double y = sc.nextInt();
        //     LatLng e = new LatLng(x, y);
        //     points.add(i, e);
        // }
        LatLng point1 = new LatLng(-2,-2);
        LatLng point3 = new LatLng(-1,0);
        LatLng point2 = new LatLng(-2,2);
        LatLng point5 = new LatLng(0,1);
        LatLng point4 = new LatLng(2,2);
        LatLng point6 = new LatLng(1,0);
        LatLng point7 = new LatLng(2,-2);
        LatLng point8 = new LatLng(0,-1);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
        points.add(point5);
        points.add(point6);
        points.add(point7);
        points.add(point8);


        PolyArea qh = new PolyArea();
        ArrayList<LatLng> p = qh.quickHull(points);

    }
    public LatLng getCentroid()
    {
        return centroid;
    }

    public ArrayList<PolygonOptions> getSmallerPolygons()
     {
         ArrayList<ArrayList<LatLng>> smallerpoly = createSmallerPolygons(vertices);
         ArrayList<PolygonOptions> spiralList = new ArrayList<PolygonOptions>();
         for (ArrayList<LatLng> a : smallerpoly)
         {
             spiralList.add(new PolygonOptions().addAll(a).strokeColor(Color.BLUE).fillColor(Color.TRANSPARENT)); //draw polygon
         }
         return spiralList;
     }

}
