/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LayerGroupWorkspaceInclusion;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.feature.NameImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

public class LocalWorkspaceCatalogTest {

    LocalWorkspaceCatalog catalog;

    @Before
    public void setUp() throws Exception {
        WorkspaceInfo ws1 = createNiceMock(WorkspaceInfo.class);
        expect(ws1.getName()).andReturn("ws1").anyTimes();
        replay(ws1);

        NamespaceInfo ns1 = createNiceMock(NamespaceInfo.class);
        expect(ns1.getPrefix()).andReturn("ws1").anyTimes();
        expect(ns1.getURI()).andReturn("ws1").anyTimes();
        replay(ns1);

        WorkspaceInfo ws2 = createNiceMock(WorkspaceInfo.class);
        expect(ws2.getName()).andReturn("ws2").anyTimes();
        replay(ws2);

        NamespaceInfo ns2 = createNiceMock(NamespaceInfo.class);
        expect(ns2.getPrefix()).andReturn("ws2").anyTimes();
        expect(ns2.getURI()).andReturn("ws2").anyTimes();
        replay(ns2);

        StyleInfo s1 = createNiceMock(StyleInfo.class);
        expect(s1.getName()).andReturn("s1").anyTimes();
        expect(s1.getWorkspace()).andReturn(ws1).anyTimes();
        replay(s1);

        StyleInfo s2 = createNiceMock(StyleInfo.class);
        expect(s2.getName()).andReturn("s2").anyTimes();
        expect(s2.getWorkspace()).andReturn(ws2).anyTimes();
        replay(s2);

        LayerGroupInfo lg1 = createNiceMock(LayerGroupInfo.class);
        expect(lg1.getName()).andReturn("lg:1").anyTimes();
        expect(lg1.getWorkspace()).andReturn(ws1).anyTimes();
        replay(lg1);

        LayerGroupInfo lg2 = createNiceMock(LayerGroupInfo.class);
        expect(lg2.getName()).andReturn("lg2").anyTimes();
        expect(lg2.getWorkspace()).andReturn(ws2).anyTimes();
        replay(lg2);

        FeatureTypeInfo ft1 = createNiceMock(FeatureTypeInfo.class);
        expect(ft1.getName()).andReturn("l1").anyTimes();
        expect(ft1.getNamespace()).andReturn(ns1).anyTimes();
        replay(ft1);
        
        LayerInfo l1 = createNiceMock(LayerInfo.class);
        expect(l1.getName()).andReturn("ws1:l1").anyTimes();
        expect(l1.getResource()).andReturn(ft1).anyTimes();
        replay(l1);

        FeatureTypeInfo ft2 = createNiceMock(FeatureTypeInfo.class);
        expect(ft2.getName()).andReturn("l2").anyTimes();
        expect(ft2.getNamespace()).andReturn(ns2).anyTimes();
        replay(ft2);
        
        LayerInfo l2 = createNiceMock(LayerInfo.class);
        expect(l2.getName()).andReturn("ws2:l2").anyTimes();
        expect(l2.getResource()).andReturn(ft2).anyTimes();
        replay(l2);

        // set up layer name collisions: lc
        // use same name, but different featuretypeinfo objects
        // pointing to different workspaces
        LayerInfo lc1 = createNiceMock(LayerInfo.class);
        expect(lc1.getName()).andReturn("ws1:lc").anyTimes();
        expect(lc1.getResource()).andReturn(ft1).anyTimes();
        replay(lc1);

        LayerInfo lc2 = createNiceMock(LayerInfo.class);
        expect(lc2.getName()).andReturn("ws2:lc").anyTimes();
        expect(lc2.getResource()).andReturn(ft2).anyTimes();
        replay(lc2);

        Catalog cat = createNiceMock(Catalog.class);

        expect(cat.getWorkspaces()).andReturn(Arrays.asList(ws1,ws2)).anyTimes();
        expect(cat.getWorkspaceByName("ws1")).andReturn(ws1).anyTimes();
        expect(cat.getWorkspaceByName("ws2")).andReturn(ws2).anyTimes();
        expect(cat.getNamespaceByPrefix("ws1")).andReturn(ns1).anyTimes();
        expect(cat.getNamespaceByPrefix("ws2")).andReturn(ns2).anyTimes();
        
        expect(cat.getStyleByName("ws1", "s1")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName(ws1, "s1")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName("s1")).andReturn(null).anyTimes();
        
        expect(cat.getStyleByName("ws2", "s2")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName(ws2, "s2")).andReturn(s1).anyTimes();
        expect(cat.getStyleByName("s2")).andReturn(null).anyTimes();
        
        expect(cat.getLayerGroupByName("ws1", "lg:1")).andReturn(lg1).anyTimes();
        expect(cat.getLayerGroupByName(ws1, "lg:1")).andReturn(lg1).anyTimes();
        expect(cat.getLayerGroupByName("lg:1")).andReturn(null).anyTimes();
        
        expect(cat.getLayerGroupByName("ws2", "lg2")).andReturn(lg2).anyTimes();
        expect(cat.getLayerGroupByName(ws2, "lg2")).andReturn(lg2).anyTimes();
        expect(cat.getLayerGroupByName("lg2")).andReturn(null).anyTimes();

        //expect(cat.getLayerByName("ws1", "l1")).andReturn(l1).anyTimes();
        //expect(cat.getLayerByName(ws1, "l1")).andReturn(l1).anyTimes();
        expect(cat.getLayerByName(new NameImpl("ws1", "l1"))).andReturn(l1).anyTimes();
        expect(cat.getLayerByName("l1")).andReturn(null).anyTimes();

        //expect(cat.getLayerByName("ws2", "l2")).andReturn(l2).anyTimes();
        //expect(cat.getLayerByName(ws2, "l2")).andReturn(l2).anyTimes();
        expect(cat.getLayerByName(new NameImpl("ws2", "l2"))).andReturn(l2).anyTimes();
        expect(cat.getLayerByName("l2")).andReturn(null).anyTimes();

        // with namespace prefixes, return the appropriate layer info
        expect(cat.getLayerByName(new NameImpl("ws1", "lc"))).andReturn(lc1).anyTimes();
        expect(cat.getLayerByName(new NameImpl("ws2", "lc"))).andReturn(lc2).anyTimes();
        // return back the first one without a namespace prefix
        expect(cat.getLayerByName("lc")).andReturn(lc1).anyTimes();

        List<LayerInfo> layers = new ArrayList<LayerInfo>(2);
        layers.add(l1);
        layers.add(lc1);
        expect(cat.getLayers()).andReturn(layers).anyTimes();
        expect(cat.list(LayerInfo.class, Filter.INCLUDE, 
                (Integer) null, (Integer) null, (SortBy) null))
            .andReturn(new CloseableIteratorAdapter<LayerInfo>(layers.iterator())).anyTimes();
        
        List<LayerGroupInfo> layerGroups = new ArrayList<LayerGroupInfo>(1);
        layerGroups.add(lg1);
        
        expect(cat.getLayerGroups()).andReturn(layerGroups).anyTimes();
        expect(cat.list(LayerGroupInfo.class, Filter.INCLUDE, 
                (Integer) null, (Integer) null, (SortBy) null))
            .andReturn(new CloseableIteratorAdapter<LayerGroupInfo>(layerGroups.iterator())).anyTimes();
        

        replay(cat);
        
        catalog = new LocalWorkspaceCatalog(cat);

        LocalWorkspace.remove();
    }

