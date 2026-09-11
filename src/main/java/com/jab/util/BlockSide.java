package com.jab.util;

import net.minecraft.core.Direction;

/**
 * Screen orientations. Each side carries three vectors:
 * face = the face normal, right = the right direction along the wall, up = the up direction along the wall.
 */
public enum BlockSide {
	BOTTOM(0, -1, 0, 1, 0, 0, 0, 0, -1),
	TOP(0, 1, 0, 1, 0, 0, 0, 0, -1),
	NORTH(0, 0, -1, -1, 0, 0, 0, 1, 0),
	SOUTH(0, 0, 1, 1, 0, 0, 0, 1, 0),
	WEST(-1, 0, 0, 0, 0, 1, 0, 1, 0),
	EAST(1, 0, 0, 0, 0, -1, 0, 1, 0);

	public final int faceX, faceY, faceZ;
	public final int rightX, rightY, rightZ;
	public final int upX, upY, upZ;

	BlockSide(int faceX, int faceY, int faceZ, int rightX, int rightY, int rightZ, int upX, int upY, int upZ) {
		this.faceX = faceX;
		this.faceY = faceY;
		this.faceZ = faceZ;
		this.rightX = rightX;
		this.rightY = rightY;
		this.rightZ = rightZ;
		this.upX = upX;
		this.upY = upY;
		this.upZ = upZ;
	}

	public static BlockSide fromDirection(Direction dir) {
		return switch (dir) {
			case DOWN -> BOTTOM;
			case UP -> TOP;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
		};
	}

	public static BlockSide lenientValueOf(String name) {
		try {
			return BlockSide.valueOf(name);
		} catch (IllegalArgumentException e) {
			return BOTTOM;
		}
	}

	public Direction toDirection() {
		return switch (this) {
			case BOTTOM -> Direction.DOWN;
			case TOP -> Direction.UP;
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
			case EAST -> Direction.EAST;
		};
	}
}
