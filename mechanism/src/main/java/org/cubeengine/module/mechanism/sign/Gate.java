/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.mechanism.sign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.mechanism.MechanismData;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.server.ServerLocation;

public class Gate implements SignMechanism
{
    public static final String NAME = "gate";
    public static final int HORIZONTAL_LIMIT = 16;
    private static final int VERTICAL_LIMIT = 16;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public ItemStack makeSign(ItemStack signStack)
    {
        signStack.offer(Keys.CUSTOM_NAME, Component.text("[Mechanism]", NamedTextColor.GOLD).append(Component.space()).append(Component.text(NAME, NamedTextColor.DARK_AQUA)));
        signStack.offer(MechanismData.MECHANISM, NAME);
        signStack.offer(Keys.LORE, Arrays.asList(Component.text(NAME, NamedTextColor.YELLOW)));
        return signStack;
    }

    @Override
    public boolean interact(InteractBlockEvent event, ServerPlayer player, ServerLocation loc, boolean hidden)
    {
        if (hidden)
        {
            return false;
        }

        final BlockType gateBlockType = loc.get(MechanismData.GATE_BLOCK_TYPE).flatMap(s -> RegistryTypes.BLOCK_TYPE.get().findValue(ResourceKey.resolve(s))).orElse(null);
        if (gateBlockType == null)
        {
            final Optional<BlockType> blockType = player.itemInHand(HandTypes.MAIN_HAND).type().block();
            final Boolean isValidGateMaterial = blockType.map(bt ->
                                                                  bt.isAnyOf(BlockTypes.IRON_BARS, BlockTypes.ACACIA_FENCE,
                                                                             BlockTypes.BIRCH_FENCE, BlockTypes.CRIMSON_FENCE,
                                                                             BlockTypes.DARK_OAK_FENCE, BlockTypes.JUNGLE_FENCE,
                                                                             BlockTypes.NETHER_BRICK_FENCE, BlockTypes.OAK_FENCE,
                                                                             BlockTypes.SPRUCE_FENCE, BlockTypes.WARPED_FENCE)
                                                             ).orElse(false);
            if (isValidGateMaterial)
            {
                loc.blockEntity().get().offer(MechanismData.GATE_BLOCK_TYPE, blockType.get().key(RegistryTypes.BLOCK_TYPE).asString());
                loc.blockEntity().get().transform(Keys.SIGN_LINES, list -> {
                    list.set(2, blockType.get().asComponent().color(NamedTextColor.DARK_GRAY));
                    return list;
                });
                return true;
            }
            return false;
        }

        final GateBlocks gateBlocks = new GateBlocks(loc, gateBlockType);
        if (!gateBlocks.findGate())
        {
            return false;
        }
        gateBlocks.toggle(player);
        return true;
    }

    @Override
    public List<Component> initLines(List<Component> lines)
    {
        lines.set(2, Component.text("?TYPE?", NamedTextColor.DARK_RED));
        lines.set(3, Component.text(0, NamedTextColor.DARK_RED));
        return lines;
    }

    private static class GateBlocks
    {
        private final List<ServerLocation> upperFrameBlocks = new ArrayList<>();
        private final List<ServerLocation> gateBlocks = new ArrayList<>();
        private final ServerLocation mainFrameBlock;
        private final ServerLocation signLoc;
        private final BlockType gateBlockType;
        private final Direction leftDir;
        private final Direction rightDir;
        private int emptyBlocks = 0;

        public GateBlocks(ServerLocation signLoc, BlockType gateBlockType)
        {
            this.signLoc = signLoc;
            this.gateBlockType = gateBlockType;
            final Direction attachedDir = signLoc.get(Keys.DIRECTION).get();
            this.mainFrameBlock = signLoc.relativeTo(attachedDir.opposite());

            switch (attachedDir) {
                case NORTH:
                    leftDir = Direction.WEST;
                    rightDir = Direction.EAST;
                    break;
                case EAST:
                    leftDir = Direction.NORTH;
                    rightDir = Direction.SOUTH;
                    break;
                case SOUTH:
                    leftDir = Direction.EAST;
                    rightDir = Direction.WEST;
                    break;
                case WEST:
                    leftDir = Direction.SOUTH;
                    rightDir = Direction.NORTH;
                    break;
                default:
                    leftDir = null;
                    rightDir = null;
            }

        }

        public boolean findGate()
        {

            if (leftDir == null || rightDir == null)
            {
                return false;
            }
            if (findFrame(leftDir) || findFrame(rightDir))
            {
                this.countEmptyBlocks();
                return true;
            }
            return false;
        }

