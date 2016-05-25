/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.ucuenca.wk.basemodule.webservices;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.marmotta.ucuenca.wk.basemodule.api.MyService;
import org.apache.marmotta.ucuenca.wk.basemodule.exceptions.DoThisException;

@Path("/base-module")
@ApplicationScoped
public class MyWebService {

    @Inject
    private Logger log;

    @Inject
    private MyService myService;
    
    private static final int MAX_TURNS = 100;
    private static final int MIN_TURNS = 0;

    @GET
    @Produces("text/plain; charset=utf8")
    public Response hello(@QueryParam("name") String name) {
        if (StringUtils.isEmpty(name)) {
            log.warn("No name given");
            // No name given? Invalid request.
            return Response.status(Status.BAD_REQUEST).entity("Missing Parameter 'name'").build();
        }

        log.debug("Sending regards to {}", name);
        // Return the greeting.
        return Response.ok(myService.helloWorld(name)).build();
    }

    @POST
    public Response doThis(@FormParam("turns") @DefaultValue("2") int turns) throws DoThisException {
        log.debug("Request to doThis {} times", turns);
        if (turns > MAX_TURNS) { throw new DoThisException("At max, 100 turns are allowed"); }
        if (turns < MIN_TURNS) { throw new DoThisException("Can't undo 'This'"); }

        myService.doThis(turns);
        return Response.noContent().build();
    }

}
