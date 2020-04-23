package org.dpppt.android.sdk.internal.crypto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.database.models.Contact;
import org.dpppt.android.sdk.internal.database.models.Handshake;

import static org.dpppt.android.sdk.internal.crypto.CryptoModule.CONTACT_THRESHOLD;

public class ContactsFactory {

	public static List<Contact> mergeHandshakesToContacts(List<Handshake> handshakes) {
		HashMap<EphId, List<Handshake>> handshakeMapping = new HashMap<>();

		// group handhakes by id
		for (Handshake handshake : handshakes) {
			if (!handshakeMapping.containsKey(handshake.getEphId())) {
				handshakeMapping.put(handshake.getEphId(), new ArrayList<>());
			}
			handshakeMapping.get(handshake.getEphId()).add(handshake);
		}

		//filter result to only contain ephIDs which have been seen more than contactThreshold times
		List<Contact> contacts = new ArrayList<>();
		for (List<Handshake> handshakeList : handshakeMapping.values()) {
			if (handshakeList.size() > CONTACT_THRESHOLD) {
				contacts.add(new Contact(-1, floorTimestampToBucket(handshakeList.get(0).getTimestamp()),
						handshakeList.get(0).getEphId(), 0));
			}
		}

		return contacts;
	}

	private static long floorTimestampToBucket(long timestamp) {
		return timestamp - (timestamp % BackendBucketRepository.BATCH_LENGTH);
	}

}
