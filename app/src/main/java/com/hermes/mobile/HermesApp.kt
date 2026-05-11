package com.hermes.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class. @HiltAndroidApp triggers Hilt component generation.
 * All Hilt singletons are scoped to this application lifecycle.
 */
@HiltAndroidApp
class HermesApp : Application()
