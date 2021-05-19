package org.dpppt.android.sdk

import android.content.Context
import android.os.Parcelable
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.parcelize.Parcelize
import org.dpppt.android.sdk.backend.ResponseCallback
import org.dpppt.android.sdk.models.DayDate
import org.dpppt.android.sdk.models.ExposeeAuthMethod
import java.util.*

@Parcelize
class PendingUploadTask(private val temporaryExposureKeys: List<TemporaryExposureKey>) : Parcelable {

	fun performUpload(context: Context, onset: Date, exposeeAuthMethod: ExposeeAuthMethod, callback: ResponseCallback<DayDate>) {
		DP3T.uploadTEKs(context, onset, exposeeAuthMethod, callback, temporaryExposureKeys)
	}

}