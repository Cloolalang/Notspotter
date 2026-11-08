import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const m = require("./model.js");

function independentThrough(couplingDb) {
  return -10 * Math.log10(1 - 10 ** (-couplingDb / 10)) + 0.2;
}

for (const coupling of [6, 7, 8, 10, 13, 15, 20]) {
  assert.ok(
    Math.abs(m.throughLossDb(coupling) - independentThrough(coupling)) < 1e-12,
    "through loss " + coupling
  );
}

assert.ok(Math.abs(m.throughLossDb(10) - (-10 * Math.log10(0.9) + 0.2)) < 1e-12);

const throughs = [6, 7, 8, 10, 13, 15, 20].map((c) => m.throughLossDb(c));
for (let i = 1; i < throughs.length; i += 1) {
  assert.ok(throughs[i] < throughs[i - 1], "tighter coupling must raise through loss");
}

assert.ok(Math.abs(m.SPLITTER_LEG_DB - 3.41) < 1e-12);
assert.equal(m.formatDb(m.SPLITTER_LEG_DB), "3.4");
assert.equal(m.formatSignedDb(16), "+16.0");
assert.equal(m.formatSignedDb(-4.26), "-4.3");
assert.equal(m.formatSignedDb(0), "+0.0");
assert.equal(m.formatSignedDb(-0.04), "+0.0");
assert.equal(m.carrierCount(undefined), 1);
assert.equal(m.carrierCount(4.4), 4);
assert.ok(Math.abs(m.perCarrierPowerDbm(21, 1) - 21) < 1e-12);
assert.ok(Math.abs(m.perCarrierPowerDbm(21, 2) - (21 - 10 * Math.log10(2))) < 1e-12);
assert.equal(m.formatPowerDbm(16), "+16.0 dBm");
assert.equal(m.formatPowerDbm(-4.3), "-4.3 dBm");
assert.equal(m.formatDeltaDb(4.2), "+4.2 dB");
assert.equal(m.formatDeltaDb(0), "+0.0 dB");
assert.equal(m.formatDeltaDb(-1.26), "-1.3 dB");
assert.equal(m.formatDb(m.throughLossDb(10)), "0.7");
assert.equal(m.formatDb(m.throughLossDb(7)), "1.2");

const zero = m.defaultState();
zero.couplerCount = 2;
zero.coupler1Db = 10;
zero.coupler2Db = 10;
zero.cables = {};
for (const id of m.cableOrder(2)) zero.cables[id] = { lengthM: 0, type: "foam12" };
const z = m.computeBranch(zero);
const t10 = independentThrough(10);
assert.ok(Math.abs(z.antennas[0].lossDb - 10) < 1e-12, "zero ant1");
assert.ok(Math.abs(z.antennas[1].lossDb - (t10 + 10)) < 1e-12, "zero ant2");
assert.ok(Math.abs(z.antennas[2].lossDb - (t10 + t10 + 3.41)) < 1e-9, "zero ant3");
assert.ok(Math.abs(z.antennas[3].lossDb - z.antennas[2].lossDb) < 1e-12, "zero ant4");

assert.ok(Math.abs(m.cableLossDb("foam12", 2100, 10) - 1.12) < 1e-12);
assert.ok(Math.abs(m.cableLossDb("sf12", 2100, 2) - 0.4) < 1e-12);

const foam = 11.2;
const ldf4rk = 11.0;
const trunk = (foam / 100) * 1;
const coupled = (ldf4rk / 100) * 30;
const leg = (ldf4rk / 100) * 30;
const t7 = independentThrough(7);
const t6 = independentThrough(6);
const split = 3.01 + 0.4;
const expect1 = trunk + 10 + coupled;
const expect2 = trunk + t10 + trunk + 7 + coupled;
const expect3 = trunk + t10 + trunk + t7 + trunk + 6 + coupled;
const expectSplit = trunk + t10 + trunk + t7 + trunk + t6 + trunk + split + leg;

