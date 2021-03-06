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

package org.apache.aries.cdi.container.internal.container;

import static javax.interceptor.Interceptor.Priority.PLATFORM_AFTER;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;

import org.apache.aries.cdi.container.internal.bean.ComponentPropertiesBean;
import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.model.ComponentPropertiesModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.util.Annotates;
import org.apache.aries.cdi.container.internal.util.Perms;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentProperties;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.reference.BindBeanServiceObjects;
import org.osgi.service.cdi.reference.BindService;
import org.osgi.service.cdi.reference.BindServiceReference;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.promise.Promise;

public class RuntimeExtension implements Extension {

	protected RuntimeExtension() { // for extension proxy
		_containerState = null;
		_log = null;
		_configurationBuilder = null;
		_singleBuilder = null;
		_factoryBuilder = null;
		_containerTemplate = null;
	}

	public RuntimeExtension(
		ContainerState containerState,
		ConfigurationListener.Builder configurationBuilder,
		SingleComponent.Builder singleBuilder,
		FactoryComponent.Builder factoryBuilder) {

		_containerState = containerState;

		_log = _containerState.containerLogs().getLogger(getClass());
		_log.debug(l -> l.debug("CCR RuntimeExtension {}", containerState.bundle()));

		_configurationBuilder = configurationBuilder;
		_singleBuilder = singleBuilder;
		_factoryBuilder = factoryBuilder;
		_containerTemplate = _containerState.containerDTO().template.components.get(0);
	}