    @After
    public void tearDown() {
        LocalWorkspace.remove();
    }
    @Test 
    public void testGetStyleByName() throws Exception {
        assertNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("ws1");
        WorkspaceInfo ws2 = catalog.getWorkspaceByName("ws2");
        
        LocalWorkspace.set(ws1);
        assertNotNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));

        LocalWorkspace.remove();
        assertNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));

        LocalWorkspace.set(ws2);
        assertNull(catalog.getStyleByName("s1"));
        assertNotNull(catalog.getStyleByName("s2"));

        LocalWorkspace.remove();
        assertNull(catalog.getStyleByName("s1"));
        assertNull(catalog.getStyleByName("s2"));
    }

    @Test
    public void testGetLayerGroupByName() throws Exception {
        assertNull(catalog.getLayerGroupByName("lg:1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("ws1");
        WorkspaceInfo ws2 = catalog.getWorkspaceByName("ws2");
        
        LocalWorkspace.set(ws1);
        assertNotNull(catalog.getLayerGroupByName("lg:1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.remove();
        assertNull(catalog.getLayerGroupByName("lg:1"));
        assertNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.set(ws2);
        assertNull(catalog.getLayerGroupByName("lg:1"));
        assertNotNull(catalog.getLayerGroupByName("lg2"));

        LocalWorkspace.remove();
        assertNull(catalog.getLayerGroupByName("lg:1"));
        assertNull(catalog.getLayerGroupByName("lg2"));
    }

    @Test
    public void testGetLayerByName() throws Exception {
        assertNull(catalog.getLayerByName("l1"));
        assertNull(catalog.getLayerByName("l2"));

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("ws1");
        WorkspaceInfo ws2 = catalog.getWorkspaceByName("ws2");

        LocalWorkspace.set(ws1);
        assertNotNull(catalog.getLayerByName("l1"));
        assertNull(catalog.getLayerByName("l2"));
        LocalWorkspace.remove();

        LocalWorkspace.set(ws2);
        assertNull(catalog.getLayerByName("l1"));
        assertNotNull(catalog.getLayerByName("l2"));
        LocalWorkspace.remove();

        assertNull(catalog.getLayerByName("l1"));
        assertNull(catalog.getLayerByName("l2"));
    }

    @Test
    public void testGetLayersWithSameName() throws Exception {
        LayerInfo layerInfo1 = catalog.getLayerByName(new NameImpl("ws1", "lc"));
        ResourceInfo resource1 = layerInfo1.getResource();
        NamespaceInfo namespace1 = resource1.getNamespace();
        String nsPrefix1 = namespace1.getPrefix();

        LayerInfo layerInfo2 = catalog.getLayerByName(new NameImpl("ws2", "lc"));
        ResourceInfo resource2 = layerInfo2.getResource();
        NamespaceInfo namespace2 = resource2.getNamespace();
        String nsPrefix2 = namespace2.getPrefix();

        assertEquals("Invalid namespace prefix", "ws1", nsPrefix1);
        assertEquals("Invalid namespace prefix", "ws2", nsPrefix2);
    }

    /**
     * The setting says to not include the prefix.  This is default behaviour
     */
    @Test
    public void testGetNonPrefixedLayerNames() {
        boolean includePrefix = false;
        boolean setLocalWorkspace = true;
        boolean createGeoServer = true;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    /**
     * No geoserver instance has been set.  This means there is no access to geoserver.
     * this should not happen but we want to protect against this consideration.  In this case
     * we have a local workspace set and we will use the default behaviour (no prefix)
     */
    @Test
    public void testGetNoGeoserverPrefixedLayerNameBehaviour() {
        boolean includePrefix = false;
        boolean setLocalWorkspace = true;
        boolean createGeoServer = false;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    /**
     * No local workspace is set this means the prefix should be included since the global capabilities
     * is probably being created.
     * 
     * The No Geoserver part is just to verify there are no nullpointer exceptions because of a coding error
     */
    @Test
    public void testGetNoGeoserverLocalWorkspacePrefixedLayerNameBehaviour() {
        boolean includePrefix = true;
        boolean setLocalWorkspace = false;
        boolean createGeoServer = false;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }
    
    /**
     * No localworkspace so prefix should be included since the global capabilities
     * is probably being created.
     */
    @Test
    public void testGetNoLocalWorkspacePrefixedLayerNameBehaviour() {
        boolean includePrefix = true;
        boolean setLocalWorkspace = false;
        boolean createGeoServer = true;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    /**
     * The setting is set to include the prefixes.
     */
    @Test
    public void testGetPrefixedLayerNames() {
        boolean includePrefix = true;
        boolean setLocalWorkspace = true;
        boolean createGeoServer = true;
        assertPrefixInclusion(includePrefix, setLocalWorkspace, createGeoServer);
    }

    private void assertPrefixInclusion(boolean includePrefix,
            boolean setLocalWorkspace, boolean createGeoServer) {
        if (createGeoServer ) {
            SettingsInfo settings = createNiceMock(SettingsInfo.class);
            expect(settings.isLocalWorkspaceIncludesPrefix()).andReturn(includePrefix)
                    .anyTimes();
            replay(settings);

            GeoServer geoServer = createNiceMock(GeoServer.class);
            expect(geoServer.getSettings()).andReturn(settings).anyTimes();
            replay(geoServer);

            catalog.setGeoServer(geoServer);
        }

        if (setLocalWorkspace) {
            WorkspaceInfo workspaceByName = catalog.getWorkspaceByName("ws1");
            LocalWorkspace.set(workspaceByName);
        }

        checkLayerNamePrefixInclusion(includePrefix, catalog.getLayers().iterator());
        
        checkLayerNamePrefixInclusion(includePrefix, catalog.list(LayerInfo.class, Filter.INCLUDE));
        
        checkLayerNamePrefixInclusion(false, catalog.getLayerGroups().iterator());

        checkLayerNamePrefixInclusion(false, catalog.list(LayerGroupInfo.class, Filter.INCLUDE));

        assertEquals(includePrefix, catalog.getLayerByName(new NameImpl("ws1", "l1")).getName().startsWith("ws1:"));
    }

    private void checkLayerNamePrefixInclusion(boolean includePrefix,
            Iterator<? extends PublishedInfo> layers) {
        while(layers.hasNext()) {
            PublishedInfo layerInfo = layers.next();
            String message;
            if(includePrefix) {
                message = layerInfo.getName() + " should contain a : because the prefix should have been kept";
            } else {
                message = layerInfo.getName() + " should contain not a : because the prefix should have been removed";
            }
            assertEquals(message, includePrefix, layerInfo.getName().startsWith("ws1:"));
        }
    }

    /**
     * When the workspace layerGroupVisibility setting is set to {@link LayerGroupWorkspaceInclusion#ALL_CONTAINED}
     * then only the layergroups that have all layers in the local workspace
     * should be shown in the LocalWorkspaceCatalog.
     */
    @Test
    public void testLayerGroupsInclusionALL_CONTAINED() {
        assertLayerGroupsInLocalWorkspace(true, true, LayerGroupWorkspaceInclusion.ALL_CONTAINED, "lgAll", "lgSubAll");
    }

    /**
     * When the workspace layerGroupVisibility setting is set to {@link LayerGroupWorkspaceInclusion#AT_LEAST_ONE_CONTAINED}
     * then only the layergroups that have a layer in the local workspace
     * should be shown in the LocalWorkspaceCatalog.
     */
    @Test
    public void testLayerGroupsInclusionAT_LEAST_ONE_CONTAINED() {
        assertLayerGroupsInLocalWorkspace(true, true, LayerGroupWorkspaceInclusion.AT_LEAST_ONE_CONTAINED, "lg1", "lgAll", "lgsub1", "lgSubAll", "lgSub2WS");
    }

    /**
     * When the workspace layerGroupVisibility setting is set to {@link LayerGroupWorkspaceInclusion#ALL}
     * then all layergroups should be shown in all LocalWorkspace Catalogs.
     */
    @Test
    public void testLayerGroupsInclusionALL() {
        assertLayerGroupsInLocalWorkspace(true, true, LayerGroupWorkspaceInclusion.ALL, "lgAll", "lg1", "lgNone", "lgsub1", "lgSubAll", "lgSub2WS", "lgSubNone");
    }

    /**
     * When the workspace layerGroupVisibility setting is set to {@link LayerGroupWorkspaceInclusion#NONE}
     * then no layergroups should be shown in all LocalWorkspace Catalogs.
     */
    @Test
    public void testLayerGroupsInclusionNONE() {
        assertLayerGroupsInLocalWorkspace(true, true, LayerGroupWorkspaceInclusion.NONE);
    }

    /**
     * When there is no local workspace set, all layergroups should be returned irregardless of what LayerGroupInclusion is.
     */
    @Test
    public void testLayerGroupsInclusionNoLocalWorkspace() {
        assertLayerGroupsInLocalWorkspace(true, false, LayerGroupWorkspaceInclusion.NONE, "lgAll", "lg1", "lgNone", "lgsub1", "lgSubAll", "lgSub2WS", "lgSubNone");        
    }

    /**
     * When there is no geoserver set, all layergroups should be returned irregardless of what LayerGroupInclusion is.
     *
     * This is mostly to guard against strange corner cases
     */
    @Test
    public void testLayerGroupsInclusionNoGeoserver() {
        assertLayerGroupsInLocalWorkspace(false, true, LayerGroupWorkspaceInclusion.NONE, "lgAll", "lg1", "lgsub1", "lgSubAll", "lgSub2WS");
    }

    /**
     * When there is no geoserver set and no localworkspace , all layergroups should be returned irregardless of what LayerGroupInclusion is.
     *
     * This is mostly to guard against strange corner cases
     */
    @Test
    public void testLayerGroupsInclusionNoGeoserverNoWorkspace() {
        assertLayerGroupsInLocalWorkspace(false, false, LayerGroupWorkspaceInclusion.NONE, "lgAll", "lg1", "lgNone", "lgsub1", "lgSubAll", "lgSub2WS", "lgSubNone");        
    }


    /**
     * The shared test setup and assertion code for the various LayerGroupsInclusion tests.
     *
     * @param createGeoserver if true create and set a {@link GeoServer} instance
     *                  on the LocalWorkspaceCatalog.
     * @param setLocalWorkspace if true set the LocalWorkspace threadLocal to ws1
     * @param inclusion the inclusion setting value to test
     * @param includedLayerGroupNames the names of the layerGrous that should be returned
     *                  when querying the LocalWorkspaceCatalog
     */
    private void assertLayerGroupsInLocalWorkspace(
            boolean createGeoserver,
            boolean setLocalWorkspace,
            LayerGroupWorkspaceInclusion inclusion,
            String... includedLayerGroupNames) {

        WorkspaceInfo ws1 = this.catalog.getWorkspaceByName("ws1");

        LayerInfo l1 = this.catalog.getLayerByName(new NameImpl("ws1", "l1"));
        LayerInfo l2 = this.catalog.getLayerByName(new NameImpl("ws2", "l2"));

        assertNotNull(l1);
        assertNotNull(l2);
        assertNotNull(ws1);

        LayerGroupInfo lg1 = createNiceMock("lg1", LayerGroupInfo.class);
        expect(lg1.getName()).andReturn("lg1").anyTimes();
        expect(lg1.getLayers()).andReturn(Arrays.<PublishedInfo>asList(l1, l2)).anyTimes();
        replay(lg1);

        LayerGroupInfo lgNone = createNiceMock("lgNone", LayerGroupInfo.class);
        expect(lgNone.getName()).andReturn("lgNone").anyTimes();
        expect(lgNone.getLayers()).andReturn(Arrays.<PublishedInfo>asList(l2)).anyTimes();
        replay(lgNone);

        LayerGroupInfo lgAll = createNiceMock("lgAll", LayerGroupInfo.class);
        expect(lgAll.getName()).andReturn("lgAll").anyTimes();
        expect(lgAll.getLayers()).andReturn(Arrays.<PublishedInfo>asList(l1)).anyTimes();
        replay(lgAll);

        LayerGroupInfo lgsub1 = createNiceMock("lgsub1", LayerGroupInfo.class);
        expect(lgsub1.getName()).andReturn("lgsub1").anyTimes();
        expect(lgsub1.getLayers()).andReturn(Arrays.<PublishedInfo>asList(lg1, lgNone, l2)).anyTimes();
        replay(lgsub1);

        LayerGroupInfo lgSubAll = createNiceMock("lgSubAll", LayerGroupInfo.class);
        expect(lgSubAll.getName()).andReturn("lgSubAll").anyTimes();
        expect(lgSubAll.getLayers()).andReturn(Arrays.<PublishedInfo>asList(lgAll)).anyTimes();
        replay(lgSubAll);

        LayerGroupInfo lgSub2WS = createNiceMock("lgSub2WS", LayerGroupInfo.class);
        expect(lgSub2WS.getName()).andReturn("lgSub2WS").anyTimes();
        expect(lgSub2WS.getLayers()).andReturn(Arrays.<PublishedInfo>asList(lgSubAll, lgsub1)).anyTimes();
        replay(lgSub2WS);

        LayerGroupInfo lgSubNone = createNiceMock("lgSubNone", LayerGroupInfo.class);
        expect(lgSubNone.getName()).andReturn("lgSubNone").anyTimes();
        expect(lgSubNone.getLayers()).andReturn(Arrays.<PublishedInfo>asList(lgNone)).anyTimes();
        replay(lgSubNone);

        ArrayList<LayerGroupInfo> allLayers = new ArrayList<LayerGroupInfo>();
        allLayers.add(lg1);
        allLayers.add(lgAll);
        allLayers.add(lgNone);
        allLayers.add(lgsub1);
        allLayers.add(lgSub2WS);
        allLayers.add(lgSubAll);
        allLayers.add(lgSubNone);

        Catalog backingCatalog = createNiceMock(Catalog.class);
        expect(backingCatalog.list(LayerGroupInfo.class, Filter.INCLUDE,
                    (Integer) null, (Integer) null, (SortBy) null))
           .andReturn(new CloseableIteratorAdapter<LayerGroupInfo>(allLayers.iterator()))
           .anyTimes();
        expect(backingCatalog.getLayerGroups())
            .andReturn(allLayers)
            .anyTimes();
        expect(backingCatalog.getLayerGroupByName((WorkspaceInfo)null, "lg1"))
            .andReturn(lg1)
            .anyTimes();
        expect(backingCatalog.getLayerGroupByName((WorkspaceInfo)null, "lgAll"))
            .andReturn(lgAll)
            .anyTimes();
        expect(backingCatalog.getLayerGroupByName((WorkspaceInfo)null, "lgNone"))
            .andReturn(lgNone)
            .anyTimes();
        expect(backingCatalog.getLayerGroupByName((WorkspaceInfo)null, "lgsub1"))
            .andReturn(lgsub1)
            .anyTimes();
        expect(backingCatalog.getLayerGroupByName((WorkspaceInfo)null, "lgSub2WS"))
            .andReturn(lgSub2WS)
            .anyTimes();
        expect(backingCatalog.getLayerGroupByName((WorkspaceInfo)null, "lgSubAll"))
            .andReturn(lgSubAll)
            .anyTimes();
        expect(backingCatalog.getLayerGroupByName((WorkspaceInfo)null, "lgSubNone"))
            .andReturn(lgSubNone)
            .anyTimes();
        replay(backingCatalog);

        LocalWorkspaceCatalog localCatalog = new LocalWorkspaceCatalog(backingCatalog);

        if (createGeoserver) {
            SettingsInfo settings = createNiceMock(SettingsInfo.class);
            expect(settings.getLayerGroupInclusion()).andReturn(inclusion).anyTimes();
            replay(settings);

            GeoServer geoServer = createNiceMock(GeoServer.class);
            expect(geoServer.getSettings()).andReturn(settings).anyTimes();
            replay(geoServer);

            localCatalog.setGeoServer(geoServer);
        }

        if (setLocalWorkspace) {
            LocalWorkspace.set(ws1);
        }

        List<String> expectedLayerNames = Arrays.asList(includedLayerGroupNames);

        checkCorrectLayerGroups(localCatalog.list(LayerGroupInfo.class, Filter.INCLUDE), expectedLayerNames);
        checkCorrectLayerGroups(localCatalog.getLayerGroups().iterator(), expectedLayerNames);
    }

    private void checkCorrectLayerGroups(
            Iterator<LayerGroupInfo> layerGroups,
            List<String> expectedLayerNames) {
        int count = 0;
        while(layerGroups.hasNext()) {
            LayerGroupInfo next = layerGroups.next();
            count ++;
            assertTrue(next.getName()+" should have been filtered out of the listed layerGroups", expectedLayerNames.contains(next.getName()));
        }

        assertEquals(count, expectedLayerNames.size());
    }
}
