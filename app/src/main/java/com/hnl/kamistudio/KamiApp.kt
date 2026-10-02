package com.hnl.kamistudio

import android.app.Application
import com.hnl.kamistudio.data.KamiDatabase

class KamiApp : Application() {
    val database: KamiDatabase by lazy { KamiDatabase.getDatabase(this) }
}
