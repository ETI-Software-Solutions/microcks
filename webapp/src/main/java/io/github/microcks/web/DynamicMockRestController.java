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
package io.github.microcks.web;

import io.github.microcks.domain.GenericResource;
import io.github.microcks.domain.Operation;
import io.github.microcks.domain.Service;
import io.github.microcks.domain.ServiceType;
import io.github.microcks.event.MockInvocationEvent;
import io.github.microcks.repository.GenericResourceRepository;
import io.github.microcks.repository.ServiceRepository;

import org.bson.Document;
import org.bson.json.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the controller for Dynamic mocks in Microcks.
 * @author laurent
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping("/dynarest")
public class DynamicMockRestController {

   /** A simple logger for diagnostic messages. */
   private static Logger log = LoggerFactory.getLogger(DynamicMockRestController.class);

   public static final String ID_FIELD = "id";

   @Autowired
   ServiceRepository serviceRepository;

   @Autowired
   GenericResourceRepository genericResourceRepository;

   @Autowired
   private ApplicationContext applicationContext;

   @RequestMapping(value = "/{service}/{version}/{resource}", method = RequestMethod.POST, produces = "application/json")
   public ResponseEntity<String> createResource(
         @PathVariable("service") String serviceName,
         @PathVariable("version") String version,
         @PathVariable("resource") String resource,
         @RequestParam(value="delay", required=false) Long delay,
         @RequestBody(required=true) String body,
         HttpServletRequest request
   ) {
      log.debug("Creating a new resource '{}' for service '{}-{}'", resource, serviceName, version);
      long startTime = System.currentTimeMillis();

      serviceName = sanitizeServiceName(serviceName);

      MockContext mockContext = getMockContext(serviceName, version, "POST /" + resource);
      if (mockContext != null) {
         Document document = null;
         GenericResource genericResource = null;

         try {
            // Try parsing body payload that should be json.
            document = Document.parse(body);
            // Now create a generic resource.
            genericResource = new GenericResource();
            genericResource.setServiceId(mockContext.service.getId());
            genericResource.setPayload(document);

            genericResource = genericResourceRepository.save(genericResource);
         } catch (JsonParseException jpe) {
            // Return a 422 code : unprocessable entity.
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
         }

         // Append id and wait if specified before returning.
         document.append(ID_FIELD, genericResource.getId());
         waitForDelay(startTime, delay, mockContext);
         return new ResponseEntity<>(document.toJson(), HttpStatus.CREATED);
      }
      // Return a 400 code : bad request.
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
   }

   @RequestMapping(value = "/{service}/{version}/{resource}", method = RequestMethod.GET, produces = "application/json")
   public ResponseEntity<String> findResources(
         @PathVariable("service") String serviceName,
         @PathVariable("version") String version,
         @PathVariable("resource") String resource,
         @RequestParam(value = "page", required = false, defaultValue = "0") int page,
         @RequestParam(value = "size", required = false, defaultValue = "20") int size,
         @RequestParam(value="delay", required=false) Long delay,
         @RequestBody(required=false) String body
   ) {
      log.debug("Find resources '{}' for service '{}-{}'", resource, serviceName, version);
      long startTime = System.currentTimeMillis();

      serviceName = sanitizeServiceName(serviceName);

      MockContext mockContext = getMockContext(serviceName, version, "GET /" + resource);
      if (mockContext != null) {

         List<GenericResource> genericResources = null;
         if (body == null) {
            genericResources = genericResourceRepository.findByServiceId(mockContext.service.getId(), PageRequest.of(page, size));
         } else {
            genericResources = genericResourceRepository.findByServiceIdAndJSONQuery(mockContext.service.getId(), body);
         }

         // Transform and collect resources.
         List<String> resources = genericResources.stream()
               .map(genericResource -> transformToResourceJSON(genericResource))
               .collect(Collectors.toList());

         // Wait if specified before returning.
         waitForDelay(startTime, delay, mockContext);
         return new ResponseEntity<>(formatToJSONArray(resources), HttpStatus.OK);
      }
      // Return a 400 code : bad request.
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
   }

   @RequestMapping(value = "/{service}/{version}/{resource}/{resourceId}", method = RequestMethod.GET,
         produces = "application/json")
   public ResponseEntity<String> getResource(
         @PathVariable("service") String serviceName,
         @PathVariable("version") String version,
         @PathVariable("resource") String resource,
         @PathVariable("resourceId") String resourceId,
         @RequestParam(value="delay", required=false) Long delay
   ) {
      log.debug("Get resource '{}:{}' for service '{}-{}'", resource, resourceId, serviceName, version);
      long startTime = System.currentTimeMillis();

      serviceName = sanitizeServiceName(serviceName);

      MockContext mockContext = getMockContext(serviceName, version, "GET /" + resource + "/:id");
      if (mockContext != null) {
         // Get the requested generic resource.
         GenericResource genericResource = genericResourceRepository.findById(resourceId).orElse(null);

         // Wait if specified before returning.
         waitForDelay(startTime, delay, mockContext);

         if (genericResource != null) {
            // Return the resource as well as a 200 code.
            return new ResponseEntity<>(transformToResourceJSON(genericResource), HttpStatus.OK);
         } else {
            // Return a 404 code : not found.
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
         }
      }

      // Return a 400 code : bad request.
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
   }

