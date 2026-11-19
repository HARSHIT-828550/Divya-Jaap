package com.example

import android.app.Application
import com.example.core.AppContainer

class DivyaJaapApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
