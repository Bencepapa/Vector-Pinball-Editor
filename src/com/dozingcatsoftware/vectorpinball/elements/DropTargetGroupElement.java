package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.TAU;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Point;

/**
 * This FieldElement subclass represents a set of drop targets, which are segments that disappear when hit. When all
 * targets are hit, the Field delegate is notified, and if the reset parameter is set, the targets will reappear after
 * a delay.
 *
 * The positions of the targets can be given as a list of [x1,y1,x2,y2] line segments. Example:
 * {
 *   "class": "DropTargetGroupElement",
 *   "score": 100,
 *   "reset": 2.0,
 *   "positions": [
 *     [10, 15, 11, 15],
 *     [11.5, 15, 12.5, 15],
 *     [13, 15, 14, 15]
 *   ]
 * }
 *
 * Alternatively, in the common case where the targets are in front of a wall, the
 * targets can be given relative to the wall, for example:
 * {
 *   "class": "DropTargetGroupElement",
 *   "score": 100,
 *   "reset": 2.0,
 *   "wallStart": [10, 12],
 *   "wallEnd": [11, 15], // just used to get direction from wallStart
 *   "gapFromWall": 0.25, // perpendicular distance from wall to targets. Negative to flip sides.
 *   "startDistanceAlongWall": 0.5, // first target is this far from wallStart.
 *   "targetWidth": 1.0, // width of each target
 *   "gapBetweenTargets": 0.2,
 *   "numTargets": 4
 * }
 *
 */

public class DropTargetGroupElement extends FieldElement {

    static final Color DEFAULT_COLOR = Color.fromRGB(0, 255, 0);

	// store all bodies and positions, use Body's active flag to determine which targets have been hit
	List<Body> allBodies = new ArrayList<Body>();
	float[][] positions;

	@Override public void finishCreateElement(Map params, FieldElementCollection collection) {
		// Individual targets can be specified in "positions" list.
	    if (hasParameterKey("positions")) {
	        List<List<Number>> positionList = (List) getRawParameterValueForKey("positions");
	        positions = new float[positionList.size()][];
	        for (int i = 0; i < positionList.size(); i++) {
	            List<Number> coords = positionList.get(i);
	            positions[i] = new float[] {asFloat(coords.get(0)), asFloat(coords.get(1)),
                                            asFloat(coords.get(2)), asFloat(coords.get(3))};
	        }
	    }
	    else {
	        float[] wallStart = getFloatArrayParameterValueForKey("wallStart");
	        float[] wallEnd = getFloatArrayParameterValueForKey("wallEnd");
	        float gapFromWall = getFloatParameterValueForKey("gapFromWall");
	        float startDistanceAlongWall = getFloatParameterValueForKey("startDistanceAlongWall");
	        float targetWidth = getFloatParameterValueForKey("targetWidth");
	        float gapBetweenTargets = getFloatParameterValueForKey("gapBetweenTargets");
	        int numTargets = getIntParameterValueForKey("numTargets");

	        positions = new float[numTargets][];
	        double wallAngle = Math.atan2(wallEnd[1] - wallStart[1], wallEnd[0] - wallStart[0]);
	        double perpToWallAngle = wallAngle + TAU/4;
	        for (int i = 0; i < numTargets; i++) {
	            double alongWallStart = startDistanceAlongWall + i * (targetWidth + gapBetweenTargets);
	            double alongWallEnd = alongWallStart + targetWidth;
                float x1 = (float) (wallStart[0] + (alongWallStart * Math.cos(wallAngle)) +
                                                   (gapFromWall * Math.cos(perpToWallAngle)));
                float y1 = (float) (wallStart[1] + (alongWallStart * Math.sin(wallAngle)) +
                                                   (gapFromWall * Math.sin(perpToWallAngle)));
                float x2 = (float) (wallStart[0] + (alongWallEnd * Math.cos(wallAngle)) +
                                                   (gapFromWall * Math.cos(perpToWallAngle)));
                float y2 = (float) (wallStart[1] + (alongWallEnd * Math.sin(wallAngle)) +
                                                   (gapFromWall * Math.sin(perpToWallAngle)));
                positions[i] = new float[] {x1, y1, x2, y2};
	        }
	    }
	}

	@Override public void createBodies(World world) {
		for (float[] parray : positions) {
			float restitution = 0f;
			Body wallBody = Box2DFactory.createThinWall(world, parray[0], parray[1], parray[2], parray[3], restitution);
			allBodies.add(wallBody);
		}
	}

	@Override public List<Body> getBodies() {
		return allBodies;
	}

	/** Returns true if all targets have been hit (and their corresponding bodies made inactive) */
	public boolean allTargetsHit() {
		int bsize = allBodies.size();
		for(int i=0; i<bsize; i++) {
			if (allBodies.get(i).isActive()) return false;
		}
		return true;
	}

	@Override public void handleCollision(Body ball, Body bodyHit, final Field field) {
		bodyHit.setActive(false);
		// if all hit, notify delegate and check for reset parameter
		if (allTargetsHit()) {
			field.getDelegate().allDropTargetsInGroupHit(field, this);

			float restoreTime = asFloat(this.parameters.get("reset"));
			if (restoreTime>0) {
				field.scheduleAction((long)(restoreTime*1000), new Runnable() {
					@Override
                    public void run() {
						makeAllTargetsVisible();
					}
				});
			}
		}
	}

	/** Makes all targets visible by calling Body.setActive(true) on each target body */
	public void makeAllTargetsVisible() {
		int bsize = allBodies.size();
		for(int i=0; i<bsize; i++) {
			allBodies.get(i).setActive(true);
		}
	}

	@Override public void draw(IFieldRenderer renderer) {
		// draw line for each target
	    Color color = currentColor(DEFAULT_COLOR);
		int bsize = allBodies.size();
		for(int i=0; i<bsize; i++) {
			Body body = allBodies.get(i);
			if (body.isActive()) {
				float[] parray = positions[i];
				renderer.drawLine(parray[0], parray[1], parray[2], parray[3], color);
			}
		}
	}

    @Override List<Point> getSamplePoints() {
        float[] firstSegment = positions[0];
        float[] lastSegment = positions[positions.length-1];
        return Arrays.asList(
                Point.fromXY(firstSegment[0], firstSegment[1]),
                Point.fromXY(lastSegment[2], lastSegment[3])
        );
    }

    @Override boolean isPointWithinDistance(Point point, double distance) {
        float[] firstSegment = positions[0];
        float[] lastSegment = positions[positions.length-1];
        double actualDist = point.distanceToLineSegment(
                Point.fromXY(firstSegment[0], firstSegment[1]),
                Point.fromXY(lastSegment[2], lastSegment[3])
        );
        return actualDist <= distance;
    }
}