const drawn = m.computeBranch(m.defaultState());
assert.equal(drawn.couplerCount, 3);
assert.equal(drawn.sources.length, 1);
assert.equal(drawn.sources[0].freqMHz, 2100);
assert.equal(drawn.sources[0].powerDbm, 25);
assert.equal(drawn.sources[0].carriers, 8);
assert.equal(m.defaultState().sources[1].enabled, false);
assert.equal(m.defaultState().sources[1].freqMHz, 800);

const branch = m.branchOutput(drawn, 0);
const sumMw = drawn.antennas.reduce((sum, antenna) => sum + 10 ** (antenna.powerDbm / 10), 0);
assert.ok(Math.abs(branch.outputDbm - 10 * Math.log10(sumMw)) < 1e-9);
assert.ok(Math.abs(branch.lossDb - (25 - branch.outputDbm)) < 1e-9);
assert.ok(branch.lossDb > 0);
assert.ok(branch.lossDb < Math.min(...drawn.antennas.map((antenna) => antenna.lossDb)));
assert.equal(m.branchOutput({ sources: [{ valid: false }], antennas: [] }, 0), null);
assert.equal(drawn.antennas.length, 5);
assert.ok(Math.abs(drawn.antennas[0].lossDb - expect1) < 1e-9, "default ant1");
assert.ok(Math.abs(drawn.antennas[1].lossDb - expect2) < 1e-9, "default ant2");
assert.ok(Math.abs(drawn.antennas[2].lossDb - expect3) < 1e-9, "default ant3");
assert.ok(Math.abs(drawn.antennas[3].lossDb - expectSplit) < 1e-9, "default ant4");
assert.ok(Math.abs(drawn.antennas[4].lossDb - expectSplit) < 1e-9, "default ant5");
assert.equal(m.formatDb(drawn.antennas[0].lossDb), m.formatDb(expect1));
assert.equal(m.formatDb(drawn.antennas[1].lossDb), m.formatDb(expect2));
assert.equal(m.formatDb(drawn.antennas[2].lossDb), m.formatDb(expect3));
assert.equal(m.formatDb(drawn.antennas[3].lossDb), m.formatDb(expectSplit));
for (const antenna of drawn.antennas) {
  assert.ok(Math.abs(antenna.powerDbm - (25 - antenna.lossDb)) < 1e-12);
}

const summary = m.describeAntennas(drawn.antennas.map((a) => ({
  loss: m.roundDb(a.lossDb),
  power: m.roundDb(a.powerDbm)
})));
assert.equal(
  summary,
  "Antenna input 1: +11.6 dBm (13.4 dB, weakest). Antenna input 2: +13.8 dBm (11.2 dB). Antenna input 3: +13.5 dBm (11.5 dB). Antenna input 4: +14.6 dBm (10.4 dB, strongest). Antenna input 5: +14.6 dBm (10.4 dB, strongest). Spread 3.0 dB."
);
assert.equal(
  m.describeAntennas([
    { loss: 4, power: 6 },
    { loss: 4, power: 6 },
    { loss: 4, power: 6 },
    { loss: 4, power: 6 }
  ]),
  "All four antenna inputs are equal at +6.0 dBm (4.0 dB). Spread 0.0 dB."
);

assert.deepEqual(m.cableOrder(2), [
  "cu-dc1", "dc1-ant1", "dc1-dc2", "dc2-ant2", "dc2-spl", "spl-ant3", "spl-ant4"
]);
assert.equal(m.splitterLegDb(), m.SPLITTER_LEG_DB);

const three = m.defaultState();
three.couplerCount = 3;
three.couplerDb = [10, 10, 10, 6];
delete three.coupler1Db;
delete three.coupler2Db;
three.sourceDbm = 10;
for (const id of m.cableOrder(3)) three.cables[id] = { lengthM: 0, type: "foam12" };
const threeResult = m.computeBranch(three);
assert.equal(threeResult.antennas.length, 5);
assert.equal(threeResult.splitterLegDb, m.SPLITTER_LEG_DB);
assert.ok(Math.abs(threeResult.antennas[0].lossDb - 10) < 1e-12, "3c ant1");
assert.ok(Math.abs(threeResult.antennas[1].lossDb - (t10 + 10)) < 1e-12, "3c ant2");
assert.ok(Math.abs(threeResult.antennas[2].lossDb - (t10 + t10 + 10)) < 1e-12, "3c ant3");
assert.ok(Math.abs(threeResult.antennas[3].lossDb - (t10 * 3 + 3.41)) < 1e-9, "3c ant4");
assert.ok(Math.abs(threeResult.antennas[4].lossDb - threeResult.antennas[3].lossDb) < 1e-12, "3c ant5");

