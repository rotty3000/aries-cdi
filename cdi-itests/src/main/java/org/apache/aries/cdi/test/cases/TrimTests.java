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

package org.apache.aries.cdi.test.cases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;

public class TrimTests extends SlimBaseTestCase {

	@Test
	public void testTrimmed() {
		Bundle tb2Bundle = installBundle.installBundle("tb17.jar");

		ContainerDTO containerDTO = getContainerDTO(tb2Bundle);
		assertNotNull(containerDTO);

		assertEquals( // expected: B, E, F, G, H
				String.join(", ", containerDTO.template.components.get(0).beans),
				5, containerDTO.template.components.get(0).beans.size());

		assertEquals(2, containerDTO.template.components.size());

		assertEquals(2, containerDTO.template.components.get(1).beans.size());
	}

}
