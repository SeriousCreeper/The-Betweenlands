package thebetweenlands.api.loot;

import java.util.List;

import net.minecraft.util.Tuple;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;

public abstract class LootTableView extends LootTable {
	public LootTableView() {
		super(new LootPool[0]);
	}

	@Override
	public final boolean isFrozen() {
		return true;
	}

	/**
	 * Returns all loot pools in this view
	 * @param manager
	 * @return
	 */
	public abstract List<Tuple<LootTableView, LootPool>> getPools(LootTableManager manager);

	/**
	 * Returns the primary shared loot pool this view belongs to
	 * @return
	 */
	public abstract ISharedLootPool getPrimarySharedLootPool();
}
