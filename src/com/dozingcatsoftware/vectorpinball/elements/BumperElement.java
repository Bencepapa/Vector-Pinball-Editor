package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Point;

/** This FieldElement subclass represents a bumper that applies an impulse to a ball when it hits. The impulse magnitude is controlled
 * by the "kick" parameter in the configuration map.
 */

public class BumperElement extends FieldElement {

    static final Color DEFAULT_COLOR = Color.fromRGB(0, 0, 255);

	Body pegBody;
	List pegBodySet;

	float radius;
	float cx, cy;
	float kick;

	@Override public void finishCreateElement(Map params, FieldElementCollection collection) {
		List pos = (List)params.get("position");
		this.radius = asFloat(params.get("radius"));
		this.cx = asFloat(pos.get(0));
		this.cy = asFloat(pos.get(1));
		this.kick = asFloat(params.get("kick"));
	}

	@Override public void createBodies(World world) {
		pegBody = Box2DFactory.createCircle(world, cx, cy, radius, true);
		pegBodySet = Collections.singletonList(pegBody);
	}

	@Override public List<Body> getBodies() {
		return pegBodySet;
	}

	@Override public boolean shouldCallTick() {
		// needs to call tick to decrement flash counter (but can use superclass tick() implementation)
		return true;
	}


	Vector2 impulseForBall(Body ball) {
		if (this.kick <= 0.01f) return null;
		// compute unit vector from center of peg to ball, and scale by kick value to get impulse
		Vector2 ballpos = ball.getWorldCenter();
		Vector2 thisPos = pegBody.getPosition();
		float ix = ballpos.x - thisPos.x;
		float iy = ballpos.y - thisPos.y;
		float mag = (float)Math.sqrt(ix*ix + iy*iy);
		float scale = this.kick / mag;
		return new Vector2(ix*scale, iy*scale);
	}

	@Override public void handleCollision(Body ball, Body bodyHit, Field field) {
		Vector2 impulse = this.impulseForBall(ball);
		if (impulse!=null) {
			ball.applyLinearImpulse(impulse, ball.getWorldCenter(), true);
			flashForFrames(3);
		}
	}

	@Override public void draw(IFieldRenderer renderer) {
		float px = pegBody.getPosition().x;
		float py = pegBody.getPosition().y;
		renderer.fillCircle(px, py, radius, currentColor(DEFAULT_COLOR));
	}

    @Override List<Point> getSamplePoints() {
        return Arrays.asList(Point.fromXY(cx, cy));
    }

    @Override boolean isPointWithinDistance(Point point, double distance) {
        return point.distanceTo(Point.fromXY(cx, cy)) <= this.radius + distance;
    }
}
