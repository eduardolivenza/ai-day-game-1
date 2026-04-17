plugins {
    id("java")
    id("application")
}

dependencies {
    implementation("org.json:json:20240303")
}

application {
    mainClass.set("com.game.Main")
    applicationDefaultJvmArgs = listOf("-Dsun.java2d.opengl=true")
}

// Run the game with the module directory as working dir so that
// config.properties and save.json are resolved relative to game/
tasks.named<JavaExec>("run") {
    workingDir = project.projectDir
}
