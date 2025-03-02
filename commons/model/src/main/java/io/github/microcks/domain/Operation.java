/*
 * Licensed to Laurent Broudoux (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.microcks.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An Operation / action of a micro service. Holds information on messages constitution
 * (inputName, outputName, bindings) and how dispatch request to them.
 * @author laurent
 */
public class Operation {

   private String name;
   private String method;
   private String action;
   private String inputName;
   private String outputName;
   private Map<String, Binding> bindings;

   private boolean override = false;
   private String dispatcher;
   private String dispatcherRules;
   private Long defaultDelay;

   private List<String> resourcePaths;
   private List<ParameterConstraint> parameterConstraints;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(String method) {
      this.method = method;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public String getInputName() {
      return inputName;
   }

   public void setInputName(String inputName) {
      this.inputName = inputName;
   }

   public String getOutputName() {
      return outputName;
   }

   public void setOutputName(String outputName) {
      this.outputName = outputName;
   }

   public Map<String, Binding> getBindings() {
      return bindings;
   }

   public void setBindings(Map<String, Binding> bindings) {
      this.bindings = bindings;
   }

   public void addBinding(String name, Binding binding) {
      if (this.bindings == null) {
         this.bindings = new HashMap<>();
      }
      bindings.put(name, binding);
   }

   public boolean hasOverride() {
      return this.override;
   }

   public void setOverride(boolean override) {
      this.override = override;
   }

   public String getDispatcher() {
      return dispatcher;
   }

   public void setDispatcher(String dispatcher) {
      this.dispatcher = dispatcher;
   }

   public String getDispatcherRules() {
      return dispatcherRules;
   }

   public void setDispatcherRules(String dispatcherRules) {
      this.dispatcherRules = dispatcherRules;
   }

   public Long getDefaultDelay() {
      return defaultDelay;
   }

   public void setDefaultDelay(Long defaultDelay) {
      this.defaultDelay = defaultDelay;
   }

   public List<String> getResourcePaths() {
      return resourcePaths;
   }

   public void setResourcePaths(List<String> resourcePaths) {
      this.resourcePaths = resourcePaths;
   }

   public void addResourcePath(String resourcePath) {
      if (this.resourcePaths == null) {
         this.resourcePaths = new ArrayList<>();
      }
      if (!this.resourcePaths.contains(resourcePath)) {
         this.resourcePaths.add(resourcePath);
      }
   }

   public List<ParameterConstraint> getParameterConstraints() {
      return parameterConstraints;
   }

   public void setParameterConstraints(List<ParameterConstraint> parameterConstraints) {
      this.parameterConstraints = parameterConstraints;
   }

   public void addParameterConstraint(ParameterConstraint constraint) {
      if (this.parameterConstraints == null) {
         this.parameterConstraints = new ArrayList<>();
      }
      parameterConstraints.add(constraint);
   }
}
