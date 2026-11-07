/*
 * Passive loss for one DAS / repeater downlink branch.
 *
 * Sanity check — 10 dB couplers, every cable length 0 (frequency does not matter):
 *
 *   Coupled-port loss = nominal coupling = 10 dB.
 *   Fraction coupled = 10^(-10/10) = 0.1
 *   Through loss = -10*log10(1 - 0.1) + 0.2 dB
 *                = -10*log10(0.9) + 0.2
 *                = 0.6575749056… dB
 *   Splitter leg = 3.01 + 0.4 = 3.41 dB
 *
 *   Antenna 1 = 10 dB
 *   Antenna 2 = 0.6575749056 + 10 = 10.6575749056 dB
 *   Antenna 3 = 0.6575749056 + 0.6575749056 + 3.41 = 4.7251498112 dB
 *   Antenna 4 = same as antenna 3
 *
 * Default drawing (defaultState): three couplers, 10 / 7 / 6 dB.
 *   Every trunk is 1 m of 1/2 in foam. Coupled runs and splitter legs are 30 m
 *   of LDF4RK-50A. At 2100 MHz that is 0.112 dB per trunk and 3.30 dB on each
 *   30 m coupled run or splitter leg.
 *
 * The end is a 2-way splitter by default: 3.01 + 0.4 dB on each of the last two
 * antennas. It can be a coupler instead. That coupler starts at 5 dB: the
 * coupled port feeds the first of those two antenna inputs, and the through
 * port feeds the second. Extra antennas come from more series couplers.
 * Three 10 dB couplers and zero-length cable:
 *   Antenna 3 = 2 * through(10) + 10
 *   Antennas 4 and 5 = 3 * through(10) + 3.41
 *
 * Source 1 starts at 2100 MHz. Source 2 starts off at 800 MHz; turn it on and both
 * carriers share the passive path. Cable attenuation is the table value at
 * 800, 900, 1800, 2100 and 2600 MHz. From 400 to 5000 MHz it follows a straight
 * line against log(frequency) between those points, extended with the end
 * segment outside them. Couplers and the splitter do not change with frequency.
 * Optimise searches 6, 7, 8, 10, 13, 15 and 20 dB and keeps the chosen band,
 * or the worse of the two bands, as flat as it can. With an end coupler it
 * also searches 5 dB and the same list for that coupler.
 */
