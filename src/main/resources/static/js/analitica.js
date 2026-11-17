// Dashboard de analítica: construye los gráficos Chart.js a partir de window.metricas.
(function () {
    "use strict";

    if (typeof metricas === "undefined" || typeof Chart === "undefined") {
        return;
    }

    var paletteBrown = "#235347";
    var paletteTeal = "#8EB69B";
    var paletteRed = "#A24A3B";
    var paletteBlue = "#E3B04B";

    // Tendencia de solicitudes por día (gráfico de línea)
    var tendencia = metricas.tendenciaDias || {};
    new Chart(document.getElementById("chartTendencia"), {
        type: "line",
        data: {
            labels: Object.keys(tendencia),
            datasets: [{
                label: "Solicitudes",
                data: Object.values(tendencia),
                borderColor: paletteBrown,
                backgroundColor: "rgba(35,83,71,0.15)",
                tension: 0.35,
                fill: true,
                pointRadius: 3
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
        }
    });

    // Resultado de evaluación (gráfico de dona)
    var resultado = metricas.conteoResultado || {};
    new Chart(document.getElementById("chartResultado"), {
        type: "doughnut",
        data: {
            labels: Object.keys(resultado),
            datasets: [{
                data: Object.values(resultado),
                backgroundColor: [paletteTeal, paletteRed]
            }]
        },
        options: { plugins: { legend: { position: "bottom" } } }
    });

    // Distribución por destino de crédito (gráfico de barras)
    var destino = metricas.distribucionDestino || {};
    new Chart(document.getElementById("chartDestino"), {
        type: "bar",
        data: {
            labels: Object.keys(destino),
            datasets: [{
                label: "Solicitudes",
                data: Object.values(destino),
                backgroundColor: paletteBlue
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
        }
    });
})();
