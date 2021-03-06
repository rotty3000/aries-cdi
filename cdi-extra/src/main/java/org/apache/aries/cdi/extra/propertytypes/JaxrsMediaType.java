/*
 * Copyright (c) OSGi Alliance (2017, 2019). All Rights Reserved.
 *
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
 */

package org.apache.aries.cdi.extra.propertytypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.BeanPropertyType;

/**
 * Component Property Type for the {@code osgi.jaxrs.media.type} service
 * property.
 * <p>
 * This annotation can be used on a JAX-RS extension or application to declare
 * the value of the
 * {@link org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants#JAX_RS_MEDIA_TYPE}
 * service property.
 */
@BeanPropertyType
@Retention(RUNTIME)
@Target({FIELD, METHOD, TYPE})
public @interface JaxrsMediaType {

	public static final class Literal extends AnnotationLiteral<JaxrsMediaType> implements JaxrsMediaType {

		private static final long serialVersionUID = 1L;

		public static final Literal of(String[] value) {
			return new Literal(value);
		}

		private Literal(String[] value) {
			_value = value;
		}

		@Override
		public String[] value() {
			return _value;
		}

		private final String[] _value;
	}

	/**
	 * Prefix for the property name. This value is prepended to each property
	 * name.
	 */
	String PREFIX_ = "osgi.";

	/**
	 * Service property identifying the name(s) of media types supported by this
	 * service.
	 *
	 * @return The JAX-RS media types supported.
	 * @see org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants#JAX_RS_MEDIA_TYPE
	 */
	String[] value();
}
