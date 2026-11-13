package com.sneakyzippy.powerlab.core.permission;

interface IShizukuShellService {
    void destroy() = 16777114;
    String runCommand(String command) = 1;
}
