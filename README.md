
For plugin use, **requires** the Caddamands plugin

````
    maven {
        ...
        url = uri("https://maven.pkg.github.com/kangawooo-events/customitemsapi")
        credentials {
            // credentials for github user token, required even thoug this package is public
            // place these in (ideally the global) gradle.properites:
            //   github.user=<your github username ex// CaddenToo>
            //   github.token=<your token>
            username = project.findProperty("github.user") as String ?: System.getenv("github.user")
            password = project.findProperty("github.token") as String ?: System.getenv("github.token")
        }
        ...
    }

    ...

    dependencies {
        ...
        compileOnly("cd.arnett:customitemsapi:<Package-Version>")
        ...
    }
````
