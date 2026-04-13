# Game Design Notes

## Working Title

Space RTS inspired by games like Cell Wars and Landgrabbers.

## Core Vision

A competitive, mobile-first, real-time strategy game set in space. Players control bases that generate ships over time, send fleets between bases, and fight for map control through constant pressure, route control, and efficient reinforcement.

The design goal is simple touch input with strong strategic depth.

## Core Match Loop

- Players own bases that spawn ships over time.
- Bases hold a limited number of ships.
- Players tap a source base and then a target base to send ships.
- Sending launches 50% of the ships currently in the source base.
- Neutral bases can be captured to expand.
- Enemy bases can be attacked and captured.
- Matches are intended to feel competitive, active, and pressure-focused rather than slow and buildup-heavy.

## Controls

- Mobile-first interaction.
- Tap source base, then tap target base.
- No manual path drawing.
- Movement uses automatic pathing.

## Space And Movement

- The game takes place in open space rather than a fixed node-link network.
- Bases are placed on the map and fleets travel physically through space.
- Obstacles such as debris or asteroids can block or shape movement routes.
- Fleets automatically path around obstacles.
- Route choice and travel time are an important part of strategy.

## Fleet Combat

- If opposing fleets pass close enough to each other, they automatically engage and deal damage.
- This means combat happens both in transit and at bases.
- Reinforcement timing and lane control are intended to matter a lot.
- Later, some base types may improve how well nearby fleets fight or intercept passing ships.

## Base Capture

- Bases defend themselves with their current garrison.
- A fleet that reaches a neutral or enemy base fights the defenders.
- Capture is intended to use simple, readable force subtraction logic.

## Match Pacing

- The target feel is constant flowing pressure with frequent partial sends.
- The game should reward strong routing, target priority, and reinforcement timing.
- Multi-front pressure should be a core skill.

## Base Capacity And Match Economy

- Every base has a maximum ship cap.
- During a match, players can spend money to increase a base's cap.
- In-match upgrades are only for cap growth, not for changing the base's role.
- Spending on cap should create a tradeoff between immediate pressure and longer-term strength.

## Base Types

- The player's starting base type is chosen before the match.
- Other base types are determined by the map and are gained by capturing those structures.
- Base bonuses depend on where ships are sent from and where they are sent to.
- Bonuses do not permanently stay on ships after arrival.
- Once ships arrive at a base, they become normal garrison there and take on the next base's bonus only when relaunched.

## Base Type Philosophy

Base roles are intended to be conditional and readable, not universally stronger in every situation.

Examples discussed so far:

- Regular
- Fast
- Reinforcement-focused
- Attack-focused
- Bigger range
- More damage to passing ships

Examples of how role logic should work:

- A reinforcement-type base is better when sending to a friendly base.
- An attack-type base is better when sending to an enemy or neutral base.
- An interception-type base is better at damaging passing enemy fleets.

This creates a network-routing strategy where it can be useful to move ships through one friendly base and then launch them onward from another specialized base.

## Strategic Identity

The game is built around three interacting layers:

- Flow: frequent 50% fleet sends and constant pressure.
- Space: movement through physical terrain with obstacles and interception.
- Infrastructure: specialized bases plus cap investment.

## Balance Direction

Current direction is balance-first.

Working assumptions:

- Starter base choice should shape the opening, not decide the whole match.
- Map control and captured structures should matter more over time.
- Each base type should have a clear job and a clear weakness.
- Bonuses should be conditional and narrow enough to keep the game readable and fair.

## Meta Progression

- Outside a match, upgrades can unlock different types of units.
- In-match upgrades are separate and only affect max cap.
- Long-term progression should expand options rather than become raw stat-based pay-to-win power.

## Prototype Priorities

The first prototype should prove the core loop before adding too many layered systems.

Current prototype-level priorities:

- Base ownership and ship generation
- 50% sending
- Auto-pathing around obstacles
- Fleet-vs-fleet interception in space
- Base capture by readable combat math
- Base caps
- Cap upgrades using money
- A small, readable set of base types

## Open Questions

- Exact combat math for fleet-vs-fleet combat
- Exact money income model
- Exact base type catalog for the first playable version
- Which unit types exist in the first prototype
- How visual language should distinguish base roles and fleet roles on mobile
