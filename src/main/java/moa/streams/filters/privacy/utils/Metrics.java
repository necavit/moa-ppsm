package moa.streams.filters.privacy.utils;

import weka.core.Instance;

/**
 * Utility class, with static methods that can be used to compute metrics between instances, like
 * the {@link #distance(Instance, Instance)} measure.
 */
public class Metrics {

	public static double sse(Instance x, Instance y) {
		double dist = 0.0;
		for (int i = 0; i < x.numAttributes(); ++i){
			if (i != x.classIndex()) { //skip all those variables that are the target class
				if (x.attribute(i).isNumeric()){
					dist += (x.value(i) - y.value(i)) * (x.value(i) - y.value(i));
				} else {
					dist += (x.value(i) !=  y.value(i)) ? 1.0 : 0.0;
				}
			}
		}
		return dist;
	}
	
	/**
	 * Computes the distance between two given instances, using the following scheme:
	 * <br>
	 * <pre>{@code
	 *   procedure distance(x,y):
	 *     dist := 0
	 *     for each attribute i in x:
	 *       if i != targetAttribute:
	 *         if isNumeric(i):
	 *           dist := dist + (x_i - y_i)^2
	 *         else:
	 *           if x_i != y_i:
	 *             dist := dist + 1
	 *     dist := sqrt(dist)
	 *     return dist
	 * }</pre>
	 * 
	 * @param x the first instance
	 * @param y the second instance
	 * @return the distance between the given instances
	 */
	public static double distance(Instance x, Instance y){
		double dist = 0.0;
		for (int i = 0; i < x.numAttributes(); ++i){
			if (i != x.classIndex()) { //skip all those variables that are the target class
				if (x.attribute(i).isNumeric()){
					dist += (x.value(i) - y.value(i)) * (x.value(i) - y.value(i));
				} else {
					dist += (x.value(i) !=  y.value(i)) ? 1.0 : 0.0;
				}
			}
		}
		return Math.sqrt(dist);
	}
	
}
