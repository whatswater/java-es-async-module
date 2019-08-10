package me.maxwell.asyncmodule;

import lombok.Data;

import java.util.List;

@Data
public class ModuleListenerList {
    private Object moduleExport;
    private List<Module> moduleList;
}
