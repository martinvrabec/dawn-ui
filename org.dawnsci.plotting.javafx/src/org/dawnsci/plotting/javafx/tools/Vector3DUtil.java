package org.dawnsci.plotting.javafx.tools;

import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import javax.vecmath.Matrix3d;

/**
 * class to hold basic vector calculations used throughout the plugin
 * 
 * @author Joel Ogden
 *
 */
public class Vector3DUtil {

	/*
	 * static functions
	 */

	/**
	 * THIS DOESN'T WORK BUT FIXING IT BREAKS A LOT OF STUFF
	 * 
	 * @param matrix
	 * @return
	 */
	public static Point3D matrixToEulerXYZ(Transform matrix) {
		Matrix3d m = new Matrix3d(
				matrix.getMxx(), matrix.getMxy(), matrix.getMxz(), 
				matrix.getMyx(), matrix.getMyy(), matrix.getMyz(),
				matrix.getMzx(), matrix.getMzy(), matrix.getMzz());
		return matrixToEuler(m);
	}

	public static Point3D extractEulerAnglersFromMatrix_Broken(Matrix3d m) {
		m.normalize();
		double x, y, z = 0;
		if (m.m10 > 0.998) { // singularity at north pole
			x = Math.toDegrees(Math.atan2(m.m02, m.m22));
			y = Math.toDegrees(Math.PI / 2);
			z = Math.toDegrees(0);
			return new Point3D(y, x, z);
		}
		if (m.m10 < -0.998) { // singularity at south pole
			x = Math.toDegrees(Math.atan2(m.m02, m.m22));
			y = Math.toDegrees(-Math.PI / 2);
			z = Math.toDegrees(0);
			return new Point3D(y, x, z);
		}
		x = Math.toDegrees(Math.atan2(-m.m20, m.m00));
		y = Math.toDegrees(Math.atan2(-m.m12, m.m11));
		z = Math.toDegrees(Math.asin(m.m10));
		return new Point3D(y, x, z);
	}
	
	public static Point3D matrixToEuler(Matrix3d  m) 
	{
		double heading, attitude, bank;
		
	    // Assuming the angles are in radians.
		if (m.m10 > 0.998) { // singularity at north pole
			heading = Math.atan2(m.m02,m.m22);
			attitude = Math.PI/2;
			bank = 0;
			return new Point3D(Math.toDegrees(heading), Math.toDegrees(attitude), Math.toDegrees(bank));
		}
		if (m.m10 < -0.998) { // singularity at south pole
			heading = Math.atan2(m.m02,m.m22);
			attitude = -Math.PI/2;
			bank = 0;
			return new Point3D(Math.toDegrees(heading), Math.toDegrees(attitude), Math.toDegrees(bank));
		}
		heading = Math.atan2(-m.m20,m.m00);
		bank = Math.atan2(-m.m12,m.m11);
		attitude = Math.asin(m.m10);
		return new Point3D(Math.toDegrees(heading), Math.toDegrees(attitude), Math.toDegrees(bank));
	}
	
	
	public static Rotate matrixToRotate(Transform matrix) {
		Matrix3d m = new Matrix3d(matrix.getMxx(), matrix.getMxy(),
				matrix.getMxz(), matrix.getMyx(), matrix.getMyy(),
				matrix.getMyz(), matrix.getMzx(), matrix.getMzy(),
				matrix.getMzz());
		return matrixToRotate(m);
	}

