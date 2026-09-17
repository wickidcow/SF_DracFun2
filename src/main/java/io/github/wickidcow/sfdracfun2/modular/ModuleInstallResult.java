package io.github.wickidcow.sfdracfun2.modular;

/** Result of validating one module installation into a modular DracFun item. */
public enum ModuleInstallResult {
    VALID,
    UNSUPPORTED_TIER,
    MODULE_TIER_TOO_HIGH,
    INCOMPATIBLE_GEAR,
    FAMILY_LIMIT_REACHED,
    MODULE_POINTS_EXCEEDED
}
