package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.machines.EnergyInfuserMachine;
import org.bukkit.Material;

/** Registers completed clean-room DracFun machine implementations. */
public final class DracFunMachineRegistry {

    private DracFunMachineRegistry() {}

    public static int registerEnergyInfuser(SFDracFun2 addon) {
        String id = "DRACFUN_ENERGY_INFUSER";
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemGroup group = DracFunItemGroups.machines(addon);
        SlimefunItemStack stack = new SlimefunItemStack(
                id,
                Material.RESPAWN_ANCHOR,
                "&dEnergy Infuser",
                "&7Charges powered DracFun modular equipment.",
                "&71000 J = 1 item charge unit.");
        new EnergyInfuserMachine(group, stack).register(addon);
        return 1;
    }
}
