package flaxbeard.cyberware.common.item;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToAccessFieldException;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.IMenuItem;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.lib.LibConstants;
import flaxbeard.cyberware.common.network.CyberwarePacketHandler;
import flaxbeard.cyberware.common.network.DodgePacket;

public class ItemBrainUpgrade extends ItemCyberware implements IMenuItem
{
    public ItemBrainUpgrade(String name, EnumSlot slot, String[] subnames)
    {
        super(name, slot, subnames);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

    }


    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other)
    {
        return other.getItem() == this && stack.getItemDamage() == 0 && other.getItemDamage() == 2;
    }

    @SubscribeEvent
    public void handleTeleJam(EnderTeleportEvent event)
    {
        EntityLivingBase te = event.getEntityLiving();
        ItemStack jam = new ItemStack(this, 1, 1);

        if (CyberwareAPI.isCyberwareInstalled(te, jam) && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(te, jam)))
        {
            event.setCanceled(true);
            return;
        }
        if (te != null)
        {
            float range = 25F;
            List<EntityLivingBase> test = te.world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(te.posX - range, te.posY - range, te.posZ - range, te.posX + te.width + range, te.posY + te.height + range, te.posZ + te.width + range));
            for (EntityLivingBase e : test)
            {
                if (te.getDistance(e) <= range)
                {
                    if (CyberwareAPI.isCyberwareInstalled(e, jam) && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(e, jam)))
                    {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }

    }

    @SubscribeEvent
    public void handleClone(PlayerEvent.Clone event)
    {
        if (event.isWasDeath())
        {
            EntityPlayer p = event.getOriginal();

            if (CyberwareAPI.isCyberwareInstalled(p, new ItemStack(this, 1, 0)) && !p.world.getGameRules().getBoolean("keepInventory"))
            {
				/*float range = 5F;
				List<EntityXPOrb> orbs = p.world.getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(p.posX - range, p.posY - range, p.posZ - range, p.posX + p.width + range, p.posY + p.height + range, p.posZ + p.width + range));
				for (EntityXPOrb orb : orbs)
				{
					orb.setDead();
				}*/

                if (!p.world.isRemote)
                {
                    ItemStack stack = new ItemStack(CyberwareContent.expCapsule);
                    NBTTagCompound c = new NBTTagCompound();
                    c.setInteger("xp", p.experienceTotal);
                    stack.setTagCompound(c);
                    EntityItem item = new EntityItem(p.world, p.posX, p.posY, p.posZ, stack);
                    p.world.spawnEntity(item);
                }
            }
            else if (CyberwareAPI.isCyberwareInstalled(p, new ItemStack(this, 1, 2)) && !p.world.getGameRules().getBoolean("keepInventory"))
            {
                event.getEntityPlayer().addExperience((int) (Math.min(100, p.experienceLevel * 7) * .9F));
            }
        }
    }

    @SubscribeEvent
    public void handleMining(BreakSpeed event)
    {
        EntityPlayer p = event.getEntityPlayer();

        ItemStack test = new ItemStack(this, 1, 3);
        if (CyberwareAPI.isCyberwareInstalled(p, test) && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(p, test)) && isContextWorking(p) && !p.isSneaking())
        {
            IBlockState state = event.getState();
            ItemStack tool = p.getHeldItem(EnumHand.MAIN_HAND);

            if (!tool.isEmpty() && (tool.getItem() instanceof ItemSword || tool.getItem().getUnlocalizedName().contains("sword"))) return;

            if (isToolEffective(tool, state)) return;

            for (int i = 0; i < 10; i++)
            {
                if (i != p.inventory.currentItem)
                {
                    ItemStack potentialTool = p.inventory.mainInventory.get(i);
                    if (isToolEffective(potentialTool, state))
                    {
                        p.inventory.currentItem = i;
                        return;
                    }
                }
            }
        }
    }

    private static Map<UUID, Boolean> isContextWorking = new HashMap<UUID, Boolean>();
    private static Map<UUID, Boolean> isMatrixWorking = new HashMap<UUID, Boolean>();
    private static Map<UUID, Boolean> isRadioWorking = new HashMap<UUID, Boolean>();

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public void handleLivingUpdate(CyberwareUpdateEvent event)
    {
        EntityLivingBase e = event.getEntityLiving();

        ItemStack test = new ItemStack(this, 1, 3);
        if (e.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(e, test) && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(e, test)))
        {
            isContextWorking.put(e.getUniqueID(), CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)));
        }

        test = new ItemStack(this, 1, 4);
        if (e.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(e, test))
        {
            isMatrixWorking.put(e.getUniqueID(), CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)));
        }

        test = new ItemStack(this, 1, 5);
        if (e.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(e, test) && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(e, test)))
        {
            isRadioWorking.put(e.getUniqueID(), CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)));
        }

    }

    public static boolean isRadioWorking(EntityLivingBase e)
    {
        if (!isRadioWorking.containsKey(e.getUniqueID()))
        {
            isRadioWorking.put(e.getUniqueID(), Boolean.FALSE);
        }

        return isRadioWorking.get(e.getUniqueID());
    }

    private boolean isContextWorking(EntityLivingBase e)
    {
        if (!isContextWorking.containsKey(e.getUniqueID()))
        {
            isContextWorking.put(e.getUniqueID(), Boolean.FALSE);
        }

        return isContextWorking.get(e.getUniqueID());
    }

    private boolean isMatrixWorking(EntityLivingBase e)
    {
        if (!isMatrixWorking.containsKey(e.getUniqueID()))
        {
            isMatrixWorking.put(e.getUniqueID(), Boolean.FALSE);
        }

        return isMatrixWorking.get(e.getUniqueID());
    }

    public boolean isToolEffective(ItemStack tool, IBlockState state)
    {
        if (!tool.isEmpty())
        {
            for (String toolType : tool.getItem().getToolClasses(tool))
            {
                if (state.getBlock().isToolEffective(toolType, state))
                {
                    return true;
                }
            }
        }
        return false;
    }


    @SubscribeEvent
    public void handleXPDrop(LivingExperienceDropEvent event)
    {
        EntityLivingBase e = event.getEntityLiving();
        if (CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 0)) || CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 2)))
        {
            event.setCanceled(true);
        }
    }

    private static ArrayList<String> lastHits = new ArrayList<String>();

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void handleHurt(LivingAttackEvent event)
    {
        EntityLivingBase e = event.getEntityLiving();

        if (CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 4)) && isMatrixWorking(e))
        {

            if (!e.world.isRemote && event.getSource() instanceof EntityDamageSource)
            {
                //Entity attacker = ((EntityDamageSource) event.getSource()).getSourceOfDamage();
                Entity attacker = ((EntityDamageSource) event.getSource()).getTrueSource();
                if (e instanceof EntityPlayer)
                {
                    String str = e.getEntityId() + " " + e.ticksExisted + " " + attacker.getEntityId();
                    if (lastHits.contains(str))
                    {
                        return;
                    }
                    else
                    {
                        lastHits.add(str);
                    }
                }

                boolean armor = false;
                for (ItemStack stack : e.getArmorInventoryList())
                {
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemArmor)
                    {
                        if (((ItemArmor) stack.getItem()).getArmorMaterial().getDamageReductionAmount(EntityEquipmentSlot.CHEST) > 4)
                        {
                            return;
                        }
                    }
                    else if (!stack.isEmpty() && stack.getItem() instanceof ISpecialArmor)
                    {
                        if (((ISpecialArmor) stack.getItem()).getProperties(e, stack, event.getSource(), event.getAmount(), 1).AbsorbRatio * 25D > 4)
                        {
                            return;
                        }
                    }

                    if (!stack.isEmpty())
                    {
                        armor = true;
                    }

                }


                if (!((float) e.hurtResistantTime > (float) e.maxHurtResistantTime / 2.0F))
                {
                    Random random = e.getRNG();
                    if (random.nextFloat() < (armor ? LibConstants.DODGE_ARMOR : LibConstants.DODGE_NO_ARMOR))
                    {
                        event.setCanceled(true);
                        e.hurtResistantTime = e.maxHurtResistantTime;
                        e.hurtTime = e.maxHurtTime = 10;

                        ReflectionHelper.setPrivateValue(EntityLivingBase.class, e, 9999F, "lastDamage", "field_110153_bc");

                        CyberwarePacketHandler.INSTANCE.sendToAllAround(new DodgePacket(e.getEntityId()), new TargetPoint(e.world.provider.getDimension(), e.posX, e.posY, e.posZ, 50));
                    }
                }
            }
        }
    }

    @Override
    public int getPowerConsumption(ItemStack stack)
    {
        return stack.getItemDamage() == 3 ? LibConstants.CONTEXTUALIZER_CONSUMPTION :
                stack.getItemDamage() == 4 ? LibConstants.MATRIX_CONSUMPTION :
                        stack.getItemDamage() == 5 ? LibConstants.RADIO_CONSUMPTION: 0;
    }


    @Override
    public boolean hasMenu(ItemStack stack)
    {
        return stack.getItemDamage() == 1 || stack.getItemDamage() == 3 || stack.getItemDamage() == 5;
    }


    @Override
    public void use(Entity e, ItemStack stack)
    {
        EnableDisableHelper.toggle(stack);
    }


    @Override
    public String getUnlocalizedLabel(ItemStack stack)
    {
        return EnableDisableHelper.getUnlocalizedLabel(stack);
    }


    private static final float[] f = new float[] { 1F, 0F, 0F };

    @Override
    public float[] getColor(ItemStack stack)
    {
        return EnableDisableHelper.isEnabled(stack) ? f : null;
    }
}
