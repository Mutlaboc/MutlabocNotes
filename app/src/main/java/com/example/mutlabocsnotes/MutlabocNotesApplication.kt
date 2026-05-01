package com.example.mutlabocsnotes

import android.app.Application

// Application живёт столько же, сколько процесс приложения, поэтому держит общий DI-контейнер.
class MutlabocNotesApplication : Application() {
    // Создаём контейнер лениво, чтобы не инициализировать сеть и хранилище раньше первого обращения.
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
