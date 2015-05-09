package gov.va.isaac.util;

import gov.va.isaac.util.CommonMenus.CommonMenuItem;
import gov.vha.isaac.ochre.api.LookupService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CommonMenusServices {
	private static final Logger LOG = LoggerFactory.getLogger(CommonMenusServices.class);

	static synchronized void setServiceCallParameters(CommonMenuItem item, Class<?> serviceClass) {
		setServiceCallParameters(item, serviceClass, (String)null);
	}
	static synchronized void setServiceCallParameters(CommonMenuItem item, Class<?> serviceClass, String serviceName) {
		_setServiceCallParameters(item, new ServiceCallParameters<>(serviceClass, serviceName));
	}

	// Initialize and/or get service availability for CommonMenuItem
	static synchronized boolean hasService(CommonMenuItem item) {
		if (_serviceAvailabilityCache.get(item) == null) {
			if (_getServiceCallParameters(item) == null) {
				_serviceAvailabilityCache.put(item, false);
			} else {
				try {
					_serviceAvailabilityCache.put(item, _hasService(_getServiceCallParameters(item)));
				} catch (Exception e) {
					_serviceAvailabilityCache.put(item, false);
					
					LOG.error("isServiceAvailable() disabling service because failed invoking service call.  Caught {} {}", e.getClass().getName(), e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}

		return _serviceAvailabilityCache.get(item);
	}
	
	static synchronized Object getService(CommonMenuItem item) {
		return hasService(item) ? _getService(_getServiceCallParameters(item)) : null;
	}

	// Class for service call parameters
	private static class ServiceCallParameters<T> {
		private final Class<T> serviceClass;
		private final String serviceName;
		
		public Class<T> getServiceClass() {
			return serviceClass;
		}
		public String getServiceName() {
			return serviceName;
		}
		public ServiceCallParameters(Class<T> serviceClass, String serviceName) {
			super();
			this.serviceClass = serviceClass;
			this.serviceName = serviceName;
		}
	}
	
	private static boolean _hasService(ServiceCallParameters<?> params) {
		if (params.serviceName != null) {
			return LookupService.hasService(params.getServiceClass(), params.getServiceName());
		} else {
			return LookupService.hasService(params.getServiceClass());
		}
	}
	private static <T> T _getService(ServiceCallParameters<T> params) {
		if (params.serviceName != null) {
			return LookupService.getService(params.getServiceClass(), params.getServiceName());
		} else {
			return LookupService.getService(params.getServiceClass());
		}
	}

	private static final Map<CommonMenus.CommonMenuItem, ServiceCallParameters<?>> _serviceCallParametersCache = Collections.synchronizedMap(new HashMap<>());

	private static synchronized void _setServiceCallParameters(CommonMenuItem item, ServiceCallParameters<?> serviceCall) {
		_serviceCallParametersCache.put(item, serviceCall);
	}
	private static synchronized CommonMenusServices.ServiceCallParameters<?> _getServiceCallParameters(CommonMenuItem item) {
		return CommonMenusServices._serviceCallParametersCache.get(item);
	}

	// Cache for availability of services for respective CommonMenuItem
	private static final Map<CommonMenuItem, Boolean> _serviceAvailabilityCache = Collections.synchronizedMap(new HashMap<>());
}
