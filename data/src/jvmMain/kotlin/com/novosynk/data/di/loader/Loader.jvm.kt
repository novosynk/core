package com.novosynk.data.di.loader

import com.Musico.media_jvm.di.loadDesktopPlayerModule

actual fun loadMediaService() {
    loadDesktopPlayerModule()
}
