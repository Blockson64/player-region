# PlayerRegion

A lightweight Paper utility plugin that gives other plugins cheap, approximate player lookups by location, without scanning every online player.

## Overview

Plugins subscribe to a grid resolution and query nearby players at three levels of precision: a fast cell-based lookup, a bounding box, or an exact radius. All plugins share the same data, which is refreshed once per second on the main thread.

## Usage

Add the dependency to your plugin's `paper-plugin.yml` (the name must match this plugin's `name:`):

```yaml
dependencies:
  server:
    PlayerRegion:
      load: BEFORE
      required: true
      join-classpath: true
```

Compile against the API only (never shade it into your jar), then subscribe once and keep the handle:

```java
PlayerRegionApi api = getServer().getServicesManager().load(PlayerRegionApi.class);
PlayerRegionSubscription players = api.subscribe(this, 128);

if (players.anyNear(world, x, z)) {
    Player target = players.getNearest(world, x, z, 64);
}

// onDisable
players.close();
```

## Building the API documentation

The public API lives in the `api` package, and its Javadocs describe every method. 
