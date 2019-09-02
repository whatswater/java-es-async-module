package me.maxwell.asyncmodule;

import lombok.Data;

import java.util.Set;

@Data
public class ModuleListenerList {
    private Object moduleExport;
    private Set<ModuleInfo> moduleInfoSet;
}
