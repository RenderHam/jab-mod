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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
		ScreenData data = new ScreenData();
		data.side = side;
		data.width = w;
		data.height = h;
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
			screens.put(s.side, s);
		}
		rebuildScreensSnapshot();
	}

	public boolean setUrl(BlockSide side, String url) {
		ScreenData s = screens.get(side);
		if (s != null) {
			s.url = url;
			setChanged();
			syncUpdate(side);
			return true;
		}
		return false;
	}

	public boolean setAudioMode(BlockSide side, ScreenData.AudioMode mode) {
		ScreenData s = screens.get(side);
		if (s != null) {
			s.audioMode = mode;
			setChanged();
			syncUpdate(side);
			return true;
		}
		return false;
	}

	public void onDestroy() {
		screens.clear();
		rebuildScreensSnapshot();
		setChanged();
		sync();
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel && !screens.isEmpty()) {
			onDestroy();
		}
		super.setRemoved();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		var listOut = output.list("Screens", CompoundTag.CODEC);
		for (ScreenData s : screens.values()) {
			listOut.add(s.serialize());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		screens.clear();
		var listIn = input.listOrEmpty("Screens", CompoundTag.CODEC);
		for (var tag : listIn) {
			ScreenData sd = ScreenData.deserialize(tag);
			screens.put(sd.side, sd);
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
	}

	public AABB getRenderBoundingBox() {
		if (screens.isEmpty()) return new AABB(worldPosition);
		AABB box = new AABB(worldPosition);
		for (ScreenData s : screens.values()) {
			Vec3 f = new Vec3(s.side.fx, s.side.fy, s.side.fz);
			Vec3 r = new Vec3(s.side.rx * s.width, s.side.ry * s.width, s.side.rz * s.width);
			Vec3 u = new Vec3(s.side.ux * s.height, s.side.uy * s.height, s.side.uz * s.height);
			Vec3 c1 = Vec3.atLowerCornerOf(worldPosition).add(f).add(r).add(u);
			Vec3 c2 = Vec3.atLowerCornerOf(worldPosition);
			box = box.minmax(new AABB(c1, c2));
		}
		return box;
	}
}