        private void countEmptyBlocks()
        {
            int count = 0;
            for (ServerLocation frameBlock : this.upperFrameBlocks)
            {
                ServerLocation downBlock = frameBlock.relativeTo(Direction.DOWN);
                for (int i = 0; i < VERTICAL_LIMIT; i++)
                {
                    downBlock = downBlock.relativeTo(Direction.DOWN);
                    if (downBlock.blockY() == this.mainFrameBlock.blockY())
                    {
                        i = 0; // Reset height when reaching the main frame-block
                    }
                    if (downBlock.blockType().isAnyOf(BlockTypes.AIR, BlockTypes.WATER))
                    {
                        this.gateBlocks.add(downBlock);
                        count++;
                    }
                    else if (!downBlock.blockType().isAnyOf(this.gateBlockType))
                    {
                         break;
                    }
                    else
                    {
                        this.gateBlocks.add(downBlock);
                    }
                }
            }
            this.emptyBlocks = count;
        }

        private boolean findFrame(Direction dir)
        {
            this.upperFrameBlocks.clear();
            ServerLocation relativeBlock = mainFrameBlock;
            // Search to left and right for gate blocks or air
            for (int i = 0; i < HORIZONTAL_LIMIT; i++) // left first
            {
                relativeBlock = relativeBlock.relativeTo(dir);
                final BlockType blockType = relativeBlock.blockType();
                if (blockType.isAnyOf(BlockTypes.AIR, BlockTypes.WATER) || blockType.isAnyOf(gateBlockType))
                {
                    final ServerLocation frameBlock = this.isValidColumn(relativeBlock);
                    if (frameBlock != null)
                    {
                        this.upperFrameBlocks.add(frameBlock);
                    }
                    else
                    {
                        // air block with no valid frame block above
                        return false;
                    }
                } else {
                    return i > 0; // reached other frame side no gate on this side?
                }
            }
            return false; // Gate not closed or too big
        }

        private ServerLocation isValidColumn(ServerLocation startBlock)
        {
            ServerLocation upBlock = startBlock;
            boolean lastUpGate = false;
            for (int i = 0; i < VERTICAL_LIMIT; i++) {
                upBlock = upBlock.relativeTo(Direction.UP);
                if (upBlock.blockType().isAnyOf(BlockTypes.AIR, BlockTypes.WATER))
                {
                    lastUpGate = false;
                    continue;
                }
                if (upBlock.blockType().isAnyOf(gateBlockType))
                {
                    lastUpGate = true;
                }
                else
                {
                    if (lastUpGate)
                    {
                        return upBlock;
                    }
                    return null;
                }
            }
            return null;
        }

        public void toggle(ServerPlayer player)
        {
            int availableBlocks = signLoc.get(MechanismData.GATE_BLOCKS).orElse(0);
            boolean allAvailable = availableBlocks >= this.emptyBlocks;
            boolean creative = player.gameMode().get() == GameModes.CREATIVE.get(); // TODO creative handling?
            if (this.emptyBlocks == 0) // closed gate?
            {
                for (ServerLocation block : this.gateBlocks)
                {
                    final Boolean isWater = block.get(Keys.IS_WATERLOGGED).orElse(block.blockType().isAnyOf(BlockTypes.WATER));
                    BlockState gateBlock = isWater ? BlockTypes.WATER.get().defaultState() : BlockTypes.AIR.get().defaultState();
                    block.setBlock(gateBlock);
                }
                availableBlocks = this.gateBlocks.size();
            }
            else
            {
                if (!allAvailable)
                {
                    final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
                    if (itemInHand.type().block().map(bt -> bt.isAnyOf(this.gateBlockType)).orElse(false))
                    {
                        final int additionalBlockFromHand = Math.min(itemInHand.quantity(), this.emptyBlocks - availableBlocks);
                        itemInHand.setQuantity(itemInHand.quantity() - additionalBlockFromHand);
                        player.setItemInHand(HandTypes.MAIN_HAND, itemInHand);
                        availableBlocks += additionalBlockFromHand;
                    }
                }

                this.gateBlocks.sort(Comparator.comparing(ServerLocation::blockY).reversed());
                for (ServerLocation block : this.gateBlocks)
                {
                    if (block.blockType().isAnyOf(gateBlockType))
                    {
                        continue;
                    }
                    if (availableBlocks <= 0)
                    {
                        break;
                    }
                    availableBlocks--;
                    BlockState gateBlock = this.gateBlockType.defaultState();
                    gateBlock = gateBlock.with(Keys.IS_WATERLOGGED, block.get(Keys.IS_WATERLOGGED).orElse(block.blockType().isAnyOf(BlockTypes.WATER))).orElse(gateBlock);
                    gateBlock = gateBlock.with(Keys.CONNECTED_DIRECTIONS, new HashSet<>(Arrays.asList(this.leftDir, this.rightDir))).orElse(gateBlock);
                    block.setBlock(gateBlock, BlockChangeFlags.ALL);

                }
            }
            signLoc.blockEntity().get().offer(MechanismData.GATE_BLOCKS, availableBlocks);
            final TextComponent newAvailableText = Component.text(availableBlocks, allAvailable ? NamedTextColor.DARK_GRAY : NamedTextColor.DARK_RED);
            signLoc.blockEntity().get().transform(Keys.SIGN_LINES, list -> {
                list.set(3, newAvailableText);
                return list;
            });
        }

    }


}
