/**
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

package org.apache.aries.cdi.weld;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.spi.Metadata;

public class ExtensionMetadata implements Metadata<Extension> {

	public ExtensionMetadata(Extension extension, String location) {
		_extension = extension;
		_location = location;
	}

	@Override
	public Extension getValue() {
		return _extension;
	}

	@Override
	public String getLocation() {
		return _location;
	}

	private final Extension _extension;
	private final String _location;

}