	public static Rotate matrixToRotate(Matrix3d m) 
	{
		m.normalize();
		
		double angle, x, y, z; // variables for result
		double epsilon = 0.01; // margin to allow for rounding errors
		double epsilon2 = 0.1; // margin to distinguish between 0 and 180
								// degrees
		// optional check that input is pure rotation, 'isRotationMatrix' is
		// defined at:
		// http://www.euclideanspace.com/maths/algebra/matrix/orthogonal/rotation/
		if ((Math.abs(m.m01 - m.m10) < epsilon)
				&& (Math.abs(m.m02 - m.m20) < epsilon)
				&& (Math.abs(m.m12 - m.m21) < epsilon)) 
		{
			// singularity found
			// first check for identity matrix which must have +1 for all terms
			// in leading diagonaland zero in other terms
			if ((Math.abs(m.m01 + m.m10) < epsilon2)
					&& (Math.abs(m.m02 + m.m20) < epsilon2)
					&& (Math.abs(m.m12 + m.m21) < epsilon2)
					&& (Math.abs(m.m00 + m.m11 + m.m22 - 3) < epsilon2)) {
				// this singularity is identity matrix so angle = 0
				return new Rotate(0, new Point3D(1, 0, 0)); // zero angle,
															// arbitrary axis
			}
			// otherwise this singularity is angle = 180
			angle = Math.PI;
			double xx = (m.m00 + 1) / 2;
			double yy = (m.m11 + 1) / 2;
			double zz = (m.m22 + 1) / 2;
			double xy = (m.m01 + m.m10) / 4;
			double xz = (m.m02 + m.m20) / 4;
			double yz = (m.m12 + m.m21) / 4;
			if ((xx > yy) && (xx > zz)) 
			{ // m00 is the largest diagonal term
				if (xx < epsilon) 
				{
					x = 0;
					y = 0.7071;
					z = 0.7071;
				} 
				else 
				{
					x = Math.sqrt(xx);
					y = xy / x;
					z = xz / x;
				}
			}
			else if (yy > zz) 
			{ // m11 is the largest diagonal term
				if (yy < epsilon) 
				{
					x = 0.7071;
					y = 0;
					z = 0.7071;
				} else {
					y = Math.sqrt(yy);
					x = xy / y;
					z = yz / y;
				}
			}
			else 
			{ // m22 is the largest diagonal term so base result on this
				if (zz < epsilon) 
				{
					x = 0.7071;
					y = 0.7071;
					z = 0;
				} else 
				{
					z = Math.sqrt(zz);
					x = xz / z;
					y = yz / z;
				}
			}
			return new Rotate(Math.toDegrees(angle), new Point3D(x, y, z)); // return 180 deg rotation
		}
		// as we have reached here there are no singularities so we can handle
		// normally
		double s = Math.sqrt((m.m21 - m.m12) * (m.m21 - m.m12)
				+ (m.m02 - m.m20) * (m.m02 - m.m20) + (m.m10 - m.m01)
				* (m.m10 - m.m01)); // used to normalise
		if (Math.abs(s) < 0.001)
			s = 1;
		// prevent divide by zero, should not happen if matrix is orthogonal and
		// should be
		// caught by singularity test above, but I've left it in just in case
		angle = Math.acos((m.m00 + m.m11 + m.m22 - 1) / 2);
		x = (m.m21 - m.m12) / s;
		y = (m.m02 - m.m20) / s;
		z = (m.m10 - m.m01) / s;
		
		
		return new Rotate(Math.toDegrees(angle), new Point3D(x, y, z));
	}
	
	
	
	public static double getMaximumValue(Point3D v) {
		double value = Double.MIN_VALUE;
		if (v.getX() > value)
			value = v.getX();
		if (v.getY() > value)
			value = v.getY();
		if (v.getZ() > value)
			value = v.getZ();

		return value;
	}

	/**
	 * Aligns the startVector projection to the endVector projection (projected
	 * across rotationVectorDirection).
	 * 
	 * @param startVector
	 *            - The start vector to be rotated
	 * @param endVector
	 *            - The end vector
	 * @param rotationVectorDirection
	 *            - The rotation direction, ie. the projected plane
	 * @return The rotation
	 * 
	 * 
	 */
	public static Rotate alignVectorOnPlane(Point3D startVector,
			Point3D endVector, Point3D rotationVectorDirection) {
		Point3D startVectorProjection = Vector3DUtil.getVectorPlaneProjection(
				rotationVectorDirection, startVector);
		Point3D endVectorProjection = Vector3DUtil.getVectorPlaneProjection(
				rotationVectorDirection, endVector);

		double angle = startVectorProjection.angle(endVectorProjection);

		if (Double.isInfinite(angle) || Double.isNaN(angle)) {
			angle = 0;
		}

		return new Rotate(angle,
				startVectorProjection.crossProduct(endVectorProjection));
	}

	/**
	 * gets the scale from the transform matrix
	 * 
	 * @param transform
	 * @return
	 */
	public static Point3D getScaleFromTransform(Transform transform) {
		double sx = new Point3D(transform.getMxx(), transform.getMyx(),
				transform.getMzx()).magnitude();
		double sy = new Point3D(transform.getMxy(), transform.getMyy(),
				transform.getMzy()).magnitude();
		double sz = new Point3D(transform.getMxz(), transform.getMyz(),
				transform.getMzz()).magnitude();

		return new Point3D(sx, sy, sz);
	}

