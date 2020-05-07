package org.dpppt.android.sdk.internal;

import android.os.Bundle;

public interface TracingController {

	void setParams(Bundle extras);

	void start();

	void stop();

	void restartClient();

	void restartServer();

	void destroy();

}
