/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.cidb.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;

/**
 * Provides access to the phenotype mappings configured for the current space. The field mappings are defined by a
 * Groovy script contained in a document. The name of that document must be configured in the "phenotypeMapping" field
 * of a "DBConfigurationClass" object attached to the homepage (WebHome) of the current space.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = ScriptService.class)
@Named("phenotypeMapping")
@Singleton
public class PhenotypeMappingService implements ScriptService, EventListener, Initializable
{
    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    /**
     * Cached mappings for faster responses.
     */
    private Map<String, Map<String, Object>> cache = new HashMap<String, Map<String, Object>>();

    /**
     * Reference serializer used for converting entities into strings.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Reference resolver used for converting strings into entities.
     */
    @Inject
    private EntityReferenceResolver<String> resolver;

    /**
     * Groovy engine used for running the groovy script containing the mapping.
     */
    @Inject
    @Named("groovy")
    private ScriptEngineFactory groovy;

    /**
     * Provides access to the script context.
     */
    @Inject
    private ScriptContextManager scmanager;

    /**
     * Provides access to documents.
     */
    @Inject
    private DocumentAccessBridge bridge;

    /**
     * Allows registering this object as an event listener.
     */
    @Inject
    private ObservationManager observationManager;

    @Override
    public void initialize() throws InitializationException
    {
        this.observationManager.addListener(this);
    }

    @Override
    public String getName()
    {
        return "phentoype-mapping-cache";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.emptyList();
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.cache.remove(((AbstractDocumentEvent) event).getEventFilter().getFilter());
    }

    /**
     * Get the configuration for the "phenotype" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getPhenotype()
    {
        return getMapping("phenotype");
    }

    /**
     * Get the configuration for the "prenatal_phenotype" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getPrenatalPhenotype()
    {
        return getMapping("prenatal_phenotype");
    }

    /**
     * Get the configuration for the "negative_phenotype" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getNegativePhenotype()
    {
        return getMapping("negative_phenotype");
    }

    /**
     * Get the configuration for the "family_history" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getFamilyHistory()
    {
        return getMapping("family_history");
    }

    /**
     * Get the configuration for the "extraMessages" pseudo-property.
     *
     * @return configuration object, should be a Map
     */
    public Object getExtraMessages()
    {
        return getMapping("extraMessages");
    }

    /**
     * Generic configuration getter.
     *
     * @param name the name of the configuration to return
     * @return configuration object, should be a Map
     */
    public Object get(String name)
    {
        return getMapping(name);
    }

    /**
     * Get the configuration for a specific property, taking into account the current space.
     *
     * @param mappingName the name of the configuration to return
     * @return configuration object, should be a Map
     */
    private Object getMapping(String mappingName)
    {
        DocumentReference mappingDoc = getMappingDocument();
        Object result = getMapping(mappingDoc, mappingName);
        if (result == null) {
            ScriptEngine e = this.groovy.getScriptEngine();
            ScriptContext c = this.scmanager.getScriptContext();
            try {
                e.eval(this.bridge.getDocumentContentForDefaultLanguage(mappingDoc), c);
            } catch (Exception ex) {
                this.logger.error("Failed to parse mapping document [{}]", mappingDoc, ex);
                return null;
            }
            this.observationManager.addEvent(this.getName(), new DocumentUpdatedEvent(mappingDoc));
            this.observationManager.addEvent(this.getName(), new DocumentDeletedEvent(mappingDoc));
            @SuppressWarnings("unchecked")
            Map<String, Object> mappings = (Map<String, Object>) c.getAttribute("mappings");
            setMappings(mappingDoc, mappings);
            result = mappings.get(mappingName);
        }
        return result;
    }

    /**
     * Get the configuration for a specific property, taking the configuration from a specified document.
     *
     * @param doc the reference of the document containing the mapping
     * @param mappingName the name of the configuration to return
     * @return configuration object, should be a Map
     */
    private Object getMapping(DocumentReference doc, String mappingName)
    {
        String docName = this.serializer.serialize(doc);
        if (!this.cache.containsKey(docName)) {
            return null;
        }
        return this.cache.get(docName).get(mappingName);
    }

    /**
     * Store all the mappings defined in the specified document in the cache.
     *
     * @param doc the reference of the document containing the mapping
     * @param mappings defined mappings, a map of maps
     */
    private void setMappings(DocumentReference doc, Map<String, Object> mappings)
    {
        this.cache.put(this.serializer.serialize(doc), mappings);
    }

    /**
     * Determine which document was configured as the mapping source in the current space's preferences.
     *
     * @return a document reference, as configured in the space preferences
     */
    private DocumentReference getMappingDocument()
    {
        DocumentReference currentDocRef = this.bridge.getCurrentDocumentReference();
        DocumentReference homeDocRef = new DocumentReference("WebHome", currentDocRef.getLastSpaceReference());
        DocumentReference classDocRef = new DocumentReference(
            currentDocRef.getWikiReference().getName(), "ClinicalInformationCode", "DBConfigurationClass");
        String targetMappingName = (String) this.bridge.getProperty(homeDocRef, classDocRef, "phenotypeMapping");
        return new DocumentReference(this.resolver.resolve(targetMappingName, EntityType.DOCUMENT));
    }
}
