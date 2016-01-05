package pl.kbieron.iomerge.server.gesture.model;

import pl.kbieron.iomerge.model.message.Message;

import java.awt.Point;
import java.util.List;


public class Pattern {

	private final List<Point> points;

	private final Message action;

	private final String name;

	public Pattern(List<Point> points, Message action, String name) {
		this.points = points;
		this.action = action;
		this.name = name;
	}

	public List<Point> getPoints() {
		return points;
	}

	public Message getAction() {
		return action;
	}

	public String getName() {
		return name;
	}
}
