package org.dpppt.android.sdk.internal.crypto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.database.models.Contact;
import org.dpppt.android.sdk.internal.database.models.Handshake;

public class ContactsFactory {

	public static final int NUMBER_OF_WINDOWS_FOR_EXPOSURE = 10;

	private static final long WINDOW_DURATION = 60 * 1000l;

	private static final double BAD_RSSI_THRESHOLD = -85.0;
	//TODO: set correct value
	private static final double CONTACT_RSSI_THRESHOLD = -80.0;
	//TODO: set correct value
	private static final double EVENT_THRESHOLD = 0.8;

	public static List<Contact> mergeHandshakesToContacts(List<Handshake> handshakes) {
		HashMap<EphId, List<Handshake>> handshakeMapping = new HashMap<>();

		// group handhakes by id
		for (Handshake handshake : handshakes) {
			if (!handshakeMapping.containsKey(handshake.getEphId())) {
				handshakeMapping.put(handshake.getEphId(), new ArrayList<>());
			}
			handshakeMapping.get(handshake.getEphId()).add(handshake);
		}

		//filter result to only contain actual contacts in close proximity
		List<Contact> contacts = new ArrayList<>();
		for (List<Handshake> handshakeList : handshakeMapping.values()) {

			List<Handshake> filteredHandshakes = new ArrayList<>();
			for (Handshake handshake : handshakeList) {
				if (handshake.getRssi() > BAD_RSSI_THRESHOLD) {
					filteredHandshakes.add(handshake);
				}
			}

			Double epochMean = mean(handshakes, (h) -> true);
			if (epochMean == null) {
				continue;
			}

			int contactCounter = 0;

			long epochStartTime = CryptoModule.getEpochStart(handshakeList.get(0).getTimestamp());
			for (long offset = 0; offset < CryptoModule.MILLISECONDS_PER_EPOCH; offset += WINDOW_DURATION) {
				long windowStart = epochStartTime + offset;
				long windowEnd = epochStartTime + offset + WINDOW_DURATION;
				Double windowMean = mean(handshakes, (h) -> h.getTimestamp() >= windowStart && h.getTimestamp() < windowEnd);

				if (windowMean != null && windowMean / epochMean > EVENT_THRESHOLD && windowMean > CONTACT_RSSI_THRESHOLD) {
					contactCounter++;
				}
			}

			contacts.add(
					new Contact(-1, floorTimestampToBucket(handshakeList.get(0).getTimestamp()), handshakeList.get(0).getEphId(),
							contactCounter,
							0));
		}

		return contacts;
	}

	private static Double mean(List<Handshake> handshakes, Condition condition) {
		Double valueSum = null;
		int count = 0;
		for (Handshake handshake : handshakes) {
			if (condition.test(handshake)) {
				if (valueSum == null) {
					valueSum = 0.0;
				}
				valueSum += handshake.getRssi();
				count++;
			}
		}
		if (valueSum != null) {
			return valueSum / count;
		} else {
			return null;
		}
	}

	private interface Condition {
		boolean test(Handshake handshake);

	}

	private static long floorTimestampToBucket(long timestamp) {
		return timestamp - (timestamp % BackendBucketRepository.BATCH_LENGTH);
	}

}
