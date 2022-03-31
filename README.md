# Docker hub demo

### Configuring credentials.

First you need to update the [settings.xml](https://dmp.fabric8.io/#authentication) from maven.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<servers>
    <server>
        <id>docker.io</id>
        <username>jolokia</username>
        <password>s!cr!t</password>
    </server>
    ....
</servers>
```

### Generate image and push to docker registry.

```
mvnn clean install
```
