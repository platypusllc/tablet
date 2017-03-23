package com.platypus.android.tablet.Path;

import java.util.ArrayList;

import com.mapbox.mapboxsdk.geometry.LatLng;

//TODO wtf is causing the random lines across the polygon that occur in spiral mode.
//TODO ok caused by the previous polygon has points that get added for some reason
public class Region extends Path
{

//  private double transectDistance = .1; //testing
private final double ONEMETER = transectDistance/10;
  AreaType regionType;// = AreaType.SPIRAL;
  private static final double LON_D_PER_M = 1.0 / 90000.0;
  private static final double LAT_D_PER_M = 1.0 / 110000.0;


	//Dont quickhull points, keep them origina
    private ArrayList<LatLng> originalPoints = new ArrayList<LatLng>();
  //Points will be the original points																																	 
  private ArrayList<LatLng> regionPoints = new ArrayList<LatLng>();

  public Region(ArrayList<LatLng> list, AreaType type) {
    setPoints(list);
    originalPoints = points;
    regionType = type;
    updateRegionPoints();

  }
  public Region(ArrayList<LatLng> list, AreaType type, Double transect) {
    setPoints(list);
    originalPoints = points;
    regionType = type;
    transectDistance = transect*ONE_METER;
    updateRegionPoints();
  }

  public Region(Path path)
  {
    points = path.getPoints();
    originalPoints = points;
    regionType = AreaType.SPIRAL;
    updateRegionPoints();
  }

  public void setAreaType(AreaType type)
  {
    regionType = type;
    updateRegionPoints();
  }
  public AreaType getAreaType()
  {
    return regionType;
  }

  /*
    Any time you call setPoints or add/removePoint/clear the new
    regionPoints is generated.
  */
  public void updateTransect(double submeters)
  {
    transectDistance = submeters*ONEMETER;
    updateRegionPoints();
  }

  public ArrayList<LatLng> getPoints()
  {
    return regionPoints;
  }
  public ArrayList<LatLng> getOriginalPoints()
  {
    return originalPoints;
  }

  public void updateRegionPoints()
  {
    regionPoints.clear();
    if (points.size() == 0)
    {
      return;
    }
    if (regionType == AreaType.SPIRAL)
    {
      quickHull();
      ArrayList<ArrayList<LatLng>> spiralPath = computeSpiralsPolygonOffset();
      for (ArrayList<LatLng> a : spiralPath)
      {
        for (LatLng p : a)
        {
          regionPoints.add(p);
        }
      }
        regionPoints.remove(regionPoints.size()-1);
        outputPointsToOctave("sp","\"b\"");
    }
    if (regionType == AreaType.LAWNMOWER)
    {
      quickHull();
      getLawnmowerPath(transectDistance/2);
      //getLawnmowerPath(10*1/90000);
    }
  }

  public boolean isEmpty()
  {
    return this.points.isEmpty() || regionPoints.isEmpty();
  }

