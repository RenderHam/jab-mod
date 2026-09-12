package com.jab.client.gui;

import de.keksuccino.rinku.RinkuBrowser;

import com.jab.JabMod;
import com.jab.client.browser.BrowserManager;
import com.jab.client.browser.ScreenBrowserManager;
import com.jab.client.network.ClientNetworking;
import com.jab.data.ScreenData;
import com.jab.util.BlockSide;
import com.jab.util.UrlUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.lwjgl.glfw.GLFW;

public class BrowserScreen extends Screen {
	private final BlockPos pos;
	private final BlockSide side;
	private String syncedUrl;
	private RinkuBrowser browser;
	private EditBox urlBox;
	private int displayX;
	private int displayY;
	private int displayW;
	private int displayH;
	private static final int TOOLBAR_HEIGHT = 30;
	private boolean browserNotReadyLogged = false;
	private int fetchRetryTick = 0;

	public BrowserScreen(BlockPos pos, BlockSide side, String currentUrl) {
		super(Component.literal("JAB - Browser"));
		this.pos = pos;
		this.side = side;
		this.syncedUrl = currentUrl != null ? currentUrl : "about:blank";
	}

	public BlockPos getPos() {
		return pos;
	}

	public BlockSide getSide() {
		return side;
	}

	@Override
	protected void init() {
		super.init();
		fetchBrowser();
		updateDisplayRect();
		initToolbar();
		resizeBrowser();
	}

	@Override
	public void tick() {
		super.tick();
		ScreenData sd = ScreenBrowserManager.getDesiredScreen(pos, side);
		if (sd == null) {
			if (minecraft != null) minecraft.setScreen(null);
			return;
		}
		if (browser == null || !browser.isTextureReady()) {
			fetchRetryTick++;
			if (fetchRetryTick % 20 == 1) fetchBrowser();
		}
		syncUrlFromBrowser();
	}

	@Override
	public void resize(Minecraft mc, int width, int height) {
		super.resize(mc, width, height);
		updateDisplayRect();
		resizeBrowser();
	}

	private void updateDisplayRect() {
		displayX = 0;
		displayY = TOOLBAR_HEIGHT;
		displayW = width;
		displayH = height - TOOLBAR_HEIGHT;
	}

	private void resizeBrowser() {
		if (browser != null) {
			int guiScale = (int) Minecraft.getInstance().getWindow().getGuiScale();
			int bW = (int) (displayW * guiScale);
			int bH = (int) (displayH * guiScale);
			browser.resize(Math.max(1, bW), Math.max(1, bH));
		}
	}

	private void fetchBrowser() {
		ScreenBrowserManager.ensureBrowser(pos, side);
		browser = ScreenBrowserManager.getBrowser(pos, side);
		if (browser == null) {
			if (!browserNotReadyLogged) {
				JabMod.LOGGER.info("Browser not ready yet for {} side={}", pos, side);
				browserNotReadyLogged = true;
			}
		}
	}

	private void initToolbar() {
		int padding = 4;
		urlBox = new EditBox(font, padding, (TOOLBAR_HEIGHT - 20) / 2, width - padding * 2, 20, Component.literal("URL"));
		urlBox.setMaxLength(UrlUtil.MAX_URL_LENGTH);
		urlBox.setValue(syncedUrl);
		urlBox.setCursorPosition(syncedUrl.length());
		urlBox.setBordered(true);
		urlBox.setVisible(true);
		addRenderableWidget(urlBox);
	}

	private void syncUrlFromBrowser() {
		if (browser == null || urlBox == null || urlBox.isFocused()) return;
		String current = browser.getURL();
		if (current == null || current.equals(syncedUrl)) return;
		syncedUrl = current;
		urlBox.setValue(current);
		urlBox.setCursorPosition(current.length());
		ClientNetworking.sendUrl(pos, side, current);
		JabMod.LOGGER.info("GUI URL synced to server for {} side={} -> {}", pos, side, current);
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.fillGradient(0, 0, width, height, 0xFF000033, 0xFF000066);
		guiGraphics.fillGradient(0, 0, width, TOOLBAR_HEIGHT, 0xFF1a1a1a, 0xFF1a1a1a);
		guiGraphics.fill(0, TOOLBAR_HEIGHT, width, TOOLBAR_HEIGHT + 1, 0xFF333333);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		renderBackground(guiGraphics, mouseX, mouseY, delta);
		super.render(guiGraphics, mouseX, mouseY, delta);
		renderBrowser(guiGraphics);
	}

