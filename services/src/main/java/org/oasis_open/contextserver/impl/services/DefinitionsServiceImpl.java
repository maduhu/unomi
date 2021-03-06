package org.oasis_open.contextserver.impl.services;

/*
 * #%L
 * context-server-services
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2015 Jahia Solutions
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.oasis_open.contextserver.api.PluginType;
import org.oasis_open.contextserver.api.PropertyMergeStrategyType;
import org.oasis_open.contextserver.api.Tag;
import org.oasis_open.contextserver.api.ValueType;
import org.oasis_open.contextserver.api.actions.ActionType;
import org.oasis_open.contextserver.api.conditions.Condition;
import org.oasis_open.contextserver.api.conditions.ConditionType;
import org.oasis_open.contextserver.api.services.DefinitionsService;
import org.oasis_open.contextserver.persistence.spi.CustomObjectMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class DefinitionsServiceImpl implements DefinitionsService, SynchronousBundleListener {

    private static final Logger logger = LoggerFactory.getLogger(DefinitionsServiceImpl.class.getName());

    private Map<String, Tag> tags = new HashMap<>();
    private Set<Tag> rootTags = new LinkedHashSet<>();
    private Map<String, ConditionType> conditionTypeById = new HashMap<>();
    private Map<String, ActionType> actionTypeById = new HashMap<>();
    private Map<String, ValueType> valueTypeById = new HashMap<>();
    private Map<Tag, Set<ConditionType>> conditionTypeByTag = new HashMap<>();
    private Map<Tag, Set<ActionType>> actionTypeByTag = new HashMap<>();
    private Map<Tag, Set<ValueType>> valueTypeByTag = new HashMap<>();
    private Map<Long, List<PluginType>> pluginTypes = new HashMap<>();
    private Map<String, PropertyMergeStrategyType> propertyMergeStrategyTypeById = new HashMap<>();

    private BundleContext bundleContext;
    public DefinitionsServiceImpl() {
        logger.info("Instantiating definitions service...");
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void postConstruct() {
        logger.debug("postConstruct {" + bundleContext.getBundle() + "}");

        processBundleStartup(bundleContext);

        // process already started bundles
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getBundleContext() != null) {
                processBundleStartup(bundle.getBundleContext());
            }
        }

        bundleContext.addBundleListener(this);
    }

    private void processBundleStartup(BundleContext bundleContext) {
        if (bundleContext == null) {
            return;
        }

        pluginTypes.put(bundleContext.getBundle().getBundleId(), new ArrayList<PluginType>());

        loadPredefinedTags(bundleContext);

        loadPredefinedConditionTypes(bundleContext);
        loadPredefinedActionTypes(bundleContext);
        loadPredefinedValueTypes(bundleContext);
        loadPredefinedPropertyMergeStrategies(bundleContext);

    }

    private void processBundleStop(BundleContext bundleContext) {
        if (bundleContext == null) {
            return;
        }
        List<PluginType> types = pluginTypes.get(bundleContext.getBundle().getBundleId());
        if (types != null) {
            for (PluginType type : types) {
                if (type instanceof ActionType) {
                    ActionType actionType = (ActionType) type;
                    actionTypeById.remove(actionType.getId());
                    for (Tag tag : actionType.getTags()) {
                        actionTypeByTag.get(tag).remove(actionType);
                    }
                } else if (type instanceof ConditionType) {
                    ConditionType conditionType = (ConditionType) type;
                    conditionTypeById.remove(conditionType.getId());
                    for (Tag tag : conditionType.getTags()) {
                        conditionTypeByTag.get(tag).remove(conditionType);
                    }
                } else if (type instanceof ValueType) {
                    ValueType valueType = (ValueType) type;
                    valueTypeById.remove(valueType.getId());
                    for (Tag tag : valueType.getTags()) {
                        valueTypeByTag.get(tag).remove(valueType);
                    }
                }
            }
        }
    }


    public void preDestroy() {
        bundleContext.removeBundleListener(this);
    }

    private void loadPredefinedTags(BundleContext bundleContext) {
        Enumeration<URL> predefinedTagEntries = bundleContext.getBundle().findEntries("META-INF/cxs/tags", "*.json", true);
        if (predefinedTagEntries == null) {
            return;
        }
        while (predefinedTagEntries.hasMoreElements()) {
            URL predefinedTagURL = predefinedTagEntries.nextElement();
            logger.debug("Found predefined tags at " + predefinedTagURL + ", loading... ");

            try {
                Tag tag = CustomObjectMapper.getObjectMapper().readValue(predefinedTagURL, Tag.class);
                tag.setPluginId(bundleContext.getBundle().getBundleId());
                tags.put(tag.getId(), tag);
            } catch (IOException e) {
                logger.error("Error while loading segment definition " + predefinedTagEntries, e);
            }
        }

        // now let's resolve all the children.
        for (Tag tag : tags.values()) {
            if (tag.getParentId() != null && tag.getParentId().length() > 0) {
                Tag parentTag = tags.get(tag.getParentId());
                if (parentTag != null) {
                    parentTag.getSubTags().add(tag);
                }
            } else {
                rootTags.add(tag);
            }
        }
    }

    private void loadPredefinedConditionTypes(BundleContext bundleContext) {
        Enumeration<URL> predefinedConditionEntries = bundleContext.getBundle().findEntries("META-INF/cxs/conditions", "*.json", true);
        if (predefinedConditionEntries == null) {
            return;
        }
        ArrayList<PluginType> pluginTypeArrayList = (ArrayList<PluginType>) pluginTypes.get(bundleContext.getBundle().getBundleId());
        while (predefinedConditionEntries.hasMoreElements()) {
            URL predefinedConditionURL = predefinedConditionEntries.nextElement();
            logger.debug("Found predefined conditions at " + predefinedConditionURL + ", loading... ");

            try {
                ConditionType conditionType = CustomObjectMapper.getObjectMapper().readValue(predefinedConditionURL, ConditionType.class);
                conditionType.setPluginId(bundleContext.getBundle().getBundleId());
                conditionTypeById.put(conditionType.getId(), conditionType);
                pluginTypeArrayList.add(conditionType);
                for (String tagId : conditionType.getTagIDs()) {
                    Tag tag = tags.get(tagId);
                    if (tag != null) {
                        conditionType.getTags().add(tag);
                        Set<ConditionType> conditionTypes = conditionTypeByTag.get(tag);
                        if (conditionTypes == null) {
                            conditionTypes = new LinkedHashSet<ConditionType>();
                        }
                        conditionTypes.add(conditionType);
                        conditionTypeByTag.put(tag, conditionTypes);
                    } else {
                        // we found a tag that is not defined, we will define it automatically
                        logger.warn("Unknown tag " + tagId + " used in condition definition " + predefinedConditionURL);
                    }
                }
            } catch (Exception e) {
                logger.error("Error while loading condition definition " + predefinedConditionURL, e);
            }
        }
        for (ConditionType type : conditionTypeById.values()) {
            if (type.getParentCondition() != null) {
                ParserHelper.resolveConditionType(this, type.getParentCondition());
            }
        }
    }

    private void loadPredefinedActionTypes(BundleContext bundleContext) {
        Enumeration<URL> predefinedActionsEntries = bundleContext.getBundle().findEntries("META-INF/cxs/actions", "*.json", true);
        if (predefinedActionsEntries == null) {
            return;
        }
        ArrayList<PluginType> pluginTypeArrayList = (ArrayList<PluginType>) pluginTypes.get(bundleContext.getBundle().getBundleId());
        while (predefinedActionsEntries.hasMoreElements()) {
            URL predefinedActionURL = predefinedActionsEntries.nextElement();
            logger.debug("Found predefined action at " + predefinedActionURL + ", loading... ");

            try {
                ActionType actionType = CustomObjectMapper.getObjectMapper().readValue(predefinedActionURL, ActionType.class);
                actionType.setPluginId(bundleContext.getBundle().getBundleId());
                actionTypeById.put(actionType.getId(), actionType);
                pluginTypeArrayList.add(actionType);
                for (String tagId : actionType.getTagIds()) {
                    Tag tag = tags.get(tagId);
                    if (tag != null) {
                        actionType.getTags().add(tag);
                        Set<ActionType> actionTypes = actionTypeByTag.get(tag);
                        if (actionTypes == null) {
                            actionTypes = new LinkedHashSet<>();
                        }
                        actionTypes.add(actionType);
                        actionTypeByTag.put(tag, actionTypes);
                    } else {
                        // we found a tag that is not defined, we will define it automatically
                        logger.warn("Unknown tag " + tagId + " used in action definition " + predefinedActionURL);
                    }
                }
            } catch (Exception e) {
                logger.error("Error while loading action definition " + predefinedActionURL, e);
            }
        }

    }

    private void loadPredefinedValueTypes(BundleContext bundleContext) {
        Enumeration<URL> predefinedPropertiesEntries = bundleContext.getBundle().findEntries("META-INF/cxs/values", "*.json", true);
        if (predefinedPropertiesEntries == null) {
            return;
        }
        ArrayList<PluginType> pluginTypeArrayList = (ArrayList<PluginType>) pluginTypes.get(bundleContext.getBundle().getBundleId());
        while (predefinedPropertiesEntries.hasMoreElements()) {
            URL predefinedPropertyURL = predefinedPropertiesEntries.nextElement();
            logger.debug("Found predefined property type at " + predefinedPropertyURL + ", loading... ");

            try {
                ValueType valueType = CustomObjectMapper.getObjectMapper().readValue(predefinedPropertyURL, ValueType.class);
                valueType.setPluginId(bundleContext.getBundle().getBundleId());
                valueTypeById.put(valueType.getId(), valueType);
                pluginTypeArrayList.add(valueType);
                for (String tagId : valueType.getTagIds()) {
                    Tag tag = tags.get(tagId);
                    if (tag != null) {
                        valueType.getTags().add(tag);
                        Set<ValueType> valueTypes = valueTypeByTag.get(tag);
                        if (valueTypes == null) {
                            valueTypes = new LinkedHashSet<ValueType>();
                        }
                        valueTypes.add(valueType);
                        valueTypeByTag.put(tag, valueTypes);
                    } else {
                        // we found a tag that is not defined, we will define it automatically
                        logger.warn("Unknown tag " + tagId + " used in property type definition " + predefinedPropertyURL);
                    }
                }
            } catch (Exception e) {
                logger.error("Error while loading property type definition " + predefinedPropertyURL, e);
            }
        }

    }

    public Set<Tag> getAllTags() {
        return new HashSet<Tag>(tags.values());
    }

    public Set<Tag> getRootTags() {
        return rootTags;
    }

    public Tag getTag(String tagId) {
        Tag completeTag = tags.get(tagId);
        if (completeTag == null) {
            return null;
        }
        return completeTag;
    }

    public Collection<ConditionType> getAllConditionTypes() {
        return conditionTypeById.values();
    }

    public Map<Long, List<PluginType>> getTypesByPlugin() {
        return pluginTypes;
    }

    public Set<ConditionType> getConditionTypesByTag(Tag tag, boolean recursive) {
        Set<ConditionType> conditionTypes = new LinkedHashSet<ConditionType>();
        Set<ConditionType> directConditionTypes = conditionTypeByTag.get(tag);
        if (directConditionTypes != null) {
            conditionTypes.addAll(directConditionTypes);
        }
        if (recursive) {
            for (Tag subTag : tag.getSubTags()) {
                Set<ConditionType> childConditionTypes = getConditionTypesByTag(subTag, true);
                conditionTypes.addAll(childConditionTypes);
            }
        }
        return conditionTypes;
    }

    public ConditionType getConditionType(String id) {
        return conditionTypeById.get(id);
    }

    public Collection<ActionType> getAllActionTypes() {
        return actionTypeById.values();
    }

    public Set<ActionType> getActionTypeByTag(Tag tag, boolean recursive) {
        Set<ActionType> actionTypes = new LinkedHashSet<ActionType>();
        Set<ActionType> directActionTypes = actionTypeByTag.get(tag);
        if (directActionTypes != null) {
            actionTypes.addAll(directActionTypes);
        }
        if (recursive) {
            for (Tag subTag : tag.getSubTags()) {
                Set<ActionType> childActionTypes = getActionTypeByTag(subTag, true);
                actionTypes.addAll(childActionTypes);
            }
        }
        return actionTypes;
    }

    public ActionType getActionType(String id) {
        return actionTypeById.get(id);
    }

    public Collection<ValueType> getAllValueTypes() {
        return valueTypeById.values();
    }

    public Set<ValueType> getValueTypeByTag(Tag tag, boolean recursive) {
        Set<ValueType> valueTypes = new LinkedHashSet<ValueType>();
        Set<ValueType> directValueTypes = valueTypeByTag.get(tag);
        if (directValueTypes != null) {
            valueTypes.addAll(directValueTypes);
        }
        if (recursive) {
            for (Tag subTag : tag.getSubTags()) {
                Set<ValueType> childValueTypes = getValueTypeByTag(subTag, true);
                valueTypes.addAll(childValueTypes);
            }
        }
        return valueTypes;
    }

    public ValueType getValueType(String id) {
        return valueTypeById.get(id);
    }

    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                processBundleStartup(event.getBundle().getBundleContext());
                break;
            case BundleEvent.STOPPING:
                processBundleStop(event.getBundle().getBundleContext());
                break;
        }
    }

    private void loadPredefinedPropertyMergeStrategies(BundleContext bundleContext) {
        Enumeration<URL> predefinedPropertyMergeStrategyEntries = bundleContext.getBundle().findEntries("META-INF/cxs/mergers", "*.json", true);
        if (predefinedPropertyMergeStrategyEntries == null) {
            return;
        }
        ArrayList<PluginType> pluginTypeArrayList = (ArrayList<PluginType>) pluginTypes.get(bundleContext.getBundle().getBundleId());
        while (predefinedPropertyMergeStrategyEntries.hasMoreElements()) {
            URL predefinedPropertyMergeStrategyURL = predefinedPropertyMergeStrategyEntries.nextElement();
            logger.debug("Found predefined property merge strategy type at " + predefinedPropertyMergeStrategyURL + ", loading... ");

            try {
                PropertyMergeStrategyType propertyMergeStrategyType = CustomObjectMapper.getObjectMapper().readValue(predefinedPropertyMergeStrategyURL, PropertyMergeStrategyType.class);
                propertyMergeStrategyType.setPluginId(bundleContext.getBundle().getBundleId());
                propertyMergeStrategyTypeById.put(propertyMergeStrategyType.getId(), propertyMergeStrategyType);
                pluginTypeArrayList.add(propertyMergeStrategyType);
            } catch (Exception e) {
                logger.error("Error while loading property type definition " + predefinedPropertyMergeStrategyURL, e);
            }
        }

    }

    public PropertyMergeStrategyType getPropertyMergeStrategyType(String id) {
        return propertyMergeStrategyTypeById.get(id);
    }

    public Set<Condition> extractConditionsByType(Condition rootCondition, String typeId) {
        if (rootCondition.containsParameter("subConditions")) {
            @SuppressWarnings("unchecked")
            List<Condition> subConditions = (List<Condition>) rootCondition.getParameter("subConditions");
            Set<Condition> matchingConditions = new HashSet<>();
            for (Condition condition : subConditions) {
                matchingConditions.addAll(extractConditionsByType(condition, typeId));
            }
            return matchingConditions;
        } else if (rootCondition.getConditionTypeId() != null && rootCondition.getConditionTypeId().equals(typeId)) {
            return Collections.singleton(rootCondition);
        } else {
            return Collections.emptySet();
        }
    }

    public Condition extractConditionByTag(Condition rootCondition, String tagId) {
        if (rootCondition.containsParameter("subConditions")) {
            @SuppressWarnings("unchecked")
            List<Condition> subConditions = (List<Condition>) rootCondition.getParameter("subConditions");
            List<Condition> matchingConditions = new ArrayList<Condition>();
            for (Condition condition : subConditions) {
                Condition c = extractConditionByTag(condition, tagId);
                if (c != null) {
                    matchingConditions.add(c);
                }
            }
            if (matchingConditions.size() == 0) {
                return null;
            } else if (matchingConditions.equals(subConditions)) {
                return rootCondition;
            } else if (rootCondition.getConditionTypeId().equals("booleanCondition") && "and".equals(rootCondition.getParameter("operator"))) {
                if (matchingConditions.size() == 1) {
                    return matchingConditions.get(0);
                } else {
                    Condition res = new Condition();
                    res.setConditionType(getConditionType("booleanCondition"));
                    res.setParameter("operator", "and");
                    res.setParameter("subConditions", matchingConditions);
                    return res;
                }
            }
            throw new IllegalArgumentException();
        } else if (rootCondition.getConditionType() != null && rootCondition.getConditionType().getTagIDs().contains(tagId)) {
            return rootCondition;
        } else {
            return null;
        }
    }

    @Override
    public boolean resolveConditionType(Condition rootCondition) {
        return ParserHelper.resolveConditionType(this, rootCondition);
    }
}
