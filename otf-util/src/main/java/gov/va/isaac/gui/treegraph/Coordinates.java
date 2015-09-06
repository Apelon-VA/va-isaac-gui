package gov.va.isaac.gui.treegraph;

public class Coordinates {
	final double x;
	final double y;
	
	public Coordinates(double x, double y) { this.x = x; this.y = y; }
	
	public double getX() { return x; }
	public double getY() { return y; }
	
	public String toString() { return "(" + x + ", " + y + ")"; }
}