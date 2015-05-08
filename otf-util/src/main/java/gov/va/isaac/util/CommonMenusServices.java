package gov.va.isaac.util;

import gov.va.isaac.util.CommonMenus.CommonMenuItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class CommonMenusServices {

	// Interface for service call lambda expressions
	static interface ServiceCall<T> {
		T executeServiceCall();
	}

	private static final Map<CommonMenus.CommonMenuItem, ServiceCall<?>> serviceCallCache = Collections.synchronizedMap(new HashMap<>());

	static synchronized void setServiceCall(CommonMenuItem item, ServiceCall<?> serviceCall) {
		serviceCallCache.put(item, serviceCall);
	}

	static synchronized CommonMenusServices.ServiceCall<?> getServiceCall(CommonMenuItem item) {
		return CommonMenusServices.serviceCallCache.get(item);
	}

	// Cache for availability of services for respective CommonMenuItem
	private static final Map<CommonMenuItem, Boolean> serviceAvailabilityCache = Collections.synchronizedMap(new HashMap<>());

	// Initialize and/or get service availability for CommonMenuItem
	static synchronized boolean isServiceAvailable(CommonMenuItem item) {
		if (serviceAvailabilityCache.get(item) == null) {
			if (getServiceCall(item) == null) {
				serviceAvailabilityCache.put(item, false);
			} else {
				serviceAvailabilityCache.put(item, getServiceCall(item).executeServiceCall() != null);
			}
		}

		return serviceAvailabilityCache.get(item);
	}
}