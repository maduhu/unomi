package org.oasis_open.contextserver.api.segments;

/*
 * #%L
 * context-server-api
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

import java.util.Map;
import java.util.Set;

public class SegmentsAndScores {
    private Set<String> segments;
    private Map<String,Integer> scores;

    public SegmentsAndScores(Set<String> segments, Map<String, Integer> scores) {
        this.segments = segments;
        this.scores = scores;
    }

    public Set<String> getSegments() {
        return segments;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }
}
