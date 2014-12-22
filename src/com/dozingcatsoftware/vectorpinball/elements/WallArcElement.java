package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.toRadians;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Point;

/**
 * This FieldElement subclass approximates a circular wall with a series of straight wall segments
 * whose endpoints lie on a circle or ellipse. These elements are defined in the layout JSON as follows:
 * {
 *     "class": "WallArcElement",
 *     "center": [5.5, 10], // center of circle or ellipse
 *     "xradius": 2.5, // radius in the horizontal direction
 *     "yradius": 2, // radius in the y direction
 *     "minangle": 45, // starting angle in degrees, 0 is to the right of the center, 90 is up.
 *     "maxangle": 135, // ending angle in degrees
 *     "segments": 10, // number of straight wall segments to use to approximate the arc.
 *     "color": [255,0,0] // optional RGB values for the arc's color
 * }
 *
 * For circular walls, the "radius" attribute can be used instead of xradius and yradius.
 *
 * @author brian
 */

public class WallArcElement extends FieldElement {

    public static final String CENTER_PROPERTY = "center";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String X_RADIUS_PROPERTY = "xradius";
    public static final String Y_RADIUS_PROPERTY = "yradius";
    public static final String NUM_SEGMENTS_PROPERTY = "segments";
    public static final String MIN_ANGLE_PROPERTY = "minangle";
    public static final String MAX_ANGLE_PROPERTY = "maxangle";

	public List wallBodies = new ArrayList();
	float[][] lineSegments;

	float centerX, centerY;
	float xRadius, yRadius;
	int numSegments;
	float minAngle, maxAngle;

	@Override public void finishCreateElement(Map params, FieldElementCollection collection) {
		List centerPos = (List)params.get(CENTER_PROPERTY);
		centerX = asFloat(centerPos.get(0));
		centerY = asFloat(centerPos.get(1));

		// can specify "radius" for circle, or "xradius" and "yradius" for ellipse
		if (params.containsKey(RADIUS_PROPERTY)) {
			xRadius = yRadius = asFloat(params.get(RADIUS_PROPERTY));
		}
		else {
			xRadius = asFloat(params.get(X_RADIUS_PROPERTY));
			yRadius = asFloat(params.get(Y_RADIUS_PROPERTY));
		}

		Number segments = (Number)params.get(NUM_SEGMENTS_PROPERTY);
		numSegments = (segments!=null) ? segments.intValue() : 5;
		minAngle = toRadians(asFloat(params.get(MIN_ANGLE_PROPERTY)));
		maxAngle = toRadians(asFloat(params.get(MAX_ANGLE_PROPERTY)));
		buildSegments();
	}

	void buildSegments() {
        float diff = maxAngle - minAngle;
        // Create line segments to approximate circular arc.
        lineSegments = new float[numSegments][];
        for(int i=0; i<numSegments; i++) {
            float angle1 = minAngle + i * diff / numSegments;
            float angle2 = minAngle + (i+1) * diff / numSegments;
            float x1 = centerX + xRadius * (float)Math.cos(angle1);
            float y1 = centerY + yRadius * (float)Math.sin(angle1);
            float x2 = centerX + xRadius * (float)Math.cos(angle2);
            float y2 = centerY + yRadius * (float)Math.sin(angle2);
            lineSegments[i] = (new float[] {x1, y1, x2, y2});
        }
	}

	@Override public void createBodies(World world) {
        for (float[] segment : this.lineSegments) {
            Body wall = Box2DFactory.createThinWall(world, segment[0], segment[1], segment[2], segment[3], 0f);
            this.wallBodies.add(wall);
        }
	}

	@Override public List<Body> getBodies() {
		return wallBodies;
	}

	@Override public void draw(IFieldRenderer renderer) {
	    Color color = currentColor(DEFAULT_WALL_COLOR);
		for (float[] segment : this.lineSegments) {
			renderer.drawLine(segment[0], segment[1], segment[2], segment[3], color);
		}
	}

	// Editor support.
	@Override public void drawForEditor(IFieldRenderer renderer, boolean isSelected) {
	    draw(renderer);
	    if (isSelected) {
	        Color color = currentColor(DEFAULT_WALL_COLOR);
	        renderer.fillCircle(lineSegments[0][0], lineSegments[0][1], 0.25f, color);
	        float[] lastSegment = lineSegments[lineSegments.length-1];
	        renderer.fillCircle(lastSegment[2], lastSegment[3], 0.25f, color);
	    }
	}

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        for (float[] segment : this.lineSegments) {
            Point start = Point.fromXY(segment[0], segment[1]);
            Point end = Point.fromXY(segment[2], segment[3]);
            if (point.distanceToLineSegment(start, end) <= distance) {
                return true;
            }
        }
        return false;
    }
    @Override public void handleDrag(Point point, Point deltaFromStart, Point deltaFromPrevious) {
        centerX += deltaFromPrevious.x;
        centerY += deltaFromPrevious.y;
        buildSegments();
    }

    @Override public Map<String, Object> getPropertyMap() {
        Map<String, Object> properties = mapWithDefaultProperties();
        properties.put(CENTER_PROPERTY, Arrays.asList(centerX, centerY));
        // Always use separate x/y radius values.
        properties.put(X_RADIUS_PROPERTY, xRadius);
        properties.put(Y_RADIUS_PROPERTY, yRadius);
        properties.put(NUM_SEGMENTS_PROPERTY, numSegments);
        properties.put(MIN_ANGLE_PROPERTY, minAngle);
        properties.put(MAX_ANGLE_PROPERTY, maxAngle);
        return properties;
    }
}
