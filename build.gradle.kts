
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.5.0"
    kotlin("plugin.serialization") version "1.9.25"
}

// todo https://blog.jetbrains.com/platform/2024/07/intellij-platform-gradle-plugin-2-0/?lidx=1&wpid=497624
// todo example: https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/build.gradle.kts

group = "nautime.io"

kotlin {
    jvmToolchain(17)
}


repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    buildSearchableOptions = true
    projectName = "nautime.io"

    pluginConfiguration {
        id = "nautime.io"
        name = "Nau Time Tracker"
        version = "1.3.0"
        description = """
            An easy automatic time tracker: Increases your work efficiency and assists with time management. Featuring a modern web UI and a mobile application
          <br/>
          <br/>
          <b>Install</b>
          <br/>
          <ul>
            <li>Inside your IDE, select Settings > Plugins</li>
            <li>Search for Nau Time Tracker</li>
            <li>Install plugin</li>
            <li>Click on the Nau icon in the status bar and register at <a href="https://nautime.io?utm_source=plugin-jetbrains&utm_content=plugin_desc">nautime.io</a></li>
            <li>Enjoy your coding stats</li>
          </ul>
          <br/>
          <br/>
          <b>Security</b>
          <br/>
          We do not store, transfer, or have access to your code. All collected statistics are private and available only to you
          <br/>
          Nau Time plugin is open source and transparent
          <br/>
          """.trimIndent()
        changeNotes = """
            1.3.0
            <br />
            Up idea build plugin version
            <br />
            1.2.22
            <br />
            Minor improves
            <br />
            1.2.20
            <br />
            Add unlink to settings
            <br />
            1.2.18
            <br />
            Add top repositories stats to status bar tooltip
            <br />
            1.2.14
            <br />
            Stability fixes
            <br />
            1.2.9
            <br />
            2024.1 compatibility
            <br />
            1.2.8
            <br />
            Minor fixes
            <br />
            1.2.6
            <br />
            Improve toolbar stats
            <br />
            1.1.0
            <br />
            Bug fixes
            <br />
            1.0.4
            <br />
            Compatibility fixes
            """.trimIndent()

//        productDescriptor {
//            // ...
//        }
        ideaVersion {
            sinceBuild = "222"
            untilBuild = provider { null }
        }

        vendor {
            name = "nautime.io"
            email = "alex@nautime.io"
            url = "https://www.nautime.io"
        }
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    intellijPlatform {
        intellijIdeaCommunity("2024.2")
    }
}
