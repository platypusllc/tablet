package com.platypus.android.tablet;

/**
 * Created by shenty on 2/3/16.
 */

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngZoom;

/* note
* Should not use yet for following reasons
* Points have to be in CLOCKWISE order (TODO)
* If polygon intersects itself wont work
* */



public class PolyAreaOld
{
    private List<LatLng> vertices; //vertex
    private List<LatLng> vectors;  //lines (vectors)
    private boolean convex;
    private LatLng centroid;
    private boolean cw;

    public PolyAreaOld(List<LatLng> polyPointList)
    {
        vertices = polyPointList;
        vectors = generateVectors(vertices); //compute vectors
        convex = isConvex(vectors); //cross each vector check sign,
        centroid = computeCentroid();
        //determine if convex or not

    }
    public ArrayList<LatLng> generateVectors(List<LatLng> polyPointList)
    {
        ArrayList<LatLng> temp = new ArrayList<LatLng>();
        //point = {x,y}
        for (int i = 0; i < polyPointList.size()-1; i++)
        {
            double pointx = -polyPointList.get(i).getLatitude() + polyPointList.get(i+1).getLatitude();
            double pointy = -polyPointList.get(i).getLongitude() + polyPointList.get(i+1).getLongitude() ;
            temp.add(new LatLng(pointx,pointy));
        }
        double pointx =  polyPointList.get(0).getLatitude()- polyPointList.get(polyPointList.size()-1).getLatitude();
        double pointy =  polyPointList.get(0).getLongitude() - polyPointList.get(polyPointList.size()-1).getLongitude();
        temp.add(new LatLng(pointx,pointy));
        return temp;
    }

    //POINTS HAVE TO BE IN ORDER (clockwise) :|
    public boolean isConvex(List<LatLng> vectors)
    {
        boolean neg = false;
        boolean pos = false;;
        for (int i = 0; i < vectors.size()-1; i++)
        {
            LatLng vector1 = vectors.get(i);
            LatLng vector2 = vectors.get(i+1);
            double tempcross = crossZ(vector1,vector2);
            if (tempcross > 0)
            {
                pos = true;
            }
            else
            {
                neg = true;
            }
        }
        LatLng vector1 = vectors.get(0);
        LatLng vector2 = vectors.get(vectors.size()-1);
        double tempcross = crossZ(vector2,vector1);
        if (tempcross > 0)
        {
            pos = true;
        }
        else
        {
            neg = true;
        }
        if (pos  == true && neg == true)
        {
            convex = false;
            return false;
        }
        cw = neg;
        convex = true;
        return true;
    }

    //if all in clockwise order all cross product signs should be negative
    public boolean isConcaveAngle (LatLng vector1, LatLng vector2)
    {

        if (cw == true && crossZ(vector1,vector2) > 0)
        {
            return true;
        }
        if (cw == false && crossZ(vector1,vector2) < 0)
        {
            return true;
        }
        return false;
    }

    /* get z component of cross product */
    public static double crossZ(LatLng u, LatLng v)
    {
        return u.getLatitude()*v.getLongitude() - v.getLatitude()*u.getLongitude();
    }


    //Doesnt always remove the wanted point since it always orders them first
    //Find last point in the list that was added before organizing, shift all elements over then run
    public void makeConvex()
    {
        orderCW();
        while(isConvex(vectors) == false)
        {

            for (int i = 0; i < vectors.size()-1; i++)
            {
                if (i == vectors.size()-2)
                {
                    if(isConcaveAngle(vectors.get(vectors.size()-1),vectors.get(0)) == true)
                    {
                        System.out.println("Removing vector between" + vectors.get(0) + " " + vectors.get(7));
                        vertices.remove(0);
                        vectors = generateVectors(vertices);
                        break;
                    }
                }

                if(isConcaveAngle(vectors.get(i),vectors.get(i+1)) == true)
                {
                    System.out.println("Removing point between" + vertices.get(i+1));
                    vertices.remove(i+1); //remove vertex
                    vectors = generateVectors(vertices); //recreate
                    //new vectors
                    break;
                }

            }
        }
        convex = isConvex(vectors);

    }



    public void printVertices()
    {
        for (LatLng i : vertices)
        {
            System.out.println(i.getLatitude() + " " + i.getLongitude());
        }
    }
    public LatLng computeCentroid()
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

    public boolean less(LatLng point1, LatLng point2)
    {
        centroid = computeCentroid();
        double det = (point1.getLatitude() - centroid.getLatitude()) * (point2.getLongitude() - centroid.getLongitude()) - (point2.getLatitude() - centroid.getLatitude()) * (point1.getLongitude() - centroid.getLongitude());
        if (det < 0)
            return true;
        else
            return false;
    }
    public void orderCW()
    {
        Map<Double,Integer> map = new TreeMap<Double,Integer>();
        for (int j = 0; j < vertices.size(); j++)
        {
            LatLng i = vertices.get(j);
            Double polar = Math.atan2(i.getLatitude()-centroid.getLatitude(),i.getLongitude()-centroid.getLongitude());
            map.put(polar,j);
        }

        List<LatLng> ordered = new ArrayList<LatLng>(vertices.size());
        for (Double key : map.keySet())
        {
            ordered.add(vertices.get(map.get(key)));
        }
        vertices = new ArrayList(ordered);
        vectors = generateVectors(vertices);
    }
    public void printPolar()
    {
        centroid = computeCentroid();
        for (LatLng i : vertices)
        {
            System.out.println(Math.atan2(i.getLatitude()-centroid.getLatitude(),i.getLongitude()-centroid.getLongitude()));
        }
    }
    public List<LatLng> getVertices()
    {
        return vertices;
    }
    public List<LatLng> getVectors()
    {
        return vectors;
    }
    public LatLng getCentroid()
    {
        return centroid;
    }

    /* Returns a list of paths that reduce in size by 10% each iteration */
    /* example on how to use


        Polygon poly = new Polygon(points);
		poly.orderCW();
		poly.makeConvex();
		for (ArrayList<LatLng> i : poly.createSmallerPolygons())
		{
			System.out.println(i);
		}

     */
    public ArrayList<ArrayList<LatLng>> createSmallerPolygons()
    {
        centroid = computeCentroid();
        List<LatLng> pointToCenter = new ArrayList<LatLng>();
        for (LatLng i : vertices)
        {
            pointToCenter.add(new LatLng(i.getLatitude()-centroid.getLatitude(),i.getLongitude()-centroid.getLongitude()));
        }
        ArrayList<ArrayList<LatLng>> spirals = new ArrayList<ArrayList<LatLng>>();
        for (double i = .1; i <= 1; i+=.1)
        {
            ArrayList<LatLng> points = new ArrayList<LatLng>();
            for (LatLng p : pointToCenter)
            {
                points.add(new LatLng(p.getLatitude()*i,p.getLongitude()*i));
            }
        }
        return spirals;
    }
}

