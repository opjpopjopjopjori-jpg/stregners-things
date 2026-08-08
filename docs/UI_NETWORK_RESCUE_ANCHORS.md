# UI, Network, Rescue, and Anchor Contract

## Network authority

The client may request fixed commands, bounded conversation topics, a fixed duo pair, a fixed power-policy role cycle/reset, and a fixed role ability. It cannot provide arbitrary world positions, NBT, inventory, target UUIDs, policy values, heart-budget values, social dialogue text, spawn data, or block actions. The server validates every request, chooses the next allowed policy itself, and sends owner-only status snapshots. Protocol `32` adds bounded Social Director status plus cave/village/travel display-action vocabulary while retaining display-only Will control and fixed Guardian Brace/Scout Signal vocabularies; no client view becomes authoritative.

## UI

- `G`: Command Wheel
- `J`: Team Journal
- `R`: Quick Safe Recall
- `H`: Diagnostics tab
- `F8`: Toggle the opt-in developer overlay
- Team Journal `Guide`: optional tutorial progress, Vanilla policy, safe controls, and accessibility reminders
- Right-click a nearby companion: bounded conversation menu
- Command Wheel / Journal: Stop All Actions, Clear Plan, Call Team Home, and bounded Recall controls

## Rescue

A rescue requires a downed companion, team ownership, range, a safe area, and a reservation. The player remains the decisive rescuer. Gifted Rescue can only reposition a valid emergency target to safe ground.

## Anchors

Anchors are player-authored data, not blocks or automatic building detection. Base Life never opens chests, sleeps, harvests, uses Redstone, or changes player blocks. HOME, ENTRY, REST, GUARD_POST, JOURNAL, TEAM_SUPPLY, LOOKOUT, QUIET, and MEMORIAL anchors are supported as safe locations.

A non-selected companion may enter a bounded RESTING snapshot only at a validated HOME or REST anchor without an active plan or nearby hostiles. Restore requires a safe base in the original dimension; the snapshot is not remote inventory storage.
