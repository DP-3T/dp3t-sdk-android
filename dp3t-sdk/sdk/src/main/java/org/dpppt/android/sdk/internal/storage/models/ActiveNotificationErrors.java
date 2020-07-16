package org.dpppt.android.sdk.internal.storage.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.dpppt.android.sdk.TracingStatus.ErrorState;

/**
 * Map: ErrorState key -> suppressed until (unit timestamp, millis)
 */
public class ActiveNotificationErrors extends HashMap<String, Long> {

	public Set<ErrorState> getUnsuppressedErrors(long now) {
		Set<ErrorState> unsuppressedErrors = new HashSet<>();
		for (String errorKey : getUnsuppressedErrorKeys(now)) {
			ErrorState error = ErrorState.tryValueOf(errorKey);
			if (error != null) {
				unsuppressedErrors.add(error);
			}
		}
		return unsuppressedErrors;
	}

	/**
	 * @return timestamp of next unsuppression event, -1 if no future event.
	 */
	public long refreshActiveErrors(Collection<ErrorState> activeErrors, long now, long suppressNewErrorsUntil,
			Collection<ErrorState> unsuppressableErrors) {
		if (activeErrors.isEmpty()) {
			clear();
			return -1;
		}

		Set<String> activeErrorKeys = new HashSet<>();
		for (ErrorState activeError : activeErrors) {
			activeErrorKeys.add(activeError.name());
		}

		// remove obsolete errors
		for (String errorKey : new HashSet<>(keySet())) {
			if (!activeErrorKeys.contains(errorKey)) {
				remove(errorKey);
			}
		}

		// add new errors with suppressNewErrorsUntil
		for (ErrorState error : activeErrors) {
			String errorKey = error.name();
			if (!containsKey(errorKey)) {
				put(errorKey, unsuppressableErrors.contains(error) ? -1 : suppressNewErrorsUntil);
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
