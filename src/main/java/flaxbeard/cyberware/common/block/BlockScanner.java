package flaxbeard.cyberware.common.block;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.block.item.ItemBlockCyberware;
import flaxbeard.cyberware.common.block.tile.TileEntityScanner;

public class BlockScanner extends BlockContainer
{

	public BlockScanner()
	{
		super(Material.IRON);
		setHardness(5.0F);
		setResistance(10.0F);
		setSoundType(SoundType.METAL);
		
		String name = "scanner";
		
		this.setRegistryName(name);
		ForgeRegistries.BLOCKS.register(this);

		ItemBlock ib = new ItemBlockCyberware(this,"cyberware.tooltip.scanner");
		ib.setRegistryName(name);
		ForgeRegistries.ITEMS.register(ib);
		
		this.setTranslationKey(Cyberware.MODID + "." + name);

		this.setCreativeTab(Cyberware.creativeTab);
		GameRegistry.registerTileEntity(TileEntityScanner.class, new ResourceLocation(Cyberware.MODID, name));
		
		CyberwareContent.blocks.add(this);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(IBlockState state)
	{
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta)
	{
		return new TileEntityScanner();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
	{
		return EnumBlockRenderType.MODEL;
	}
	
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		if (stack.hasDisplayName())
		{
			TileEntity tileentity = worldIn.getTileEntity(pos);

			if (tileentity instanceof TileEntityScanner)
			{
				((TileEntityScanner) tileentity).setCustomInventoryName(stack.getDisplayName());
			}
		}
	}
	
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer entityPlayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if (tileentity instanceof TileEntityScanner)
		{
			if (entityPlayer.isCreative() && entityPlayer.isSneaking())
			{
				TileEntityScanner scanner = ((TileEntityScanner) tileentity);
				scanner.ticks = CyberwareConfig.SCANNER_TIME - 200;
			}
			entityPlayer.openGui(Cyberware.INSTANCE, 3, worldIn, pos.getX(), pos.getY(), pos.getZ());
		}
		
		return true;
		
	}
	
	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{ 
		TileEntity tileentity = worldIn.getTileEntity(pos);

		if (tileentity instanceof TileEntityScanner && !worldIn.isRemote)
		{
			TileEntityScanner scanner = (TileEntityScanner) tileentity;
			
			for (int i = 0; i < scanner.slots.getSlots(); i++)
			{
				ItemStack stack = scanner.slots.getStackInSlot(i);
				if (!stack.isEmpty())
				{
					InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
				}
			}
		}
		super.breakBlock(worldIn, pos, state);

	}

}