	private void renderBrowser(GuiGraphics guiGraphics) {
		if (browser == null) {
			drawLoadingText(guiGraphics);
			return;
		}
		try {
			if (!browser.isTextureReady()) {
				drawLoadingText(guiGraphics);
				return;
			}
			ResourceLocation texId = browser.getTextureIdentifier();
			if (texId == null) {
				drawLoadingText(guiGraphics);
				return;
			}
			guiGraphics.blit(texId, displayX, displayY, 0f, 0f, displayW, displayH, displayW, displayH);
		} catch (Exception e) {
			JabMod.LOGGER.warn("Browser render failed for pos={} side={}", pos, side, e);
			browser = null;
			drawLoadingText(guiGraphics);
		}
	}

	private void drawLoadingText(GuiGraphics guiGraphics) {
		String text = "Loading...";
		int x = width / 2 - font.width(text) / 2;
		int y = height / 2 - 10;
		guiGraphics.drawString(font, text, x, y, 0xFFFFFFFF);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (browser != null && mouseY >= TOOLBAR_HEIGHT) {
			int bx = toBrowserX(mouseX);
			int by = toBrowserY(mouseY);
			if (bx >= 0 && by >= 0) {
				browser.sendMouseMove(bx, by);
			}
		}
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (mouseY < TOOLBAR_HEIGHT) {
			if (urlBox != null && urlBox.isMouseOver(mouseX, mouseY)) {
				urlBox.setFocused(true);
				return urlBox.mouseClicked(mouseX, mouseY, button);
			}
			return true;
		}

		if (browser != null) {
			int bx = toBrowserX(mouseX);
			int by = toBrowserY(mouseY);
			if (bx >= 0 && by >= 0) {
				browser.sendMouseMove(bx, by);
				browser.sendMousePress(bx, by, button);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (browser != null && mouseY >= TOOLBAR_HEIGHT) {
			int bx = toBrowserX(mouseX);
			int by = toBrowserY(mouseY);
			if (bx >= 0 && by >= 0) {
				browser.sendMouseMove(bx, by);
				browser.sendMouseRelease(bx, by, button);
				return true;
			}
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (browser != null && mouseY >= TOOLBAR_HEIGHT) {
			int bx = toBrowserX(mouseX);
			int by = toBrowserY(mouseY);
			if (bx >= 0 && by >= 0) {
				browser.sendMouseWheel(bx, by, vertical * 100, 0);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (urlBox != null && urlBox.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				navigateToUrl(urlBox.getValue());
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				urlBox.setFocused(false);
				return true;
			}
			return urlBox.keyPressed(keyCode, scanCode, modifiers);
		}

		if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_L) {
			if (urlBox != null) {
				urlBox.setFocused(true);
				urlBox.setCursorPosition(urlBox.getValue().length());
			}
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		if (browser != null) {
			browser.sendKeyPress(keyCode, scanCode, modifiers);
		}
		return true;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (browser != null && !(urlBox != null && urlBox.isFocused())) {
			browser.sendKeyRelease(keyCode, scanCode, modifiers);
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (urlBox != null && urlBox.isFocused()) {
			return urlBox.charTyped(chr, modifiers);
		}
		if (browser != null) {
			browser.sendKeyTyped(chr, modifiers);
		}
		return true;
	}

	private void navigateToUrl(String url) {
		if (url == null || url.isEmpty()) return;
		url = UrlUtil.sanitize(url);
		syncedUrl = url;
		if (browser != null) {
			browser.loadURL(url);
		}
		if (urlBox != null) {
			urlBox.setValue(url);
			urlBox.setFocused(false);
		}
		ClientNetworking.sendUrl(pos, side, url);
		JabMod.LOGGER.info("GUI URL changed for {} side={} -> {}", pos, side, url);
	}

	private int toBrowserX(double guiX) {
		if (browser == null || displayW <= 0) return -1;
		return (int) (guiX * Minecraft.getInstance().getWindow().getGuiScale());
	}

	private int toBrowserY(double guiY) {
		if (browser == null || displayH <= 0) return -1;
		return (int) ((guiY - TOOLBAR_HEIGHT) * Minecraft.getInstance().getWindow().getGuiScale());
	}

	@Override
	public void onClose() {
		BrowserManager.resetCursor();
		JabMod.LOGGER.info("Closed browser GUI for pos={} side={}", pos, side);
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
