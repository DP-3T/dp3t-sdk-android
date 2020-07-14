package org.dpppt.android.sdk.internal.storage.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.dpppt.android.sdk.TracingStatus;

/**
 * Map: ErrorState key -> suppressed until (unit timestamp, millis)
 */
public class ActiveNotificationErrors extends HashMap<String, Long> {

	public Set<TracingStatus.ErrorState> getUnsuppressedErrors(long now) {
		Set<TracingStatus.ErrorState> unsuppressedErrors = new HashSet<>();
		for (String errorKey : getUnsuppressedErrorKeys(now)) {
			TracingStatus.ErrorState error = TracingStatus.ErrorState.tryValueOf(errorKey);
			if (error != null) {
				unsuppressedErrors.add(error);
			}
		}
		return unsuppressedErrors;
	}

	/**
	 * @return timestamp of next unsuppression event, -1 if no future event.
	 */
	public long refreshActiveErrors(Collection<TracingStatus.ErrorState> activeErrors, long now, long suppressNewErrorsUntil) {
		if (activeErrors.isEmpty()) {
			clear();
			return -1;
		}

		Set<String> activeErrorKeys = new HashSet<>();
		for (TracingStatus.ErrorState activeError : activeErrors) {
			activeErrorKeys.add(activeError.name());
		}

		// remove obsolete errors
		for (String errorKey : new HashSet<>(keySet())) {
			if (!activeErrorKeys.contains(errorKey)) {
				remove(errorKey);
			}
		}

		// add new errors with suppressNewErrorsUntil
		for (String errorKey : activeErrorKeys) {
			if (!containsKey(errorKey)) {
				put(errorKey, suppressNewErrorsUntil);
			}
		}

		Long nextUnsuppressionTime = getNextUnsuppressionTime(now);
		if (nextUnsuppressionTime != null) {
			return nextUnsuppressionTime;
		} else {
			return -1;
		}
	}

	private Set<String> getUnsuppressedErrorKeys(long now) {
		Set<String> unsuppressedErrorKeys = new HashSet<>();
		for (Entry<String, Long> errorKeySuppressedUntil : entrySet()) {
			if (errorKeySuppressedUntil.getValue() <= now) {
				unsuppressedErrorKeys.add(errorKeySuppressedUntil.getKey());
			}
		}
		return unsuppressedErrorKeys;
	}

	private Long getNextUnsuppressionTime(long now) {
		Long nextUnsuppressionTime = null;
		for (Entry<String, Long> errorKeySuppressedUntil : entrySet()) {
			if (errorKeySuppressedUntil.getValue() > now &&
					(nextUnsuppressionTime == null || errorKeySuppressedUntil.getValue() < nextUnsuppressionTime)) {
				nextUnsuppressionTime = errorKeySuppressedUntil.getValue();
			}
		}
		return nextUnsuppressionTime;
	}

}
