package org.oasis_open.contextserver.impl.conditions.initializers;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.oasis_open.contextserver.api.conditions.initializers.ChoiceListInitializer;
import org.oasis_open.contextserver.api.conditions.initializers.ChoiceListValue;

/**
 * Initializer for the set of known countries.
 * 
 * @author Sergiy Shyrkov
 */
public class CountryChoiceListInitializer implements ChoiceListInitializer {

    private static final Comparator<ChoiceListValue> NAME_COMPARATOR = new Comparator<ChoiceListValue>() {
        @Override
        public int compare(ChoiceListValue o1, ChoiceListValue o2) {
            // we do not need to deal with null values, so make it straight
            return o1.getName().compareTo(o2.getName());
        }
    };

    private Map<String, Locale> countryLocales;

    private Map<String, Locale> getContryLocales() {
        if (countryLocales == null) {
            Map<String, Locale> l = new HashMap<>();
            String[] countryCodes = Locale.getISOCountries();
            for (String code : countryCodes) {
                l.put(code, new Locale("en", code));
            }
            countryLocales = l;
        }

        return countryLocales;
    }

    @Override
    public List<ChoiceListValue> getValues(Object context) {
        Locale locale = context instanceof Locale ? (Locale) context : Locale.ENGLISH;

        Map<String, Locale> locales = getContryLocales();
        List<ChoiceListValue> options = new ArrayList<ChoiceListValue>(locales.size());
        for (Map.Entry<String, Locale> entry : locales.entrySet()) {
            options.add(new ChoiceListValue(entry.getKey(), entry.getValue().getDisplayCountry(locale)));
        }
        Collections.sort(options, NAME_COMPARATOR);

        return options;
    }
}
