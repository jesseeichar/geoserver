package org.geoserver.config;

/**
 * The possible values of the {@link SettingsInfo#getLayerGroupInclusion()} setting.
 *
 * This setting controls which layer groups are available in a LocalWorkspaceCatalog
 * when a LocalWorkspace is set.
 *
 * @author Jesse Eichar
 */
public enum LayerGroupWorkspaceInclusion {
    /**
     * Always include all LayerGroups in local workspace
     */
    ALL,
    /**
     * Never include LayerGroups in local workspace
     */
    NONE,
    /**
     * Include LayerGroups if at least one of the layers in the
     * LayerGroup is also in the LocalWorkspace
     */
    AT_LEAST_ONE_CONTAINED,
    /**
     * Only include a LayerGroup if all layers in the layer group 
     * are in the LocalWorkspace
     */
    ALL_CONTAINED
}
