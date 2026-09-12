package com.jab.blockentity;

import com.jab.data.ScreenData;
import com.jab.network.packet.ScreenStateS2CPacket;
import com.jab.network.packet.ScreenUpdateS2CPacket;
import com.jab.registry.ModBlockEntities;
import com.jab.util.BlockSide;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;

/**
 * Holds the screen data for one multiblock wall. Only exists on the origin block.
 * Screens are keyed by the block side they're attached to, so a single wall can
 * display different pages on each of its faces.
 */
public class ScreenBlockEntity extends BlockEntity {
	private final EnumMap<BlockSide, ScreenData> screens = new EnumMap<>(BlockSide.class);
	private List<ScreenData> screensSnapshot = List.of();
	private AABB cachedBoundingBox = null;

	public ScreenBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SCREEN_BLOCK_ENTITY, pos, state);
	}

	private void broadcast(CustomPacketPayload packet) {
		if (level instanceof ServerLevel sl) {
			for (var trackingPlayer : PlayerLookup.tracking(sl, worldPosition)) {
				ServerPlayNetworking.send(trackingPlayer, packet);
			}
		}
	}

	/** Sends the full screen state to every player tracking this block. */
	public void sync() {
		broadcast(new ScreenStateS2CPacket(worldPosition, List.copyOf(screens.values())));
	}

	/** Sends a targeted update for a single screen instead of the whole wall. */
	public void syncUpdate(BlockSide side) {
		ScreenData screen = screens.get(side);
		if (screen != null) {
			broadcast(new ScreenUpdateS2CPacket(worldPosition, screen));
		}
	}

	public ScreenData addScreen(BlockSide side, int w, int h) {
		ScreenData existing = screens.get(side);
		if (existing != null) return existing;
		ScreenData data = ScreenData.create(side, w, h);
		screens.put(side, data);
		rebuildScreensSnapshot();
		setChanged();
		sync();
		return data;
	}

	public ScreenData getScreen(BlockSide side) {
		return screens.get(side);
	}

	public List<ScreenData> getScreens() {
		return screensSnapshot;
	}

	public void replaceAllScreens(List<ScreenData> list) {
		screens.clear();
		for (ScreenData s : list) {
			screens.put(s.side(), s);
		}
		rebuildScreensSnapshot();
	}

	public boolean setUrl(BlockSide side, String url) {
		ScreenData s = screens.get(side);
		if (s != null) {
			s.setUrl(url);
			setChanged();
			syncUpdate(side);
			return true;
		}
		return false;
	}

	public boolean setAudioMode(BlockSide side, ScreenData.AudioMode mode) {
		ScreenData s = screens.get(side);
		if (s != null) {
			s.setAudioMode(mode);
			setChanged();
			syncUpdate(side);
			return true;
		}
		return false;
	}

	public boolean removeScreen(BlockSide side) {
		if (screens.remove(side) != null) {
			rebuildScreensSnapshot();
			setChanged();
			sync();
			return true;
		}
		return false;
	}

	public void onDestroy() {
		clearAndBroadcastEmpty();
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel) {
			clearAndBroadcastEmpty();
		}
		super.setRemoved();
	}

	private void clearAndBroadcastEmpty() {
		if (screens.isEmpty()) return;
		screens.clear();
		rebuildScreensSnapshot();
		setChanged();
		broadcast(new ScreenStateS2CPacket(worldPosition, List.of()));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ListTag listTag = new ListTag();
		for (ScreenData s : screens.values()) {
			listTag.add(s.serialize());
		}
		tag.put("Screens", listTag);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		screens.clear();
		ListTag listTag = tag.getList("Screens", 10);
		for (int i = 0; i < listTag.size(); i++) {
			ScreenData sd = ScreenData.deserialize(listTag.getCompound(i));
			screens.put(sd.side(), sd);
		}
		rebuildScreensSnapshot();
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		ListTag listTag = new ListTag();
		for (ScreenData s : screens.values()) {
			listTag.add(s.serialize());
		}
		tag.put("Screens", listTag);
		return tag;
	}

	private void rebuildScreensSnapshot() {
		screensSnapshot = List.copyOf(screens.values());
		cachedBoundingBox = null;
	}

	public AABB getRenderBoundingBox() {
		if (cachedBoundingBox != null) return cachedBoundingBox;
		if (screens.isEmpty()) {
			cachedBoundingBox = new AABB(worldPosition);
			return cachedBoundingBox;
		}
		AABB box = new AABB(worldPosition);
		for (ScreenData s : screens.values()) {
			BlockSide side = s.side();
			Vec3 f = new Vec3(side.faceX, side.faceY, side.faceZ);
			Vec3 r = new Vec3(side.rightX * s.width(), side.rightY * s.width(), side.rightZ * s.width());
			Vec3 u = new Vec3(side.upX * s.height(), side.upY * s.height(), side.upZ * s.height());
			Vec3 c1 = Vec3.atLowerCornerOf(worldPosition).add(f).add(r).add(u);
			Vec3 c2 = Vec3.atLowerCornerOf(worldPosition);
			box = box.minmax(new AABB(c1, c2));
		}
		cachedBoundingBox = box;
		return box;
	}
}
