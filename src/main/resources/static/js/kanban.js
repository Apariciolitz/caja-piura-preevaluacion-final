// Tablero Kanban: arrastrar y soltar para cambiar el estado de una solicitud.
(function () {
    "use strict";

    var csrfToken = null;
    var csrfHeader = null;
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
        csrfToken = tokenMeta.getAttribute("content");
        csrfHeader = headerMeta.getAttribute("content");
    }

    var draggedCard = null;

    document.querySelectorAll(".kanban-card").forEach(function (card) {
        card.addEventListener("dragstart", function () {
            draggedCard = card;
            card.classList.add("dragging");
        });
        card.addEventListener("dragend", function () {
            card.classList.remove("dragging");
            draggedCard = null;
        });
    });

    document.querySelectorAll(".kanban-column").forEach(function (zone) {
        zone.addEventListener("dragover", function (e) {
            e.preventDefault();
            zone.classList.add("drag-over");
        });
        zone.addEventListener("dragleave", function () {
            zone.classList.remove("drag-over");
        });
        zone.addEventListener("drop", function (e) {
            e.preventDefault();
            zone.classList.remove("drag-over");
            if (!draggedCard) {
                return;
            }

            var id = draggedCard.getAttribute("data-id");
            var nuevoEstado = zone.getAttribute("data-estado");
            var origenZone = draggedCard.parentElement;

            zone.appendChild(draggedCard);

            var headers = { "Content-Type": "application/x-www-form-urlencoded" };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            fetch("/asesor/" + id + "/estado-ajax", {
                method: "POST",
                headers: headers,
                body: "estado=" + encodeURIComponent(nuevoEstado)
            }).then(function (response) {
                if (!response.ok) {
                    throw new Error("No se pudo actualizar el estado");
                }
            }).catch(function (err) {
                console.error(err);
                origenZone.appendChild(draggedCard);
                alert("No se pudo mover la tarjeta. Intenta de nuevo.");
            });
        });
    });
})();
