(function () {
    function initialiseSqlEditor(element, form) {
        CodeMirror.fromTextArea(element, {
            mode: "sql",
            extraKeys: {
                'Ctrl-Enter': function () {
                    htmx.trigger(form, "submit");
                },
                'Cmd-Enter': function () {
                    htmx.trigger(form, "submit");
                }
            }
        });
    }

    window.Sakila = {"initialiseSqlEditor": initialiseSqlEditor}
})();
