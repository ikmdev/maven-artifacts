/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.maven.export.config;

import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.terms.EntityProxy;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.Serializable;

public class PublicIdConfig implements Serializable {

    private String[] uuids;

    private String bindingName;
    private String bindingClassName = "dev.ikm.tinkar.terms.TinkarTerm";

    public PublicId getPublicId() throws MojoExecutionException {
        validate();
        if (!emptyUuids()) {
            return PublicIds.of(uuids);
        }
        return getProxy().publicId();
    }

    public <T extends EntityProxy> T getProxy() throws MojoExecutionException {
        validate();
        if (!emptyUuids()) {
            return EntityService.get().getEntityFast(getPublicId().asUuidArray()).toProxy();
        }
        try {
            Class bindingClass = Class.forName(bindingClassName);
            return (T) bindingClass.getField(bindingName).get(bindingClass);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new MojoExecutionException(e);
        }
    }

    private void validate() throws MojoExecutionException {
        if (!emptyUuids() && !emptyBindingName()) {
            throw new MojoExecutionException(PublicIdConfig.class.getSimpleName() + " cannot be instantiated with both uuids and bindings.");
        }
    }

    private boolean emptyUuids() {
        return uuids==null || uuids.length==0;
    }

    private boolean emptyBindingName() {
        return bindingName==null || bindingName.isEmpty();
    }
}