   @RequestMapping(value = "/{service}/{version}/{resource}/{resourceId}", method = RequestMethod.PUT,
         produces = "application/json")
   public ResponseEntity<String> updateResource(
         @PathVariable("service") String serviceName,
         @PathVariable("version") String version,
         @PathVariable("resource") String resource,
         @PathVariable("resourceId") String resourceId,
         @RequestParam(value="delay", required=false) Long delay,
         @RequestBody(required=true) String body,
         HttpServletRequest request
   ) {
      log.debug("Update resource '{}:{}' for service '{}-{}'", resource, resourceId, serviceName, version);
      long startTime = System.currentTimeMillis();

      serviceName = sanitizeServiceName(serviceName);

      MockContext mockContext = getMockContext(serviceName, version, "PUT /" + resource + "/:id");
      if (mockContext != null) {
         // Get the requested generic resource.
         GenericResource genericResource = genericResourceRepository.findById(resourceId).orElse(null);
         if (genericResource != null) {
            Document document = null;

            try {
               // Try parsing body payload that should be json.
               document = Document.parse(body);
               document.remove(ID_FIELD);

               // Now update the generic resource payload.
               genericResource.setPayload(document);

               genericResourceRepository.save(genericResource);
            } catch (JsonParseException jpe) {
               // Return a 422 code : unprocessable entity.
               return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
            }
            // Wait if specified before returning.
            waitForDelay(startTime, delay, mockContext);

            // Return the updated resource as well as a 200 code.
            return new ResponseEntity<>(transformToResourceJSON(genericResource), HttpStatus.OK);

         } else {
            // Wait if specified before returning.
            waitForDelay(startTime, delay, mockContext);

            // Return a 404 code : not found.
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
         }
      }

      // Return a 400 code : bad request.
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
   }

   @RequestMapping(value = "/{service}/{version}/{resource}/{resourceId}", method = RequestMethod.DELETE)
   public ResponseEntity<String> deleteResource(
         @PathVariable("service") String serviceName,
         @PathVariable("version") String version,
         @PathVariable("resource") String resource,
         @PathVariable("resourceId") String resourceId,
         @RequestParam(value="delay", required=false) Long delay
   ) {
      log.debug("Update resource '{}:{}' for service '{}-{}'", resource, resourceId, serviceName, version);
      long startTime = System.currentTimeMillis();

      serviceName = sanitizeServiceName(serviceName);

      MockContext mockContext = getMockContext(serviceName, version, "DELETE /" + resource + "/:id");
      if (mockContext != null) {
         genericResourceRepository.deleteById(resourceId);

         // Wait if specified before returning.
         waitForDelay(startTime, delay, mockContext);

         // Return a 204 code : done and no content returned.
         return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      }

      // Return a 400 code : bad request.
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
   }

   /** Sanitize the service name (check encoding and so on...) */
   private String sanitizeServiceName(String serviceName) {
      // If serviceName was encoded with '+' instead of '%20', replace them.
      if (serviceName.contains("+")) {
         return serviceName.replace('+', ' ');
      }
      return serviceName;
   }

   /** Retrieve a MockContext corresponding to operation on service. Null if not found or not valid. */
   private MockContext getMockContext(String serviceName, String version, String operationName) {
      Service service = serviceRepository.findByNameAndVersion(serviceName, version);
      if (service != null && ServiceType.GENERIC_REST.equals(service.getType())) {
         for (Operation operation : service.getOperations()) {
            if (operationName.equals(operation.getName())) {
               return new MockContext(service, operation);
            }
         }
      }
      return null;
   }

   private String transformToResourceJSON(GenericResource genericResource) {
      Document document = genericResource.getPayload();
      document.append(ID_FIELD, genericResource.getId());
      return document.toJson();
   }

   private String formatToJSONArray(List<String> resources) {
      StringBuilder builder = new StringBuilder("[");
      for (int i=0; i<resources.size(); i++) {
         builder.append(resources.get(i));
         if (i < resources.size() - 1) {
            builder.append(", ");
         }
      }
      return builder.append("]").toString();
   }

   private void waitForDelay(Long since, Long delay, MockContext mockContext) {
      // Setting delay to default one if not set.
      if (delay == null && mockContext.operation.getDefaultDelay() != null) {
         delay = mockContext.operation.getDefaultDelay();
      }

      if (delay != null && delay > -1) {
         log.debug("Mock delay is turned on, waiting if necessary...");
         long duration = System.currentTimeMillis() - since;
         if (duration < delay) {
            Object semaphore = new Object();
            synchronized (semaphore) {
               try {
                  semaphore.wait(delay - duration);
               } catch (Exception e) {
                  log.debug("Delay semaphore was interrupted");
               }
            }
         }
         log.debug("Delay now expired, releasing response !");
      }

      // Publish an invocation event before returning.
      MockInvocationEvent event = new MockInvocationEvent(this, mockContext.service.getName(),
            mockContext.service.getVersion(),
            "DynamicMockRestController",
            new Date(since), since - System.currentTimeMillis());
      applicationContext.publishEvent(event);
      log.debug("Mock invocation event has been published");
   }

   private class MockContext {
      public Service service;
      public Operation operation;

      public MockContext(Service service, Operation operation) {
         this.service = service;
         this.operation = operation;
      }
   }
}
