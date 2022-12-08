package gg.dirtcraft.dirtutils.commands.core.result;

import gg.dirtcraft.dirtutils.commands.core.DirtCommandBase;

public interface CommandResult {

    /**
     * Says if the {@link DirtCommandBase} is executable when returning the result.
     *
     * @return true, if the command is executable, false, if otherwise.
     */
    boolean isExecutable();
}
