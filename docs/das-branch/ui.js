(function () {
  "use strict";

  var api = window.DasBranch;
  var form = document.getElementById("branch");
  var cableMemory = {};
  var couplerMemory = {};

  var DIPOLE = '<svg class="dipole" viewBox="0 0 48 28" aria-hidden="true"><line x1="24" y1="26" x2="24" y2="14" stroke="currentColor" stroke-width="2" stroke-linecap="round"></line><line x1="24" y1="14" x2="8" y2="3" stroke="currentColor" stroke-width="2" stroke-linecap="round"></line><line x1="24" y1="14" x2="40" y2="3" stroke="currentColor" stroke-width="2" stroke-linecap="round"></line></svg>';

  function cableOptions(selected) {
    return api.CABLE_TYPES
      ? Object.keys(api.CABLE_TYPES).map(function (id) {
          var type = api.CABLE_TYPES[id];
          return '<option value="' + id + '"' + (id === selected ? " selected" : "") + ">" + type.label + "</option>";
        }).join("")
      : "";
  }

  var COUPLER_GLYPH = '<svg class="glyph" viewBox="0 0 44 28" aria-hidden="true"><line x1="2" y1="8" x2="42" y2="8" stroke="currentColor" stroke-width="2"></line><circle cx="22" cy="8" r="2.4" fill="currentColor"></circle><line x1="22" y1="8" x2="22" y2="26" stroke="#0f5f5a" stroke-width="2"></line></svg>';

  function cleanLength(value, fallback) {
    var lengthValue = String(value == null ? "" : value).replace(/[^0-9.]/g, "");
    return lengthValue || fallback;
  }

  function rememberAll() {
    document.querySelectorAll(".cable").forEach(function (cable) {
      var input = cable.querySelector('input[type="number"]');
      var select = cable.querySelector("select");
      if (!input || !select || input.id.indexOf("len-") !== 0) return;
      cableMemory[input.id.slice(4)] = { lengthM: input.value, type: select.value };
    });
    document.querySelectorAll('select[id^="coup-"]').forEach(function (select) {
      couplerMemory[Number(select.id.slice(5))] = select.value;
    });
  }

  function bindCable(cable, id, srLength, srType, srLoss) {
    var labels = cable.querySelectorAll("label.field");
    var len = cable.querySelector('input[type="number"]');
    var type = cable.querySelector("select");
    var rate = cable.querySelector("[data-seg-rate]");
    var loss = cable.querySelector("[data-seg-loss]");
    var seg = cable.querySelector(".seg-loss");
    len.id = "len-" + id;
    type.id = "type-" + id;
    labels[0].setAttribute("for", len.id);
    labels[0].querySelector(".sr-only").textContent = srLength;
    labels[1].setAttribute("for", type.id);
    labels[1].querySelector(".sr-only").textContent = srType;
    rate.setAttribute("data-seg-rate", id);
    loss.setAttribute("data-seg-loss", id);
    seg.id = "segloss-" + id;
    len.setAttribute("aria-describedby", seg.id);
    seg.querySelector(".sr-only").textContent = srLoss;
  }

  function assignAntenna(card, index) {
    var key = index == null ? "park-" + card.closest("[data-coupler]").dataset.coupler : String(index);
    card.setAttribute("data-ant-card", key);
    var chart = card.querySelector("[data-level-chart]");
    if (chart) chart.setAttribute("data-level-chart", key);
    if (index == null) return;
    card.querySelector(".ant-name").textContent = "Antenna input " + index;
  }

  function assignLeg(leg, index) {
    var id = "spl-ant" + index;
    bindCable(
      leg.querySelector(".cable"),
      id,
      "from the splitter to antenna input " + index,
      "from the splitter to antenna input " + index,
      "Segment loss, splitter to antenna input " + index + ": "
    );
    assignAntenna(leg.querySelector(".antenna"), index);
  }

  function defaultCoupling(index) {
    if (index === 1) return "10";
    if (index === 2) return "7";
    return "6";
  }

  function couplerSelect(selected) {
    var chosen = String(selected);
    return api.COUPLER_VALUES.map(function (value) {
      var text = String(value);
      return '<option value="' + text + '"' + (text === chosen ? " selected" : "") + ">" + text + " dB</option>";
    }).join("");
  }

  function buildTrunk(id, fromName, toName, lengthM, typeId) {
    var lengthValue = cleanLength(lengthM, "1");
    var type = api.CABLE_TYPES[typeId] ? typeId : "foam12";
    var col = document.createElement("div");
    col.className = "col col-link";
    col.innerHTML =
      '<div class="h-slot"><div class="cable">' +
        '<p class="role">Trunk</p>' +
        '<label class="field" for="len-' + id + '">Length <span class="sr-only">from ' + fromName + " to " + toName + "</span>" +
          '<span class="with-unit"><input id="len-' + id + '" type="number" min="0" step="0.1" value="' + lengthValue + '" inputmode="decimal" autocomplete="off"><span class="unit">m</span></span>' +
        "</label>" +
        '<label class="field" for="type-' + id + '">Type <span class="sr-only">from ' + fromName + " to " + toName + "</span>" +
          '<select id="type-' + id + '">' + cableOptions(type) + "</select>" +
        "</label>" +
        '<div class="cable-meta">' +
          '<p class="rate"><span data-seg-rate="' + id + '">0.0</span> dB/100 m</p>' +
          '<p class="seg-loss" id="segloss-' + id + '"><span class="sr-only">Segment loss, ' + fromName + " to " + toName + ': </span><span data-seg-loss="' + id + '">0.0 dB</span></p>' +
        "</div>" +
      "</div></div>";
    return col;
  }

  function buildCoupler(index, coupling, lengthM, typeId) {
    var id = "dc" + index + "-ant" + index;
    var lengthValue = cleanLength(lengthM, "30");
    var type = api.CABLE_TYPES[typeId] ? typeId : "ldf4rk";
    var chosen = api.COUPLER_VALUES.indexOf(Number(coupling)) === -1 ? defaultCoupling(index) : String(coupling);
    var col = document.createElement("div");
    col.className = "col col-coup";
    col.dataset.coupler = String(index);
    col.dataset.generated = "1";
    col.innerHTML =
      '<article class="device">' + COUPLER_GLYPH +
        '<p class="device-kicker">Directional</p>' +
        '<p class="device-name">Coupler ' + index + "</p>" +
        '<label class="field" for="coup-' + index + '">Coupling <span class="sr-only">for directional coupler ' + index + "</span>" +
          '<select id="coup-' + index + '">' + couplerSelect(chosen) + "</select>" +
        "</label>" +
        '<p class="through foot">Through <strong id="thru-' + index + '">0.0</strong> dB <span class="est">estimate</span></p>' +
      "</article>" +
      '<div class="drop" data-kind="coupled"><div class="stem" aria-hidden="true"></div><div class="cable">' +
        '<p class="role">Coupled</p>' +
        '<label class="field" for="len-' + id + '">Length <span class="sr-only">from coupler ' + index + " coupled port to antenna input " + index + "</span>" +
          '<span class="with-unit"><input id="len-' + id + '" type="number" min="0" step="0.1" value="' + lengthValue + '" inputmode="decimal" autocomplete="off"><span class="unit">m</span></span>' +
        "</label>" +
        '<label class="field" for="type-' + id + '">Type <span class="sr-only">from coupler ' + index + " coupled port to antenna input " + index + "</span>" +
          '<select id="type-' + id + '">' + cableOptions(type) + "</select>" +
        "</label>" +
        '<div class="cable-meta">' +
          '<p class="rate"><span data-seg-rate="' + id + '">0.0</span> dB/100 m</p>' +
          '<p class="seg-loss" id="segloss-' + id + '"><span class="sr-only">Segment loss, coupler ' + index + " to antenna input " + index + ': </span><span data-seg-loss="' + id + '">0.0 dB</span></p>' +
        "</div>" +
      "</div>" +
      '<article class="antenna" data-ant-card="' + index + '">' + DIPOLE +
        '<p class="ant-name">Antenna input ' + index + "</p>" +
        '<div class="level-chart" data-level-chart="' + index + '"></div>' +
      "</article></div>";
    return col;
  }

  function endMode() {
    var pick = form.querySelector('input[name="end"]:checked');
    return pick && pick.value === "coupler" ? "coupler" : "splitter";
  }

  function captionFor(count) {
    var last = " The through path ends in a 2-way splitter for antenna inputs " + (count + 1) + " and " + (count + 2) + ".";
    if (endMode() === "coupler") {
      last = " The through path ends in a coupler: antenna input " + (count + 1) + " on the coupled port and antenna input " + (count + 2) + " on the through port.";
    }
    if (count === 2 && endMode() !== "coupler") {
      return "Downlink, left to right. Coupled ports drop to antenna inputs 1 and 2." + last;
    }
    var tap = count === 1
      ? "Coupler 1 drops to antenna input 1."
      : "Couplers 1 to " + count + " each drop to one antenna input.";
    return "Downlink, left to right. " + tap + last;
  }

  function setCableStory(cable, story, lossStory) {
    if (!cable) return;
    var notes = cable.querySelectorAll("label.field .sr-only");
    if (notes[0]) notes[0].textContent = story;
    if (notes[1]) notes[1].textContent = story;
    var loss = cable.querySelector(".seg-loss .sr-only");
    if (loss) loss.textContent = lossStory;
  }

  function syncEnd(count) {
    var coupler = endMode() === "coupler";
    var col = document.querySelector(".col-spl");
    col.classList.toggle("is-coupler", coupler);
    document.getElementById("end-glyph-spl").hidden = coupler;
    document.getElementById("end-glyph-coup").hidden = !coupler;
    document.getElementById("end-kicker").textContent = coupler ? "Directional" : "Passive";
    document.getElementById("spl-name").textContent = coupler ? "End coupler" : "2-way splitter";
    document.getElementById("end-coupler-field").hidden = !coupler;
    document.getElementById("end-loss-prefix").textContent = coupler ? "Through " : "";
    document.getElementById("end-loss-suffix").textContent = coupler ? "dB" : "dB / leg";
    var roles = document.querySelectorAll(".legs > .leg .role");
    if (roles[0]) roles[0].textContent = coupler ? "Coupled" : "Leg";
    if (roles[1]) roles[1].textContent = coupler ? "Through" : "Leg";
    var endName = coupler ? "the end coupler" : "the splitter";
    var toSpl = document.querySelector('[data-trunk="to-spl"]');
    setCableStory(
      toSpl.querySelector(".cable"),
      "from coupler " + count + " through port to " + endName,
      "Segment loss, coupler " + count + " to " + endName + ": "
    );
    var legs = document.querySelectorAll(".legs > .leg");
    var outputs = coupler
      ? ["coupled port", "through port"]
      : ["leg", "leg"];
    [0, 1].forEach(function (slot) {
      var antenna = count + 1 + slot;
      setCableStory(
        legs[slot].querySelector(".cable"),
        "from " + endName + " " + outputs[slot] + " to antenna input " + antenna,
        "Segment loss, " + endName + " to antenna input " + antenna + ": "
      );
    });
    document.getElementById("caption").textContent = captionFor(count);
  }

  function syncCouplers(count, skipRemember) {
    if (!skipRemember) rememberAll();
    var toSpl = document.querySelector('[data-trunk="to-spl"]');
    [1, 2].forEach(function (index) {
      var col = document.querySelector('.col-coup[data-coupler="' + index + '"]');
      var show = index <= count;
      col.hidden = !show;
      assignAntenna(col.querySelector(".antenna"), show ? index : null);
    });
    document.querySelector('[data-trunk="1-2"]').hidden = count < 2;
    document.querySelectorAll("[data-generated]").forEach(function (node) {
      node.remove();
    });
    var index;
    for (index = 3; index <= count; index += 1) {
      var betweenId = "dc" + (index - 1) + "-dc" + index;
      var between = cableMemory[betweenId] || { lengthM: "1", type: "foam12" };
      var trunk = buildTrunk(
        betweenId,
        "coupler " + (index - 1) + " through port",
        "coupler " + index,
        between.lengthM,
        between.type
      );
      trunk.dataset.trunk = (index - 1) + "-" + index;
      trunk.dataset.generated = "1";
      toSpl.parentNode.insertBefore(trunk, toSpl);
      var jumperId = "dc" + index + "-ant" + index;
      var jumper = cableMemory[jumperId] || { lengthM: "30", type: "ldf4rk" };
      toSpl.parentNode.insertBefore(
        buildCoupler(index, couplerMemory[index] || defaultCoupling(index), jumper.lengthM, jumper.type),
        toSpl
      );
    }
    bindCable(
      toSpl.querySelector(".cable"),
      "dc" + count + "-spl",
      "from coupler " + count + " through port to the splitter",
      "from coupler " + count + " through port to the splitter",
      "Segment loss, coupler " + count + " to the splitter: "
    );
    var legs = document.querySelectorAll(".legs > .leg");
    assignLeg(legs[0], 90);
    assignLeg(legs[1], 91);
    assignLeg(legs[0], count + 1);
    assignLeg(legs[1], count + 2);
    syncEnd(count);
    document.querySelector(".col-spl").dataset.couplers = String(count);
  }

  function applyState(state) {
    var sources = state.sources && state.sources.length
      ? state.sources
      : [
          { freqMHz: state.freqMHz, powerDbm: state.sourceDbm },
          { freqMHz: 800, powerDbm: state.sourceDbm, enabled: false }
        ];
    [0, 1].forEach(function (index) {
      var source = sources[index] || { freqMHz: index === 0 ? 2100 : 800, powerDbm: index === 0 ? 25 : 21, carriers: index === 0 ? 8 : 1, enabled: index === 0 };
      document.getElementById("src-" + (index + 1) + "-dbm").value = String(source.powerDbm);
      document.getElementById("src-" + (index + 1) + "-mhz").value = String(source.freqMHz);
      document.getElementById("src-" + (index + 1) + "-carriers").value = String(api.carrierCount(source.carriers));
    });
    document.getElementById("src-2-on").checked = !sources[1] || sources[1].enabled !== false;
    var count = state.couplerCount || 2;
    var couplerPick = form.querySelector('input[name="couplers"][value="' + count + '"]');
    if (!couplerPick) throw new Error("Missing coupler count " + count);
    couplerPick.checked = true;
    var end = state.end === "coupler" ? "coupler" : "splitter";
    var endPick = form.querySelector('input[name="end"][value="' + end + '"]');
    if (!endPick) throw new Error("Missing end device " + end);
    endPick.checked = true;
    var endDb = Number(state.endCouplerDb);
    if (api.END_COUPLER_VALUES.indexOf(endDb) === -1) endDb = api.END_COUPLER_DEFAULT_DB;
    document.getElementById("end-coupler").value = String(endDb);
    cableMemory = {};
    couplerMemory = {};
    (state.couplerDb || []).forEach(function (value, index) {
      couplerMemory[index + 1] = value;
    });
    Object.keys(state.cables).forEach(function (id) {
      cableMemory[id] = { lengthM: state.cables[id].lengthM, type: state.cables[id].type };
    });
    syncCouplers(count, true);
    for (var index = 1; index <= count; index += 1) {
      var coupling = state.couplerDb && state.couplerDb[index - 1] != null
        ? state.couplerDb[index - 1]
        : defaultCoupling(index);
      document.getElementById("coup-" + index).value = String(coupling);
    }
    api.cableOrder(count).forEach(function (id) {
      var spec = state.cables[id];
      if (!spec) return;
      document.getElementById("len-" + id).value = String(spec.lengthM);
      var type = document.getElementById("type-" + id);
      type.value = spec.type;
      if (type.value !== spec.type) throw new Error("Missing cable type " + spec.type);
    });
  }

  function syncSource2() {
    var on = document.getElementById("src-2-on").checked;
    document.getElementById("source-2").classList.toggle("is-off", !on);
    document.getElementById("src-2-dbm").disabled = !on;
    document.getElementById("src-2-mhz").disabled = !on;
    document.getElementById("src-2-carriers").disabled = !on;
    document.getElementById("legend-src-2").classList.toggle("is-off", !on);
    var bandB = form.querySelector('input[name="optimise"][value="b"]');
    bandB.disabled = !on;
    if (!on && bandB.checked) {
      form.querySelector('input[name="optimise"][value="a"]').checked = true;
    }
  }

  function readSources() {
    var source2On = document.getElementById("src-2-on").checked;
    return [1, 2].map(function (index) {
      var powerEl = document.getElementById("src-" + index + "-dbm");
      var freqEl = document.getElementById("src-" + index + "-mhz");
      var carrierEl = document.getElementById("src-" + index + "-carriers");
      var power = powerEl.valueAsNumber;
      var freq = freqEl.valueAsNumber;
      var enabled = index === 1 || source2On;
      var powerOk = !enabled || Number.isFinite(power);
      var freqOk = !enabled || (Number.isFinite(freq) && freq >= api.FREQ_MIN_MHZ && freq <= api.FREQ_MAX_MHZ);
      powerEl.setAttribute("aria-invalid", powerOk ? "false" : "true");
      freqEl.setAttribute("aria-invalid", freqOk ? "false" : "true");
      return { freqMHz: freq, powerDbm: power, carriers: api.carrierCount(carrierEl.value), enabled: enabled };
    });
  }

  function readState() {
    var sources = readSources();
    var active = sources.filter(function (source) { return source.enabled !== false; });
    var lead = active[0] || sources[0];
    var count = Number(form.querySelector('input[name="couplers"]:checked').value);
    var cables = {};
    var couplerDb = [];
    var index;
    for (index = 1; index <= count; index += 1) {
      couplerDb.push(Number(document.getElementById("coup-" + index).value));
    }
    api.cableOrder(count).forEach(function (id) {
      var lenEl = document.getElementById("len-" + id);
      var raw = lenEl.valueAsNumber;
      var valid = Number.isFinite(raw) && raw >= 0;
      lenEl.setAttribute("aria-invalid", valid ? "false" : "true");
      cables[id] = {
        lengthM: valid ? raw : 0,
        type: document.getElementById("type-" + id).value
      };
    });
    return {
      freqMHz: lead.freqMHz,
      sourceDbm: lead.powerDbm,
      sources: sources,
      couplerCount: count,
      couplerDb: couplerDb,
      end: endMode(),
      endCouplerDb: Number(document.getElementById("end-coupler").value),
      cables: cables
    };
  }

  function levelScale(powers) {
    var finite = powers.filter(function (value) { return Number.isFinite(value); });
    if (!finite.length) return { floor: 0, span: 1 };
    var min = Math.min.apply(null, finite);
    var max = Math.max.apply(null, finite);
    var span = Math.max(10, (max - min) * 1.25);
    return { floor: max - span, span: span };
  }

  function fillLevelChart(chart, levels, scale, labelPrefix) {
    chart.replaceChildren();
    chart.setAttribute("role", "img");
    var bits = [];
    levels.forEach(function (level) {
      var row = document.createElement("div");
      row.className = "level-row";
      var freqOk = Number.isFinite(level.freqMHz) && level.freqMHz >= api.FREQ_MIN_MHZ && level.freqMHz <= api.FREQ_MAX_MHZ;
      var freq = document.createElement("span");
      freq.className = "level-freq src-" + level.source;
      freq.textContent = freqOk ? api.formatFreq(level.freqMHz) : "—";
      var track = document.createElement("span");
      track.className = "level-track";
      track.setAttribute("aria-hidden", "true");
      var fill = document.createElement("span");
      fill.className = "level-fill src-" + level.source;
      var width = 0;
      if (Number.isFinite(level.powerDbm) && scale.span > 0) {
        width = ((level.powerDbm - scale.floor) / scale.span) * 100;
      }
      fill.style.width = Math.max(0, Math.min(100, width)).toFixed(2) + "%";
      track.appendChild(fill);
      var power = document.createElement("span");
      power.className = "level-dbm";
      power.textContent = Number.isFinite(level.powerDbm) ? api.formatPowerDbm(level.powerDbm) : "— dBm";
      row.appendChild(freq);
      row.appendChild(track);
      row.appendChild(power);
      chart.appendChild(row);
      bits.push((freqOk ? api.formatFreq(level.freqMHz) + " MHz, " : "frequency unset, ") + (Number.isFinite(level.powerDbm) ? api.formatPowerDbm(level.powerDbm) : "unset"));
    });
    if (levels.length >= 2 && Number.isFinite(levels[0].powerDbm) && Number.isFinite(levels[1].powerDbm)) {
      var delta = api.roundDb(levels[0].powerDbm - levels[1].powerDbm);
      var line = document.createElement("p");
      line.className = "level-delta";
      line.textContent = "Δ " + api.formatDeltaDb(delta);
      var magnitude = api.formatDb(Math.abs(delta));
      var relation = delta === 0 ? "equal to" : delta > 0 ? "above" : "below";
      bits.push("source 1 is " + magnitude + " dB " + relation + " source 2");
      chart.appendChild(line);
    }
    chart.setAttribute("aria-label", labelPrefix + bits.join(". "));
  }

  function fillCarrierBox(card, antenna) {
    var box = card.querySelector(".carrier-box");
    if (!box) {
      box = document.createElement("aside");
      box.className = "carrier-box";
      card.appendChild(box);
    }
    box.replaceChildren();
    var note = document.createElement("p");
    note.className = "carrier-note";
    note.textContent = "For DAS/Repeater the total power is divided by the number of carriers.";
    box.appendChild(note);
    antenna.levels.forEach(function (level) {
      var line = document.createElement("p");
      line.className = "carrier-power src-" + level.source;
      var value = Number.isFinite(level.perCarrierDbm) ? api.formatPowerDbm(level.perCarrierDbm) : "— dBm";
      var prefix = antenna.levels.length > 1 && Number.isFinite(level.freqMHz)
        ? api.formatFreq(level.freqMHz) + " MHz. "
        : "";
      line.textContent = prefix + "Per carrier power = " + value;
      box.appendChild(line);
    });
  }

  function fillRadiatedBox(card, antenna) {
    var box = card.nextElementSibling;
    if (!box || !box.classList.contains("radiated-box")) {
      box = document.createElement("aside");
      box.className = "radiated-box";
      card.insertAdjacentElement("afterend", box);
    }
    box.replaceChildren();
    var title = document.createElement("p");
    title.className = "radiated-title";
    title.textContent = "Estimated radiated power at 1.5m below antenna";
    box.appendChild(title);
    antenna.levels.forEach(function (level) {
      var line = document.createElement("p");
      line.className = "carrier-power src-" + level.source;
      var value = Number.isFinite(level.radiatedDbm) ? api.formatPowerDbm(level.radiatedDbm) : "— dBm";
      var prefix = antenna.levels.length > 1 && Number.isFinite(level.freqMHz)
        ? api.formatFreq(level.freqMHz) + " MHz. "
        : "";
      line.textContent = prefix + "Per carrier power = " + value;
      box.appendChild(line);
    });
  }

  function fillRsrpBox(card, antenna) {
    var radiated = card.nextElementSibling;
    if (!radiated || !radiated.classList.contains("radiated-box")) return;
    var box = radiated.nextElementSibling;
    if (!box || !box.classList.contains("rsrp-box")) {
      box = document.createElement("aside");
      box.className = "rsrp-box";
      radiated.insertAdjacentElement("afterend", box);
    }
    box.replaceChildren();
    var title = document.createElement("p");
    title.className = "rsrp-title";
    title.textContent = "Estimated carrier RSRP at 15 MHz BW";
    box.appendChild(title);
    antenna.levels.forEach(function (level) {
      var line = document.createElement("p");
      line.className = "carrier-power src-" + level.source;
      var value = Number.isFinite(level.rsrpDbm) ? api.formatPowerDbm(level.rsrpDbm) : "— dBm";
      var prefix = antenna.levels.length > 1 && Number.isFinite(level.freqMHz)
        ? api.formatFreq(level.freqMHz) + " MHz. "
        : "";
      line.textContent = prefix + "RSRP = " + value;
      box.appendChild(line);
    });
  }

  function renderBars(result, scale) {
    var list = document.querySelector(".bars");
    list.replaceChildren();
    result.antennas.forEach(function (antenna) {
      var item = document.createElement("li");
      item.className = "bar-block";
      var name = document.createElement("span");
      name.className = "bar-name";
      name.textContent = "Antenna input " + antenna.index;
      var chart = document.createElement("div");
      chart.className = "level-chart";
      fillLevelChart(chart, antenna.levels, scale, "Antenna input " + antenna.index + ". ");
      item.appendChild(name);
      item.appendChild(chart);
      list.appendChild(item);
    });
  }

  function renderSpread(result) {
    var box = document.getElementById("spread");
    box.replaceChildren();
    result.sources.forEach(function (source, index) {
      var block = document.createElement("p");
      var strong = document.createElement("strong");
      if (!source.valid) {
        strong.textContent = "—";
      } else {
        var losses = result.antennas.map(function (antenna) {
          return api.roundDb(antenna.levels[index].lossDb);
        });
        var minF = Math.min.apply(null, losses);
        var maxF = Math.max.apply(null, losses);
        strong.textContent = api.formatDb((Math.round(maxF * 10) - Math.round(minF * 10)) / 10) + " dB";
      }
      var caption = document.createElement("span");
      caption.textContent = source.valid ? api.formatFreq(source.freqMHz) + " MHz" : "Source " + source.index;
      block.appendChild(strong);
      block.appendChild(caption);
      box.appendChild(block);
    });
  }

  function renderSummary(result) {
    var problems = result.sources.filter(function (source) {
      return !source.valid;
    }).map(function (source) {
      return "Source " + source.index + " needs a power and a frequency from 400 to 5000 MHz.";
    });
    if (problems.length) return problems.join(" ");
    return "Antenna input power spread between antennas";
  }

  function render(result) {
    result.couplers.forEach(function (coupler) {
      var thru = document.getElementById("thru-" + coupler.index);
      if (thru) thru.textContent = api.formatDb(coupler.throughDb);
    });
    var endLoss = result.end && result.end.mode === "coupler" ? result.end.throughDb : result.splitterLegDb;
    document.getElementById("spl-loss").textContent = api.formatDb(endLoss);
    var readout = document.getElementById("source-readout");
    readout.replaceChildren();
    result.sources.forEach(function (source) {
      var line = document.createElement("span");
      line.className = "port-line src-" + source.index;
      var freq = source.valid ? api.formatFreq(source.freqMHz) : "—";
      var power = Number.isFinite(source.powerDbm) ? api.formatPowerDbm(source.powerDbm) : "— dBm";
      line.textContent = freq + " MHz · " + power;
      readout.appendChild(line);
    });

    result.cables.forEach(function (cable) {
      var lossEl = document.querySelector('[data-seg-loss="' + cable.id + '"]');
      var rateEl = document.querySelector('[data-seg-rate="' + cable.id + '"]');
      lossEl.textContent = cable.levels.map(function (level) { return api.formatDb(level.lossDb) + " dB"; }).join(" / ");
      rateEl.textContent = cable.levels.map(function (level) {
        return Number.isFinite(level.dbPer100m) ? level.dbPer100m.toFixed(1) : "—";
      }).join(" / ");
      var sr = lossEl.parentElement.querySelector(".sr-only");
      if (sr) {
        sr.textContent = cable.levels.map(function (level) {
          return "Source " + level.source + ", " + api.formatFreq(level.freqMHz) + " MHz, " + api.formatDb(level.lossDb) + " dB. ";
        }).join("");
      }
    });

    var powers = [];
    result.antennas.forEach(function (antenna) {
      antenna.levels.forEach(function (level) { powers.push(level.powerDbm); });
    });
    var scale = levelScale(powers);
    result.antennas.forEach(function (antenna) {
      var card = document.querySelector('[data-ant-card="' + antenna.index + '"]');
      if (!card) return;
      delete card.dataset.role;
      var chart = card.querySelector(".level-chart");
      if (!chart) {
        chart = document.createElement("div");
        chart.className = "level-chart";
        chart.dataset.levelChart = String(antenna.index);
        card.appendChild(chart);
      }
      fillLevelChart(chart, antenna.levels, scale, "Antenna input " + antenna.index + ". ");
      fillCarrierBox(card, antenna);
      fillRadiatedBox(card, antenna);
      fillRsrpBox(card, antenna);
    });

    renderBars(result, scale);
    renderSpread(result);
    renderBranchTotal(result);
    document.getElementById("summary").textContent = renderSummary(result);
  }

  function renderBranchTotal(result) {
    var lines = document.getElementById("branch-lines");
    lines.replaceChildren();
    result.sources.forEach(function (source, index) {
      var line = document.createElement("p");
      line.className = "branch-line src-" + source.index;
      if (!source.valid) {
        line.textContent = "Source " + source.index + " needs a power and a frequency from 400 to 5000 MHz.";
        lines.appendChild(line);
        return;
      }
      var total = api.branchOutput(result, index);
      var freq = api.formatFreq(source.freqMHz) + " MHz";
      if (!total) {
        line.textContent = freq + " output is unset.";
        lines.appendChild(line);
        return;
      }
      var outputRounded = api.roundDb(total.outputDbm);
      var sourceRounded = api.roundDb(source.powerDbm);
      var lossRounded = api.roundDb(sourceRounded - outputRounded);
      var strong = document.createElement("strong");
      strong.textContent = api.formatPowerDbm(outputRounded);
      line.appendChild(document.createTextNode(freq + " output "));
      line.appendChild(strong);
      line.appendChild(document.createTextNode(". Branch loss " + api.formatDb(lossRounded) + " dB from " + api.formatPowerDbm(sourceRounded) + "."));
      lines.appendChild(line);
    });
  }

  function recalc() {
    syncSource2();
    var count = Number(form.querySelector('input[name="couplers"]:checked').value);
    if (document.querySelector(".col-spl").dataset.couplers !== String(count)) syncCouplers(count);
    syncEnd(count);
    render(api.computeBranch(readState()));
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();
  });
  function onEdit(event) {
    if (!event.target || event.target.name !== "optimise") {
      document.getElementById("optimise-note").textContent = "";
    }
    recalc();
  }
  form.addEventListener("input", onEdit);
  form.addEventListener("change", onEdit);
  document.getElementById("optimise").addEventListener("click", function () {
    var mode = form.querySelector('input[name="optimise"]:checked').value;
    var best = api.optimizeCouplers(readState(), mode);
    var note = document.getElementById("optimise-note");
    if (!best) {
      note.textContent = mode === "b" && !document.getElementById("src-2-on").checked
        ? "Turn source 2 on before optimising band B."
        : "Set the frequencies you want, from 400 to 5000 MHz, before optimising.";
      return;
    }
    best.couplerDb.forEach(function (value, index) {
      document.getElementById("coup-" + (index + 1)).value = String(value);
    });
    if (endMode() === "coupler" && api.END_COUPLER_VALUES.indexOf(best.endCouplerDb) !== -1) {
      document.getElementById("end-coupler").value = String(best.endCouplerDb);
    }
    recalc();
    var names = { all: "all bands", a: "band A", b: "band B" };
    var label = mode === "all" && !document.getElementById("src-2-on").checked
      ? "band A, because source 2 is off"
      : names[mode];
    var text = "Optimised for " + label + ". Couplers " + best.couplerDb.join(", ") + " dB.";
    if (endMode() === "coupler") text += " End coupler " + best.endCouplerDb + " dB.";
    if (Number.isFinite(best.spreadA)) text += " Band A spread " + api.formatDb(best.spreadA) + " dB.";
    if (Number.isFinite(best.spreadB)) text += " Band B spread " + api.formatDb(best.spreadB) + " dB.";
    note.textContent = text;
  });
  form.addEventListener("wheel", function (event) {
    if (event.target && event.target.type === "number") event.target.blur();
  }, { passive: true });
  document.getElementById("reset").addEventListener("click", function () {
    document.getElementById("optimise-note").textContent = "";
    applyState(api.defaultState());
    recalc();
  });

  applyState(api.defaultState());
  recalc();
})();
