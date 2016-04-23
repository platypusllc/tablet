
/**
 * Created by shenty on 2/13/16.
 */
package com.platypus.android.tablet;

import com.mapbox.mapboxsdk.geometry.LatLng;
import java.util.ArrayList;


public class PolyArea
{

     final double MAXDISTFROMSIDE = .0000898; //distance between wayp
     final double SUBTRACTDIST = MAXDISTFROMSIDE/2; //subtracted
    // dist
//    final double MAXDISTFROMSIDE = .4; //distance between wayp
//    final double SUBTRACTDIST = .2; //subtracted dist

    private LatLng centroid;
    ArrayList<LatLng> vertices;
    ArrayList<LatLng> originalVerts;
    ArrayList<ArrayList<LatLng>> bisect = new ArrayList<ArrayList<LatLng>>();

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

    public LatLng getCentroid()
    {
        return centroid;
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


            //              System.out.println();
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

        //ArrayList<LatLng> pointToCenter = new ArrayList<LatLng>();
        //normalized vectors from vertex to center
        // centroid = computeCentroid(vertices);
        // for (LatLng it : vertices)
        //     {
        //         pointToCenter.add(normalizeVector(new LatLng(it.getLatitude()-centroid.getLatitude(),it.getLongitude()-centroid.getLongitude())));
        //     }
        ArrayList<LatLng> centers = new ArrayList<LatLng>();
        while(!isNonAdjacentLessThan10Meters(spirals.get(spirals.size()-1)))
        {

            centroid = computeCentroid(spirals.get(spirals.size()-1));
            centers.add(centroid);
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
                System.out.println("dist " + calculateLength(temp));
                if (calculateLength(temp) < MAXDISTFROMSIDE)
                {
                    System.out.println("centroid");
                    System.out.print("centx = [");
                    for (LatLng t : centers)
                    {
                        System.out.print(t.getLatitude()+",");
                    }

                    System.out.print("]\n\ncenty=[");
                    for (LatLng t : centers)
                    {
                        System.out.print(t.getLongitude()+",");
                    }
                    System.out.print("]\n\n");

                    return spirals;
                }
            }

            //for (LatLng p : pointToCenter)
            for (int t = 0; t < pointToCenter.size(); t++)
            {
                LatLng p = pointToCenter.get(t);
                LatLng temp = new LatLng(centroid.getLatitude() - p.getLatitude(),centroid.getLongitude()-p.getLongitude());
                if (calculateLength(temp) < MAXDISTFROMSIDE)
                {
                    continue;
                }

                nextPolygon.add(new LatLng(previousPolygon.get(t).getLatitude()-pointToCenter.get(t).getLatitude()*SUBTRACTDIST,previousPolygon.get(t).getLongitude()-pointToCenter.get(t).getLongitude()*SUBTRACTDIST));
            }
            System.out.println(nextPolygon.size());
            spirals.add(nextPolygon);
            // System.out.println("New Polygon");
            // for (LatLng a : nextPolygon)
            //  {
            //      System.out.println(a);
            //  }
            // System.out.println("");
        }

