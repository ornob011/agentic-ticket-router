let pageReady = false;

// Cache document
const $doc = $(document);

// 1) When DOM is ready, decide whether to show/hide the loader
$(function () {
    pageReady = true;

    const $loader = $("#se-pre-con");
    if (!$loader.length) return; // no loader on this page

    if ($.active > 0) {
        $loader.show();
    } else {
        $loader.fadeOut("slow");
    }
});

// 2) Show loader when any global AJAX starts (only if page is ready)
$doc.on("ajaxStart", function () {
    if (pageReady) {
        $("#se-pre-con").show();
    }
});

// 3) Hide loader when all global AJAX have stopped (only if page is ready)
$doc.on("ajaxStop", function () {
    if (pageReady) {
        $("#se-pre-con").fadeOut("slow");
    }
});
