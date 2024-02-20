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

    function copySliderValueToDateInput(slider, datetimeLocal) {
        const val = Number.parseInt(slider.value);
        const date = new Date(val);
        const formattedDate = date.toISOString().substring(0, 16);
        if(formattedDate !== datetimeLocal.value) {
            datetimeLocal.value = formattedDate;
        }
    }

    function copyDateInputToSlider(datetimeLocal, slider) {
        const val = datetimeLocal.value;
        const date = new Date(val);
        const epochMillis = date.getTime()
        if(epochMillis.toString() !== slider.value?.toString()) {
            slider.value = epochMillis.toString()
        }
    }

    window.Sakila = {
        "initialiseSqlEditor": initialiseSqlEditor,
        "copySliderValueToDateInput": copySliderValueToDateInput,
        "copyDateInputToSlider": copyDateInputToSlider
    }
})();
