const fs = require("node:fs");

const scenarios = {
    noChange: "no-change",
    introduceRisk: "risk-introduction",
    growing: "growing",
    unstable: "unstable",
}
const featureSet = {
    full: "auctioning",
    knowledgeSharing: "knowledge-sharing",
    local: "local",
}

function main() {
   Object.values(scenarios).forEach(scenario => {
       writeToTexFile("overall-damage", scenario, createDamageTotal(scenario));
       writeToTexFile("messages", scenario, createMessageTotal(scenario));
       writeToTexFile("proposals", scenario, createProposalTotal(scenario));
       writeToTexFile("risk-count", scenario, createRiskCountTotal(scenario));
       writeToTexFile("risk-remaining", scenario, createRemainingRisks(scenario));
       writeToTexFile("auctioning-time", scenario, createTimeSpentAuctioning(scenario));
       writeToTexFile("adapting-time", scenario, createTimeSpentAdapting(scenario));
   });
//    writeToTexFile("multi-run", "no-change", createMultiRun());
    // writeToTexFile("small-infra", "no-change", createSmallInfra());
    //  writeToTexFile("large-infra", "large", createLargeInfra());
}

main();

function createDamageTotal(scenario) {
    const metric = 'riskDamage-global';
    return createForMetric(scenario, metric)(`Total infrastructure damage`, "Damage Total");
}
function createMessageTotal(scenario) {
    const metric = 'messages-total';
    return createForMetric(scenario, metric, true)(`Total messages sent`, "Messages");
}
function createProposalTotal(scenario) {
    const metric = 'proposals-total';
    return createForMetric(scenario, metric, true)(`Total number of adaptations applied`, "Adaptations");
}
function createRiskCountTotal(scenario) {
    const metric = 'riskUnique-total';
    return createForMetric(scenario, metric, true)(`Total number of unique risks found`, "Unique Risks");
}
function createTimeSpentAuctioning(scenario) {
    const metric = 'auctioning-time-global';
    return createForMetric(scenario, metric, true)(`Time spent auctioning`, "\\shortstack{Cumulative \\\\ Auctioning Time [ms]}");
}
function createTimeSpentAdapting(scenario) {
    const metric = 'migrating-time-global';
    return createForMetric(scenario, metric, true)(`Time spent adapting`, "\\shortstack{Cumulative \\\\ Adapting Time [ms]}");
}
function createRemainingRisks(scenario) {
    const metric = 'riskCount-global';
    return createForMetric(scenario, metric, true)(`Remaining Risks`, "Risks");
}
function createMultiRun() {
    const raw = [1,2,3,4,5].map(i => loadCSV(`./output/multi-run/${i}/metrics.csv`));
    const metric = 'riskDamage-global'
    const limits = getLimits(
        raw[0].timestamps, 
        [].concat(raw[0][metric], raw[1][metric], raw[2][metric], raw[3][metric], raw[4][metric]));
    const average = raw[0][metric].map((_, index) => {
        const values = raw.map(r => parseFloat(r[metric][index]));
        return values.reduce((acc, v) => acc + v, 0) / values.length;
    });
    const events = getEvents(scenarios.noChange);
    const data = [
        {
            label: "Run 1",
            data: zipTimestamps(raw[0].timestamps, raw[0][metric]),
        },
        {
            label: "Run 2",
            data: zipTimestamps(raw[1].timestamps, raw[1][metric]),
        },
        {
            label: "Run 3",
            data: zipTimestamps(raw[2].timestamps, raw[2][metric]),
        },
        {
            label: "Run 4",
            data: zipTimestamps(raw[3].timestamps, raw[3][metric]),
        },
        {
            label: "Run 5",
            data: zipTimestamps(raw[4].timestamps, raw[4][metric]),
        },
        {
            label: "Average",
            data: zipTimestamps(raw[0].timestamps, average),
        }
    ];
    return pgfPlotTemplate("Total infrastructure damage", "Damage Total", data, limits, events, false, (index) => index === 5 ? "red" : "gray");
}
function createSmallInfra() {
    const metric = 'riskDamage-global';
    return createForMetric("small", metric, true)("Damage Total", "Damage");
}

function createLargeInfra() {
    const metric = 'riskDamage-global';
    return createForMetric("large", metric, true)("Damage Total", "Damage");
}

