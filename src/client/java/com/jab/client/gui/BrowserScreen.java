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
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

/**
 * The interactive browser view. A URL bar sits on top; everything below it is the live
 * page, which receives mouse and keyboard input. URL changes round-trip to the server
 * so the wall state stays authoritative.
 */
public class BrowserScreen extends Screen {
	private final BlockPos pos;
	private final BlockSide side;
	private String currentUrl;
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
		this.currentUrl = currentUrl != null ? currentUrl : "about:blank";
		this.syncedUrl = this.currentUrl;
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
	public void resize(int width, int height) {
		super.resize(width, height);
		updateDisplayRect();
		resizeBrowser();
	}

	private void updateDisplayRect() {
		displayX = 0;
		displayY = TOOLBAR_HEIGHT;
		displayW = width;
		displayH = height - TOOLBAR_HEIGHT;
	}

	/** The browser runs at screen resolution, so it has to be resized when the GUI is. */
	private void resizeBrowser() {
		if (browser != null) {
			int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
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
		urlBox.setValue(currentUrl);
		urlBox.setCursorPosition(currentUrl.length());
		urlBox.setBordered(true);
		urlBox.setVisible(true);
		addRenderableWidget(urlBox);
	}

	/** Round-trips in-page navigation (back/forward/links) to the server. */
	private void syncUrlFromBrowser() {
		if (browser == null || urlBox == null || urlBox.isFocused()) return;
		String current = browser.getURL();
		if (current == null || current.equals(syncedUrl)) return;
		syncedUrl = current;
		currentUrl = current;
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
			Identifier texId = browser.getTextureIdentifier();
			if (texId == null) {
				drawLoadingText(guiGraphics);
				return;
			}
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texId, displayX, displayY, 0f, 0f, displayW, displayH, displayW, displayH);
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
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		double mx = event.x();
		double my = event.y();

		if (my < TOOLBAR_HEIGHT) {
			if (urlBox != null && urlBox.isMouseOver(mx, my)) {
				urlBox.setFocused(true);
				return urlBox.mouseClicked(event, true);
			}
			return true;
		}

		if (browser != null) {
			int bx = toBrowserX(mx);
			int by = toBrowserY(my);
			if (bx >= 0 && by >= 0) {
				browser.sendMouseMove(bx, by);
				browser.sendMousePress(bx, by, event.button());
				return true;
			}
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		double mx = event.x();
		double my = event.y();

		if (browser != null && my >= TOOLBAR_HEIGHT) {
			int bx = toBrowserX(mx);
			int by = toBrowserY(my);
			if (bx >= 0 && by >= 0) {
				browser.sendMouseMove(bx, by);
				browser.sendMouseRelease(bx, by, event.button());
				return true;
			}
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		if (browser != null && y >= TOOLBAR_HEIGHT) {
			int bx = toBrowserX(x);
			int by = toBrowserY(y);
			if (bx >= 0 && by >= 0) {
				browser.sendMouseWheel(bx, by, vertical * 100, 0);
				return true;
			}
		}
		return super.mouseScrolled(x, y, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (urlBox != null && urlBox.isFocused()) {
			if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
				navigateToUrl(urlBox.getValue());
				return true;
			}
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				urlBox.setFocused(false);
				return true;
			}
			return urlBox.keyPressed(event);
		}

		if (event.modifiers() == GLFW.GLFW_MOD_CONTROL && event.key() == GLFW.GLFW_KEY_L) {
			if (urlBox != null) {
				urlBox.setFocused(true);
				urlBox.setCursorPosition(urlBox.getValue().length());
			}
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		if (browser != null) {
			browser.sendKeyPress(event.key(), event.scancode(), event.modifiers());
		}
		return true;
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (browser != null && !(urlBox != null && urlBox.isFocused())) {
			browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (urlBox != null && urlBox.isFocused()) {
			return urlBox.charTyped(event);
		}
		if (browser != null) {
			browser.sendKeyTyped((char) event.codepoint(), event.modifiers());
		}
		return true;
	}

	private void navigateToUrl(String url) {
		if (url == null || url.isEmpty()) return;
		url = UrlUtil.sanitize(url);
		currentUrl = url;
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