const extended = m.defaultState();
extended.couplerCount = 4;
extended.couplerDb = extended.couplerDb.slice();
extended.cables = { ...extended.cables };
for (const id of m.cableOrder(4)) {
  if (!extended.cables[id]) extended.cables[id] = { lengthM: 1, type: "foam12" };
}
const extendedResult = m.computeBranch(extended);
assert.ok(Math.abs(extendedResult.antennas[0].lossDb - drawn.antennas[0].lossDb) < 1e-12);
assert.ok(extendedResult.antennas[4].lossDb > drawn.antennas[3].lossDb);

const raised = m.defaultState();
raised.sourceDbm = 26;
raised.sources = raised.sources.map((source) => ({ ...source }));
raised.sources[0].powerDbm = 26;
const raisedResult = m.computeBranch(raised);
assert.ok(Math.abs(raisedResult.antennas[0].powerDbm - (drawn.antennas[0].powerDbm + 1)) < 1e-9);
assert.ok(Math.abs(raisedResult.antennas[0].lossDb - drawn.antennas[0].lossDb) < 1e-12);
assert.equal(raisedResult.sources.length, 1);

const low = m.defaultState();
low.freqMHz = 800;
low.sources = [{ freqMHz: 800, powerDbm: 10 }];
const high = m.defaultState();
high.freqMHz = 2600;
high.sources = [{ freqMHz: 2600, powerDbm: 10 }];
assert.ok(m.computeBranch(high).antennas[0].lossDb > m.computeBranch(low).antennas[0].lossDb);

assert.equal(m.attenuationDbPer100m("foam12", 2100), 11.2);
assert.equal(m.attenuationDbPer100m("sf12", 800), 11.6);
const between = m.attenuationDbPer100m("foam12", 1450);
assert.ok(between > 7 && between < 10.2, "interpolated foam at 1450");
assert.ok(m.attenuationDbPer100m("foam12", 400) < m.attenuationDbPer100m("foam12", 800));
assert.ok(m.attenuationDbPer100m("foam12", 5000) > m.attenuationDbPer100m("foam12", 2600));
let previousRate = 0;
for (const freq of [400, 650, 800, 850, 900, 1400, 1800, 2000, 2100, 2400, 2600, 3600, 5000]) {
  const rate = m.attenuationDbPer100m("sf12", freq);
  assert.ok(rate > previousRate, "rising attenuation at " + freq);
  previousRate = rate;
}
assert.throws(() => m.attenuationDbPer100m("foam12", 399), /400/);
assert.throws(() => m.attenuationDbPer100m("sf12", 5001), /5000/);

const both = m.defaultState();
both.sources = [
  { freqMHz: 800, powerDbm: 21, enabled: true },
  { freqMHz: 2100, powerDbm: 21, enabled: true }
];
const dual = m.computeBranch(both);
assert.equal(dual.sources.length, 2);
assert.equal(dual.antennas[0].levels[0].freqMHz, 800);
assert.equal(dual.antennas[0].levels[1].freqMHz, 2100);
assert.ok(dual.antennas[0].levels[0].lossDb < dual.antennas[0].levels[1].lossDb);
assert.ok(Math.abs(dual.antennas[0].levels[1].lossDb - expect1) < 1e-9);
assert.equal(m.formatDb(dual.antennas[0].levels[1].powerDbm), m.formatDb(21 - expect1));
assert.ok(Math.abs(dual.antennas[0].levels[0].powerDbm - (21 - dual.antennas[0].levels[0].lossDb)) < 1e-12);
const louder = m.defaultState();
louder.sources = both.sources.map((source) => ({ ...source }));
louder.sources[0].powerDbm = 27;
const louderResult = m.computeBranch(louder);
assert.ok(Math.abs(louderResult.antennas[2].levels[0].powerDbm - (dual.antennas[2].levels[0].powerDbm + 6)) < 1e-9);
assert.ok(Math.abs(louderResult.antennas[2].levels[1].powerDbm - dual.antennas[2].levels[1].powerDbm) < 1e-12);

