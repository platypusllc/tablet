/*
 * Copyright (c) 2010, Bart Kiers
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * NOTE: modified by Jason Blum Nov. 2017 to use floating point numbers and Android
 *
 */
package com.platypus.android.tablet.Path;

import android.graphics.PointF;
import java.util.*;

/**
 *
 */
final class GrahamScan {

		/**
		 * An enum denoting a directional-turn between 3 points (vectors).
		 */
		protected enum Turn { CLOCKWISE, COUNTER_CLOCKWISE, COLLINEAR }

		/**
		 * Returns true iff all points in <code>points</code> are collinear.
		 *
		 * @param points the list of points.
		 * @return       true iff all points in <code>points</code> are collinear.
		 */
		private static boolean areAllCollinear(List<PointF> points) {

				if(points.size() < 2) {
						return true;
				}

				final PointF a = points.get(0);
				final PointF b = points.get(1);

				for(int i = 2; i < points.size(); i++) {

						PointF c = points.get(i);

						if(getTurn(a, b, c) != Turn.COLLINEAR) {
								return false;
						}
				}

				return true;
		}

		/**
		 * Returns the convex hull of the points created from <code>points</code>.
		 *
		 * @param points the x,y coordinates of the points
		 * @return   the convex hull of the points
		 */
		static ArrayList<Integer> getConvexHull(ArrayList<Double[]> points)
		{
				List<PointF> p = new ArrayList<>();

				for(int i = 0; i < points.size(); i++) {
						float x = points.get(i)[0].floatValue();
						float y = points.get(i)[1].floatValue();
						p.add(new PointF(x, y));
				}
				return getConvexHull(p);
		}


		/**
		 * Returns the convex hull of the points created from the list
		 * <code>points</code>. Note that the first and last point in the
		 * returned <code>List&lt;java.awt.Point&gt;</code> are the same
		 * point.
		 *
		 * @param points the list of points.
		 * @return       the convex hull of the points created from the list
		 *               <code>points</code>.
		 * @throws IllegalArgumentException if all points are collinear or if there
		 *                                  are less than 3 unique points present.
		 */
		private static ArrayList<Integer> getConvexHull(List<PointF> points) throws IllegalArgumentException {

				List<PointF> sorted = new ArrayList<>(getSortedPointSet(points));

				if(sorted.size() < 3) {
						throw new IllegalArgumentException("can only create a convex hull of 3 or more unique points");
				}

				if(areAllCollinear(sorted)) {
						throw new IllegalArgumentException("cannot create a convex hull from collinear points");
				}

				Stack<PointF> stack = new Stack<>();
				stack.push(sorted.get(0));
				stack.push(sorted.get(1));

				for (int i = 2; i < sorted.size(); i++) {

						PointF head = sorted.get(i);
						PointF middle = stack.pop();
						PointF tail = stack.peek();

						Turn turn = getTurn(tail, middle, head);

						switch(turn) {
								case COUNTER_CLOCKWISE:
										stack.push(middle);
										stack.push(head);
										break;
								case CLOCKWISE:
										i--;
										break;
								case COLLINEAR:
										stack.push(head);
										break;
						}
				}

				// DON'T close the hull for our spiral creation
				// stack.push(sorted.get(0));

				// TODO: instead of returning the points, return the index of the points in the original list
				ArrayList<Integer> original_indices = new ArrayList<>();
				for (PointF p : stack)
				{
						original_indices.add(points.indexOf(p));
				}

				//return new ArrayList<>(stack);
				return original_indices;
		}

		/**
		 * Returns the points with the lowest y coordinate. In case more than 1 such
		 * point exists, the one with the lowest x coordinate is returned.
		 *
		 * @param points the list of points to return the lowest point from.
		 * @return       the points with the lowest y coordinate. In case more than
		 *               1 such point exists, the one with the lowest x coordinate
		 *               is returned.
		 */
		protected static PointF getLowestPoint(List<PointF> points) {

				PointF lowest = points.get(0);

				for(int i = 1; i < points.size(); i++) {

						PointF temp = points.get(i);

						if(temp.y < lowest.y || (temp.y == lowest.y && temp.x < lowest.x)) {
								lowest = temp;
						}
				}

				return lowest;
		}

		/**
		 * Returns a sorted set of points from the list <code>points</code>. The
		 * set of points are sorted in increasing order of the angle they and the
		 * lowest point <tt>P</tt> make with the x-axis. If tow (or more) points
		 * form the same angle towards <tt>P</tt>, the one closest to <tt>P</tt>
		 * comes first.
		 *
		 * @param points the list of points to sort.
		 * @return       a sorted set of points from the list <code>points</code>.
		 * @see GrahamScan#getLowestPoint(java.util.List)
		 */
		protected static Set<PointF> getSortedPointSet(List<PointF> points) {

				final PointF lowest = getLowestPoint(points);

				TreeSet<PointF> set = new TreeSet<PointF>(new Comparator<PointF>() {
						@Override
						public int compare(PointF a, PointF b) {

								if(a == b || a.equals(b)) {
										return 0;
								}

								double thetaA = Math.atan2(a.y - lowest.y, a.x - lowest.x);
								double thetaB = Math.atan2(b.y - lowest.y, b.x - lowest.x);

								if(thetaA < thetaB) {
										return -1;
								}
								else if(thetaA > thetaB) {
										return 1;
								}
								else {
										// collinear with the 'lowest' point, let the point closest to it come first

										double distanceA = Math.sqrt(((lowest.x - a.x) * (lowest.x - a.x)) +
														((lowest.y - a.y) * (lowest.y - a.y)));
										double distanceB = Math.sqrt(((lowest.x - b.x) * (lowest.x - b.x)) +
														((lowest.y - b.y) * (lowest.y - b.y)));

										if(distanceA < distanceB) {
												return -1;
										}
										else {
												return 1;
										}
								}
						}
				});

				set.addAll(points);

				return set;
		}

		/**
		 * Returns the GrahamScan#Turn formed by traversing through the
		 * ordered points <code>a</code>, <code>b</code> and <code>c</code>.
		 * More specifically, the cross product <tt>C</tt> between the
		 * 3 points (vectors) is calculated:
		 *
		 * <tt>(b.x-a.x * c.y-a.y) - (b.y-a.y * c.x-a.x)</tt>
		 *
		 * and if <tt>C</tt> is less than 0, the turn is CLOCKWISE, if
		 * <tt>C</tt> is more than 0, the turn is COUNTER_CLOCKWISE, else
		 * the three points are COLLINEAR.
		 *
		 * @param a the starting point.
		 * @param b the second point.
		 * @param c the end point.
		 * @return the GrahamScan#Turn formed by traversing through the
		 *         ordered points <code>a</code>, <code>b</code> and
		 *         <code>c</code>.
		 */
		protected static Turn getTurn(PointF a, PointF b, PointF c) {

				float crossProduct = ((b.x - a.x) * (c.y - a.y)) -
								((b.y - a.y) * (c.x - a.x));

				if(crossProduct > 0) {
						return Turn.COUNTER_CLOCKWISE;
				}
				else if(crossProduct < 0) {
						return Turn.CLOCKWISE;
				}
				else {
						return Turn.COLLINEAR;
				}
		}
}