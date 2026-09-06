# LagBudget

A [BentoBox](https://github.com/BentoBoxWorld/BentoBox) addon that gives every island a **lag budget** and quietly drops spawns on islands that exceed it.

## Why

Limits started life as a way to stop players lagging a server with thousands of hoppers. Over time it became a general block and entity counting tool that admins also use for progression, and the two jobs pull in different directions. LagBudget goes back to the original problem and does only that, for admins.

## What it does

- Gives every island a **weighted score** of the things on it that cost server time every tick: ticking block entities (hoppers, spawners, furnaces, beacons and so on) and expensive entities (villagers, minecarts, dense mob farms).
- Samples only **loaded chunks**, because only loaded chunks cause lag, and reads the block entities and entities the server already holds in memory. No block scan, no database, nothing persisted.
- When an island is **over budget**, new spawns there are silently dropped (natural spawns, spawner spawns and breeding by default). Nothing is blocked and players get no message. Optionally, hopper transfers can be dropped too.
- The admin can see what is going on with `/<admin> lagbudget top` and `/<admin> lagbudget info <player>`, and the console logs when an island crosses the budget line.
- Placeholders exist but are **off by default**: the moment players can see this number, some will try to max it out.

## Commands

| Command | Description |
|---|---|
| `/<admin> lagbudget` | Islands ranked by lag score, with their top contributors (same as `top`) |
| `/<admin> lagbudget top [count]` | The same ranking, limited to `count` islands |
| `/<admin> lagbudget info <player>` | Full score breakdown for a player's island |
| `/<admin> lagbudget reload` | Reload the config |

Permission: `[gamemode].lagbudget.admin` (default op).

## Weights

The weights in `config.yml` are relative units, so only the ratios matter. The defaults reflect the usual Spark profile suspects. If you have a Spark profile of your own server, tune them to match: the `TileEntity tick` and `Entity tick` sections give you the per-type cost directly.

## Requirements

- Paper 1.21.11 or later
- BentoBox 3.17.0 or later
- Java 21

## Building

```
mvn clean package
```

## License

Eclipse Public License 2.0. See `LICENSE`.
