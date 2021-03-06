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

package org.apache.aries.cdi.container.internal.model;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.apache.aries.cdi.container.test.AbstractTestBase;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_TupleTest extends AbstractTestBase {

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public Map.Entry m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wildcard() throws Exception {
		class C {
			@Inject
			@Reference
			public Map.Entry<Map<String, ?>, ?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wildcard_b() throws Exception {
		class C {
			@Inject
			@Reference
			public Map.Entry<Map<String, Object>, ?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test
	public void withoutServiceType_typed() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public Map.Entry<Map<String, ?>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test
	public void withoutServiceType_typed_b() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, Object>, Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public Map.Entry<Map<String, Object>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wrongKeyType_A() throws Exception {
		class C {
			@Inject
			@Reference
			public Map.Entry<?, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wrongKeyType_B() throws Exception {
		class C {
			@Inject
			@Reference
			public Map.Entry<Collection<?>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wrongKeyType_C() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public Map.Entry<Map, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wrongKeyType_D() throws Exception {
		class C {
			@Inject
			@Reference
			public Map.Entry<Map<?, Foo>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wrongKeyType_E() throws Exception {
		class C {
			@Inject
			@Reference
			public Map.Entry<Map<String, Foo>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceType_wrongKeyType_F() throws Exception {
		class C {
			@Inject
			@Reference
			public Map.Entry<Map<String, ? extends Foo>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference(Integer.class)
			public Map.Entry m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test
	public void withServiceType_wildcard() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, ?>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, ?>, ?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceType_wildcard_b() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, Object>, ?>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, Object>, ?> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceType_typed() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, ?>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceType_typed_b() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, Object>, Integer>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, Object>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_A() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<?, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_B() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Collection<?>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_C() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_D() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<?, Foo>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_E() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, Foo>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongKeyType_F() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, ? extends Foo>, Integer> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withServiceType_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, ?>, Foo> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test
	public void withServiceType_supertype() throws Exception {
		Type type = new TypeReference<
			Map.Entry<Map<String, ?>, Number>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Map.Entry<Map<String, ?>, Number> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Map.Entry.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertFalse(referenceModel.optional());
		assertTrue(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceType_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public Collection<Map.Entry> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test
	public void collectionWithoutServiceType_typed() throws Exception {
		Type type = new TypeReference<
			Collection<Map.Entry<Map<String, Object>, Foo>>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public Collection<Map.Entry<Map<String, Object>, Foo>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test
	public void collectionWithServiceType_typed_b() throws Exception {
		Type type = new TypeReference<
			Collection<Map.Entry<Map<String, ?>, Number>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<Map.Entry<Map<String, ?>, Number>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithServiceType_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Collection<Map.Entry<Map<String, Object>, Foo>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceType_raw() throws Exception {
		class C {
			@SuppressWarnings("rawtypes")
			@Inject
			@Reference
			public List<Map.Entry> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test
	public void listWithoutServiceType_typed() throws Exception {
		Type type = new TypeReference<
			List<Map.Entry<Map<String, Object>, Foo>>
		>(){}.getType();

		class C {
			@Inject
			@Reference
			public List<Map.Entry<Map<String, Object>, Foo>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceType_typed_b() throws Exception {
		Type type = new TypeReference<
			List<Map.Entry<Map<String, ?>, Number>>
		>(){}.getType();

		class C {
			@Inject
			@Reference(Integer.class)
			public List<Map.Entry<Map<String, ?>, Number>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Integer.class, referenceModel.getServiceType());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertFalse(referenceModel.dynamic());
		assertTrue(referenceModel.optional());
		assertFalse(referenceModel.unary());
		assertEquals(CollectionType.TUPLE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithServiceType_wrongtype() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public List<Map.Entry<Map<String, ?>, Foo>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceType() throws Exception {
		class C {
			@Inject
			@Reference
			public Instance<Map.Entry<Map<String, ?>, Number>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithServiceType() throws Exception {
		class C {
			@Inject
			@Reference(Integer.class)
			public Instance<Map.Entry<Map<String, ?>, Number>> m;
		}

		InjectionPoint injectionPoint = new MockInjectionPoint(C.class.getField("m"));

		new ReferenceModel.Builder(injectionPoint.getAnnotated()).type(injectionPoint.getType()).build();
	}

}