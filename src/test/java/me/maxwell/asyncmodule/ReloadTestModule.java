package me.maxwell.asyncmodule;

import static org.junit.jupiter.api.Assertions.*;

public class ReloadTestModule implements Module {
    private static Symbol<Integer> SYMBOL_MAX_INT_VALUE = new Symbol<>("MAX_INT_VALUE");

    @Override
    public void register(ModuleInfo moduleInfo) {
        moduleInfo.require(ModuleSystemTest.ArticleService.class, SYMBOL_MAX_INT_VALUE.getKey());
    }

    @Override
    public void onRequireResolved(ModuleInfo moduleInfo, ModuleInfo requireModuleInfo, String name) {
        Integer integer = requireModuleInfo.getExport(SYMBOL_MAX_INT_VALUE);
        assertTrue(integer == Integer.MAX_VALUE);
        moduleInfo.setModuleLoaded();
    }

    @Override
    public void onRequireMissed(ModuleInfo moduleInfo, ModuleInfo requireModuleInfo, String name) {

    }

    @Override
    public void onReloadRequireResolved(ModuleInfo moduleInfo, ModuleInfo requireModuleInfo, String name) {
        System.out.println("11111111111111111111111");
    }

    @Override
    public void onReloadRequireMissed(ModuleInfo moduleInfo, ModuleInfo requireModuleInfo, String name) {

    }
}