  public void quickHull()
  {
    regionPoints = new ArrayList<LatLng>(points);
    ArrayList<LatLng> convexHull = new ArrayList<LatLng>();
    if (points.size() < 3) {
      //points = points..
      return;
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
    regionPoints.remove(A);
    regionPoints.remove(B);

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
    points = new ArrayList<LatLng>(convexHull);
      quickHulledPoints = new ArrayList<LatLng>(points); //im such a bad programmer :(
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

  public double distance(LatLng A, LatLng B, LatLng C) {
    double ABx = B.getLongitude() - A.getLongitude();
    double ABy = B.getLatitude() - A.getLatitude();
    double num = ABx * (A.getLatitude() - C.getLatitude()) - ABy * (A.getLongitude() - C.getLongitude());
    if (num < 0)
      num = -num;
    return num;
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
      if (computeDistance(verts.get(0), verts.get(1)) < transectDistance) {
        return true;
      } else if (computeDistance(verts.get(0), verts.get(2)) < transectDistance) {
        return true;
      } else if (computeDistance(verts.get(1), verts.get(2)) < transectDistance) {
        return true;
      }
      return false;
    }

    for (int i = 0; i < verts.size(); i++) {
      for (int p = i + 2; p < verts.size(); p++) {
        if (i == 0 && p == verts.size() - 1) {
          continue;
        }
        if (computeDistance(verts.get(i), verts.get(p)) < 2 * transectDistance) {
          return true;
        }
      }
    }

    return false;
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

  public ArrayList <ArrayList<LatLng>> computeSpiralsPolygonOffset() {
    ArrayList<ArrayList<LatLng>> spirals = new ArrayList<ArrayList<LatLng>>();
    //quickHull(); //causing the fuckshit lines everywhere
      spirals.add(points); //add first polygon
    if (points.size() <= 2) {
      System.out.println("poly size <= 2 is: " + points.size());
        return spirals;
    }

    int counter = 0;
      System.out.println("spiral start one polygon");
    while (!isNonAdjacentLessThan10Meters(spirals.get(spirals.size() - 1))) {
      counter++;
      ArrayList<LatLng> lastSpiral = spirals.get(spirals.size() - 1);
      if (lastSpiral.size() < 3)
      {
        return spirals;
      }

      LatLng centroid = computeCentroid(lastSpiral);
      for (LatLng p : lastSpiral) {
        LatLng temp = new LatLng(centroid.getLatitude() - p.getLatitude(), centroid.getLongitude() - p.getLongitude());
        if (calculateLength(temp) < transectDistance/2) {
          return spirals;
        }
      }

      ArrayList<LatLng> edgeVectors = new ArrayList<LatLng>();
      for (int i = 0; i < lastSpiral.size() - 1; i++) {
        edgeVectors.add(findVector(lastSpiral.get(i), lastSpiral.get(i + 1)));
      }
      edgeVectors.add(findVector(lastSpiral.get(lastSpiral.size() - 1), lastSpiral.get(0)));
      ArrayList<Double> interiorAngles = new ArrayList<Double>();
      for (int i = 0; i < lastSpiral.size() - 1; i++) {
        LatLng v;
        LatLng u;
        if (i == 0) {
          v = subtract(lastSpiral.get(i), lastSpiral.get(lastSpiral.size() - 1));
          u = subtract(lastSpiral.get(i), lastSpiral.get(i + 1));
          interiorAngles.add(findInteriorAngle(v, u));
          continue;
        }
        v = subtract(lastSpiral.get(i), lastSpiral.get(i + 1));
        u = subtract(lastSpiral.get(i), lastSpiral.get(i - 1));
        interiorAngles.add(findInteriorAngle(v, u));

      }
      LatLng v1 = subtract(lastSpiral.get(lastSpiral.size() - 1), lastSpiral.get(0));
      LatLng u1 = subtract(lastSpiral.get(lastSpiral.size() - 1), lastSpiral.get(lastSpiral.size() - 2));
      interiorAngles.add(findInteriorAngle(v1, u1));

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

      ArrayList<LatLng> nextPolygon = new ArrayList<LatLng>();
      for (int i = 0; i < lastSpiral.size(); i++) {
        LatLng point = lastSpiral.get(i);
        LatLng nextPoint = multiply(bisectingVectors.get(i), -1 * transectDistance/2 / Math.sin(interiorAngles.get(i) / 2));

        nextPolygon.add(findVector(point, nextPoint));
      }

      for (int i = 0; i < nextPolygon.size() - 1; i++) {
          LatLng lastPoint0 = lastSpiral.get(i);
          LatLng nextPoint0 = nextPolygon.get(i);
          LatLng lastPoint1 = lastSpiral.get(i + 1);
          LatLng nextPoint1 = nextPolygon.get(i + 1);
          if (linesIntersect(lastPoint0.getLatitude(), lastPoint0.getLongitude(), nextPoint0.getLatitude(), nextPoint0.getLongitude(), lastPoint1.getLatitude(), lastPoint1.getLongitude(), nextPoint1.getLatitude(), nextPoint1.getLongitude())) {
              LatLng average = new LatLng((nextPoint0.getLatitude() + nextPoint1.getLatitude()) / 2, (nextPoint0.getLongitude() + nextPoint1.getLongitude()) / 2);
              nextPolygon.set(i + 1, average);
              nextPolygon.remove(i);

          }
      }
        System.out.println("spiral size "+nextPolygon.size());

      spirals.add((nextPolygon));
    }
      return spirals;
  }

  private static double relativeCCW(final double X1, final double Y1, double X2, double Y2, double PX,
                                    double PY) {
    X2 -= X1;
    Y2 -= Y1;
    PX -= X1;
    PY -= Y1;
    double ccw = PX * Y2 - PY * X2;
    if (ccw == 0) {
      ccw = PX * X2 + PY * Y2;
      if (ccw > 0) {
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

//  public void updateTransect(double submeters) {
//    transectDistance = submeters*ONEMETER;
//    updateRegionPoints();
//  }


  public void  getLawnmowerPath(double stepSize) {
    if (points.size() == 0)
    {
      return;
    }
    // Compute the bounding box
    /*Since we have to add the original point to the end we dont
     * want to edit the points arraylist */
    ArrayList<LatLng> area = new ArrayList<LatLng>(points);
    area.add(area.get(0)); //adds first point to end so the final vector can be computed...
    double minLat = 360;
    double maxLat = -360;
    double minLon = 360;
    double maxLon = -360;
    Double curLat = null;

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
    System.out.println("called");
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
        //System.out.println("null");
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
          //System.out.println("null");
        }
      }
      curLat = curLat + stepSize;
      if (curLat <= maxLat) {
        totalLength += stepSize;
      }
    }
    //return new Object[]{path, totalLength};
    regionPoints = path;
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
    ArrayList<LatLng> reordered = new ArrayList<LatLng>();
    int start = -1;
    for (int i = 0; i < list.size(); i++)
    {
      if (list.get(i).equals(new LatLng(maxLat,maxLon)))
      {
        start = i;
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
    return reordered;
  }

  public static boolean linesIntersect(final double X1, final double Y1, final double X2, final double Y2, final double X3, final double Y3, final double X4, final double Y4) {
    return ((relativeCCW(X1, Y1, X2, Y2, X3, Y3)
             * relativeCCW(X1, Y1, X2, Y2, X4, Y4) <= 0) && (relativeCCW(X3,
                                                                         Y3, X4, Y4, X1, Y1)
                                                             * relativeCCW(X3, Y3, X4, Y4, X2, Y2) <= 0));
  }

  public double findInteriorAngle(LatLng a, LatLng b) {
    double dot = dot(a, b);
    double cross = a.getLatitude() * b.getLongitude() - a.getLongitude() * b.getLatitude();
    double output = Math.atan2(dot, cross);
    if (output < 0) {
      output += Math.PI;
    }
    if (output == 0) {
      output = Math.PI / 2;
    }
    return Math.acos(dot(a, b) / (calculateLength(a) * calculateLength(b)));
  }
  public void outputOriginalPointsToOctave()
  {
    super.outputPointsToOctave();
  }


  public void outputPointsToOctave(String prefix,String plot)
  {
    String output = "";
    System.out.print(prefix+"x=[");
    for (LatLng a : regionPoints)
    {
      System.out.print(a.getLatitude() + ",");
    }
    System.out.print("]");
    System.out.println("\n");
    System.out.print(prefix+"y=[");
    for (LatLng a : regionPoints)
    {
      System.out.print(a.getLongitude() + ",");
    }
    System.out.print("]");
    System.out.println("");
    System.out.println("plot("+prefix+"x,"+prefix+"y," + plot+")");
		System.out.println("hold on;");
  }

}
