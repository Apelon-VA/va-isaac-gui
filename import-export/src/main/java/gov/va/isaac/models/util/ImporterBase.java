/**
 * Copyright Notice
 * 
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
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
package gov.va.isaac.models.util;

import java.io.IOException;

import org.ihtsdo.otf.tcc.api.blueprint.TerminologyBuilderBI;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.datastore.BdbTermBuilder;

/**
 * Base class containing common methods for importing information models.
 *
 * @author ocarlsen
 */
public class ImporterBase extends CommonBase {

    private final TerminologyBuilderBI builder;

    protected ImporterBase() throws ValidationException, IOException {
        super();

        this.builder = new BdbTermBuilder(getEC(), getVC());
    }

    protected final TerminologyBuilderBI getBuilder() {
        return builder;
    }
}