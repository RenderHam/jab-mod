package com.jab.util;

import net.minecraft.core.Direction;

/**
 * Screen orientations. Each side carries three vectors:
 * f = the face normal, r = the right direction along the wall, u = the up direction along the wall.
 */
public enum BlockSide {
	BOTTOM(0, -1, 0, 1, 0, 0, 0, 0, -1),
	TOP(0, 1, 0, 1, 0, 0, 0, 0, -1),
	NORTH(0, 0, -1, -1, 0, 0, 0, 1, 0),
	SOUTH(0, 0, 1, 1, 0, 0, 0, 1, 0),
	WEST(-1, 0, 0, 0, 0, 1, 0, 1, 0),
	EAST(1, 0, 0, 0, 0, -1, 0, 1, 0);

	public final int fx, fy, fz;
	public final int rx, ry, rz;
	public final int ux, uy, uz;

	BlockSide(int fx, int fy, int fz, int rx, int ry, int rz, int ux, int uy, int uz) {
		this.fx = fx;
		this.fy = fy;
		this.fz = fz;
		this.rx = rx;
		this.ry = ry;
		this.rz = rz;
		this.ux = ux;
		this.uy = uy;
		this.uz = uz;
	}

	public static BlockSide fromDirection(Direction dir) {
		return values()[dir.ordinal()];
	}

	public Direction toDirection() {
		return Direction.values()[ordinal()];
	}
}