        return spirals;
    }

    public static void main1(String args[])
    {
        System.out.println("\nQuick Hull Test \n");

        ArrayList<LatLng> points = new ArrayList<LatLng>();


        //these works
        // LatLng point0 = new LatLng(-2,-2);
        // LatLng point1 = new LatLng(-2,2);
        // LatLng point2 = new LatLng(2,2);
        // LatLng point3 = new LatLng(2,-2);


        //these dont
        LatLng point0 = new LatLng(-15 ,145);
        LatLng point1 = new LatLng(3,94);
        LatLng point2 = new LatLng(35,113);
        LatLng point3 = new LatLng(15,200);
        LatLng point4 = new LatLng(-10,200);

        points.add(point0);
        points.add(point1);
        points.add(point2);
        //points.add(point3);
        //points.add(point4);

        PolyArea qh = new PolyArea();
        ArrayList<LatLng> p = qh.quickHull(points);

        System.out.println(qh.isNonAdjacentLessThan10Meters(qh.vertices));

        // PolyArea qh = new PolyArea();
        // ArrayList<LatLng> p = qh.quickHull(points);
        //ArrayList<ArrayList<LatLng>> spirals = qh.createSmallerPolygonsFlat(qh.vertices);

        // System.out.print("x = [");
        // for (ArrayList<LatLng> i : spirals)
        //     {
        //         for (LatLng t : i)
        //             {
        //                 System.out.print(t.getLatitude()+",");
        //             }
        //     }
        // System.out.print("]\n\ny=[");
        // for (ArrayList<LatLng> i : spirals)
        //     {
        //         for (LatLng t : i)
        //             {
        //                 System.out.print(t.getLongitude()+",");
        //             }
        //     }
        // System.out.print("]\n\n");
        //        System.out.println(qh.centroid);
    }

    public double findInteriorAngle(LatLng a, LatLng b)
    {
        //System.out.println(180/Math.PI*Math.acos(dot(a,b)/(calculateLength(a)*calculateLength(b))));
        // System.out.println("a " + calculateLength(a));
        // System.out.println("b " + calculateLength(b));
        // System.out.println("a.b: " + dot(a,b));
        // System.out.println("dot(a,b)/lengtha*lengthb : " + Math.acos(dot(a,b)/(calculateLength(a)*calculateLength(b
        //                                                                                                                ))));
        return Math.acos(dot(a,b)/(calculateLength(a)*calculateLength(b)));
    }
    public LatLng findBisectNormal(LatLng a, LatLng b)
    {
        a = normalizeVector(a);
        b = normalizeVector(b);
        LatLng added = add(a,b);
        return normalizeVector(added);
    }
    public double dot(LatLng a, LatLng b)
    {
        return a.getLatitude()*b.getLatitude() + a.getLongitude()*b.getLongitude();
    }
    public LatLng add(LatLng a, LatLng b)
    {
        return new LatLng(a.getLatitude() + b.getLatitude(),a.getLongitude()+b.getLongitude());
    }
    public LatLng subtract(LatLng a, LatLng b)
    {
        return new LatLng(a.getLatitude() - b.getLatitude(),a.getLongitude() - b.getLongitude());
    }

    public LatLng findVector(LatLng a, LatLng b)
    {
        return subtract(a,b);
    }
    public LatLng multiply(LatLng a, double amount)
    {
        return new LatLng(a.getLatitude()*amount, a.getLongitude()*amount);
    }

    /*This works by finding the bisecting vector of each angle and
     * moving along the bisecting vector of distance dist/sin(angle/2) */
    public ArrayList<ArrayList<LatLng>> computeSpiralsPolygonOffset(ArrayList<LatLng> polygon)
    {

        ArrayList<ArrayList<LatLng>> spirals = new ArrayList<ArrayList<LatLng>>();
        spirals.add(polygon); //add first polygon
        if (polygon.size() < 3)
        {
            return spirals;
        }

        //compute all of the bisecting vectors note these look wrong

        while(!isNonAdjacentLessThan10Meters(spirals.get(spirals.size()-1)))
        {
            //Last Polygon to be added
            ArrayList<LatLng> lastSpiral = spirals.get(spirals.size()-1);
            //Comput the centroid of the last spiral
            LatLng centroid = computeCentroid(lastSpiral);
            //Check to see if any points are less than
            //subtractdist from the centroid, if so return(this is
            //a stopping point)
            for (LatLng p : lastSpiral)
            {
                LatLng temp = new LatLng(centroid.getLatitude() - p.getLatitude(),centroid.getLongitude()-p.getLongitude());
                //System.out.println("dist " + calculateLength(temp));
                if (calculateLength(temp) < SUBTRACTDIST)
                {
                    return spirals;
                }
            }

            //compute all of the vectors that make the edges
            ArrayList<LatLng> edgeVectors = new ArrayList<LatLng>();
            for (int i = 0; i < lastSpiral.size()-1; i++)
            {
                edgeVectors.add(findVector(lastSpiral.get(i),lastSpiral.get(i+1)));
            }
            edgeVectors.add(findVector(lastSpiral.get(lastSpiral.size()-1),lastSpiral.get(0)));
            // System.out.println(lastSpiral);
            // System.out.println("");
            // System.out.println(edgeVectors);
            //Compute all of the angles between these edges

            ArrayList<Double> interiorAngles = new ArrayList<Double>();
            for (int i = 0; i < edgeVectors.size()-1; i++)
            {
                interiorAngles.add(findInteriorAngle(edgeVectors.get(i),edgeVectors.get(i+1)));
            }
            interiorAngles.add(findInteriorAngle(edgeVectors.get(edgeVectors.size()-1),edgeVectors.get(0)));
//			System.out.println(interiorAngles);
            for (Double i : interiorAngles)
            {
//				System.out.println(i);
                if (i >= 2.8)
                {
                    //return spirals;
                }
            }



            //compute all of the bisecting vectors note these look wrong
            ArrayList<LatLng> bisectingVectors = new ArrayList<LatLng>();
            for (int i = 0; i < edgeVectors.size()-1; i++)
            {
                bisectingVectors.add(findBisectNormal(edgeVectors.get(i),edgeVectors.get(i+1)));
            }
            bisectingVectors.add(findBisectNormal(edgeVectors.get(edgeVectors.size()-1),edgeVectors.get(0)));
            bisect.add(bisectingVectors);

            // System.out.println("last "+lastSpiral.size());
            // System.out.println("edge "+edgeVectors.size());
            // System.out.println("bisect " +bisectingVectors.size());
            // System.out.println("");

            //p = p- dist
            //finds vector between the point and the bisecting
            //vector with length dist/math.sin(angle/2)
            ArrayList<LatLng> nextPolygon = new ArrayList<LatLng>();
            for (int i = 0; i < lastSpiral.size(); i++)
            {
                //LatLng point =
                //spirals.get(spirals.size()-1).get(i);
                LatLng point = lastSpiral.get(i);
                //add the vector between the original point
                //and the point subtracted by the bisecting
                //vector with length
                //subtractdist/math.sin(interiorangle/2)
                LatLng nextPoint = multiply(bisectingVectors.get(i),SUBTRACTDIST/Math.sin(interiorAngles.get(i)/2));
                nextPolygon.add(findVector(point,nextPoint));
            }
            //spirals.add(nextPolygon);
            //dont use quickHull it reorders points
            spirals.add(quickHull(nextPolygon));

        }
        return  spirals;
    }


    public static void main(String[] args)
    {
        ArrayList<LatLng> points = new ArrayList<LatLng>();
        // LatLng point0 = new LatLng(-15 ,145);
        // LatLng point1 = new LatLng(3,94);
        // LatLng point2 = new LatLng(35,113);
        // LatLng point3 = new LatLng(15,200);
        //        LatLng point4 = new LatLng(-10,200);


        LatLng point0 = new LatLng(-1,-2);
        LatLng point1 = new LatLng(-1,1);
        LatLng point2 = new LatLng(1,1);
        LatLng point3 = new LatLng(1,-1);
        // LatLng point4 = new LatLng(2,4);


        // LatLng point0 = new LatLng(-2,-3);
        // LatLng point1 = new LatLng(-2,2);
        // LatLng point2 = new LatLng(2,2);
        // LatLng point3 = new LatLng(2,-2);
        //LatLng point4 = new LatLng(2,4);


        points.add(point0);
        points.add(point1);
        points.add(point2);
        points.add(point3);
        //points.add(point4);

        PolyArea qh = new PolyArea();
        // qh.test();
        // System.exit(0);


        // qh.test();
        // System.exit(0);
        ArrayList<LatLng> p = qh.quickHull(points);
        ArrayList<ArrayList<LatLng>> spirals =  qh.computeSpiralsPolygonOffset(qh.vertices);
        ArrayList<LatLng> allpoints = new ArrayList<LatLng>();

        // for (ArrayList<LatLng> a : spirals)
        //  {
        //      for (LatLng b : a)
        //          {
        //              System.out.println(b);
        //          }
        //      System.out.println("");
        //  }
        // System.exit(0);
        //System.out.print("x = [");
        int counter = 0;
        for (ArrayList<LatLng> i : spirals)
        {
            System.out.print("x" + counter + " = [");
            for (LatLng t : i)
            {
                System.out.print(t.getLatitude()+",");
            }
            System.out.print(i.get(0).getLatitude()+",");
            System.out.println("]\n");
            counter++;
        }
        int counter2 = 0;
        for (ArrayList<LatLng> i : spirals)
        {
            System.out.print("y" + counter2 + " = [");
            for (LatLng t : i)
            {
                System.out.print(t.getLongitude()+",");
            }
            System.out.print(i.get(0).getLongitude()+",");
            System.out.println("]\n");
            counter2++;
        }

        for (int i = 0; i < counter; i++)
        {
            System.out.print("plot(x"+i+",y"+i+");" + "hold on; ");
        }
        System.out.println("\n");
        //for each polygon
        counter = 0;
        for (int i = 0; i < qh.bisect.size(); i++)
        {
            //for each vertex
            for (int t = 0; t < qh.bisect.get(i).size(); t++)
            {
                //qh.bisect.get(i).set(t,qh.normalizeVector(qh.bisect.get(i).get(t)));

                System.out.println("bx"+counter+"=["+spirals.get(i).get(t).getLatitude()+","+qh.subtract(spirals.get(i).get(t),qh.bisect.get(i).get(t)).getLatitude()+"]");
                counter++;
            }
        }

        counter2 = 0;
        for (int i = 0; i < qh.bisect.size(); i++)
        {
            //for each vertex
            for (int t = 0; t < qh.bisect.get(i).size(); t++)
            {
                System.out.println("by"+counter2+"=["+spirals.get(i).get(t).getLongitude()+","+qh.subtract(spirals.get(i).get(t),qh.bisect.get(i).get(t)).getLongitude()+"]");
                counter2++;
            }
        }
        for (int i = 0; i < counter; i++)
        {
            System.out.print("plot(bx"+i+",by"+i+",\"r\");" + "hold on; ");
        }

    }
}
/*
 */
