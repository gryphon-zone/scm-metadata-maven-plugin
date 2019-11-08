/*
 * Copyright 2019-2019 Gryphon Zone
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

package zone.gryphon.maven.plugins.scm.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ScmUrl {

    /**
     * Parses the <code>&lt;scm_provider&gt;</code> component of a
     * the given Maven <a href="https://maven.apache.org/scm/scm-url-format.html">Maven SCM URL</a>.
     * <br>
     * The complete URL format is
     * <code>scm:&lt;scm_provider&gt;&lt;delimiter&gt;&lt;provider_specific_part&gt;</code>,
     * where <code>&lt;delimiter&gt;</code> can be either "<code>:</code>" or "<code>|</code>"
     *
     * @param scm The URL
     * @return The <code>&lt;scm_provider&gt;</code> component of the URL
     * @throws IllegalArgumentException if the provided URL is invalid
     */
    public static ScmUrl parse(String scm) throws IllegalArgumentException {
        final String requiredPrefix = "scm:";
        final String requiredFormat = "scm:<scm_provider><delimiter><provider_specific_part>";

        if (scm == null) {
            throw new IllegalArgumentException("SCM URL is null");
        }

        if (!scm.startsWith(requiredPrefix)) {
            throw new IllegalArgumentException(String.format("SCM URL is malformed, does not start with \"%s\": \"%s\"", requiredPrefix, scm));
        }

        int colonIndex = scm.indexOf(':', requiredPrefix.length());
        int pipeIndex = scm.indexOf('|', requiredPrefix.length());

        int index;

        // Values < 0 indicate the character wasn't found.
        if (colonIndex < 0 || pipeIndex < 0) {
            index = Math.max(colonIndex, pipeIndex);
        } else {
            index = Math.min(pipeIndex, colonIndex);
        }

        if (index < 0) {
            throw new IllegalArgumentException(String.format("SCM URL is malformed, does not adhere to format \"%s\": \"%s\"", requiredFormat, scm));
        }

        String provider = scm.substring(requiredPrefix.length(), index);
        String delimiter = scm.substring(index, index + 1);
        String url = scm.substring(index + 1);

        return ScmUrl.builder()
            .provider(provider)
            .delimiter(delimiter)
            .url(url)
            .build();
    }

    private String provider;

    private String delimiter;

    private String url;

}
