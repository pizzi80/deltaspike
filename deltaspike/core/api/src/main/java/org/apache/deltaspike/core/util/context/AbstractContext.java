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

package org.apache.deltaspike.core.util.context;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.PassivationCapable;
import java.util.Map;

/**
 * A skeleton containing the most important parts of a custom CDI Contexts.
 * An implementing Context needs to implement the missing methods from the
 * {@link Context} interface and {@link #getContextualStorage(boolean)}.
 */
public abstract class AbstractContext implements Context
{
    /**
     * Whether the Context is for a passivating scope.
     */
    private boolean isPassivatingScope;

    protected AbstractContext(BeanManager beanManager)
    {
        isPassivatingScope = beanManager.isPassivatingScope(getScope());
    }

    /**
     * An implementation has to return the underlying storage which
     * contains the items held in the Context.
     * @parm createIfNotExist whether a ContextualStorage shall get created if it doesn't yet exist.
     * @return the underlying storage
     */
    protected abstract ContextualStorage getContextualStorage(boolean createIfNotExist);


    @Override
    public <T> T get(Contextual<T> bean)
    {
        checkActive();

        ContextualStorage storage = getContextualStorage(false);
        if (storage == null)
        {
            return null;
        }

        Map<Contextual<?>, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(bean);
        if (contextualInstanceInfo == null)
        {
            return null;
        }

        return (T) contextualInstanceInfo.getContextualInstance();
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> creationalContext)
    {
        checkActive();

        if (isPassivatingScope)
        {
            if (!(bean instanceof PassivationCapable))
            {
                throw new IllegalStateException(bean.toString() +
                        " doesn't implement " + PassivationCapable.class.getName());
            }
        }

        ContextualStorage storage = getContextualStorage(true);

        Map<Contextual<?>, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(bean);

        T instance = null;

        if (contextualInstanceInfo != null)
        {
            instance =  (T) contextualInstanceInfo.getContextualInstance();
        }

        if (instance != null)
        {
            return instance;
        }

        return storage.createContextualInstance(bean, creationalContext);
    }

    /**
     * Destroy the Contextual Instance of the given Bean.
     * @param bean dictates which bean shall get cleaned up
     * @return <code>true</code> if the bean was destroyed, <code>false</code> if there was no such a bean.
     */
    public boolean destroy(Contextual bean)
    {
        ContextualStorage storage = getContextualStorage(false);
        if (storage == null)
        {
            return false;
        }
        ContextualInstanceInfo<?> contextualInstanceInfo = storage.getStorage().get(bean);

        if (contextualInstanceInfo == null)
        {
            return false;
        }

        bean.destroy(contextualInstanceInfo.getContextualInstance(), contextualInstanceInfo.getCreationalContext());

        return true;
    }

    /**
     * destroys all the Contextual Instances in the Storage returned by
     * {@link #getContextualStorage(boolean)}.
     */
    public void destroyAllActive()
    {
        ContextualStorage storage = getContextualStorage(false);
        if (storage == null)
        {
            return;
        }

        Map<Contextual<?>, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        for (Map.Entry<Contextual<?>, ContextualInstanceInfo<?>> entry : contextMap.entrySet())
        {
            Contextual bean = entry.getKey();
            ContextualInstanceInfo<?> contextualInstanceInfo = entry.getValue();
            bean.destroy(contextualInstanceInfo.getContextualInstance(), contextualInstanceInfo.getCreationalContext());
        }
    }

    protected void checkActive()
    {
        if (!isActive())
        {
            throw new ContextNotActiveException("CDI context with scope annotation @"
                + getScope().getName() + " is not active with respect to the current thread");
        }
    }

}
