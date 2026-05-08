
For plugin use, **requires** the Caddamands plugin

````
    maven {
        ...
        url = uri("https://maven.pkg.github.com/kangawooo-events/customitemsapi")
        ...
    }

    ...

    dependencies {
        ...
        compileOnly("cd.arnett:customitemsapi:<Package-Version>")
        ...
    }
````
