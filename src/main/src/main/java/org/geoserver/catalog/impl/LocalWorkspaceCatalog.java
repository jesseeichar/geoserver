/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LayerGroupWorkspaceInclusion;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.LocalWorkspaceCatalogFilter;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.sort.SortBy;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Catalog decorator handling cases when a {@link LocalWorkspace} is set.
 * <p>
 * This wrapper handles some additional cases that {@link LocalWorkspaceCatalogFilter} can not 
 * handle by simple filtering.
 * </p> 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class LocalWorkspaceCatalog extends AbstractCatalogDecorator implements Catalog {

	private GeoServer geoServer;

    public LocalWorkspaceCatalog(Catalog delegate) {
        super(delegate);
    }

	public void setGeoServer(GeoServer geoServer ) {
		this.geoServer = geoServer;
	}

    @Override
    public StyleInfo getStyleByName(String name) {
        if (LocalWorkspace.get() != null) {
            StyleInfo style = super.getStyleByName(LocalWorkspace.get(), name);
            if (style != null) {
                return style;
            }
        }
        return super.getStyleByName(name);
    }


    @Override
    public LayerInfo getLayer(String id) {
        return wrap(super.getLayer(id));
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        if (LocalWorkspace.get() != null) {
            String wsName = LocalWorkspace.get().getName();

            //prefix the unqualified name
            if (name.contains(":")) {
                //name already prefixed, ensure it is prefixed with the correct one
                if (name.startsWith(wsName+":")) {
                    //good to go, just pass call through
                    return wrap(super.getLayerByName(name));
                }
                else {
                    //JD: perhaps strip of existing prefix?
                }
            }

            //prefix it explicitly
            NamespaceInfo ns = super.getNamespaceByPrefix(LocalWorkspace.get().getName());
            LayerInfo layer = super.getLayerByName(new NameImpl(ns.getURI(), name));
            return wrap(layer);
        }
        return super.getLayerByName(name);
    }

    @Override
    public LayerInfo getLayerByName(Name name) {
        if (LocalWorkspace.get() != null) {
            //if local workspace active drop the prefix
            return getLayerByName(name.getLocalPart());
        } else {
            return super.getLayerByName(name);
        }
    }

    @Override
    public List<LayerInfo> getLayers() {
        if (useNameDequalifyingProxy()) {
            return NameDequalifyingProxy.createList(super.getLayers(),
                    LayerInfo.class, LocalWorkspace.get());
        }
        return super.getLayers();
    }

    private boolean useNameDequalifyingProxy() {
        WorkspaceInfo workspaceInfo = LocalWorkspace.get();
        boolean hidePrefix = geoServer == null || !geoServer.getSettings().isLocalWorkspaceIncludesPrefix();
        boolean useNameDequalifyingProxy = workspaceInfo != null && hidePrefix;
        return useNameDequalifyingProxy;
    }

    @Override
    public void add(LayerInfo layer) {
        super.add(unwrap(layer));
    }

    @Override
    public void save(LayerInfo layer) {
        super.save(unwrap(layer));
    }

    
    @Override
    public void remove(LayerInfo layer) {
        super.remove(unwrap(layer));
    }

    @Override
    public LayerInfo detach(LayerInfo layer) {
        return super.detach(unwrap(layer));
    }

    @Override
    public List<RuntimeException> validate(LayerInfo layer, boolean isNew) {
        return super.validate(unwrap(layer), isNew);
    }

    LayerInfo wrap(LayerInfo layer) {
        return wrap(layer, LayerInfo.class);
    }

    LayerInfo unwrap(LayerInfo layer) {
        return NameDequalifyingProxy.unwrap(layer);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return wrap(super.getLayerGroup(id));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        if (LocalWorkspace.get() != null) {
            LayerGroupInfo layerGroup = super.getLayerGroupByName(LocalWorkspace.get(), name);
            if (layerGroup != null) {
                return wrap(layerGroup);
            }
            // else fall back on unqualified lookup
        }

        return wrap(super.getLayerGroupByName(name));
    }

    /*
     * check that the layer group workspace matches the 
     */
    LayerGroupInfo check(LayerGroupInfo layerGroup) {
        if (LocalWorkspace.get() != null) {
            if (layerGroup.getWorkspace() != null && 
                !LocalWorkspace.get().equals(layerGroup.getWorkspace())) {
                return null;
            }
        }
        return layerGroup;
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return wrap(super.getLayerGroupByName(workspaceName, name));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace,
            String name) {
        return wrap(super.getLayerGroupByName(workspace, name));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        // Need to check the setting controlling which layer groups are included in the
        // local workspace
        //
        // getLayerGroupInclusion() will check if a local workspace is set and will
        // ensure all groups are returned if there is no local workspace
        LayerGroupWorkspaceInclusion layerGroupInclusion = getLayerGroupInclusion();

        if (layerGroupInclusion == LayerGroupWorkspaceInclusion.NONE) {
            return Collections.emptyList();
        }

        List<LayerGroupInfo> layerGroups = super.getLayerGroups();
        if (layerGroupInclusion == LayerGroupWorkspaceInclusion.ALL) {
            return wrap(layerGroups);
        } else {
            boolean allContained = layerGroupInclusion == LayerGroupWorkspaceInclusion.ALL_CONTAINED;
            ArrayList<LayerGroupInfo> filteredList = Lists.newArrayList(Iterators.filter(layerGroups.iterator(),
                            new LayerGroupInclusionFilterPredicate(allContained)));
            return wrap(filteredList);
        }
    }

    /**
     * Check the setting that controlls which LayerGroups are included.
     *
     * If there is no Geoserver it will use the default {@link LayerGroupWorkspaceInclusion#AT_LEAST_ONE_CONTAINED}
     * value.
     *
     * If there is no localworkspace then naturally all groups will be returned.
     *
     * @see SettingsInfo#getLayerGroupInclusion()
     */
    private LayerGroupWorkspaceInclusion getLayerGroupInclusion() {
        if (LocalWorkspace.get() == null) {
            return LayerGroupWorkspaceInclusion.ALL;
        } else if(this.geoServer == null) {
            return LayerGroupWorkspaceInclusion.AT_LEAST_ONE_CONTAINED;
        } else {
            return this.geoServer.getSettings().getLayerGroupInclusion();
        }
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return wrap(super.getLayerGroupsByWorkspace(workspaceName));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(
            WorkspaceInfo workspace) {
        return wrap(super.getLayerGroupsByWorkspace(workspace));
    }

    public void add(LayerGroupInfo layerGroup) {
        super.add(unwrap(layerGroup));
    }

    public void save(LayerGroupInfo layerGroup) {
        super.save(unwrap(layerGroup));
    }

    public void remove(LayerGroupInfo layerGroup) {
        super.remove(unwrap(layerGroup));
    }
    
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return super.detach(unwrap(layerGroup));
    }

    public List<RuntimeException> validate(LayerGroupInfo layerGroup, boolean isNew) {
        return super.validate(unwrap(layerGroup), isNew);
    }

    
    LayerGroupInfo wrap(LayerGroupInfo layerGroup) {
        return wrap(layerGroup, LayerGroupInfo.class);
    }

    <T> T wrap(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (useNameDequalifyingProxy()) {
            return NameDequalifyingProxy.create(obj, clazz, LocalWorkspace.get());
        }
        return obj;
    }
    
    <T> T unwrap(T obj) {
        return NameDequalifyingProxy.unwrap(obj);
    }

    List<LayerGroupInfo> wrap(List<LayerGroupInfo> layerGroups) {
        if (useNameDequalifyingProxy()) {
            return NameDequalifyingProxy.createList(layerGroups, LayerGroupInfo.class, LocalWorkspace.get());
        }
        return layerGroups;
    }

    static class NameDequalifyingProxy implements WrappingProxy, Serializable {

        Object object;
        private String workspacePrefix;

        NameDequalifyingProxy(Object object, String workspacePrefix) {
            this.object = object;
            this.workspacePrefix = workspacePrefix+':';
        }

        public Object getProxyObject() {
            return object;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if ("prefixedName".equals(method.getName()) || 
                "getPrefixedName".equals(method.getName()) || 
                "getName".equals(method.getName())) {
                String val = (String) method.invoke(object, args);
                if (val == null || !val.startsWith(workspacePrefix)) {
                    return val;
                }

                return val.split(":", 2)[1];
            }

            return method.invoke(object, args);
        }
    
        public static <T> T create( T object, Class<T> clazz, WorkspaceInfo workspaceInfo) {
            return ProxyUtils.createProxy(object, clazz, new NameDequalifyingProxy(object, workspaceInfo.getName()));
        }

        public static <T> List<T> createList(List<T> object, Class<T> clazz, final WorkspaceInfo workspaceInfo) {
            return new ProxyList(object, clazz) {
                @Override
                protected <T> T createProxy(T proxyObject, Class<T> proxyInterface) {
                    return create(proxyObject, proxyInterface, workspaceInfo);
                }

                @Override
                protected <T> T unwrapProxy(T proxy, Class<T> proxyInterface) {
                    return unwrap(proxy);
                }
            };
        }

        public static <T> T unwrap( T object ) {
            return ProxyUtils.unwrap(object, NameDequalifyingProxy.class);
        }

    }
    
    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return delegate.count(of, filter);
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {
        return wrap(delegate.get(type, filter), type);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return list(of, filter, (Integer) null, (Integer) null, (SortBy) null);
    }

    /**
     * Returns a decorating iterator over the one returned by the delegate that wraps every object
     * it returns, if possible.
     * 
     * @see #wrap(Object, Class)
     * @see org.geoserver.catalog.Catalog#list(java.lang.Class, org.geoserver.catalog.Predicate,
     *      java.lang.Integer, java.lang.Integer, org.geoserver.catalog.OrderBy)
     */
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(final Class<T> of,
            final Filter filter, final Integer offset, final Integer count, final SortBy sortBy) {

        CloseableIterator<T> iterator;

        if (LayerGroupInfo.class.isAssignableFrom(of)) {
            // Need to check the setting controlling which layer groups are included in the
            // local workspace
            //
            // getLayerGroupInclusion() will check if a local workspace is set and will
            // ensure all groups are returned if there is no local workspace
            LayerGroupWorkspaceInclusion layerGroupInclusion = getLayerGroupInclusion();

            if(layerGroupInclusion == LayerGroupWorkspaceInclusion.NONE) {
                return CloseableIteratorAdapter.empty();
            }

            iterator = new ClearLocalWorkspaceThreadLocalIterator(delegate.list(of, filter, offset, count, sortBy));

            if (layerGroupInclusion != LayerGroupWorkspaceInclusion.ALL) {
                boolean allContained = layerGroupInclusion == LayerGroupWorkspaceInclusion.ALL_CONTAINED;
                iterator = CloseableIteratorAdapter.filter(iterator, new LayerGroupInclusionFilterPredicate(allContained));
            }
        } else {
            iterator = delegate.list(of, filter, offset, count, sortBy);
        }

        Function<T, T> wrappingFunction = new Function<T, T>() {

            final Class<T> type = of;

            @Override
            public T apply(T catalogObject) {
                return wrap(catalogObject, type);
            }
        };

        return CloseableIteratorAdapter.transform(iterator, wrappingFunction);
    }

    public void removeListeners(Class listenerClass) {
        delegate.removeListeners(listenerClass);
    }

    /**
     * A predicate used for filtering out LayerGroups that are not allowed as
     * configured by the {@link SettingsInfo#getLayerGroupInclusion()}
     *
     * This filter only applies to the two cases:
     * <ul>
     *          <li>{@link LayerGroupWorkspaceInclusion#ALL_CONTAINED}</li>
     *          <li>{@link LayerGroupWorkspaceInclusion#AT_LEAST_ONE_CONTAINED}</li>
     * </ul>
     *
     * The other two options are trivial and more efficiently implemented without filtering
     * the collection.
     *
     * @author Jesse Eichar
     */
    private static class LayerGroupInclusionFilterPredicate implements Predicate<LayerGroupInfo>, Filter {
        private boolean allContained;

        /**
         * Constructor.
         *
         * @param allContained 
         *      If true then all the layers contained in the layer group MUST
         *      be contained within the current workspace.  Otherwise only one 
         *      of the layers has to be contained in the current workspace.
         */
        public LayerGroupInclusionFilterPredicate(boolean allContained) {
            this.allContained = allContained;
        }

        @Override
        public boolean apply(@Nullable LayerGroupInfo input) {
            if (input == null || input.getLayers() == null) {
                return false;
            } else {
                WorkspaceInfo localWorkspace = LocalWorkspace.get();
                List<PublishedInfo> layers = input.getLayers();

                for (PublishedInfo info : layers) {
                    if (info == null) {
                        continue;
                    }
                    boolean contained;
                    if (info instanceof LayerGroupInfo) {
                        LayerGroupInfo layerGroup = (LayerGroupInfo) info;

                        // If the layer group is in the current workspace then we know all
                        // contained layers are also in localWorkspace. 
                        contained = localWorkspace.equals(layerGroup.getWorkspace());
                        if (!contained) {
                            // we can't shortcut so we need to investigate each sublayer
                            contained = apply(layerGroup);
                        }
                    } else {
                        LayerInfo layer = (LayerInfo) info;
                        ResourceInfo resource = layer.getResource();
                        String prefix = resource.getNamespace().getPrefix();
                        contained = localWorkspace.getName().equals(prefix);
                    }

                    // We do short-circuit checks here to
                    // see if we can bail out of the processing
                    if (contained && !allContained) {
                        return true;
                    } else if (!contained && allContained) {
                        return false;
                    }
                }

                return allContained;
            }
        }

        @Override
        public boolean evaluate(Object object) {
            return apply((LayerGroupInfo) object);
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return extraData;
        }
    }
    
    /**
     * Clear the {@link LocalWorkspace} thread local before calling the corresponding method on
     * the delegate iterator.
     * 
     * This is used in the list method when listing LayerGroups.  The reason for this is that
     * when listing layergroups we want all layers listed then filtering afterwords.  This removes
     * the {@link LocalWorkspaceCatalogFilter} (in effect) so that all layers are listed then filtered
     * in this method.
     * 
     * @author Jesse
     *
     * @param <T> contained type of iterator
     */
    private static final class ClearLocalWorkspaceThreadLocalIterator<T> implements CloseableIterator<T> {

        private CloseableIterator<T> delegate;

        public ClearLocalWorkspaceThreadLocalIterator(CloseableIterator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            WorkspaceInfo oldVal = LocalWorkspace.get();
            try {
                LocalWorkspace.remove();
                return delegate.hasNext();
            } finally {
                LocalWorkspace.set(oldVal);
            }
        }

        @Override
        public T next() {
            WorkspaceInfo oldVal = LocalWorkspace.get();
            try {
                LocalWorkspace.remove();
                return delegate.next();
            } finally {
                LocalWorkspace.set(oldVal);
            }
        }

        @Override
        public void remove() {
            WorkspaceInfo oldVal = LocalWorkspace.get();
            try {
                LocalWorkspace.remove();
                delegate.remove();
            } finally {
                LocalWorkspace.set(oldVal);
            }
        }

        @Override
        public void close() {
            WorkspaceInfo oldVal = LocalWorkspace.get();
            try {
                LocalWorkspace.remove();
                delegate.close();
            } finally {
                LocalWorkspace.set(oldVal);
            }

        }
        
    }
}
