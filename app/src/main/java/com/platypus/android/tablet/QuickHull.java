package com.platypus.android.tablet;

/**
 * Created by zeshengxi on 2/12/16.
 */
//This is a java program to find a points in convex hull using quick hull method
//source: Alexander Hrishov's website
//URL: http://www.ahristov.com/tutorial/geometry-games/convex-hull.html

//import android.graphics.Point;

//import android.graphics.Point;

import java.util.ArrayList;
import java.util.Scanner;


public class QuickHull {



    public ArrayList<doublePoint> quickHull(ArrayList<doublePoint> points) {
        ArrayList<doublePoint> convexHull = new ArrayList<doublePoint>();
        if (points.size() < 3)
            return (ArrayList) points.clone();

        int minPoint = -1, maxPoint = -1;
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).x < minX) {
                minX = points.get(i).x;
                minPoint = i;
            }
            if (points.get(i).x > maxX) {
                maxX = points.get(i).x;
                maxPoint = i;
            }
        }
        doublePoint A = points.get(minPoint);
        doublePoint B = points.get(maxPoint);
        convexHull.add(A);
        convexHull.add(B);
        points.remove(A);
        points.remove(B);

        ArrayList<doublePoint> leftSet = new ArrayList<doublePoint>();
        ArrayList<doublePoint> rightSet = new ArrayList<doublePoint>();

        for (int i = 0; i < points.size(); i++) {
            doublePoint p = points.get(i);

            if (pointLocation(A, B, p) == -1)
                leftSet.add(p);
            else if (pointLocation(A, B, p) == 1)
                rightSet.add(p);
        }
        hullSet(A, B, rightSet, convexHull);
        hullSet(B, A, leftSet, convexHull);

        return convexHull;
    }

    public double distance(doublePoint A, doublePoint B, doublePoint C) {
        double ABx = B.x - A.x;
        double ABy = B.y - A.y;
        double num = ABx * (A.y - C.y) - ABy * (A.x - C.x);
        if (num < 0)
            num = -num;
        return num;
    }

    public void hullSet(doublePoint A, doublePoint B, ArrayList<doublePoint> set,
                        ArrayList<doublePoint> hull) {
        int insertPosition = hull.indexOf(B);
        if (set.size() == 0)
            return;
        if (set.size() == 1) {
            doublePoint p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        double dist = Double.MIN_VALUE;
        int furthestPoint = -1;
        for (int i = 0; i < set.size(); i++) {
            doublePoint p = set.get(i);
            double distance = distance(A, B, p);
            if (distance > dist) {
                dist = distance;
                furthestPoint = i;
            }
        }
        doublePoint P = set.get(furthestPoint);
        set.remove(furthestPoint);
        hull.add(insertPosition, P);

        // Determine who's to the left of AP
        ArrayList<doublePoint> leftSetAP = new ArrayList<doublePoint>();
        for (int i = 0; i < set.size(); i++) {
            doublePoint M = set.get(i);
            if (pointLocation(A, P, M) == 1) {
                leftSetAP.add(M);
            }
        }

        // Determine who's to the left of PB
        ArrayList<doublePoint> leftSetPB = new ArrayList<doublePoint>();
        for (int i = 0; i < set.size(); i++) {
            doublePoint M = set.get(i);
            if (pointLocation(P, B, M) == 1) {
                leftSetPB.add(M);
            }
        }
        hullSet(A, P, leftSetAP, hull);
        hullSet(P, B, leftSetPB, hull);

    }

    public int pointLocation(doublePoint A, doublePoint B, doublePoint P) {
        double cp1 = (B.x - A.x) * (P.y - A.y) - (B.y - A.y) * (P.x - A.x);
        if (cp1 > 0)
            return 1;
        else if (cp1 == 0)
            return 0;
        else
            return -1;
    }

    public static void main(String args[]) {
        System.out.println("Quick Hull Test");
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the number of points");
        int N = sc.nextInt();

        ArrayList<doublePoint> points = new ArrayList<doublePoint>();
        System.out.println("Enter the coordinates of each points: <x> <y>");
        for (int i = 0; i < N; i++) {
            double x = sc.nextInt();
            double y = sc.nextInt();
            doublePoint e = new doublePoint(x, y);
            points.add(i, e);
        }

        QuickHull qh = new QuickHull();
        ArrayList<doublePoint> p = qh.quickHull(points);
        System.out
                .println("The points in the Convex hull using Quick Hull are: ");
        for (int i = 0; i < p.size(); i++)
            System.out.println("(" + p.get(i).x + ", " + p.get(i).y + ")");
        sc.close();
    }
}
