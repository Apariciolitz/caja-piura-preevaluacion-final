// Interacciones de interfaz: aparición al hacer scroll, contadores y barra de navegación.
(function () {
    "use strict";
    document.documentElement.classList.add("js");

    function ready(fn) {
        if (document.readyState !== "loading") { fn(); } else { document.addEventListener("DOMContentLoaded", fn); }
    }

    ready(function () {
        var reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

        // Aparición progresiva de bloques
        var items = document.querySelectorAll(".reveal, .reveal-left, .reveal-right");
        if ("IntersectionObserver" in window && !reduce) {
            var io = new IntersectionObserver(function (entries) {
                entries.forEach(function (e) {
                    if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
                });
            }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });
            items.forEach(function (el) { io.observe(el); });
        } else {
            items.forEach(function (el) { el.classList.add("in"); });
        }

        // Contadores animados: <span data-count="140" data-suffix="+">
        function animateCount(el) {
            var target = parseFloat(el.getAttribute("data-count"));
            var suffix = el.getAttribute("data-suffix") || "";
            var prefix = el.getAttribute("data-prefix") || "";
            var dur = 1600, start = null;
            if (reduce) { el.textContent = prefix + target.toLocaleString("es-PE") + suffix; return; }
            function step(ts) {
                if (!start) { start = ts; }
                var p = Math.min((ts - start) / dur, 1);
                var eased = 1 - Math.pow(1 - p, 3);
                el.textContent = prefix + Math.round(target * eased).toLocaleString("es-PE") + suffix;
                if (p < 1) { requestAnimationFrame(step); }
            }
            requestAnimationFrame(step);
        }
        var counters = document.querySelectorAll("[data-count]");
        if ("IntersectionObserver" in window) {
            var co = new IntersectionObserver(function (entries) {
                entries.forEach(function (e) {
                    if (e.isIntersecting) { animateCount(e.target); co.unobserve(e.target); }
                });
            }, { threshold: 0.6 });
            counters.forEach(function (el) { co.observe(el); });
        } else {
            counters.forEach(animateCount);
        }

        // Sombra de la barra de navegación al desplazarse
        var nav = document.querySelector(".navbar");
        if (nav) {
            var onScroll = function () { nav.classList.toggle("scrolled", window.scrollY > 12); };
            window.addEventListener("scroll", onScroll, { passive: true });
            onScroll();
        }
    });
})();
