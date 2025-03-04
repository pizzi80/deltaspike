/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.deltaspike.jsf.impl.listener.request;

import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.spi.scope.window.WindowContext;
import org.apache.deltaspike.core.util.ClassDeactivationUtils;

import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseListener;
import jakarta.faces.lifecycle.ClientWindow;
import jakarta.faces.lifecycle.Lifecycle;
import org.apache.deltaspike.core.impl.scope.DeltaSpikeContextExtension;
import org.apache.deltaspike.core.impl.scope.viewaccess.ViewAccessContext;
import org.apache.deltaspike.jsf.impl.clientwindow.ClientSideClientWindow;
import org.apache.deltaspike.jsf.impl.clientwindow.LazyClientWindow;
import org.apache.deltaspike.jsf.spi.scope.window.ClientWindowConfig;

class DeltaSpikeLifecycleWrapper extends Lifecycle
{
    private final Lifecycle wrapped;

    private JsfRequestBroadcaster jsfRequestBroadcaster;
    private ClientWindowConfig clientWindowConfig;

    private WindowContext windowContext;
    private DeltaSpikeContextExtension contextExtension;

    private volatile Boolean initialized;

    DeltaSpikeLifecycleWrapper(Lifecycle wrapped)
    {
        this.wrapped = wrapped;
    }

    Lifecycle getWrapped()
    {
        return wrapped;
    }

    @Override
    public void addPhaseListener(PhaseListener phaseListener)
    {
        this.wrapped.addPhaseListener(phaseListener);
    }

    /**
     * Broadcasts
     * {@link org.apache.deltaspike.core.api.lifecycle.Initialized} and
     * {@link org.apache.deltaspike.core.api.lifecycle.Destroyed}
     * //TODO StartupEvent
     */
    @Override
    public void execute(FacesContext facesContext)
    {
        //can happen due to the window-handling of deltaspike
        if (facesContext.getResponseComplete())
        {
            return;
        }

        lazyInit();

        //TODO broadcastApplicationStartupBroadcaster();
        broadcastInitializedJsfRequestEvent(facesContext);

        // ClientWindow handling
        ClientWindow clientWindow = facesContext.getExternalContext().getClientWindow();
        if (clientWindow != null)
        {
            String windowId = clientWindow.getId();
            if (windowId != null)
            {
                windowContext.activateWindow(windowId);
            }
        }

        if (!FacesContext.getCurrentInstance().getResponseComplete())
        {
            this.wrapped.execute(facesContext);
        }
    }

    @Override
    public PhaseListener[] getPhaseListeners()
    {
        return this.wrapped.getPhaseListeners();
    }

    @Override
    public void removePhaseListener(PhaseListener phaseListener)
    {
        this.wrapped.removePhaseListener(phaseListener);
    }

    /**
     * Performs cleanup tasks after the rendering process
     */
    @Override
    public void render(FacesContext facesContext)
    {
        // prevent jfwid rendering
        boolean delegateWindowHandling = ClientWindowConfig.ClientWindowRenderMode.DELEGATED.equals(
                clientWindowConfig.getClientWindowRenderMode(facesContext));
        if (!delegateWindowHandling && facesContext.getExternalContext().getClientWindow() != null)
        {
            facesContext.getExternalContext().getClientWindow().disableClientWindowRenderMode(facesContext);
        }
        
        this.wrapped.render(facesContext);
        
        if (facesContext.getViewRoot() != null && facesContext.getViewRoot().getViewId() != null)
        {
            ViewAccessContext viewAccessContext = contextExtension.getViewAccessScopedContext();
            if (viewAccessContext != null)
            {
                viewAccessContext.onProcessingViewFinished(facesContext.getViewRoot().getViewId());
            }
        }
    }

    @Override
    public void attachWindow(FacesContext facesContext)
    {
        lazyInit();

        ClientWindowConfig.ClientWindowRenderMode clientWindowRenderMode =
                clientWindowConfig.getClientWindowRenderMode(facesContext);

        if (clientWindowRenderMode == ClientWindowConfig.ClientWindowRenderMode.DELEGATED)
        {
            this.wrapped.attachWindow(facesContext);
        }
        else
        {
            if (!facesContext.getResponseComplete())
            {
                if (clientWindowRenderMode == ClientWindowConfig.ClientWindowRenderMode.LAZY)
                {
                    facesContext.getExternalContext().setClientWindow(new LazyClientWindow());
                }
                else
                {
                    facesContext.getExternalContext().setClientWindow(new ClientSideClientWindow());
                }
            }
        }
    }

    private void broadcastInitializedJsfRequestEvent(FacesContext facesContext)
    {
        if (this.jsfRequestBroadcaster != null)
        {
            this.jsfRequestBroadcaster.broadcastInitializedJsfRequestEvent(facesContext);
        }
    }

    private void lazyInit()
    {
        if (this.initialized == null)
        {
            init();
        }
    }

    private synchronized void init()
    {
        // switch into paranoia mode
        if (this.initialized == null)
        {
            if (ClassDeactivationUtils.isActivated(JsfRequestBroadcaster.class))
            {
                this.jsfRequestBroadcaster =
                        BeanProvider.getContextualReference(JsfRequestBroadcaster.class, true);
            }

            this.windowContext = BeanProvider.getContextualReference(WindowContext.class, true);
            this.contextExtension = BeanProvider.getContextualReference(DeltaSpikeContextExtension.class, true);
            this.clientWindowConfig = BeanProvider.getContextualReference(ClientWindowConfig.class);
            
            this.initialized = true;
        }
    }
}