const tight = m.defaultState();
tight.coupler1Db = 6;
const loose = m.defaultState();
loose.coupler1Db = 20;
const tightResult = m.computeBranch(tight);
const looseResult = m.computeBranch(loose);
assert.ok(tightResult.antennas[0].lossDb < looseResult.antennas[0].lossDb);
assert.ok(tightResult.antennas[2].lossDb > looseResult.antennas[2].lossDb);

const longLeg = m.defaultState();
longLeg.cables = { ...longLeg.cables, "spl-ant4": { ...longLeg.cables["spl-ant4"], lengthM: 60 } };
const longResult = m.computeBranch(longLeg);
assert.ok(longResult.antennas[3].lossDb > longResult.antennas[4].lossDb);

assert.equal(m.attenuationDbPer100m("lmr400", 900), 12.8);
assert.equal(m.attenuationDbPer100m("lmr400", 2100), 20.2);
assert.ok(m.attenuationDbPer100m("lmr400", 2100) > m.attenuationDbPer100m("foam12", 2100));
assert.ok(m.cableLossDb("lmr400", 2100, 30) > m.cableLossDb("foam12", 2100, 30));
assert.equal(m.CABLE_TYPES.ldf4rk.label, "LDF4RK-50A");
assert.equal(m.attenuationDbPer100m("ldf4rk", 800), 6.5);
assert.equal(m.attenuationDbPer100m("ldf4rk", 900), 6.9);
assert.equal(m.attenuationDbPer100m("ldf4rk", 1800), 10.1);
assert.equal(m.attenuationDbPer100m("ldf4rk", 2100), 11.0);
assert.equal(m.attenuationDbPer100m("ldf4rk", 2600), 12.4);
assert.ok(m.cableLossDb("ldf4rk", 2100, 30) < m.cableLossDb("foam12", 2100, 30));
const defaults = m.defaultState();
assert.equal(defaults.cables["dc1-ant1"].type, "ldf4rk");
assert.equal(defaults.cables["dc2-ant2"].type, "ldf4rk");
assert.equal(defaults.cables["dc3-ant3"].type, "ldf4rk");
assert.equal(defaults.cables["cu-dc1"].type, "foam12");
assert.equal(defaults.cables["spl-ant4"].type, "ldf4rk");
assert.equal(defaults.cables["spl-ant5"].type, "ldf4rk");

