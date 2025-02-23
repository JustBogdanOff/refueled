package bogdan.refueled.mixin.car.client;

import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.mixin.accessor.IBlockBehaviourAccess;
import com.dragn0007.dragnvehicles.vehicle.car.CarRender;
import com.dragn0007.dragnvehicles.vehicle.classic.ClassicRender;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.MotorcycleRender;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCarRender;
import com.dragn0007.dragnvehicles.vehicle.suv.SUVRender;
import com.dragn0007.dragnvehicles.vehicle.truck.TruckRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

@Mixin(value = {CarRender.class, ClassicRender.class, TruckRender.class, SUVRender.class, SportCarRender.class, MotorcycleRender.class})
public abstract class CarRenderMixin {
    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 0
    )
    private float shrinkRenderX(float originalX){
        return -ICarInvoker.sizeFactor.floatValue();
    }

    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 1
    )
    private float shrinkRenderY(float originalY){
        return -ICarInvoker.sizeFactor.floatValue();
    }

    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 2
    )
    private float shrinkRenderZ(float originalZ){
        return ICarInvoker.sizeFactor.floatValue();
    }

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation$KeyFrame;<init>(FFFF)V",
                    ordinal = 6
            ),
            index = 0
    )
    private static float modifyFrontKeyFrame(float original){
        return 1f;
    }

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation$KeyFrame;<init>(FFFF)V",
                    ordinal = 8
            ),
            index = 0
    )
    private static float modifyBackKeyFrame(float original){
        return 1f;
    }

    @Inject(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectCheck(@Coerce Object car, float rotation, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci){
        double modelToStackFactor = 1 / 16d;
        Entity vehicle = (Entity) car;

        int leftRight = Math.round((float) -Math.sin(Math.toRadians(vehicle.getYRot())));
        int frontBack = Math.round((float) Math.cos(Math.toRadians(vehicle.getYRot())));
        double wheelRadius = car instanceof Motorcycle ? 38d * 0.5d : 53d * 0.5d;

        Function<Vec3, BlockPos> find = pos -> {
            AABB area = new AABB(
                    pos.x - (frontBack * vehicle.getBbWidth() * 0.49d), pos.y, pos.z - ((leftRight * vehicle.getBbWidth() * 0.49d)),
                    pos.x + (frontBack * vehicle.getBbWidth() * 0.49d), pos.y, pos.z + (leftRight * vehicle.getBbWidth() * 0.49d));
            List<BlockPos> positions = BlockPos.betweenClosedStream(area).map(BlockPos::new).toList();
            BlockPos finalPos = null;
            double maxY = 0;

            for(BlockPos position : positions) {
                BlockState state = vehicle.level().getBlockState(position);

                if (!state.isAir() && ((IBlockBehaviourAccess) state.getBlock()).hasCollision()) {
                    if (state.getVisualShape(vehicle.level(), position, CollisionContext.empty()).max(Direction.Axis.Y) > maxY) {
                        finalPos = position;
                        maxY = state.getVisualShape(vehicle.level(), position, CollisionContext.empty()).max(Direction.Axis.Y);
                    }
                }
            }

            return finalPos;
        };

        Function<Vec3, BlockPos> check = anchorPos -> {
            int searchIndex = 1;
            double maxY = 0;
            BlockPos finalPos = null;

            while(searchIndex < 4){
                Vec3 pos = anchorPos;

                switch(searchIndex){
                    case 1:
                        if(find.apply(pos) != null){
                            searchIndex += 2;
                            break;
                        }
                        searchIndex++;
                    case 2:
                        pos = new Vec3(anchorPos.x, anchorPos.y - 1, anchorPos.z);
                        if(find.apply(pos) != null){
                            finalPos = find.apply(pos);
                            maxY = vehicle.level().getBlockState(find.apply(pos)).getVisualShape(vehicle.level(), find.apply(pos), CollisionContext.empty()).max(Direction.Axis.Y);
                        }
                        searchIndex++;
                    case 3:
                        pos = new Vec3(anchorPos.x, anchorPos.y - 1, anchorPos.z);
                        if(find.apply(pos) == null){
                            pos = new Vec3(pos.x, pos.y - 1, pos.z);
                            if(find.apply(pos) != null && vehicle.level().getBlockState(find.apply(pos)).getVisualShape(vehicle.level(), find.apply(pos), CollisionContext.empty()).max(Direction.Axis.Y) > maxY){
                                finalPos = find.apply(pos);
                            }
                        }
                        searchIndex++;
                }
            }

            return finalPos;
        };

        boolean opposite = false;
        BlockPos foundPos = null;
        Vec3[] anchors = new Vec3[4];
        anchors[0] = new Vec3(vehicle.getX() + (leftRight * wheelRadius * ICarInvoker.sizeFactor * modelToStackFactor), vehicle.getY() + 2, vehicle.getZ() + (frontBack * wheelRadius * modelToStackFactor * ICarInvoker.sizeFactor));
        anchors[1] = new Vec3(vehicle.getX() - (leftRight * wheelRadius * ICarInvoker.sizeFactor * modelToStackFactor), vehicle.getY() + 2, vehicle.getZ() - (frontBack * wheelRadius * modelToStackFactor * ICarInvoker.sizeFactor));
        anchors[2] = new Vec3(anchors[0].x + leftRight, anchors[0].y, anchors[0].z + frontBack);
        anchors[3] = new Vec3(anchors[1].x - leftRight, anchors[1].y, anchors[1].z - frontBack);
        double globalMaxY = vehicle.getY();

        for(int i = 0; i < 4; i++){
            if(check.apply(anchors[i]) != null){
                if(check.apply(anchors[i]).getY() + vehicle.level().getBlockState(check.apply(anchors[i])).getVisualShape(vehicle.level(), check.apply(anchors[i]), CollisionContext.empty()).max(Direction.Axis.Y) > globalMaxY){
                    foundPos = check.apply(anchors[i]);
                    globalMaxY = check.apply(anchors[i]).getY() + vehicle.level().getBlockState(check.apply(anchors[i])).getVisualShape(vehicle.level(), check.apply(anchors[i]), CollisionContext.empty()).max(Direction.Axis.Y);
                    opposite = i % 2 == 1;
                }
            }
        }

        if(foundPos == null){
            ((ICarInvoker) car).car$setOffsets(new double[3]);
            poseStack.mulPose(Axis.XP.rotation(0));
            return;
        }

        VoxelShape shape = vehicle.level().getBlockState(foundPos).getVisualShape(vehicle.level(), foundPos, CollisionContext.empty());
        double blockHeight = (foundPos.getY() + shape.max(Direction.Axis.Y)) - vehicle.getY(),
                edgeLengthX = leftRight * (opposite ? -1 : 1) > 0 ? shape.min(Direction.Axis.X) : shape.max(Direction.Axis.X),
                edgeLengthZ = frontBack * (opposite ? -1 : 1) > 0 ? shape.min(Direction.Axis.Z) : shape.max(Direction.Axis.Z),
                blockDistance = frontBack == 0 ? Math.max(vehicle.getX(), (foundPos.getX() + edgeLengthX)) - Math.min(vehicle.getX(), (foundPos.getX() + edgeLengthX)) : Math.max(vehicle.getZ(), (foundPos.getZ() + edgeLengthZ)) - Math.min(vehicle.getZ(), (foundPos.getZ() + edgeLengthZ)),
                bodyRadius = wheelRadius * modelToStackFactor * ICarInvoker.sizeFactor;
        blockDistance = Math.max(Math.abs(blockDistance), bodyRadius);
        blockHeight = Math.max(blockHeight, 0);

        double distance = Math.sqrt(Mth.square(2d * bodyRadius) - Mth.square((blockHeight / blockDistance) * bodyRadius)),
                radians = Math.acos(distance / (2d * bodyRadius)),
                offsetY = (modelToStackFactor * wheelRadius * Math.sin(radians)) - 1.5d + (modelToStackFactor * 17d * 0.5d) + ((1.5d - (modelToStackFactor * 17d * 0.5d)) * Math.cos(radians));

        ((ICarInvoker) car).car$setOffsets(new double[]{offsetY * ICarInvoker.sizeFactor, radians, (opposite ? -1d : 1d)});
        poseStack.translate(0, -offsetY, 0);
        poseStack.mulPose(Axis.XP.rotation((float) -radians * (opposite ? -1f : 1f)));
    }
}
