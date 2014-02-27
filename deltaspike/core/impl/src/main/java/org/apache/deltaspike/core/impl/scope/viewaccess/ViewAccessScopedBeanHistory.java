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

package org.apache.deltaspike.core.impl.scope.viewaccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.deltaspike.core.api.scope.WindowScoped;

@WindowScoped
public class ViewAccessScopedBeanHistory implements Serializable
{
    private static final long serialVersionUID = 3617603930728148927L;
    
    private List<String> accessedBeans;
    private List<String> lastAccessedBeans;
    private String lastView;

    public ViewAccessScopedBeanHistory()
    {
        accessedBeans = new ArrayList<String>();
        lastAccessedBeans = new ArrayList<String>();
    }
    
    public List<String> getAccessedBeans()
    {
        return accessedBeans;
    }

    public void setAccessedBeans(List<String> accessedBeans)
    {
        this.accessedBeans = accessedBeans;
    }

    public List<String> getLastAccessedBeans()
    {
        return lastAccessedBeans;
    }

    public void setLastAccessedBeans(List<String> lastAccessedBeans)
    {
        this.lastAccessedBeans = lastAccessedBeans;
    }

    public String getLastView()
    {
        return lastView;
    }

    public void setLastView(String lastView)
    {
        this.lastView = lastView;
    }
}