function spreadOf(result, index) {
  const powers = result.antennas.map((antenna) => antenna.levels[index].powerDbm);
  return Math.max(...powers) - Math.min(...powers);
}
function enableSource2(state) {
  state.sources = state.sources.map((source) => ({ ...source, enabled: true }));
  return state;
}
const uneven = enableSource2(m.defaultState());
uneven.cables = {
  ...uneven.cables,
  "dc1-ant1": { lengthM: 5, type: "foam12" },
  "dc2-ant2": { lengthM: 30, type: "foam12" },
  "dc3-ant3": { lengthM: 60, type: "foam12" }
};
const unevenBefore = m.computeBranch(uneven);
const forB = m.optimizeCouplers(uneven, "b");
const forA = m.optimizeCouplers(uneven, "a");
const forAll = m.optimizeCouplers(uneven, "all");
assert.equal(uneven.couplerDb.join(","), "10,7,6,6");
assert.equal(forB.couplerDb.length, 3);
for (const value of forB.couplerDb) assert.ok(m.COUPLER_VALUES.includes(value));
const tuned = m.computeBranch({ ...uneven, couplerDb: forB.couplerDb });
assert.ok(spreadOf(tuned, 1) <= spreadOf(unevenBefore, 1) + 1e-9);
assert.ok(Math.abs(spreadOf(tuned, 1) - forB.spreadB) < 1e-9);
const tunedA = m.computeBranch({ ...uneven, couplerDb: forA.couplerDb });
assert.ok(spreadOf(tunedA, 0) <= spreadOf(unevenBefore, 0) + 1e-9);
const shortHop = enableSource2(m.defaultState());
shortHop.cables = { ...shortHop.cables, "dc1-ant1": { lengthM: 5, type: "foam12" } };
const longHop = enableSource2(m.defaultState());
longHop.cables = { ...longHop.cables, "dc1-ant1": { lengthM: 80, type: "foam12" } };
assert.ok(m.optimizeCouplers(shortHop, "b").couplerDb[0] > m.optimizeCouplers(longHop, "b").couplerDb[0]);
const allTuned = m.computeBranch({ ...uneven, couplerDb: forAll.couplerDb });
assert.ok(Math.max(spreadOf(allTuned, 0), spreadOf(allTuned, 1)) <= Math.max(spreadOf(unevenBefore, 0), spreadOf(unevenBefore, 1)) + 1e-9);
assert.equal(m.optimizeCouplers({ ...uneven, sources: [{ freqMHz: 300, powerDbm: 10 }] }, "a"), null);

const source2Off = m.defaultState();
source2Off.sources = source2Off.sources.map((source) => ({ ...source }));
source2Off.sources[1].enabled = false;
const single = m.computeBranch(source2Off);
assert.equal(single.sources.length, 1);
assert.equal(single.sources[0].index, 1);
assert.equal(single.sources[0].freqMHz, 2100);
assert.equal(single.antennas[0].levels.length, 1);
assert.equal(single.cables[0].levels.length, 1);
assert.ok(Math.abs(single.antennas[0].lossDb - drawn.antennas[0].levels[0].lossDb) < 1e-9);
assert.ok(Math.abs(single.antennas[4].powerDbm - (25 - single.antennas[4].lossDb)) < 1e-9);
assert.equal(m.optimizeCouplers(source2Off, "b"), null);
assert.deepEqual(m.optimizeCouplers(source2Off, "all").couplerDb, m.optimizeCouplers(source2Off, "a").couplerDb);
const cannotDisableFirst = m.defaultState();
cannotDisableFirst.sources = cannotDisableFirst.sources.map((source) => ({ ...source, enabled: false }));
assert.equal(m.computeBranch(cannotDisableFirst).sources.length, 1);
assert.equal(m.computeBranch(cannotDisableFirst).sources[0].freqMHz, 2100);

assert.equal(m.defaultState().end, "splitter");
assert.equal(m.defaultState().endCouplerDb, 5);
assert.deepEqual(m.END_COUPLER_VALUES, [5, 6, 7, 8, 10, 13, 15, 20]);
assert.equal(m.endSpec({}).mode, "splitter");
assert.equal(m.endSpec({ end: "coupler", endCouplerDb: 4 }).couplingDb, 5);
assert.equal(m.formatDb(m.throughLossDb(5)), "1.9");
assert.ok(m.throughLossDb(5) > m.throughLossDb(6));
assert.equal(drawn.end.mode, "splitter");

const ended = m.defaultState();
ended.end = "coupler";
ended.endCouplerDb = 5;
const endedResult = m.computeBranch(ended);
const t5 = independentThrough(5);
assert.equal(endedResult.end.mode, "coupler");
assert.equal(endedResult.end.couplingDb, 5);
assert.ok(Math.abs(endedResult.antennas[0].lossDb - drawn.antennas[0].lossDb) < 1e-9);
assert.ok(Math.abs(endedResult.antennas[1].lossDb - drawn.antennas[1].lossDb) < 1e-9);
assert.ok(Math.abs(endedResult.antennas[2].lossDb - drawn.antennas[2].lossDb) < 1e-9);
assert.ok(Math.abs(endedResult.antennas[3].lossDb - (expectSplit - split + 5)) < 1e-9, "end coupled port");
assert.ok(Math.abs(endedResult.antennas[4].lossDb - (expectSplit - split + t5)) < 1e-9, "end through port");
assert.ok(endedResult.antennas[3].lossDb > endedResult.antennas[4].lossDb);

