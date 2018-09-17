package thebetweenlands.common.entity.mobs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import thebetweenlands.common.entity.ai.EntityAIHurtByTargetImproved;
import thebetweenlands.common.entity.projectiles.EntitySapSpit;
import thebetweenlands.common.registries.BlockRegistry;

public abstract class EntitySpiritTreeFace extends EntityWallFace {
	public EntitySpiritTreeFace(World world) {
		super(world);
	}

	@Override
	protected void initEntityAI() {
		super.initEntityAI();

		this.targetTasks.addTask(0, new EntityAIHurtByTargetImproved(this, true));
		this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, false));

		this.tasks.addTask(0, new AITrackTarget(this));
		this.tasks.addTask(1, new AIAttackMelee(this, 1, true));
		this.tasks.addTask(2, new AISpit(this));
	}

	@Override
	public boolean isActive() {
		//return this.world.getDifficulty() != EnumDifficulty.PEACEFUL;
		return true;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	@Override
	public boolean canResideInBlock(BlockPos pos) {
		return this.world.getBlockState(pos).getBlock() == BlockRegistry.LOG_SPIRIT_TREE;
	}

	@Override
	public boolean canMoveFaceInto(BlockPos pos) {
		return this.world.isAirBlock(pos);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if(this.isMoving() && this.world.isRemote) {
			if(this.ticksExisted % 3 == 0) {
				EnumFacing facing = this.getFacing();
				double px = this.posX + facing.getFrontOffsetX() * this.width / 2;
				double py = this.posY + this.height / 2 + facing.getFrontOffsetY() * this.height / 2;
				double pz = this.posZ + facing.getFrontOffsetZ() * this.width / 2;
				for(int i = 0; i < 24; i++) {
					double rx = (this.world.rand.nextDouble() - 0.5D) * this.width;
					double ry = (this.world.rand.nextDouble() - 0.5D) * this.height;
					double rz = (this.world.rand.nextDouble() - 0.5D) * this.width;
					BlockPos pos = new BlockPos(px + rx, py + ry, pz + rz);
					IBlockState state = this.world.getBlockState(pos);
					if(!state.getBlock().isAir(state, this.world, pos)) {
						double mx = facing.getFrontOffsetX() * 0.15F + (this.world.rand.nextDouble() - 0.5D) * 0.25F;
						double my = facing.getFrontOffsetY() * 0.15F + (this.world.rand.nextDouble() - 0.5D) * 0.25F;
						double mz = facing.getFrontOffsetZ() * 0.15F + (this.world.rand.nextDouble() - 0.5D) * 0.25F;
						this.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, px + rx, py + ry, pz + rz, mx, my, mz, Block.getStateId(state));
					}
				}
			}
		}
	}

	@Override
	protected void fixUnsuitablePosition() {
		if(this.ticksExisted % 5 == 0) {
			for(int i = 0; i < 50; i++) {
				float rx = this.world.rand.nextFloat() * 2 - 1;
				float ry = this.world.rand.nextFloat() * 2 - 1;
				float rz = this.world.rand.nextFloat() * 2 - 1;
				BlockPos rndPos = new BlockPos(this.posX + this.world.rand.nextInt(8) - 4, this.posY + this.height / 2 + this.world.rand.nextInt(8) - 4, this.posZ + this.world.rand.nextInt(8) - 4);
				Vec3d pos = new Vec3d(rndPos.getX() + 0.5D, rndPos.getY() + 0.5D, rndPos.getZ() + 0.5D);
				if(this.canAnchorAt(pos, new Vec3d(rx, ry, rz))) {
					this.lookHelper.setLookDirection(rx, ry, rz);
					this.moveHelper.setMoveTo(pos.x, pos.y, pos.z, 1);
					break;
				}
			}
		}
	}

	public List<BlockPos> findNearbyWoodBlocks() {
		final int radius = 8;
		BlockPos center = new BlockPos(this);
		List<BlockPos> blocks = new ArrayList<>();
		MutableBlockPos pos = new MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius / 2; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					pos.setPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					IBlockState state = this.world.getBlockState(pos);
					if (state.getBlock() == BlockRegistry.LOG_SPIRIT_TREE) {
						blocks.add(pos.toImmutable());
					}
				}
			}
		}
		return blocks;
	}

	public static class AITrackTarget extends EntityAIBase {
		protected final EntitySpiritTreeFace entity;

		protected int findWoodCooldown = 0;
		protected List<BlockPos> woodBlocks;

		protected int checkCooldown = 0;

		public AITrackTarget(EntitySpiritTreeFace entity) {
			this.entity = entity;
			this.setMutexBits(1);
		}

		@Override
		public boolean shouldExecute() {
			return !this.entity.isMoving() && this.entity.getAttackTarget() != null && !this.entity.getEntitySenses().canSee(this.entity.getAttackTarget());
		}

		@Override
		public void startExecuting() {
			this.checkCooldown = 0;
			this.findWoodCooldown = 0;
			this.woodBlocks = null;
		}

		@Override
		public void updateTask() {
			if(this.findWoodCooldown == 0) {
				this.findWoodCooldown = 20 + this.entity.rand.nextInt(40);
				this.woodBlocks = this.entity.findNearbyWoodBlocks();
			}

			if(this.woodBlocks != null && this.checkCooldown == 0) {
				this.checkCooldown = 5 + this.entity.rand.nextInt(15);

				for(int i = 0; i < 6; i++) {
					BlockPos pos = this.woodBlocks.get(this.entity.rand.nextInt(this.woodBlocks.size()));
					Vec3d center = new Vec3d(pos.getX() + this.entity.getBlockWidth() / 2.0D, pos.getY() + this.entity.getBlockHeight() / 2.0D, pos.getZ() + this.entity.getBlockWidth() / 2.0D);
					Vec3d lookPos = this.entity.getAttackTarget().getPositionEyes(1);

					EnumFacing facing = EnumFacing.getFacingFromVector((float)(lookPos.x - center.x), (float)(lookPos.y - center.y), (float)(lookPos.z - center.z));

					if(this.canSeeFrom(pos, facing, this.entity.getAttackTarget()) && this.entity.canAnchorAt(center, lookPos)) {
						this.entity.moveHelper.setMoveTo(center.x, center.y, center.z, 1);
						this.entity.lookHelper.setLookDirection(facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ());
						break;
					}
				}
			}

			this.checkCooldown--;
			this.findWoodCooldown--;
		}

		protected boolean canSeeFrom(BlockPos pos, EnumFacing facing, Entity entity) {
			return this.entity.world.rayTraceBlocks(new Vec3d(pos.getX() + this.entity.getBlockWidth() / 2.0D + facing.getFrontOffsetX() * (this.entity.width / 2 + this.entity.getPeek()), pos.getY() + this.entity.getBlockHeight() / 2.0D + facing.getFrontOffsetY() * (this.entity.height / 2 + this.entity.getPeek()), pos.getZ() + this.entity.getBlockWidth() / 2.0D + facing.getFrontOffsetZ() * (this.entity.width / 2 + this.entity.getPeek())), new Vec3d(entity.posX, entity.posY + (double)entity.getEyeHeight(), entity.posZ), false, true, false) == null;
		}

		@Override
		public boolean shouldContinueExecuting() {
			return !this.entity.isMoving() && this.entity.getAttackTarget() != null && !this.entity.getEntitySenses().canSee(this.entity.getAttackTarget());
		}
	}

	public static class AIAttackMelee extends EntityAIAttackMelee {
		public AIAttackMelee(EntityCreature creature, double speedIn, boolean useLongMemory) {
			super(creature, speedIn, useLongMemory);
		}

		@Override
		protected double getAttackReachSqr(EntityLivingBase attackTarget) {
			return this.attacker.width * this.attacker.width;
		}
	}

	public static class AISpit extends EntityAIBase {
		protected final EntitySpiritTreeFace entity;

		protected int cooldown = 0;

		public AISpit(EntitySpiritTreeFace entity) {
			this.entity = entity;
			this.setMutexBits(0);
		}

		@Override
		public boolean shouldExecute() {
			return !this.entity.isMoving() && this.entity.getAttackTarget() != null;
		}

		@Override
		public void startExecuting() {
			this.cooldown = 20 + this.entity.rand.nextInt(40);
		}

		@Override
		public void updateTask() {
			if(this.cooldown <= 0 && this.entity.getEntitySenses().canSee(this.entity.getAttackTarget())) {
				this.cooldown = 50 + this.entity.rand.nextInt(120);

				Entity target = this.entity.getAttackTarget();
				EnumFacing facing = this.entity.getFacing();

				EntitySapSpit spit = new EntitySapSpit(this.entity.world, this.entity);
				spit.setPosition(this.entity.posX + facing.getFrontOffsetX() * (this.entity.width / 2 + 0.1F), this.entity.posY + this.entity.height / 2.0F + facing.getFrontOffsetY() * (this.entity.height / 2 + 0.1F), this.entity.posZ + facing.getFrontOffsetZ() * (this.entity.width / 2 + 0.1F));

				double dx = target.posX - spit.posX;
				double dy = target.getEntityBoundingBox().minY + (double)(target.height / 3.0F) - spit.posY;
				double dz = target.posZ - spit.posZ;
				double dist = (double)MathHelper.sqrt(dx * dx + dz * dz);
				spit.shoot(dx, dy + dist * 0.20000000298023224D, dz, 1, 1);

				this.entity.world.spawnEntity(spit);
			}
			this.cooldown--;
		}

		@Override
		public boolean shouldContinueExecuting() {
			return this.shouldExecute();
		}
	}
}
