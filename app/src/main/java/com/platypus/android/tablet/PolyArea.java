package com.platypus.android.tablet;
/**
 * Created by shenty on 2/13/16.
 */

import com.mapbox.mapboxsdk.geometry.LatLng;
import java.util.ArrayList;


//import javax.measure.unit.SI;

//issues
//subtract method does subtraction in wrong order
//for bisect vectors it multiples -1 and for findInteriorAngles it
//switches order of parameters. Should fix this but no rush...
public class PolyArea {
    // none usually meaning nothing selected yet..
    public enum AreaType
    {
        LAWNMOWER,SPIRAL,WAYPOINT
    }

    //double MAXDISTFROMSIDE = .4; //distance between waypoints 10 meters
    double MAXDISTFROMSIDE = .0000898; //distance between waypoints 10 meters
    final double ONE_METER = MAXDISTFROMSIDE / 10; //one meter
    double SUBTRACTDIST = MAXDISTFROMSIDE / 2; //5 meters
    final double FIVE_METERS = MAXDISTFROMSIDE / 2;
    public static final double LON_D_PER_M = 1.0 / 90000.0;
    public static final double LAT_D_PER_M = 1.0 / 110000.0;

    // dist
    // final double MAXDISTFROMSIDE = .4; //distance between wayp
    // final double SUBTRACTDIST = .2; //subtracted dist

    private LatLng centroid;
    ArrayList<LatLng> vertices;
    ArrayList<LatLng> originalVerts;
    ArrayList<ArrayList<LatLng>> bisect = new ArrayList<ArrayList<LatLng>>();