const zeroEnd = m.defaultState();
zeroEnd.couplerCount = 2;
zeroEnd.couplerDb = [10, 10];
zeroEnd.end = "coupler";
zeroEnd.endCouplerDb = 5;
zeroEnd.cables = {};
for (const id of m.cableOrder(2)) zeroEnd.cables[id] = { lengthM: 0, type: "foam12" };
const zeroEndResult = m.computeBranch(zeroEnd);
assert.ok(Math.abs(zeroEndResult.antennas[2].lossDb - (t10 + t10 + 5)) < 1e-9);
assert.ok(Math.abs(zeroEndResult.antennas[3].lossDb - (t10 + t10 + t5)) < 1e-9);

const unevenEnd = m.defaultState();
unevenEnd.end = "coupler";
unevenEnd.endCouplerDb = 5;
unevenEnd.cables = {
  ...unevenEnd.cables,
  "spl-ant4": { lengthM: 5, type: "ldf4rk" },
  "spl-ant5": { lengthM: 80, type: "ldf4rk" }
};
const beforeEnd = m.computeBranch(unevenEnd);
const tunedEnd = m.optimizeCouplers(unevenEnd, "a");
assert.ok(m.END_COUPLER_VALUES.includes(tunedEnd.endCouplerDb));
const afterEnd = m.computeBranch({
  ...unevenEnd,
  couplerDb: tunedEnd.couplerDb,
  end: "coupler",
  endCouplerDb: tunedEnd.endCouplerDb
});
assert.ok(spreadOf(afterEnd, 0) <= spreadOf(beforeEnd, 0) + 1e-9);
const splitterTune = m.optimizeCouplers(m.defaultState(), "a");
assert.equal(splitterTune.endCouplerDb, 5);
const fourCarriers = m.defaultState();
fourCarriers.sources = fourCarriers.sources.map((source) => ({ ...source }));
fourCarriers.sources[0].carriers = 4;
const divided = m.computeBranch(fourCarriers);
assert.equal(divided.sources[0].carriers, 4);
assert.ok(Math.abs(
  divided.antennas[2].levels[0].perCarrierDbm -
  (divided.antennas[2].levels[0].powerDbm - 10 * Math.log10(4))
) < 1e-9, "per carrier split");
assert.equal(m.RADIATED_OFFSET_DB, 50);
assert.ok(Math.abs(
  divided.antennas[2].levels[0].radiatedDbm -
  (divided.antennas[2].levels[0].perCarrierDbm - 50)
) < 1e-9, "radiated estimate");
assert.equal(m.LTE_15_RB, 75);
assert.equal(m.rsrp15OffsetDb(), 29.5);
assert.ok(Math.abs(
  divided.antennas[2].levels[0].rsrpDbm -
  (m.roundDb(divided.antennas[2].levels[0].radiatedDbm) - 29.5)
) < 1e-9, "15 MHz RSRP");
assert.equal(m.COVERAGE_REF_DBM, -27);
assert.equal(m.COVERAGE_REF_M, 20);
assert.ok(Math.abs(m.coverageRadiusM(-27) - 20) < 1e-9, "20 m at -27 dBm");
assert.ok(m.coverageRadiusM(-17) > m.coverageRadiusM(-27), "stronger input reaches further");
assert.ok(Math.abs(
  drawn.antennas[0].levels[0].coverageRadiusM -
  m.coverageRadiusM(drawn.antennas[0].levels[0].carrierRsrpDbm)
) < 1e-9, "antenna coverage follows its RSRP");

console.log("check-math ok");
console.log(drawn.antennas.map((a) => m.formatDb(a.lossDb)).join(" "));
console.log(summary);
