package flaxbeard.cyberware.common.handler;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.client.ClientUtils;
import flaxbeard.cyberware.client.KeyBinds;
import flaxbeard.cyberware.client.gui.GuiCyberwareMenu;
import flaxbeard.cyberware.common.network.CyberwarePacketHandler;
import flaxbeard.cyberware.common.network.OpenRadialMenuPacket;

public class CyberwareMenuHandler
{
	public static final CyberwareMenuHandler INSTANCE = new CyberwareMenuHandler();
	private Minecraft mc = Minecraft.getMinecraft();

	int wasInScreen = 0;
	public static boolean wasSprinting = false;
	private static List<Integer> lastPressed = new ArrayList<Integer>();
	private static List<Integer> pressed = new ArrayList<Integer>();
	
	@SubscribeEvent
	public void tick(ClientTickEvent event)
	{
		if(event.phase == Phase.START)
		{
			if (!KeyBinds.menu.isPressed() && mc.currentScreen == null && wasInScreen > 0)
			{
				KeyConflictContext inGame = KeyConflictContext.IN_GAME;
				mc.gameSettings.keyBindForward.setKeyConflictContext(inGame);
				mc.gameSettings.keyBindLeft.setKeyConflictContext(inGame);
				mc.gameSettings.keyBindBack.setKeyConflictContext(inGame);
				mc.gameSettings.keyBindRight.setKeyConflictContext(inGame);
				mc.gameSettings.keyBindJump.setKeyConflictContext(inGame);
				mc.gameSettings.keyBindSneak.setKeyConflictContext(inGame);
				mc.gameSettings.keyBindSprint.setKeyConflictContext(inGame);
				
				if (wasSprinting)
				{
					mc.player.setSprinting(wasSprinting);
				}
				wasInScreen--;
			}

		}
		if(event.phase == Phase.END)
		{
			
			if (mc.player != null && mc.currentScreen == null)
			{

				ICyberwareUserData data = CyberwareAPI.getCapability(mc.player);
				
				for (int keyCode : data.getHotkeys())
				{
					if (isPressed(data, keyCode))
					{
						pressed.add(keyCode);
						if (!lastPressed.contains(keyCode))
						{
							ClientUtils.useActiveItemClient(mc.player, data.getHotkey(keyCode));
						}
					}
				}
				
				lastPressed = pressed;
				pressed = new ArrayList<Integer>();
			
				
			}
			
			if (mc.player != null && CyberwareAPI.getCapability(mc.player).getNumActiveItems() > 0 && KeyBinds.menu.isPressed() && mc.currentScreen == null)
			{

				KeyConflictContext gui = KeyConflictContext.GUI;
				mc.gameSettings.keyBindForward.setKeyConflictContext(gui);
				mc.gameSettings.keyBindLeft.setKeyConflictContext(gui);
				mc.gameSettings.keyBindBack.setKeyConflictContext(gui);
				mc.gameSettings.keyBindRight.setKeyConflictContext(gui);
				mc.gameSettings.keyBindJump.setKeyConflictContext(gui);
				mc.gameSettings.keyBindSneak.setKeyConflictContext(gui);
				mc.gameSettings.keyBindSprint.setKeyConflictContext(gui);
				
				mc.displayGuiScreen(new GuiCyberwareMenu());
				CyberwareAPI.getCapability(mc.player).setOpenedRadialMenu(true);
				CyberwarePacketHandler.INSTANCE.sendToServer(new OpenRadialMenuPacket());

				wasInScreen = 5;
			}
			else if (wasInScreen > 0 && mc.currentScreen instanceof GuiCyberwareMenu)
			{
				wasSprinting = mc.player.isSprinting();
			}
			
		}
	}

	private boolean isPressed(ICyberwareUserData data, int keyCode)
	{
		if (keyCode < 0)
		{
			keyCode = keyCode + 100;
			return Mouse.isButtonDown(keyCode);
		}
		else if (keyCode > 900)
		{
			boolean shiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

			keyCode = keyCode - 900;
			return Keyboard.isKeyDown(keyCode) && shiftPressed;
		}
		else
		{
			if (data.getHotkey(keyCode + 900) != null)
			{
				boolean shiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

				return Keyboard.isKeyDown(keyCode) && !shiftPressed;
			}
			return Keyboard.isKeyDown(keyCode);
		}
	}
}