    public ArrayList<LatLng> quickHull(ArrayList<LatLng> points) {
        originalVerts = new ArrayList<LatLng>(points);
        ArrayList<LatLng> convexHull = new ArrayList<LatLng>();
        if (points.size() < 3) {
            return points;
        }

        int minLatLng = -1, maxLatLng = -1;
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            // System.out.println("max x " + maxX);
            // System.out.println(points.get(i).getLatitude());
            if (points.get(i).getLatitude() < minX) {
                minX = points.get(i).getLatitude();
                minLatLng = i;
            }
            //System.out.println("i lat: " +
            //points.get(i).getLatitude() + " max " + maxX);

            if (points.get(i).getLatitude() > maxX) //is this right?
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

        for (int i = 0; i < points.size(); i++) {
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

    public double distance(LatLng A, LatLng B, LatLng C) {
        double ABx = B.getLongitude() - A.getLongitude();
        double ABy = B.getLatitude() - A.getLatitude();
        double num = ABx * (A.getLatitude() - C.getLatitude()) - ABy * (A.getLongitude() - C.getLongitude());
        if (num < 0)
            num = -num;
        return num;
    }

    public void hullSet(LatLng A, LatLng B, ArrayList<LatLng> set,
                        ArrayList<LatLng> hull) {
        int insertPosition = hull.indexOf(B);
        if (set.size() == 0)
            return;
        if (set.size() == 1) {
            LatLng p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        double dist = Double.MIN_VALUE;
        int furthestLatLng = -1;
        for (int i = 0; i < set.size(); i++) {
            LatLng p = set.get(i);
            double distance = distance(A, B, p);
            if (distance > dist) {
                dist = distance;
                furthestLatLng = i;
            }
        }
        LatLng P = set.get(furthestLatLng);
        set.remove(furthestLatLng);
        hull.add(insertPosition, P);
        // Determine who's to the left of AP

        ArrayList<LatLng> leftSetAP = new ArrayList<LatLng>();
        for (int i = 0; i < set.size(); i++) {
            LatLng M = set.get(i);
            if (pointLocation(A, P, M) == 1) {
                leftSetAP.add(M);
            }
        }

        // Determine who's to the left of PB

        ArrayList<LatLng> leftSetPB = new ArrayList<LatLng>();
        for (int i = 0; i < set.size(); i++) {
            LatLng M = set.get(i);
            if (pointLocation(P, B, M) == 1) {
                leftSetPB.add(M);
            }
        }
        hullSet(A, P, leftSetAP, hull);
        hullSet(P, B, leftSetPB, hull);
    }

    public double pointLocation(LatLng A, LatLng B, LatLng P) {
        double cp1 = (B.getLongitude() - A.getLongitude()) * (P.getLatitude() - A.getLatitude()) - (B.getLatitude() - A.getLatitude()) * (P.getLongitude() - A.getLongitude());
        if (cp1 > 0)
            return 1;
        else if (cp1 == 0)
            return 0;
        else
            return -1;
    }


    public LatLng computeCentroid(ArrayList<LatLng> vertices) {
        double tempLat = 0;
        double tempLon = 0;

        for (LatLng i : vertices) {
            tempLat += i.getLatitude();
            tempLon += i.getLongitude();
        }
        return new LatLng(tempLat / vertices.size(), tempLon / vertices.size());
    }


    public ArrayList<ArrayList<LatLng>> createSmallerPolygons(ArrayList<LatLng> vertices) {

        centroid = computeCentroid(vertices);
        ArrayList<LatLng> pointToCenter = new ArrayList<LatLng>();
        for (LatLng i : vertices) {
            pointToCenter.add(new LatLng(i.getLatitude() - centroid.getLatitude(), i.getLongitude() - centroid.getLongitude()));
        }
        ArrayList<ArrayList<LatLng>> spirals = new ArrayList<ArrayList<LatLng>>();
        for (double i = 1; i >= 0; i -= .1) {
            ArrayList<LatLng> points = new ArrayList<LatLng>();
            for (LatLng p : pointToCenter) {
                points.add(new LatLng(centroid.getLatitude() + p.getLatitude() * i, centroid.getLongitude() + p.getLongitude() * i));
            }
            spirals.add(points);
        }
        return spirals;
    }

    public LatLng getCentroid() {
        return centroid;
    }

    public double computeDistance(LatLng firstPoint, LatLng secondPoint) {
        double x = Math.pow((secondPoint.getLatitude() - firstPoint.getLatitude()), 2);
        double y = Math.pow((secondPoint.getLongitude() - firstPoint.getLongitude()), 2);
        return Math.sqrt(x + y);
    }

    public double calculateLength(LatLng vector) {
        return Math.sqrt(Math.pow(vector.getLatitude(), 2) + Math.pow(vector.getLongitude(), 2));
    }

    public LatLng normalizeVector(LatLng vector) {
        double distance = calculateLength(vector);
        return new LatLng(vector.getLatitude() / distance, vector.getLongitude() / distance);
    }

    public boolean isNonAdjacentLessThan10Meters(ArrayList<LatLng> verts) {

        //since all points in triangle are adjacent
        if (verts.size() == 3) {

            // System.out.println("points 0 and 1" + verts.get(0) + " " + verts.get(1) + " distance: " + comput// eDistance(verts.get(0),verts.get(1)));
            // // System.out.println("points 0 and 2" + verts.get(0) + " " + verts.get(2) + " distance: " + computeDistance(verts.get(0),verts.get(2)));
            // // System.out.println("points 1 and 2" +
            // verts.get(1) + " " + verts.get(2) + " distance: " +
            // computeDistance(verts.get(1),verts.get(2)));

            // System.out.println("distance between points 0 and 1: " + computeDistance(verts.get(0),verts.get(1)));
            // System.out.println("distance between points 0 and 2: " + computeDistance(verts.get(0),verts.get(2)));
            // System.out.println("distance between points 0 and 3: " + computeDistance(verts.get(1),verts.get(2)));


            //              System.out.println();
            if (computeDistance(verts.get(0), verts.get(1)) < MAXDISTFROMSIDE) {
                return true;
            } else if (computeDistance(verts.get(0), verts.get(2)) < MAXDISTFROMSIDE) {
                return true;
            } else if (computeDistance(verts.get(1), verts.get(2)) < MAXDISTFROMSIDE) {
                return true;
            }
            return false;
        }

        for (int i = 0; i < verts.size(); i++) {
            for (int p = i + 2; p < verts.size(); p++) {
                //case where the first and last point (adjacent are
                //being compared

                //System.out.println("size" + (verts.size()-1));
                if (i == 0 && p == verts.size() - 1) {
                    continue;
                }
                //  System.out.println(i + " " + p + "\n");
                //                      System.out.println(computeDistance(verts.get(i),verts.get(p)));
                if (computeDistance(verts.get(i), verts.get(p)) < 2 * MAXDISTFROMSIDE) {
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
        System.out.println(!isNonAdjacentLessThan10Meters(spirals.get(spirals.size() - 1)));
        while (!isNonAdjacentLessThan10Meters(spirals.get(spirals.size() - 1))) {

            centroid = computeCentroid(spirals.get(spirals.size() - 1));
            centers.add(centroid);
            ArrayList<LatLng> pointToCenter = new ArrayList<LatLng>();
            //normalized vectors from vertex to center
            for (LatLng it : spirals.get(spirals.size() - 1)) {
                pointToCenter.add(normalizeVector(new LatLng(it.getLatitude() - centroid.getLatitude(), it.getLongitude() - centroid.getLongitude())));
            }

            //the last layer added
            ArrayList<LatLng> previousPolygon = spirals.get(spirals.size() - 1);
            //upcoming layer
            ArrayList<LatLng> nextPolygon = new ArrayList<LatLng>();

            for (LatLng p : previousPolygon) {
                LatLng temp = new LatLng(centroid.getLatitude() - p.getLatitude(), centroid.getLongitude() - p.getLongitude());
                //System.out.println("dist " + calculateLength(temp));
                if (calculateLength(temp) < MAXDISTFROMSIDE) {
//                    System.out.println("centroid");
//                    System.out.print("centx = [");
//                    for (LatLng t : centers)
//                    {
//                        System.out.print(t.getLatitude()+",");
//                    }

//                    System.out.print("]\n\ncenty=[");
//                    for (LatLng t : centers)
//                    {
////                        System.out.print(t.getLongitude()+",");
//                    }
//                    System.out.print("]\n\n");

                    return spirals;
                }
            }

            //for (LatLng p : pointToCenter)
            for (int t = 0; t < pointToCenter.size(); t++) {
                LatLng p = pointToCenter.get(t);
                LatLng temp = new LatLng(centroid.getLatitude() - p.getLatitude(), centroid.getLongitude() - p.getLongitude());
                if (calculateLength(temp) < MAXDISTFROMSIDE) {
                    continue;
                }

                nextPolygon.add(new LatLng(previousPolygon.get(t).getLatitude() - pointToCenter.get(t).getLatitude() * SUBTRACTDIST, previousPolygon.get(t).getLongitude() - pointToCenter.get(t).getLongitude() * SUBTRACTDIST));
            }
            //System.out.println(nextPolygon.size());
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

    public static void main1(String args[]) {
        System.out.println("\nQuick Hull Test \n");

        ArrayList<LatLng> points = new ArrayList<LatLng>();


        //these works
        // LatLng point0 = new LatLng(-2,-2);
        // LatLng point1 = new LatLng(-2,2);
        // LatLng point2 = new LatLng(2,2);
        // LatLng point3 = new LatLng(2,-2);


        //these dont
        LatLng point0 = new LatLng(-15, 145);
        LatLng point1 = new LatLng(3, 94);
        LatLng point2 = new LatLng(35, 113);
        LatLng point3 = new LatLng(15, 200);
        LatLng point4 = new LatLng(-10, 200);

        points.add(point0);
        points.add(point1);
        points.add(point2);
        //points.add(point3);
        //points.add(point4);

        PolyArea qh = new PolyArea();
        ArrayList<LatLng> p = qh.quickHull(points);

        //System.out.println(qh.isNonAdjacentLessThan10Meters(qh.vertices));

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

    public double findInteriorAngle(LatLng a, LatLng b) {
        //System.out.println(180/Math.PI*Math.acos(dot(a,b)/(calculateLength(a)*calculateLength(b))));
        // System.out.println("a " + calculateLength(a));
        // System.out.println("b " + calculateLength(b));
        // System.out.println("a.b: " + dot(a,b));
        // System.out.println("dot(a,b)/lengtha*lengthb : " + Math.acos(dot(a,b)/(calculateLength(a)*calculateLength(b
        //                                                                                                                ))));
        double dot = dot(a, b);
        double cross = a.getLatitude() * b.getLongitude() - a.getLongitude() * b.getLatitude();
        double output = Math.atan2(dot, cross);
        if (output < 0) {
            output += Math.PI;
        }
        if (output == 0) {
            output = Math.PI / 2;
        }
        // if (output > Math.PI/2)
        // {
        //  output = Math.PI - output;
        // }
        //return output;
        return Math.acos(dot(a, b) / (calculateLength(a) * calculateLength(b)));
    }

    public LatLng findBisectNormal(LatLng a, LatLng b) {
        a = normalizeVector(a);
        b = normalizeVector(b);
        LatLng added = add(a, b);
        return normalizeVector(added);
    }

    public double dot(LatLng a, LatLng b) {
        return a.getLatitude() * b.getLatitude() + a.getLongitude() * b.getLongitude();
    }

    public LatLng add(LatLng a, LatLng b) {
        return new LatLng(a.getLatitude() + b.getLatitude(), a.getLongitude() + b.getLongitude());
    }

    public LatLng subtract(LatLng a, LatLng b) {
        return new LatLng(a.getLatitude() - b.getLatitude(), a.getLongitude() - b.getLongitude());
        //return new LatLng(b.getLatitude() - a.getLatitude(),b.getLongitude() - a.getLongitude());
    }

    public LatLng findVector(LatLng a, LatLng b) {
        return subtract(a, b);
    }

    public LatLng multiply(LatLng a, double amount) {
        return new LatLng(a.getLatitude() * amount, a.getLongitude() * amount);
    }

    /*This works by finding the bisecting vector of each angle and
     * moving along the secting vector of distance dist/sin(angle/2) */
    public ArrayList<ArrayList<LatLng>> computeSpiralsPolygonOffset(ArrayList<LatLng> polygon) {
        ArrayList<ArrayList<LatLng>> spirals = new ArrayList<ArrayList<LatLng>>();
        spirals.add(polygon); //add first polygon
        if (polygon.size() <= 2) {
            System.out.println("poly size <= 2 is: " + polygon.size());
            return spirals;
        }


        //compute all of the bisecting vectors note these look wrong
        int counter = 0;
        //while(counter < 10)
        while (!isNonAdjacentLessThan10Meters(spirals.get(spirals.size() - 1))) {
            //System.out.println("while");
            counter++;
            //System.out.println(spirals.get(spirals.size()-1).size());
            //Last Polygon to be added
            ArrayList<LatLng> lastSpiral = spirals.get(spirals.size() - 1);
            //Comput the centroid of the last spiral
            LatLng centroid = computeCentroid(lastSpiral);
            //Check to see if any points are less than
            //subtractdist from the centroid, if so return(this is
            //a stopping point)
            //oSystem.out.println(lastSpiral);
            for (LatLng p : lastSpiral) {
                LatLng temp = new LatLng(centroid.getLatitude() - p.getLatitude(), centroid.getLongitude() - p.getLongitude());
                //System.out.println("dist " + calculateLength(temp));
                //System.out.println(temp);
                if (calculateLength(temp) < SUBTRACTDIST) {
                    // System.out.println(lastSpiral);
                    // System.out.println(computeCentroid(spirals.get(spirals.size()-2)));
                    //System.out.println("poly area center close");
                    return spirals;
                }
            }

            //compute all of the vectors that make the edges
            ArrayList<LatLng> edgeVectors = new ArrayList<LatLng>();
            for (int i = 0; i < lastSpiral.size() - 1; i++) {
                edgeVectors.add(findVector(lastSpiral.get(i), lastSpiral.get(i + 1)));
            }
            edgeVectors.add(findVector(lastSpiral.get(lastSpiral.size() - 1), lastSpiral.get(0)));
            //System.out.println(edgeVectors);
            //            System.out.println(edgeVectors);
            //Compute all of the angles between these edges
            //System.out.println("points");
            //System.out.println(lastSpiral);
            ArrayList<Double> interiorAngles = new ArrayList<Double>();
            for (int i = 0; i < lastSpiral.size() - 1; i++) {
                LatLng v;
                LatLng u;
                if (i == 0) {
                    v = subtract(lastSpiral.get(i), lastSpiral.get(lastSpiral.size() - 1));
                    u = subtract(lastSpiral.get(i), lastSpiral.get(i + 1));
                    interiorAngles.add(findInteriorAngle(v, u));
                    // System.out.println("u " + u);
                    // System.out.println("v " + v);
                    // System.out.println("0 Angle " + findInteriorAngle(v,u)*180/Math.PI);
                    continue;
                }
                v = subtract(lastSpiral.get(i), lastSpiral.get(i + 1));
                u = subtract(lastSpiral.get(i), lastSpiral.get(i - 1));
                interiorAngles.add(findInteriorAngle(v, u));
                // System.out.println("u " + u);
                // System.out.println("v " + v);
                // System.out.println(i + " Angle " + findInteriorAngle(v,u)*180/Math.PI);

            }
            LatLng v1 = subtract(lastSpiral.get(lastSpiral.size() - 1), lastSpiral.get(0));
            LatLng u1 = subtract(lastSpiral.get(lastSpiral.size() - 1), lastSpiral.get(lastSpiral.size() - 2));
            interiorAngles.add(findInteriorAngle(v1, u1));
            // System.out.println("u " + u1);
            // System.out.println("v " + v1);
            // System.out.println("Angle last" + (findInteriorAngle(v1,u1)*180/Math.PI));


            // for (int i = 0; i < edgeVectors.size()-1; i++)
            //  {
            //      interiorAngles.add(findInteriorAngle(edgeVectors.get(i),edgeVectors.get(i+1)));
            //  }
            // interiorAngles.add(findInteriorAngle(edgeVectors.get(0),edgeVectors.get(edgeVectors.size()-1)));

            // System.out.println("points");
            // System.out.println(lastSpiral);
            // System.out.println("edges");
            // System.out.println(edgeVectors);
            // System.out.println("Angles");
            // for (Double i : interiorAngles)
            // {
            //     System.out.println(i*180/Math.PI);
            // }
            // System.out.println("");

            ArrayList<LatLng> bisectingVectors = new ArrayList<LatLng>();
            for (int i = 0; i < lastSpiral.size() - 1; i++) {
                LatLng v;
                LatLng u;
                if (i == 0) {
                    v = subtract(lastSpiral.get(lastSpiral.size() - 1), lastSpiral.get(i));
                    u = subtract(lastSpiral.get(i + 1), lastSpiral.get(i));
                    bisectingVectors.add(findBisectNormal(v, u));
                    continue;
                }
                v = subtract(lastSpiral.get(i + 1), lastSpiral.get(i));
                u = subtract(lastSpiral.get(i - 1), lastSpiral.get(i));
                bisectingVectors.add(findBisectNormal(v, u));
            }
            LatLng v = subtract(lastSpiral.get(0), lastSpiral.get(lastSpiral.size() - 1));
            LatLng u = subtract(lastSpiral.get(lastSpiral.size() - 2), lastSpiral.get(lastSpiral.size() - 1));
            bisectingVectors.add(findBisectNormal(v, u));

            //p = p- dist
            //finds vector between the point and the bisecting
            //vector with length dist/math.sin(angle/2)
            ArrayList<LatLng> nextPolygon = new ArrayList<LatLng>();
            for (int i = 0; i < lastSpiral.size(); i++) {
                //LatLng point =
                //spirals.get(spirals.size()-1).get(i);
                LatLng point = lastSpiral.get(i);
                //add the vector between the original point
                //and the point subtracted by the bisecting
                //vector with length
                //subtractdist/math.sin(interiorangle/2)
                //System.out.println("dist" +
                //SUBTRACTDIST/Math.sin(interiorAngles.get(i)/2));
                //System.out.println(interiorAngles.get(i)*180/Math.PI);
                LatLng nextPoint = multiply(bisectingVectors.get(i), -1 * SUBTRACTDIST / Math.sin(interiorAngles.get(i) / 2));

                //System.out.println(point + " " + nextPoint + " " + findVector(point,nextPoint));
                nextPolygon.add(findVector(point, nextPoint));
            }

            //check intersections
            //if so remove one points and set the other as the avergae
            //of both
            for (int i = 0; i < nextPolygon.size() - 1; i++) {
                LatLng lastPoint0 = lastSpiral.get(i);
                LatLng nextPoint0 = nextPolygon.get(i);
                LatLng lastPoint1 = lastSpiral.get(i + 1);
                LatLng nextPoint1 = nextPolygon.get(i + 1);
                if (linesIntersect(lastPoint0.getLatitude(), lastPoint0.getLongitude(), nextPoint0.getLatitude(), nextPoint0.getLongitude(), lastPoint1.getLatitude(), lastPoint1.getLongitude(), nextPoint1.getLatitude(), nextPoint1.getLongitude())) {
                    //System.out.println("Removed vertex : " + i + " from polygon: " + spirals.size());
                    LatLng average = new LatLng((nextPoint0.getLatitude() + nextPoint1.getLatitude()) / 2, (nextPoint0.getLongitude() + nextPoint1.getLongitude()) / 2);
                    nextPolygon.set(i + 1, average);
                    nextPolygon.remove(i);

                }
            }
            //System.out.println(nextPolygon.size());
            //spirals.add(quickHull(nextPolygon));
            spirals.add((nextPolygon));
        }
        return spirals;
    }

    // check for intersection between bisects
    // if the two will intersect in the next spira
    // meaning the points will "swap place"
    // merge points at average? or something
    // if not the shape changes and the angles change drastically
    // causing overlap
    //check if intersect is between area defined by old points and new points
    public static void main(String[] args) {
        ArrayList<LatLng> points = new ArrayList<LatLng>();

        // LatLng point0 = new LatLng(5, 5); //2
        // LatLng point1 = new LatLng(4, 1); //1
        // LatLng point2 = new LatLng(0, 2);


        LatLng point0 = new LatLng(1, 1); //3
        LatLng point1 = new LatLng(0, 1); //2
        LatLng point2 = new LatLng(0, 0); //1
        LatLng point3 = new LatLng(1, 0);


        points.add(point0);
        points.add(point1);
        points.add(point2);
        points.add(point3);


        //points.add(point3);


        PolyArea qh = new PolyArea();
        ArrayList<LatLng> output = qh.getPoints(points,AreaType.LAWNMOWER);
        //ArrayList<LatLng> output = qh.getPoints(points,AreaType.SPIRAL);
        qh.print(output);
    }

    //http://www.java2s.com/Code/Android/Game/TestsifthelinesegmentfromX1nbspY1toX2nbspY2intersectsthelinesegmentfromX3nbspY3toX4nbspY4.htm
    public static boolean linesIntersect(final double X1, final double Y1, final double X2, final double Y2, final double X3, final double Y3, final double X4, final double Y4) {
        return ((relativeCCW(X1, Y1, X2, Y2, X3, Y3)
                * relativeCCW(X1, Y1, X2, Y2, X4, Y4) <= 0) && (relativeCCW(X3,
                Y3, X4, Y4, X1, Y1)
                * relativeCCW(X3, Y3, X4, Y4, X2, Y2) <= 0));
    }

    private static double relativeCCW(final double X1, final double Y1, double X2, double Y2, double PX,
                                      double PY) {
        X2 -= X1;
        Y2 -= Y1;
        PX -= X1;
        PY -= Y1;
        double ccw = PX * Y2 - PY * X2;
        if (ccw == 0) {
            // The point is colinear, classify based on which side of
            // the segment the point falls on. We can calculate a
            // relative value using the projection of PX,PY onto the
            // segment - a negative value indicates the point projects
            // outside of the segment in the direction of the particular
            // endpoint used as the origin for the projection.
            ccw = PX * X2 + PY * Y2;
            if (ccw > 0) {
                // Reverse the projection to be relative to the original X2,Y2
                // X2 and Y2 are simply negated.
                // PX and PY need to have (X2 - X1) or (Y2 - Y1) subtracted
                // from them (based on the original values)
                // Since we really want to get a positive answer when the
                // point is "beyond (X2,Y2)", then we want to calculate
                // the inverse anyway - thus we leave X2 & Y2 negated.
                PX -= X2;
                PY -= Y2;
                ccw = PX * X2 + PY * Y2;
                if (ccw < 0) {
                    ccw = 0;
                }
            }
        }
        return (ccw < 0) ? -1 : ((ccw > 0) ? 1 : 0);
    }

    public void doAdjacentLinesIntersect(ArrayList<LatLng> last, ArrayList<LatLng> next) {
        for (int i = 0; i < last.size() - 1; i++) {
            LatLng lastPoint0 = last.get(i);
            LatLng nextPoint0 = next.get(i);
            LatLng lastPoint1 = last.get(i + 1);
            LatLng nextPoint1 = next.get(i + 1);
            if (linesIntersect(lastPoint0.getLatitude(), lastPoint0.getLongitude(), nextPoint0.getLatitude(), nextPoint0.getLongitude(), lastPoint1.getLatitude(), lastPoint1.getLongitude(), nextPoint1.getLatitude(), nextPoint1.getLongitude())) {
                System.out.println("intersecting bisect at: " + i + " " + (i + 1));
            }
        }
    }

    //this is not accurate since earth isnt flat.

    public void updateTransect(double submeters) {
//        System.out.printf("subm  %f\n", submeters);
//        System.out.printf("sub10  %f\n", submeters * ONE_METER * 2);
//        System.out.printf("sub5  %f\n", submeters * ONE_METER);
//        System.out.printf("sub1  %f\n", ONE_METER);
//        System.out.printf("current max %f\n", MAXDISTFROMSIDE);
        MAXDISTFROMSIDE = submeters * ONE_METER * 2;
        SUBTRACTDIST = MAXDISTFROMSIDE / 2;
    }


    public static Object[] getLawnmowerPath(ArrayList<LatLng> area, double stepSize) {

        // Compute the bounding box
        area.add(area.get(0)); //adds first point to end so the final vector can be computed...
        double minLat = 360;
        double maxLat = -360;
        double minLon = 360;
        double maxLon = -360;
        Double curLat = null;
        System.out.println(area);
        for (LatLng latLon : area) {  //get list of points
            if (latLon.getLatitude() > maxLat) { //if latlong.getLatitude()...
                maxLat = latLon.getLatitude();
            } else if (latLon.getLatitude() < minLat) {
                minLat = latLon.getLatitude();
            }
            if (latLon.getLongitude() > maxLon) {
                maxLon = latLon.getLongitude();
            } else if (latLon.getLongitude() < minLon) {
                minLon = latLon.getLongitude();
            }
        }
        curLat = minLat;
        System.out.println(curLat);
        double totalLength = 0.0;
        Double leftLon = null, rightLon = null; //locations
        ArrayList<LatLng> path = new ArrayList<LatLng>(); //can just
        //be latlng
        while (curLat <= maxLat) {
            // Left to right
            leftLon = getMinLonAt(area, minLon, maxLon, curLat);
            rightLon = getMaxLonAt(area, minLon, maxLon, curLat);
            if (leftLon != null && rightLon != null) {
                path.add(new LatLng(curLat, leftLon));
                path.add(new LatLng(curLat, rightLon));
                totalLength += Math.abs((rightLon - leftLon) * LON_D_PER_M);
            } else {
                System.out.println("null");
            }
            // Right to left
            curLat = curLat+stepSize;
            if (curLat <= maxLat) {
                totalLength += stepSize;
                rightLon = getMaxLonAt(area, minLon, maxLon, curLat);
                leftLon = getMinLonAt(area, minLon, maxLon, curLat);
                if (leftLon != null && rightLon != null) {
                    path.add(new LatLng(curLat, rightLon));
                    path.add(new LatLng(curLat, leftLon));
                    totalLength += Math.abs((rightLon - leftLon) * LON_D_PER_M);
                } else {
                    System.out.println("null");
                }
            }
            curLat = curLat + stepSize;
            if (curLat <= maxLat) {
                totalLength += stepSize;
            }
        }

        return new Object[]{path, totalLength};
    }
    public static Double getMinLonAt(ArrayList<LatLng> area, double minLon, double maxLon, double lat) {
        final double lonDiff = 1.0 / 90000.0 * 10.0;
        LatLng latLon = new LatLng(lat, minLon);
        while (!isLocationInside(latLon, (ArrayList<LatLng>) area) && minLon <= maxLon) {
            minLon = minLon + lonDiff;
            latLon = new LatLng(lat, minLon);
            if (minLon > maxLon) {
                // Overshot (this part of the area is tiny), so ignore it by returning null
                return null;
            }
        }
        return minLon;
    }

    private static Double getMaxLonAt(ArrayList<LatLng> area, double minLon, double maxLon, double lat) {
        final double lonDiff = 1.0 / 90000.0 * 10.0;
        LatLng latLon = new LatLng(lat, maxLon);
        while (!isLocationInside(latLon, (ArrayList<LatLng>) area)) {
            maxLon = maxLon-lonDiff;
            latLon = new LatLng(lat, maxLon);
            if (maxLon < minLon) {
                // Overshot (this part of the area is tiny), so ignore it by returning null
                return null;
            }
        }
        return maxLon;
    }
    public static boolean isLocationInside(LatLng point, ArrayList<? extends LatLng> positions) {
        boolean result = false;
        LatLng p1 = positions.get(0);
        for (int i = 1; i < positions.size(); i++) {
            LatLng p2 = positions.get(i);

            if (((p2.getLatitude() <= point.getLatitude()
                    && point.getLatitude() < p1.getLatitude())
                    || (p1.getLatitude() <= point.getLatitude()
                    && point.getLatitude() < p2.getLatitude()))
                    && (point.getLongitude() < (p1.getLongitude() - p2.getLongitude())
                    * (point.getLatitude() - p2.getLatitude())
                    / (p1.getLatitude() - p2.getLatitude()) + p2.getLongitude())) {
                result = !result;
            }
            p1 = p2;
        }
        return result;
    }
    public static ArrayList<LatLng> orderCCWUpRight(ArrayList<LatLng> list)
    {
        if (list.size() < 3)
        {
            return list;
        }
        //look for most left up point
        double minLat = 360;
        double maxLat = -360;
        double minLon = 360;
        double maxLon = -360;
        Double curLat = null;
        for (LatLng latLon : list) {  //get list of points
            if (latLon.getLatitude() > maxLat) { //if latlong.getLatitude()...
                maxLat = latLon.getLatitude();
            } else if (latLon.getLatitude() < minLat) {
                minLat = latLon.getLatitude();
            }
            if (latLon.getLongitude() > maxLon) {
                maxLon = latLon.getLongitude();
            } else if (latLon.getLongitude() < minLon) {
                minLon = latLon.getLongitude();
            }
        }
        curLat = minLat;
//        System.out.print(maxLat);
//        System.out.print(",");
//        System.out.print(maxLon);
//        System.out.println("");
        ArrayList<LatLng> reordered = new ArrayList<LatLng>();
        int start = -1;
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).equals(new LatLng(maxLat,maxLon)))
            {
                start = i;
//                System.out.println(i);
                break;
            }
        }
        for (int i = start; i < list.size(); i++)
        {
            reordered.add(list.get(i));
        }
        for (int i = 0; i < start; i++)
        {
            reordered.add(list.get(i));
        }
        //      System.out.println(reordered);
        return reordered;
    }
    public ArrayList<LatLng> getPoints(ArrayList<LatLng> area, AreaType type)
    {

        ArrayList<LatLng> flatList = new ArrayList<LatLng>();
        if (type == AreaType.LAWNMOWER)
        {
            Object[] output = getLawnmowerPath(area,SUBTRACTDIST);
            flatList = (ArrayList<LatLng>)output[0];
        }
        if (type == AreaType.SPIRAL)
        {
            area = quickHull(area);
            ArrayList<ArrayList<LatLng>> spirals = computeSpiralsPolygonOffset(vertices);
            for (ArrayList<LatLng> a : spirals)
            {
                for (LatLng p : a)
                {
                    flatList.add(p);
                }
            }
        }
        return flatList;
    }
    public void print(ArrayList<LatLng> area)
    {
        System.out.print("x=[");
        for (LatLng a : area)
        {
            System.out.print(a.getLatitude() + ",");
        }
        System.out.print("]");
        System.out.println("\n");
        System.out.print("y=[");
        for (LatLng a : area)
        {
            System.out.print(a.getLongitude() + ",");
        }
        System.out.print("]");
        System.out.println("");
        System.out.println("plot(x,y)");
    }
}
/*
 */
