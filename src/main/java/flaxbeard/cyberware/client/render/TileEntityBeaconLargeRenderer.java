package flaxbeard.cyberware.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

import flaxbeard.cyberware.client.ClientUtils;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.block.tile.TileEntityBeaconPost.TileEntityBeaconPostMaster;

public class TileEntityBeaconLargeRenderer extends TileEntitySpecialRenderer<TileEntityBeaconPostMaster>
{
	private static ModelBeaconLarge model = new ModelBeaconLarge();
	private static String texture = "cyberware:textures/models/radio.png";
	private static String texture2 = "cyberware:textures/models/radio_base.png";

	@Override
	public void render(TileEntityBeaconPostMaster te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if (te != null)
		{
			float ticks = Minecraft.getMinecraft().player.ticksExisted + partialTicks;

			
			IBlockState state = te.getWorld().getBlockState(te.getPos());
			if (state.getBlock() == CyberwareContent.radioPost)
			{
				boolean showIcon = true;
				GlStateManager.pushMatrix();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				GlStateManager.translate(x + 0.5, y + 10.5, z + 0.5);

				ClientUtils.bindTexture(texture);
				model.render(null, 0, 0, 0, 0, 0, 0.0625F);
				ClientUtils.bindTexture(texture2);
				model.renderBase(null, 0, 0, 0, 0, 0, 0.0625F);
				GlStateManager.popMatrix();
			}
		}
	}

}
