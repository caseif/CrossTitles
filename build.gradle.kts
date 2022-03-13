// Apply Gradle plugins
plugins {
    java
    `maven-publish`
    eclipse
    idea
    checkstyle

    id("org.cadixdev.licenser") version "0.6.1"
}

defaultTasks("clean", "licenseFormat", "build")

// Project information
group = "net.caseif.crosstitles"
version = "0.1.3"
description = "A Minecraft version-independent title API for Bukkit 1.8+."

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

// Project repositories
repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
}

// Project dependencies
dependencies {
    implementation("org.bukkit:bukkit:1.8-R0.1-SNAPSHOT")
}

// Read source files using UTF-8
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<Copy>("processResources") {
    from("crosstitles-LICENSE")
}

// License header formatting
license {
    header(project.file("etc/HEADER.txt"))
    include("**/*.java")
    ignoreFailures(false)
}

// check code style
checkstyle {
    configFile = file("etc/checkstyle.xml")
}

tasks.create<Jar>("sourceJar") {
    from(sourceSets["main"].java)
    from(sourceSets["main"].resources)
    classifier = "sources"
}

tasks.create<Jar>("javadocJar") {
    dependsOn(":javadoc")
    from(tasks["javadoc"])
    classifier = "javadoc"
}

artifacts {
    archives(tasks["jar"])
    archives(tasks["sourceJar"])
    archives(tasks["javadocJar"])
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("CrossTitles")
                description.set(project.description)
                url.set("http://github.com/caseif/CrossTitles")
                packaging = "jar"
                inceptionYear.set("2015")

                scm {
                    url.set("https://github.com/caseif/CrossTitles")
                    connection.set("scm:git:git://github.com/caseif/CrossTitles.git")
                    developerConnection.set("scm:git:git@github.com:caseif/CrossTitles.git")
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set("http://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
            }
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.4"
}
