# Branch point-of-failure list

Default drawing: 3 directional couplers, one 2-way splitter, 5 antenna inputs, 9 cables, 18 mated N connections. Best-quality straight N connectors. The loss sheet leaves their 0.05 dB out; this list still counts every mate, because a loose, wet, or open N takes the path down.

One cable has two connection points, one at each end. A trunk or splitter-input failure darkens every antenna input after it. A coupled cable or one splitter leg darkens one antenna input.

Setting the sheet to 1, 2, or 4 couplers keeps the same pattern. Cables = 2 × couplers + 3. Connections = 2 × cables. Passives = the couplers plus the one splitter. Antenna inputs = couplers + 2. The splitter legs are always the last two antenna inputs.

## Cables

| No. | Sheet id | Role | From | To | Default |
| --- | --- | --- | --- | --- | --- |
| C1 | cu-dc1 | Trunk | Coverage unit, downlink | Coupler 1, input | 1 m, 1/2 in foam |
| C2 | dc1-ant1 | Coupled | Coupler 1, coupled | Antenna input 1 | 30 m, LDF4RK-50A |
| C3 | dc1-dc2 | Trunk | Coupler 1, through | Coupler 2, input | 1 m, 1/2 in foam |
| C4 | dc2-ant2 | Coupled | Coupler 2, coupled | Antenna input 2 | 30 m, LDF4RK-50A |
| C5 | dc2-dc3 | Trunk | Coupler 2, through | Coupler 3, input | 1 m, 1/2 in foam |
| C6 | dc3-ant3 | Coupled | Coupler 3, coupled | Antenna input 3 | 30 m, LDF4RK-50A |
| C7 | dc3-spl | Trunk | Coupler 3, through | Splitter, input | 1 m, 1/2 in foam |
| C8 | spl-ant4 | Splitter leg | Splitter, leg A | Antenna input 4 | 30 m, LDF4RK-50A |
| C9 | spl-ant5 | Splitter leg | Splitter, leg B | Antenna input 5 | 30 m, LDF4RK-50A |

## Passives

| No. | Device | Ports | Default |
| --- | --- | --- | --- |
| P1 | Directional coupler 1 | Input, through, coupled | 10 dB |
| P2 | Directional coupler 2 | Input, through, coupled | 7 dB |
| P3 | Directional coupler 3 | Input, through, coupled | 6 dB |
| P4 | 2-way splitter | Input, leg A, leg B | 3.4 dB per leg, estimate |

The coverage unit is the source port on C1. It is not a branch passive. Each antenna input is the far end of C2, C4, C6, C8, or C9, where the antenna connector mates. That power is the antenna input, not radiated power.

## Connection points

| No. | Mate | Cable | What goes dark |
| --- | --- | --- | --- |
| J1 | Coverage unit downlink ↔ C1 | C1 | All five antenna inputs |
| J2 | C1 ↔ coupler 1 input | C1 | All five antenna inputs |
| J3 | Coupler 1 coupled ↔ C2 | C2 | Antenna input 1 |
| J4 | C2 ↔ antenna input 1 | C2 | Antenna input 1 |
| J5 | Coupler 1 through ↔ C3 | C3 | Antenna inputs 2 to 5 |
| J6 | C3 ↔ coupler 2 input | C3 | Antenna inputs 2 to 5 |
| J7 | Coupler 2 coupled ↔ C4 | C4 | Antenna input 2 |
| J8 | C4 ↔ antenna input 2 | C4 | Antenna input 2 |
| J9 | Coupler 2 through ↔ C5 | C5 | Antenna inputs 3 to 5 |
| J10 | C5 ↔ coupler 3 input | C5 | Antenna inputs 3 to 5 |
| J11 | Coupler 3 coupled ↔ C6 | C6 | Antenna input 3 |
| J12 | C6 ↔ antenna input 3 | C6 | Antenna input 3 |
| J13 | Coupler 3 through ↔ C7 | C7 | Antenna inputs 4 and 5 |
| J14 | C7 ↔ splitter input | C7 | Antenna inputs 4 and 5 |
| J15 | Splitter leg A ↔ C8 | C8 | Antenna input 4 |
| J16 | C8 ↔ antenna input 4 | C8 | Antenna input 4 |
| J17 | Splitter leg B ↔ C9 | C9 | Antenna input 5 |
| J18 | C9 ↔ antenna input 5 | C9 | Antenna input 5 |

Eighteen mates. J1 through J2 are the only mates that can take the whole branch. Each later trunk adds a pair that takes everything downstream and leaves the earlier antenna inputs up.

## What fails

**Open, unmated, or broken cable.** The antenna inputs in the “what goes dark” column drop out. Earlier taps stay up. An open on the main line also raises the match seen by the coverage unit.

**Loose, corroded, or wet N.** Same reach as an open on that mate, often first as extra loss, PIM, or a level that moves when the joint is touched. J1, J2, J5, J6, J9, J10, J13, and J14 sit on the main line, so one bad trunk joint moves every antenna input after it. J3–J4, J7–J8, J11–J12, J15–J16, and J17–J18 each move one antenna input.

**Coupler body (P1, P2, P3).** A dead input or an internal main-line open matches the trunk into that coupler: P1 takes all five, P2 takes antenna inputs 2 to 5, P3 takes antenna inputs 3 to 5. A dead coupled port matches that coupler’s own coupled cable: one antenna input.

**Coupler reversed or the wrong value.** The branch stays connected. The tapped antenna input and everything downstream move off the design levels. The loss sheet is what shows it. Default values are P1 10 dB, P2 7 dB, P3 6 dB.

**Splitter (P4).** A dead input matches J14: antenna inputs 4 and 5 go dark, and 1 to 3 stay up. One dead leg matches that leg’s cable. An open leg also changes the match on the other leg, so the surviving splitter antenna input moves as well.

**Cable damage on a 30 m run (C2, C4, C6, C8, C9).** One antenna input. The same damage on a 1 m trunk (C1, C3, C5, C7) follows that trunk’s connection points and takes every antenna input downstream.

## End coupler option

The drawing can end in a coupler instead of P4, the 2-way splitter. That coupler defaults to 5 dB. On the default 3-coupler branch, C8 leaves its coupled port for antenna input 4, and C9 leaves its through port for antenna input 5. J14 is the coupler input and takes both of those antenna inputs. J15 and J16 are the coupled port and take antenna input 4. J17 and J18 are the through port and take antenna input 5.
