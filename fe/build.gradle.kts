plugins {
    id("com.android.application") version "8.13.2" apply false
    kotlin("android") version "1.9.24" apply false
}

subprojects {
    if (tasks.findByName("prepareKotlinBuildScriptModel") == null) {
        tasks.register("prepareKotlinBuildScriptModel")
    }
}
