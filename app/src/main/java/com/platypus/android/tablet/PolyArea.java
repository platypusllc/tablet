//*TODO  CHANGE THIS ADD MAPBOX STUFF BACK
package com.platypus.android.tablet;

/**
 * Created by shenty on 2/13/16.
 */
import android.graphics.Color;

import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.constants.MathConstants;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;

public class PolyArea
{
    final double MAXDISTFROMSIDE = .0000898;; //10 meters
    final double SUBTRACTDIST = MAXDISTFROMSIDE/2; //5 meters
    private LatLng centroid;
    ArrayList<LatLng> vertices;
    ArrayList<LatLng> originalVerts;
    final double DIST= .0000898; //10 meters
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
    public double computeDistance(LatLng firstPoint, LatLng secondPoint)
    {
        double x = Math.pow((secondPoint.getLatitude() - firstPoint.getLatitude()),2);
        double y = Math.pow((secondPoint.getLongitude() - firstPoint.getLongitude()),2);
        return Math.sqrt(x+y);
    }
    public double calculateLength(LatLng vector)
    {
        return Math.sqrt(Math.pow(vector.getLatitude(),2) + Math.pow(vector.getLongitude(),2));
    }
    public LatLng normalizeVector(LatLng vector)
    {
        double distance = calculateLength(vector);
        return new LatLng(vector.getLatitude()/distance, vector.getLongitude()/distance);
    }
    public boolean isNonAdjacentLessThan10Meters(ArrayList<LatLng> verts) {

        //since all points in triangle are adjacent
        if (verts.size() == 3)
        {

            // System.out.println("points 0 and 1" + verts.get(0) + " " + verts.get(1) + " distance: " + comput// eDistance(verts.get(0),verts.get(1)));
            // // System.out.println("points 0 and 2" + verts.get(0) + " " + verts.get(2) + " distance: " + computeDistance(verts.get(0),verts.get(2)));
            // // System.out.println("points 1 and 2" +
            // verts.get(1) + " " + verts.get(2) + " distance: " +
            // computeDistance(verts.get(1),verts.get(2)));

            // System.out.println("distance between points 0 and 1: " + computeDistance(verts.get(0),verts.get(1)));
            // System.out.println("distance between points 0 and 2: " + computeDistance(verts.get(0),verts.get(2)));
            // System.out.println("distance between points 0 and 3: " + computeDistance(verts.get(1),verts.get(2)));


            //				System.out.println();
            if (computeDistance(verts.get(0),verts.get(1)) < MAXDISTFROMSIDE)
            {
                return true;
            }
            else if(computeDistance(verts.get(0),verts.get(2)) < MAXDISTFROMSIDE)
            {
                return true;
            }
            else if(computeDistance(verts.get(1),verts.get(2)) < MAXDISTFROMSIDE)
            {
                return true;
            }
            return false;
        }

        for (int i = 0; i < verts.size(); i++)
        {
            for (int p = i+2; p < verts.size(); p++)
            {
                //case where the first and last point (adjacent are
                //being compared
                //                        System.out.println(i + " " + p);
                if (i == 0 && p == verts.size()-1)
                {
                    continue;
                }
                //                      System.out.println(computeDistance(verts.get(i),verts.get(p)));
                if (computeDistance(verts.get(i),verts.get(p)) < MAXDISTFROMSIDE)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<ArrayList<LatLng>> createSmallerPolygonsFlat(ArrayList<LatLng> vertices) {

        ArrayList<ArrayList<LatLng>> spirals = new ArrayList<ArrayList<LatLng>>();
        spirals.add(vertices);
        int i = 0;
        while(!isNonAdjacentLessThan10Meters(spirals.get(spirals.size()-1)))
        {
            i++;
            //uncomment this for testing if printlns are flooding output
            centroid = computeCentroid(spirals.get(spirals.size()-1));
            ArrayList<LatLng> pointToCenter = new ArrayList<LatLng>();
            //normalized vectors from vertex to center
            for (LatLng it : spirals.get(spirals.size()-1))
            {
                pointToCenter.add(normalizeVector(new LatLng(it.getLatitude()-centroid.getLatitude(),it.getLongitude()-centroid.getLongitude())));
            }

            //the last layer added
            ArrayList<LatLng> previousPolygon = spirals.get(spirals.size()-1);
            //upcoming layer
            ArrayList<LatLng> nextPolygon = new ArrayList<LatLng>();

            for (LatLng p : previousPolygon)
            {
                LatLng temp = new LatLng(centroid.getLatitude() - p.getLatitude(),centroid.getLongitude()-p.getLongitude());
                //System.out.println("dist " + calculateLength(temp));
                if (calculateLength(temp) < SUBTRACTDIST)
                {
                    //System.out.println("spirals: " + spirals.size());
                    return spirals;
                }
            }

            //for (LatLng p : pointToCenter)
            for (int t = 0; t < pointToCenter.size(); t++)
            {
                nextPolygon.add(new LatLng(previousPolygon.get(t).getLatitude()-pointToCenter.get(t).getLatitude()*SUBTRACTDIST,previousPolygon.get(t).getLongitude()-pointToCenter.get(t).getLongitude()*SUBTRACTDIST));
            }
            spirals.add(nextPolygon);
            // System.out.println("New Polygon");
            // for (LatLng a : nextPolygon)
            // 	{
            // 		System.out.println(a);
            // 	}
            // System.out.println("");
        }
        return spirals;
    }

}
