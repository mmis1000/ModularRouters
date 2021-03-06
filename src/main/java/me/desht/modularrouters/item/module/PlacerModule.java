package me.desht.modularrouters.item.module;

import me.desht.modularrouters.client.util.TintColor;
import me.desht.modularrouters.core.ModItems;
import me.desht.modularrouters.logic.compiled.CompiledPlacerModule;

public class PlacerModule extends ItemModule {

    private static final TintColor TINT_COLOR = new TintColor(240, 208, 208);

    public PlacerModule() {
        super(ModItems.defaultProps(), CompiledPlacerModule::new);
    }

    @Override
    public TintColor getItemTint() {
        return TINT_COLOR;
    }
}