function createForMetric(scenario, metric, noLegend = false) {
    const full = loadCSV(`./output/${scenario}/${featureSet.full}/metrics.csv`);
    const knowledgeSharing = loadCSV(`./output/${scenario}/${featureSet.knowledgeSharing}/metrics.csv`);
    const local = loadCSV(`./output/${scenario}/${featureSet.local}/metrics.csv`);

    const limits = getLimits(
        full.timestamps, 
        [].concat(full[metric], knowledgeSharing[metric], local[metric]));
    const events = getEvents(scenario);
    const data = [
        {
            label: "Auctioning",
            data: zipTimestamps(full.timestamps, full[metric]),
        },
        {
            label: "Knowledge Sharing",
            data: zipTimestamps(knowledgeSharing.timestamps, knowledgeSharing[metric]),
        },
        {
            label: "Local",
            data: zipTimestamps(local.timestamps, local[metric]),
        },
    ]
    return (title, ylabel, colors) => pgfPlotTemplate(title, ylabel, data, limits, events, false, colors);
}

function loadCSV(path) {
    const data = fs.readFileSync(path, "utf8");
    const lines = data.split("\n");

    return lines.map(line => line.split(";"))
        .reduce((acc, line) => Object.assign(acc, { [line[0]]: line.splice(1) }), {});
}

function getLimits(timestamps, data) {
    const _data = data.map(d => parseFloat(d));
    return {
        timestamp: {
            min: Math.min(...timestamps),
            max: Math.max(...timestamps),
        },
        data: {
            min: Math.min(..._data),
            max: Math.max(..._data),
        }
    }
}
function zipTimestamps(timestamps, data) {
    return timestamps.map((timestamp, index) => [timestamp, data[index]]);
}

function pgfPlotTemplate(title, yLabel, data, limits, events, noLegend, colors = color) {
    const xmax = Math.round(limits.timestamp.max / 5000) * 5000 + 5000;
    const ymax = (() => {
        const len = limits.data.max.toString().length;
        const factor = Math.max(Math.pow(10, len - 2), 1);
        const _ = Math.max(Math.ceil(limits.data.max / factor) * factor, factor);
        return _ + (_/factor);
    })();
    return `\\begin{tikzpicture}
\\begin{axis}[${true ? '' : `\ntitle={${title}}, `}
    xlabel={Timestamp [s]},
    ylabel={${yLabel}},
    xmin=0, xmax=${xmax},
    ymin=0, ymax=${ymax},
    legend columns=-1,
    legend style={at={(0.5,-0.1)},anchor=north},
    ymajorgrids=true,
    grid style=dashed,
    width=\\textwidth,
    height=0.5\\textwidth,
    scaled x ticks=base 10:-3,
    xtick scale label code/.code={}
]

${data.map((d, index) => createPlot(d, index, colors)).join("\n")}

${events.map(e => `\t\\addplot[color=gray, dashed,] coordinates {(${e},0) (${e},${ymax})};`).join("\n")}
${noLegend ? '\\legend{}' : ''}

\\end{axis}
\\end{tikzpicture}`;
}

function createPlot(data, index, colors) {
    return `\t\\addplot[color=${colors(index)},mark=${mark(index)}] coordinates {
        ${data.data.map(([t, d]) => d !== undefined ? `(${t},${d.toString().replace(",",".")})` : "").join("")}
    };
    \\addlegendentry{${data.label}}`
}

function color(index) {
    const colors = ["blue", "red", "orange", "black", "green", "brown"];
    return colors[index];
}
function mark(index) {
    const marks = ["square", "triangle", "diamond", "pentagon", "star", "oplus"];
    return marks[index];
}

function writeToTexFile(name, scenario, plot) {
    fs.writeFileSync(`./packages/thesis/src/graphs/${name}-${scenario}.tex`, plot);
}

function getEvents(scenario) {
    switch(scenario) {
        case scenarios.noChange:
            return [];
        case scenarios.introduceRisk:
            return [120000];
        case scenarios.growing:
            return [30000, 60000, 90000, 120000, 150000];
        case scenarios.unstable:
            return [30000, 60000, 90000, 120000];
        default: return [];
    }
}