(function (root, factory) {
  var api = factory();
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  } else {
    root.DasBranch = api;
  }
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  "use strict";

  // Typical published-style attenuation, dB per 100 m, rounded to 0.1.
  // 1/2 in superflex: jumper class similar to FSJ4-50B.
  // 1/2 in foam: feeder class similar to LDF4-50A.
  // LDF4RK-50A: Andrew HELIAX 1/2 in foam, non-halogen fire-retardant jacket.
  //   Published typical dB/100 m: 800 = 6.456, 1800 = 10.058, 2100 = 10.961.
  //   900 is the log-frequency point between 894 (6.855) and 960 (7.124): 6.880.
  //   2600 is the log-frequency point between 2500 (12.090) and 2700 (12.627): 12.364.
  //   Andrew states those attenuation values as typical and within 5%.
  // Other catalog magnitudes are not a specific manufacturer's guarantee.
  var CABLE_TYPES = {
    sf12: {
      id: "sf12",
      label: "1/2 in superflex",
      dbPer100m: { 800: 11.6, 900: 12.4, 1800: 18.3, 2100: 20.0, 2600: 22.6 }
    },
    foam12: {
      id: "foam12",
      label: "1/2 in foam",
      dbPer100m: { 800: 6.5, 900: 7.0, 1800: 10.2, 2100: 11.2, 2600: 12.6 }
    },
    ldf4rk: {
      id: "ldf4rk",
      label: "LDF4RK-50A",
      dbPer100m: { 800: 6.5, 900: 6.9, 1800: 10.1, 2100: 11.0, 2600: 12.4 }
    },
    // LMR-400 class: Times Microwave style k1*sqrt(f)+k2*f, dB/100 ft,
    // k1 = 0.122290, k2 = 0.000260, converted to dB/100 m and rounded to 0.1.
    lmr400: {
      id: "lmr400",
      label: "LMR-400",
      dbPer100m: { 800: 12.0, 900: 12.8, 1800: 18.6, 2100: 20.2, 2600: 22.7 }
    }
  };

  var FREQUENCIES = [800, 900, 1800, 2100, 2600];
  var FREQ_MIN_MHZ = 400;
  var FREQ_MAX_MHZ = 5000;
  var COUPLER_VALUES = [6, 7, 8, 10, 13, 15, 20];
  var END_COUPLER_VALUES = [5, 6, 7, 8, 10, 13, 15, 20];
  var END_COUPLER_DEFAULT_DB = 5;
  var COUPLER_COUNTS = [1, 2, 3, 4];
  var COUPLER_DISSIPATION_DB = 0.2;
  var SPLITTER_SPLIT_DB = 3.01;
  var SPLITTER_EXCESS_DB = 0.4;
  var SPLITTER_LEG_DB = SPLITTER_SPLIT_DB + SPLITTER_EXCESS_DB;

  function cableOrder(couplerCount) {
    var n = couplerCount || 2;
    if (COUPLER_COUNTS.indexOf(n) === -1) {
      throw new Error("Coupler count must be 1, 2, 3, or 4");
    }
    var ids = ["cu-dc1"];
    var i;
    for (i = 1; i <= n; i += 1) {
      ids.push("dc" + i + "-ant" + i);
      ids.push(i < n ? "dc" + i + "-dc" + (i + 1) : "dc" + i + "-spl");
    }
    ids.push("spl-ant" + (n + 1));
    ids.push("spl-ant" + (n + 2));
    return ids;
  }

  var CABLE_ORDER = cableOrder(2);

  function splitterLegDb() {
    return SPLITTER_LEG_DB;
  }

  function endSpec(state) {
    var mode = state && state.end === "coupler" ? "coupler" : "splitter";
    var coupling = Number(state && state.endCouplerDb);
    if (END_COUPLER_VALUES.indexOf(coupling) === -1) coupling = END_COUPLER_DEFAULT_DB;
    return {
      mode: mode,
      couplingDb: coupling,
      throughDb: throughLossDb(coupling)
    };
  }

  function throughLossDb(couplingDb) {
    if (!Number.isFinite(couplingDb) || couplingDb <= 0) {
      throw new Error("Coupling must be a positive number of dB");
    }
    var coupledFraction = Math.pow(10, -couplingDb / 10);
    var theoretical = -10 * Math.log10(1 - coupledFraction);
    return theoretical + COUPLER_DISSIPATION_DB;
  }

  function attenuationDbPer100m(typeId, freqMHz) {
    var type = CABLE_TYPES[typeId];
    if (!type) {
      throw new Error("Unknown cable type: " + typeId);
    }
    if (!Number.isFinite(freqMHz) || freqMHz < FREQ_MIN_MHZ || freqMHz > FREQ_MAX_MHZ) {
      throw new Error("Frequency must be from " + FREQ_MIN_MHZ + " to " + FREQ_MAX_MHZ + " MHz");
    }
    var knots = FREQUENCIES;
    var i;
    for (i = 0; i < knots.length; i += 1) {
      if (Math.abs(freqMHz - knots[i]) < 1e-9) {
        return type.dbPer100m[knots[i]];
      }
    }
    var low = knots[0];
    var high = knots[1];
    if (freqMHz > knots[knots.length - 1]) {
      low = knots[knots.length - 2];
      high = knots[knots.length - 1];
    } else if (freqMHz >= knots[0]) {
      for (i = 1; i < knots.length; i += 1) {
        if (freqMHz <= knots[i]) {
          low = knots[i - 1];
          high = knots[i];
          break;
        }
      }
    }
    var fraction = Math.log(freqMHz / low) / Math.log(high / low);
    return type.dbPer100m[low] + fraction * (type.dbPer100m[high] - type.dbPer100m[low]);
  }

  function cableLossDb(typeId, freqMHz, lengthM) {
    if (!Number.isFinite(lengthM) || lengthM < 0) {
      throw new Error("Length must be zero or positive");
    }
    return (attenuationDbPer100m(typeId, freqMHz) * lengthM) / 100;
  }

  function formatFreq(freqMHz) {
    if (!Number.isFinite(freqMHz)) {
      return "—";
    }
    if (Math.abs(freqMHz - Math.round(freqMHz)) < 1e-6) {
      return String(Math.round(freqMHz));
    }
    return (Math.round(freqMHz * 10) / 10).toFixed(1);
  }

  function sourcesFor(state) {
    var raw = state.sources && state.sources.length
      ? state.sources
      : [{ freqMHz: state.freqMHz, powerDbm: state.sourceDbm }];
    return raw.slice(0, 2).map(function (source, index) {
      var freq = Number(source.freqMHz);
      var power = Number(source.powerDbm);
      var enabled = index === 0 || source.enabled !== false;
      var valid = enabled && Number.isFinite(freq) && freq >= FREQ_MIN_MHZ && freq <= FREQ_MAX_MHZ && Number.isFinite(power);
      return { index: index + 1, freqMHz: freq, powerDbm: power, enabled: enabled, valid: valid };
    }).filter(function (source) {
      return source.enabled;
    });
  }

  function antennaLossesFrom(cableLoss, couplings, throughs, count, end) {
    var reached = cableLoss["cu-dc1"];
    var losses = [];
    var i;
    for (i = 1; i <= count; i += 1) {
      losses.push(reached + couplings[i - 1] + cableLoss["dc" + i + "-ant" + i]);
      var nextId = i < count ? "dc" + i + "-dc" + (i + 1) : "dc" + i + "-spl";
      reached += throughs[i - 1] + cableLoss[nextId];
    }
    var coupledOut = SPLITTER_LEG_DB;
    var throughOut = SPLITTER_LEG_DB;
    if (end && end.mode === "coupler") {
      coupledOut = end.couplingDb;
      throughOut = end.throughDb;
    }
    losses.push(reached + coupledOut + cableLoss["spl-ant" + (count + 1)]);
    losses.push(reached + throughOut + cableLoss["spl-ant" + (count + 2)]);
    return losses;
  }

  function roundDb(value) {
    if (!Number.isFinite(value)) {
      return NaN;
    }
    return Math.round(value * 10) / 10;
  }

  function formatDb(value) {
    var rounded = roundDb(value);
    if (!Number.isFinite(rounded)) {
      return "—";
    }
    return rounded.toFixed(1);
  }

  function formatSignedDb(value) {
    var rounded = roundDb(value);
    if (!Number.isFinite(rounded)) {
      return "—";
    }
    if (rounded > 0) {
      return "+" + rounded.toFixed(1);
    }
    if (rounded < 0) {
      return rounded.toFixed(1);
    }
    return "+0.0";
  }

  function formatPowerDbm(value) {
    var signed = formatSignedDb(value);
    return signed === "—" ? "— dBm" : signed + " dBm";
  }

  function formatDeltaDb(value) {
    var signed = formatSignedDb(value);
    return signed === "—" ? "— dB" : signed + " dB";
  }

  function branchOutput(result, sourceIndex) {
    var source = result.sources[sourceIndex];
    if (!source || !source.valid || !result.antennas || result.antennas.length < 1) {
      return null;
    }
    var sumMw = 0;
    var i;
    for (i = 0; i < result.antennas.length; i += 1) {
      var level = result.antennas[i].levels[sourceIndex];
      if (!level || !Number.isFinite(level.powerDbm)) {
        return null;
      }
      sumMw += Math.pow(10, level.powerDbm / 10);
    }
    if (!(sumMw > 0)) {
      return null;
    }
    var outputDbm = 10 * Math.log10(sumMw);
    return {
      outputDbm: outputDbm,
      lossDb: source.powerDbm - outputDbm
    };
  }

  var COUNT_WORDS = { 2: "two", 3: "three", 4: "four", 5: "five", 6: "six" };

  function describeAntennas(rows) {
    if (!rows || rows.length < 2) {
      throw new Error("Expected at least two antenna inputs");
    }
    var lossTenths = rows.map(function (row) {
      return Math.round(row.loss * 10);
    });
    var minT = Math.min.apply(null, lossTenths);
    var maxT = Math.max.apply(null, lossTenths);
    var spread = ((maxT - minT) / 10).toFixed(1);
    function powerText(power) {
      return formatPowerDbm(power);
    }
    if (minT === maxT) {
      var word = COUNT_WORDS[rows.length] || String(rows.length);
      return "All " + word + " antenna inputs are equal at " + powerText(rows[0].power) + " (" + (minT / 10).toFixed(1) + " dB). Spread 0.0 dB.";
    }
    var parts = rows.map(function (row, i) {
      var lossT = lossTenths[i];
      var rank = "";
      if (lossT === maxT) {
        rank = ", weakest";
      } else if (lossT === minT) {
        rank = ", strongest";
      }
      return "Antenna input " + (i + 1) + ": " + powerText(row.power) + " (" + (lossT / 10).toFixed(1) + " dB" + rank + ")";
    });
    return parts.join(". ") + ". Spread " + spread + " dB.";
  }

  function couplingsFor(state, count) {
    var list = [];
    var i;
    for (i = 0; i < count; i += 1) {
      var value = i === 0 ? 10 : i === 1 ? 7 : 6;
      if (state.couplerDb && state.couplerDb[i] != null && state.couplerDb[i] !== "") {
        value = Number(state.couplerDb[i]);
      }
      list.push(value);
    }
    if (state.coupler1Db != null) list[0] = Number(state.coupler1Db);
    if (count > 1 && state.coupler2Db != null) list[1] = Number(state.coupler2Db);
    return list;
  }

  function defaultState() {
    return {
      freqMHz: 2100,
      sourceDbm: 21,
      sources: [
        { freqMHz: 2100, powerDbm: 21 },
        { freqMHz: 800, powerDbm: 21, enabled: false }
      ],
      couplerCount: 3,
      couplerDb: [10, 7, 6, 6],
      end: "splitter",
      endCouplerDb: END_COUPLER_DEFAULT_DB,
      cables: {
        "cu-dc1": { lengthM: 1, type: "foam12" },
        "dc1-ant1": { lengthM: 30, type: "ldf4rk" },
        "dc1-dc2": { lengthM: 1, type: "foam12" },
        "dc2-ant2": { lengthM: 30, type: "ldf4rk" },
        "dc2-dc3": { lengthM: 1, type: "foam12" },
        "dc3-ant3": { lengthM: 30, type: "ldf4rk" },
        "dc3-spl": { lengthM: 1, type: "foam12" },
        "spl-ant4": { lengthM: 30, type: "ldf4rk" },
        "spl-ant5": { lengthM: 30, type: "ldf4rk" }
      }
    };
  }

  function computeBranch(state) {
    var count = state.couplerCount || 2;
    var order = cableOrder(count);
    var couplings = couplingsFor(state, count);
    var throughs = couplings.map(throughLossDb);
    var end = endSpec(state);
    var sources = sourcesFor(state);
    order.forEach(function (id) {
      if (!state.cables[id]) {
        throw new Error("Missing cable " + id);
      }
    });

    var solved = sources.map(function (source) {
      if (!source.valid) {
        return null;
      }
      var cableLoss = {};
      var cableRate = {};
      order.forEach(function (id) {
        var spec = state.cables[id];
        cableLoss[id] = cableLossDb(spec.type, source.freqMHz, spec.lengthM);
        cableRate[id] = attenuationDbPer100m(spec.type, source.freqMHz);
      });
      return {
        cableLoss: cableLoss,
        cableRate: cableRate,
        antennaLoss: antennaLossesFrom(cableLoss, couplings, throughs, count, end)
      };
    });

    var primary = 0;
    sources.forEach(function (source, index) {
      if (source.valid && Math.abs(source.freqMHz - Number(state.freqMHz)) < 1e-9) {
        primary = index;
      }
    });
    if (!solved[primary]) {
      solved.forEach(function (row, index) {
        if (row && !solved[primary]) primary = index;
      });
    }

    var antennas = [];
    var antennaCount = count + 2;
    var i;
    for (i = 0; i < antennaCount; i += 1) {
      var levels = sources.map(function (source, index) {
        var row = solved[index];
        var lossDb = row ? row.antennaLoss[i] : NaN;
        return {
          source: source.index,
          freqMHz: source.freqMHz,
          sourceDbm: source.powerDbm,
          lossDb: lossDb,
          powerDbm: row && Number.isFinite(source.powerDbm) ? source.powerDbm - lossDb : NaN
        };
      });
      var primaryLevel = levels[primary] || levels[0];
      antennas.push({
        index: i + 1,
        lossDb: primaryLevel.lossDb,
        powerDbm: primaryLevel.powerDbm,
        levels: levels
      });
    }

    return {
      freqMHz: state.freqMHz,
      sourceDbm: state.sourceDbm,
      sources: sources,
      couplerCount: count,
      couplers: couplings.map(function (coupling, index) {
        return { index: index + 1, couplingDb: coupling, throughDb: throughs[index] };
      }),
      end: end,
      splitterLegDb: SPLITTER_LEG_DB,
      cables: order.map(function (id) {
        var levels = sources.map(function (source, index) {
          var row = solved[index];
          return {
            source: source.index,
            freqMHz: source.freqMHz,
            lossDb: row ? row.cableLoss[id] : NaN,
            dbPer100m: row ? row.cableRate[id] : NaN
          };
        });
        var primaryCable = levels[primary] || levels[0];
        return {
          id: id,
          lossDb: primaryCable.lossDb,
          dbPer100m: primaryCable.dbPer100m,
          levels: levels
        };
      }),
      antennas: antennas
    };
  }

  function bandStats(result, index) {
    var source = result.sources[index];
    if (!source || !source.valid) return null;
    var powers = [];
    result.antennas.forEach(function (antenna) {
      var power = antenna.levels[index].powerDbm;
      if (Number.isFinite(power)) powers.push(power);
    });
    if (powers.length < 2) return null;
    var min = Math.min.apply(null, powers);
    var max = Math.max.apply(null, powers);
    return { spread: max - min, minPower: min };
  }

  function betterScore(candidate, current) {
    var i;
    for (i = 0; i < candidate.spreads.length; i += 1) {
      var delta = candidate.spreads[i] - current.spreads[i];
      if (Math.abs(delta) > 1e-9) return delta < 0;
    }
    return candidate.minPower > current.minPower + 1e-9;
  }

  function optimizeCouplers(state, mode) {
    var goal = mode || "all";
    if (goal !== "all" && goal !== "a" && goal !== "b") {
      throw new Error("Optimise for all, a, or b");
    }
    var count = state.couplerCount || 2;
    if (COUPLER_COUNTS.indexOf(count) === -1) {
      throw new Error("Coupler count must be 1, 2, 3, or 4");
    }
    var probe = computeBranch(state);
    if (goal === "all" && probe.sources.length < 2) goal = "a";
    if (goal === "a" && !bandStats(probe, 0)) return null;
    if (goal === "b" && !bandStats(probe, 1)) return null;
    if (goal === "all" && (!bandStats(probe, 0) || !bandStats(probe, 1))) return null;

    var end = endSpec(state);
    var best = null;
    var choice = [];
    var endChoice = end.couplingDb;
    function scoreOf(result) {
      var a = bandStats(result, 0);
      var b = result.sources.length > 1 ? bandStats(result, 1) : null;
      if (goal === "a") return { spreads: [a.spread], minPower: a.minPower, spreadA: a.spread, spreadB: b ? b.spread : null };
      if (goal === "b") return { spreads: [b.spread], minPower: b.minPower, spreadA: a ? a.spread : null, spreadB: b.spread };
      return {
        spreads: [Math.max(a.spread, b.spread), a.spread + b.spread],
        minPower: Math.min(a.minPower, b.minPower),
        spreadA: a.spread,
        spreadB: b.spread
      };
    }
    function consider() {
      var trial = {
        freqMHz: state.freqMHz,
        sourceDbm: state.sourceDbm,
        sources: state.sources,
        couplerCount: count,
        couplerDb: choice.slice(),
        end: end.mode,
        endCouplerDb: endChoice,
        cables: state.cables
      };
      var result = computeBranch(trial);
      var score = scoreOf(result);
      if (!best || betterScore(score, best)) {
        best = {
          couplerDb: choice.slice(),
          endCouplerDb: endChoice,
          spreadA: score.spreadA,
          spreadB: score.spreadB,
          spreads: score.spreads,
          minPower: score.minPower
        };
      }
    }
    function walk(index) {
      if (index === count) {
        if (end.mode === "coupler") {
          var vEnd;
          for (vEnd = 0; vEnd < END_COUPLER_VALUES.length; vEnd += 1) {
            endChoice = END_COUPLER_VALUES[vEnd];
            consider();
          }
        } else {
          endChoice = end.couplingDb;
          consider();
        }
        return;
      }
      var v;
      for (v = 0; v < COUPLER_VALUES.length; v += 1) {
        choice[index] = COUPLER_VALUES[v];
        walk(index + 1);
      }
    }
    walk(0);
    return {
      mode: goal,
      couplerDb: best.couplerDb,
      endCouplerDb: best.endCouplerDb,
      spreadA: best.spreadA,
      spreadB: best.spreadB
    };
  }

  return {
    CABLE_TYPES: CABLE_TYPES,
    FREQUENCIES: FREQUENCIES,
    FREQ_MIN_MHZ: FREQ_MIN_MHZ,
    FREQ_MAX_MHZ: FREQ_MAX_MHZ,
    COUPLER_VALUES: COUPLER_VALUES,
    END_COUPLER_VALUES: END_COUPLER_VALUES,
    END_COUPLER_DEFAULT_DB: END_COUPLER_DEFAULT_DB,
    COUPLER_COUNTS: COUPLER_COUNTS,
    CABLE_ORDER: CABLE_ORDER,
    COUPLER_DISSIPATION_DB: COUPLER_DISSIPATION_DB,
    SPLITTER_SPLIT_DB: SPLITTER_SPLIT_DB,
    SPLITTER_EXCESS_DB: SPLITTER_EXCESS_DB,
    SPLITTER_LEG_DB: SPLITTER_LEG_DB,
    cableOrder: cableOrder,
    splitterLegDb: splitterLegDb,
    endSpec: endSpec,
    throughLossDb: throughLossDb,
    attenuationDbPer100m: attenuationDbPer100m,
    cableLossDb: cableLossDb,
    formatFreq: formatFreq,
    roundDb: roundDb,
    formatDb: formatDb,
    formatSignedDb: formatSignedDb,
    formatPowerDbm: formatPowerDbm,
    formatDeltaDb: formatDeltaDb,
    branchOutput: branchOutput,
    describeAntennas: describeAntennas,
    defaultState: defaultState,
    computeBranch: computeBranch,
    optimizeCouplers: optimizeCouplers
  };
});