	/**
	 * Calculates the Rotate object to aligned u with v
	 * 
	 * @param vectorToAlign
	 *            - vector to be aligned
	 * @param endResult
	 *            - result of alignment
	 * @return the new rotation
	 */
	public static Rotate alignVector(Point3D vectorToAlign, Point3D endResult) {
		double angle = vectorToAlign.angle(endResult);

		// find the normal of the vectors
		Point3D normal = vectorToAlign.crossProduct(endResult);

		// create the rotation via the normal and angle
		Rotate returnRotate = new Rotate();
		returnRotate.setAxis(new Point3D(normal.getX(), normal.getY(), normal
				.getZ()));
		returnRotate.setAngle(angle);

		return returnRotate;

	}

	/**
	 * Aligns plane1 to plane2 by rotating plane1 in a clockwise direction.
	 * 
	 * @param startingVector1
	 * @param startingVector2
	 * @param finalVector1
	 * @param finalVector2
	 * @return the new rotation
	 */
	public static Rotate alignPlaneUsingClockWiseRotation(
			Point3D startingVector1, Point3D startingVector2,
			Point3D finalVector1, Point3D finalVector2) {
		// this should be renamed
		// find the normals of each vector in sequence 1-1 -> 1-2 -> 2-2
		Point3D startingNormal = startingVector2.crossProduct(startingVector1);
		Point3D finalNormal = finalVector1.crossProduct(startingVector2);
		Point3D startingEndFinalEndNormal = finalVector2
				.crossProduct(finalVector1);

		Point3D averageNormal = startingNormal.add(finalNormal).add(
				startingEndFinalEndNormal);

		Point3D projectedstartingVector1 = getVectorPlaneProjection(
				averageNormal, startingVector1);
		Point3D projectedstartingfinalVector1 = getVectorPlaneProjection(
				averageNormal, finalVector1);
		double angleOfRotation = projectedstartingVector1
				.angle(projectedstartingfinalVector1);

		Rotate clockWiseRotation = new Rotate(angleOfRotation, averageNormal);

		return clockWiseRotation;
	}

	/**
	 * Finds the projection of a vector onto a plane.
	 * 
	 * @param planeNormal
	 *            - The planes normal
	 * @param vector
	 *            - the vector to project
	 * @return the projected vector
	 */
	public static Point3D getVectorPlaneProjection(Point3D planeNormal,
			Point3D vector) {
		double dot = vector.dotProduct(planeNormal);
		double magnitude = planeNormal.magnitude();

		Point3D projectedNormalVector = planeNormal.multiply(dot
				/ Math.pow(magnitude, 2));

		Point3D projectedVector = vector.subtract(projectedNormalVector);

		return projectedVector;
	}

	public static double getScaleAcrossProjectedVector(Point3D vector,
			Point3D vectorToScale) {
		double dot = vectorToScale.dotProduct(vector);
		double magnitude = vector.magnitude();

		double returnDouble = vector.multiply(dot / Math.pow(magnitude, 2))
				.magnitude();

		if (dot <= 0) {
			returnDouble *= -1;
		}

		return returnDouble;
	}

	/**
	 * Applies only a single type of transform to a vector from a list of
	 * transforms... (e.g. only applies rotations)
	 * 
	 * @param transformList
	 *            - the list of transforms
	 * @param vector
	 *            - the vector to be transformed
	 * @param transformClassType
	 *            - the type of transform (eg. Rotate<b>.class</b>)
	 * @return - the new transformed vector
	 */
	public static Point3D applyExclusiveTransforms(
			ObservableList<Transform> transformList, Point3D vector,
			Class<?> transformClassType) {
		Point3D direction = vector;
		for (Transform currentTransform : transformList) {
			if (transformClassType.isInstance(currentTransform)) {
				direction = currentTransform.transform(direction);
			}
		}
		return direction;
	}

	public static Point3D applyEclusiveRotation(
			ObservableList<Transform> transformList, Point3D vector,
			boolean invertRotation) {

		Point3D direction = vector;
		for (Transform currentTransform : transformList) {
			if (currentTransform instanceof Rotate) {
				try {
					if (invertRotation)
						direction = ((Rotate) currentTransform).createInverse()
								.transform(direction);
					else
						direction = ((Rotate) currentTransform)
								.transform(direction);
				} catch (NonInvertibleTransformException e) {
					e.printStackTrace();
				}
			}
		}
		return direction;
	}

}