	void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
		bbd.addQualifier(org.osgi.service.cdi.annotations.ComponentProperties.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.MinimumCardinality.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.PID.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.PrototypeRequired.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Reference.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Reluctant.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Service.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.ServiceInstance.class);
		bbd.addScope(org.osgi.service.cdi.annotations.ComponentScoped.class, false, false);
		bbd.addStereotype(org.osgi.service.cdi.annotations.FactoryComponent.class);
		bbd.addStereotype(org.osgi.service.cdi.annotations.SingleComponent.class);
	}

	void processBindObject(@Observes ProcessInjectionPoint<?, BindService<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	void processBindServiceObjects(@Observes ProcessInjectionPoint<?, BindBeanServiceObjects<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	void processBindServiceReference(@Observes ProcessInjectionPoint<?, BindServiceReference<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
		processInjectionPoint0(pip, false);
	}

	<X> void processBean(@Observes ProcessBean<X> pb) {
		final Class<X> declaringClass = Annotates.declaringClass(pb);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _containerState.beansModel().getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		final Annotated annotated = pb.getAnnotated();

		try {
			List<String> serviceTypes = Annotates.serviceClassNames(annotated);
			Map<String, Object> componentProperties = Annotates.componentProperties(annotated);
			ServiceScope serviceScope = Annotates.serviceScope(annotated);

			if (annotated.isAnnotationPresent(org.osgi.service.cdi.annotations.SingleComponent.class) ||
				annotated.isAnnotationPresent(org.osgi.service.cdi.annotations.FactoryComponent.class)) {

				ActivationTemplateDTO activationTemplate = osgiBean.getComponent().activations.get(0);
				activationTemplate.scope = serviceScope;
				activationTemplate.serviceClasses = serviceTypes;
				osgiBean.getComponent().properties = componentProperties;
			}
			else if (annotated.isAnnotationPresent(org.osgi.service.cdi.annotations.ComponentScoped.class)) {
				// Explicitly ignore this case
			}
			else if (!serviceTypes.isEmpty()) {
				AnnotatedMember<?> producer = null;

				if (pb instanceof ProcessProducerField) {
					producer = ((ProcessProducerField<?, ?>) pb).getAnnotatedProducerField();
				}
				else if (pb instanceof ProcessProducerMethod) {
					producer = ((ProcessProducerMethod<?, ?>) pb).getAnnotatedProducerMethod();
				}

				ExtendedActivationTemplateDTO activationTemplate = null;

				for (ActivationTemplateDTO at : _containerTemplate.activations) {
					ExtendedActivationTemplateDTO extended = (ExtendedActivationTemplateDTO)at;
					if (extended.declaringClass.equals(declaringClass) &&
							equals(extended.producer, producer)) {

						activationTemplate = extended;
						break;
					}
				}

				if (activationTemplate == null) {
					activationTemplate = new ExtendedActivationTemplateDTO();
					activationTemplate.cdiScope = pb.getBean().getScope();
					activationTemplate.declaringClass = declaringClass;
					activationTemplate.producer = producer;
					_containerTemplate.activations.add(activationTemplate);
				}

				activationTemplate.properties = componentProperties;
				activationTemplate.scope = serviceScope;
				activationTemplate.serviceClasses = serviceTypes;
			}
		}
		catch (Exception e) {
			pb.addDefinitionError(e);
		}
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
		abd.addContext(_containerState.componentContext());

		_containerState.containerDTO().template.components.forEach(
			ct -> addBeans(ct, abd, bm)
		);
	}

	void afterDeploymentValidation(
		@Observes @Priority(PLATFORM_AFTER + 100) AfterDeploymentValidation adv,
		BeanManager bm) {

		_log.debug(l -> l.debug("CCR AfterDeploymentValidation on {}", _containerState.bundle()));

		_containerState.beanManager(bm);

		ComponentDTO componentDTO = _containerState.containerDTO().components.get(0);

		registerServices(componentDTO, bm);

		_containerState.submit(
			Op.of(Mode.OPEN, Type.CONTAINER_INIT_COMPONENTS, _containerState.id()),
			this::initComponents
		).then(s -> {
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(CDIConstants.CDI_CONTAINER_ID, _containerState.id());
			properties.put(Constants.SERVICE_DESCRIPTION, "Aries CDI - BeanManager for " + _containerState.bundle());
			properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");

			List<String> serviceTypes = new ArrayList<>();

			serviceTypes.add(BeanManager.class.getName());

			registerService(serviceTypes, bm, properties);

			_log.debug(l -> l.debug("CCR Container READY for {}", _containerState.bundle()));

			return s;
		});
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		_log.debug(l -> l.debug("CCR BeforeShutdown on {}", _containerState.bundle()));

		_containerState.beanManager(null);

		_configurationListeners.removeIf(
			cl -> {
				_containerState.submit(cl.closeOp(), cl::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error while closing configuration listener {} on {}", cl, _containerState.bundle(), f));
					}
				);

				return true;
			}
		);

		_registrations.removeIf(
			r -> {
				try {
					r.unregister();
				}
				catch (Exception e) {
					_log.error(l -> l.error("CCR Error while unregistring {} on {}", r, _containerState.bundle(), e));
				}
				return true;
			}
		);
	}

	private void addBeans(ComponentTemplateDTO componentTemplate, AfterBeanDiscovery abd, BeanManager bm) {
		ComponentDTO componentDTO = _containerState.containerDTO().components.get(0);

		componentTemplate.references.stream().map(ExtendedReferenceTemplateDTO.class::cast).forEach(
			t -> {
				ReferenceBean bean = t.bean;
				bean.setBeanManager(bm);
				if (componentTemplate.type == ComponentType.CONTAINER) {
					componentDTO.instances.get(0).references.stream().filter(
						r -> r.template == t
					).findFirst().map(
						ExtendedReferenceDTO.class::cast
					).ifPresent(
						bean::setReferenceDTO
					);
				}

				_log.debug(l -> l.debug("CCR Adding synthetic bean {} on {}", bean, _containerState.bundle()));

				abd.addBean(bean);
			}
		);

		componentTemplate.configurations.stream().map(ExtendedConfigurationTemplateDTO.class::cast).filter(
			t -> Objects.nonNull(t.injectionPointType)
		).forEach(
			t -> {
				ComponentPropertiesBean bean = t.bean;

				_log.debug(l -> l.debug("CCR Adding synthetic bean {} on {}", bean, _containerState.bundle()));

				abd.addBean(bean);
			}
		);
	}

	@SuppressWarnings("unchecked")
	private Producer<Object> createProducer(Object producerObject, Bean<Object> bean, BeanManager bm) {
		ProducerFactory<Object> producerFactory = null;
		if (producerObject instanceof AnnotatedField)
			producerFactory = bm.getProducerFactory((AnnotatedField<Object>)producerObject, bean);
		else if (producerObject instanceof AnnotatedMethod)
			producerFactory = bm.getProducerFactory((AnnotatedMethod<Object>)producerObject, bean);

		if (producerFactory == null)
			return null;

		return producerFactory.createProducer(bean);
	}

	// Objects.equals(producer, producer1) is not expected to work so impl it as expected there
	private boolean equals(AnnotatedMember<?> producerA, AnnotatedMember<?> producerB) {
		if ((producerA == null) && (producerB == null)) return true;
		if (!Objects.equals(producerA.getJavaMember(), producerB.getJavaMember())) {
			return false;
		}
		if (!Objects.equals(producerA.getAnnotations(), producerB.getAnnotations())) {
			return false;
		}
		return true;
	}

	private Promise<Boolean> initComponents() {
		_containerState.containerDTO().template.components.stream().filter(
			t -> t.type != ComponentType.CONTAINER
		).map(ExtendedComponentTemplateDTO.class::cast).forEach(
			this::initComponent
		);

		return null;
	}

	private void initComponent(ExtendedComponentTemplateDTO componentTemplateDTO) {
		if (componentTemplateDTO.type == ComponentType.FACTORY) {
			initFactoryComponent(componentTemplateDTO);
		}
		else {
			initSingleComponent(componentTemplateDTO);
		}
	}

	private Promise<Boolean> initFactoryComponent(ExtendedComponentTemplateDTO componentTemplateDTO) {
		ConfigurationListener cl = _configurationBuilder.component(
			_factoryBuilder.template(componentTemplateDTO).build()
		).build();

		_configurationListeners.add(cl);

		return _containerState.submit(cl.openOp(), cl::open);
	}

	private Promise<Boolean> initSingleComponent(ExtendedComponentTemplateDTO componentTemplateDTO) {
		ConfigurationListener cl = _configurationBuilder.component(
			_singleBuilder.template(componentTemplateDTO).build()
		).build();

		_configurationListeners.add(cl);

		return _containerState.submit(cl.openOp(), cl::open);
	}

	private void processConfiguration(OSGiBean osgiBean, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = Annotates.declaringClass(injectionPoint.getAnnotated());

		ConfigurationTemplateDTO current = new ComponentPropertiesModel.Builder(injectionPoint.getType()).declaringClass(
			declaringClass
		).qualifiers(
			injectionPoint.getQualifiers()
		).build().toDTO();

		osgiBean.getComponent().configurations.stream().map(
			t -> (ExtendedConfigurationTemplateDTO)t
		).filter(
			t -> current.equals(t)
		).findFirst().ifPresent(
			t -> {
				final Mark mark = Mark.Literal.from(MARK_IP_COUNTER.incrementAndGet());
				pip.configureInjectionPoint().addQualifiers(mark);

				t.bean.setMark(mark);
			}
		);
	}

	private boolean matchReference(OSGiBean osgiBean, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Annotated annotated = injectionPoint.getAnnotated();

		ReferenceModel.Builder builder = new ReferenceModel.Builder(annotated);

		ReferenceModel referenceModel = builder.type(injectionPoint.getType()).build();

		ExtendedReferenceTemplateDTO current = referenceModel.toDTO();

		return osgiBean.getComponent().references.stream().map(
			t -> (ExtendedReferenceTemplateDTO)t
		).filter(
			t -> current.equals(t)
		).findFirst().map(
			t -> {
				final Mark mark = Mark.Literal.from(MARK_IP_COUNTER.incrementAndGet());
				pip.configureInjectionPoint().addQualifier(mark);

				t.bean.setMark(mark);

				_log.debug(l -> l.debug("CCR maping InjectionPoint {} to reference template {}", injectionPoint, t));

				return true;
			}
		).orElse(false);
	}

	private void processInjectionPoint0(ProcessInjectionPoint<?, ?> pip, boolean special) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = Annotates.declaringClass(injectionPoint.getAnnotated());

		String className = declaringClass.getName();

		OSGiBean osgiBean = _containerState.beansModel().getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();
		ComponentProperties componentProperties = annotated.getAnnotation(ComponentProperties.class);
		Reference reference = annotated.getAnnotation(Reference.class);

		if (((reference != null) || special) && matchReference(osgiBean, pip)) {
			return;
		}

		if (componentProperties != null) {
			processConfiguration(osgiBean, pip);
		}
	}

	private void registerServiceHandleFailure(
		ExtendedComponentInstanceDTO componentInstance,
		ExtendedActivationTemplateDTO activationTemplate,
		BeanManager bm) {

		try {
			registerService(componentInstance, activationTemplate, bm);
		}
		catch (Throwable t) {
			_log.error("CDI - An error occured", t);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void registerService(
		ExtendedComponentInstanceDTO componentInstance,
		ExtendedActivationTemplateDTO activationTemplate,
		BeanManager bm) {

		ServiceScope scope = activationTemplate.scope;

		if (activationTemplate.cdiScope == ApplicationScoped.class) {
			scope = ServiceScope.SINGLETON;
		}

		final Context context = bm.getContext(activationTemplate.cdiScope);
		final Bean<Object> bean = (Bean<Object>)bm.resolve(
			bm.getBeans(activationTemplate.declaringClass, Any.Literal.INSTANCE));
		final Producer producer = createProducer(activationTemplate.producer, bean, bm);

		Object serviceObject;

		if (scope == ServiceScope.PROTOTYPE) {
			serviceObject = new PrototypeServiceFactory<Object>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					CreationalContext<Object> cc = bm.createCreationalContext(bean);
					if (producer != null) {
						return producer.produce(cc);
					}
					return context.get(bean, cc);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
				}
			};
		}
		else if (scope == ServiceScope.BUNDLE) {
			serviceObject = new ServiceFactory<Object>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					CreationalContext<Object> cc = bm.createCreationalContext(bean);
					if (producer != null) {
						return producer.produce(cc);
					}
					return context.get(bean, cc);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
				}
			};
		}
		else {
			CreationalContext<Object> cc = bm.createCreationalContext(bean);
			if (producer != null) {
				serviceObject = producer.produce(cc);
			}
			else {
				serviceObject = context.get(bean, cc);
			}
		}

		Objects.requireNonNull(serviceObject, "The service object is somehow null on " + this);

		Dictionary<String, Object> properties = new Hashtable<>(
			componentInstance.componentProperties(activationTemplate.properties));

		ServiceRegistration<?> serviceRegistration = registerService(
			activationTemplate.serviceClasses,
			serviceObject, properties);

		if (serviceRegistration != null) {
			ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
			activationDTO.errors = new CopyOnWriteArrayList<>();
			activationDTO.service = SRs.from(serviceRegistration.getReference());
			activationDTO.template = activationTemplate;
			componentInstance.activations.add(activationDTO);
		}
	}

	private ServiceRegistration<?> registerService(List<String> serviceTypes, Object serviceObject, Dictionary<String, Object> properties) {
		List<String> list = serviceTypes.stream().filter(serviceType ->
			Perms.hasRegisterServicePermission(serviceType, _containerState.bundleContext())
		).collect(Collectors.toList());

		if (list.isEmpty()) {
			return null;
		}

		ServiceRegistration<?> serviceRegistration = _containerState.bundleContext().registerService(
			serviceTypes.toArray(new String[0]), serviceObject, properties);

		_registrations.add(serviceRegistration);

		return serviceRegistration;
	}

	private boolean registerServices(ComponentDTO componentDTO, BeanManager bm) {
		componentDTO.template.activations.stream().map(
			ExtendedActivationTemplateDTO.class::cast
		).forEach(
			a -> registerServiceHandleFailure((ExtendedComponentInstanceDTO)componentDTO.instances.get(0), a, bm)
		);

		return true;
	}

	private final ConfigurationListener.Builder _configurationBuilder;
	private final List<ConfigurationListener> _configurationListeners = new CopyOnWriteArrayList<>();
	private final ContainerState _containerState;
	private final ComponentTemplateDTO _containerTemplate;
	private final FactoryComponent.Builder _factoryBuilder;
	private final Logger _log;
	private final List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();
	private final SingleComponent.Builder _singleBuilder;

	private static final AtomicInteger MARK_IP_COUNTER = new AtomicInteger();
}
