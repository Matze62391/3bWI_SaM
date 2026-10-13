package com.glodblock.github.extendedae.xmod.aae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder;
import net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Lets the pattern modifier edit Advanced AE's processing patterns without losing their input directions. Upstream
 * Advanced AE does this with a mixin into ExtendedAE; here ExtendedAE calls it directly. Only loaded with Advanced AE.
 */
public final class AAEPatterns {

    private AAEPatterns() {
    }

    public interface Replacer {
        void replace(GenericStack[] stacks, GenericStack[] des, AEKey replace, @Nullable AEKey with);
    }

    public interface Checker {
        boolean check(GenericStack[] stacks, int scale, boolean div);
    }

    public interface Modifier {
        void modify(GenericStack[] stacks, GenericStack[] des, int scale, boolean div);
    }

    /**
     * @return the replaced pattern, or null if this isn't an advanced processing pattern.
     */
    @Nullable
    public static ItemStack replace(IPatternDetails detail, ItemStack replace, ItemStack with, Replacer replacer) {
        if (!(detail instanceof AdvProcessingPattern pattern)) {
            return null;
        }
        var input = pattern.getSparseInputs().toArray(new GenericStack[0]);
        var output = pattern.getOutputs().toArray(new GenericStack[0]);
        var replaceInput = new GenericStack[input.length];
        var replaceOutput = new GenericStack[output.length];
        replacer.replace(input, replaceInput, AEItemKey.of(replace), AEItemKey.of(with));
        replacer.replace(output, replaceOutput, AEItemKey.of(replace), AEItemKey.of(with));
        var newDirMap = new HashMap<AEKey, Direction>();
        for (var entry : pattern.getDirectionMap().entrySet()) {
            if (!with.isEmpty() && AEItemKey.matches(entry.getKey(), replace)) {
                newDirMap.put(AEItemKey.of(with), entry.getValue());
            } else {
                newDirMap.put(entry.getKey(), entry.getValue());
            }
        }
        return AdvPatternDetailsEncoder.encodeProcessingPattern(Arrays.asList(replaceInput), Arrays.asList(replaceOutput), newDirMap);
    }

    /**
     * @return the multiplied/divided pattern, {@link ItemStack#EMPTY} if it can't be changed by that factor, or null if
     *         this isn't an advanced processing pattern.
     */
    @Nullable
    public static ItemStack modify(IPatternDetails detail, int scale, boolean div, Checker checker, Modifier modifier) {
        if (!(detail instanceof AdvProcessingPattern pattern)) {
            return null;
        }
        var input = pattern.getSparseInputs().toArray(new GenericStack[0]);
        var output = pattern.getOutputs().toArray(new GenericStack[0]);
        if (!checker.check(input, scale, div) || !checker.check(output, scale, div)) {
            return ItemStack.EMPTY;
        }
        var mulInput = new GenericStack[input.length];
        var mulOutput = new GenericStack[output.length];
        modifier.modify(input, mulInput, scale, div);
        modifier.modify(output, mulOutput, scale, div);
        return AdvPatternDetailsEncoder.encodeProcessingPattern(Arrays.asList(mulInput), Arrays.asList(mulOutput), pattern.getDirectionMap());
    }

}
